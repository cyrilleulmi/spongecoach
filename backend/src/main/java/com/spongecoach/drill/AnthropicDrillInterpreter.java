package com.spongecoach.drill;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.errors.AnthropicException;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Asks Claude to read the sketches and answer with an {@link Interpretation} (ADR-0019).
 *
 * <p>The request is append-only: the system prompt, then the first coach turn with every photo,
 * then the conversation. Both the system prompt and the photos carry a cache breakpoint, so a later
 * turn re-reads the photos from the cache instead of paying for them again.
 *
 * <p>The answer's JSON schema is part of the system prompt rather than a structured-output format:
 * the API rejects it as too large a grammar to enforce. So the answer is parsed leniently, an
 * answer that isn't JSON at all gets one retry here, and the validator judges the rest.
 *
 * <p>Plain Java, not a bean, so {@code DrillExamplesIT} can build one directly; {@link
 * DrillInterpreterProducer} makes it the application's interpreter.
 */
public class AnthropicDrillInterpreter implements DrillInterpreter {

    private static final String SYSTEM_PROMPT = DrillJson.resource("drill/system-prompt.md")
            + "\n\n# Answer format\n\n"
            + "Answer with exactly one JSON object that follows this JSON schema, and nothing else: "
            + "no text before or after it, no code fence.\n\n"
            + DrillJson.resource("drill/interpretation.schema.json");

    /** Room for adaptive thinking plus a large script with readings; streaming keeps it clear of HTTP timeouts. */
    private static final long MAX_TOKENS = 64_000;

    private final AnthropicClient client;
    private final String model;

    public AnthropicDrillInterpreter(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public Interpretation interpret(Input input) {
        String text = answer(input);
        try {
            return parse(text);
        } catch (JsonProcessingException notJson) {
            List<Turn> turns = new ArrayList<>(input.turns());
            turns.add(new Turn(false, text));
            turns.add(new Turn(true, "Das war kein gültiges JSON nach dem Schema ("
                    + notJson.getOriginalMessage() + "). Antworte nur mit dem JSON-Objekt."));
            String retried = answer(new Input(input.drillName(), input.sketches(), input.tags(), input.relation(), turns));
            try {
                return parse(retried);
            } catch (JsonProcessingException stillNotJson) {
                throw new DrillUnavailableException("Claudes Antwort war kein gültiges Skript.", stillNotJson);
            }
        }
    }

    /** One request; the text of the answer. */
    private String answer(Input input) {
        Message message;
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(request(input))) {
            MessageAccumulator accumulator = MessageAccumulator.create();
            stream.stream().forEach(accumulator::accumulate);
            message = accumulator.message();
        } catch (AnthropicException e) {
            throw new DrillUnavailableException("Claude ist nicht erreichbar: " + e.getMessage(), e);
        }
        StopReason stopReason = message.stopReason().orElse(null);
        if (StopReason.REFUSAL.equals(stopReason)) {
            throw new DrillUnavailableException("Claude hat die Anfrage abgelehnt.");
        }
        if (StopReason.MAX_TOKENS.equals(stopReason)) {
            throw new DrillUnavailableException("Claudes Antwort war zu lang und wurde abgeschnitten.");
        }
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());
    }

    /** The JSON object in the answer, tolerating a stray code fence or sentence around it. */
    private static Interpretation parse(String text) throws JsonProcessingException {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        String json = start >= 0 && end > start ? text.substring(start, end + 1) : text;
        return DrillJson.readLenient(json, Interpretation.class);
    }

    private MessageCreateParams request(Input input) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.HIGH).build())
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(SYSTEM_PROMPT)
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                // A declined request is re-run server-side on the recommended fallback model.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));

        builder.addMessage(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(firstTurn(input))
                .build());
        for (Turn turn : mergeConsecutive(input.turns())) {
            builder.addMessage(MessageParam.builder()
                    .role(turn.fromCoach() ? MessageParam.Role.USER : MessageParam.Role.ASSISTANT)
                    .content(turn.text())
                    .build());
        }
        return builder.build();
    }

    /** The sketches with their notes, then what the coach said on upload. The last photo ends the cached prefix. */
    private static List<ContentBlockParam> firstTurn(Input input) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        for (int i = 0; i < input.sketches().size(); i++) {
            Sketch sketch = input.sketches().get(i);
            String caption = "Foto " + sketch.position()
                    + (sketch.note() == null || sketch.note().isBlank() ? "" : " — Notiz: " + sketch.note());
            blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(caption).build()));
            ImageBlockParam.Builder image = ImageBlockParam.builder().source(Base64ImageSource.builder()
                    .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                    .data(Base64.getEncoder().encodeToString(sketch.jpeg()))
                    .build());
            if (i == input.sketches().size() - 1) {
                image.cacheControl(CacheControlEphemeral.builder().build());
            }
            blocks.add(ContentBlockParam.ofImage(image.build()));
        }
        String tags = input.tags().isEmpty() ? "keine" : String.join(", ", input.tags());
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder()
                .text("Übung: " + input.drillName() + "\n"
                        + "Tags: " + tags + "\n"
                        + "Beziehung der Fotos: " + relation(input) + "\n\n"
                        + "Lies die Fotos und erstelle das Skript, oder stelle Rückfragen.")
                .build()));
        return blocks;
    }

    private static String relation(Input input) {
        if (input.sketches().size() == 1) {
            return "nur ein Foto";
        }
        return switch (input.relation()) {
            case PROGRESSION -> "Steigerung (jedes Foto eine eigene Stufe)";
            case CONTINUOUS -> "Fortlaufend (die Fotos sind Phasen desselben Ablaufs)";
            case MIXED -> "gemischt";
        };
    }

    /** The API wants alternating roles; a coach message after a failed job would make two in a row. */
    private static List<Turn> mergeConsecutive(List<Turn> turns) {
        List<Turn> merged = new ArrayList<>();
        for (Turn turn : turns) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).fromCoach() == turn.fromCoach()) {
                Turn previous = merged.remove(merged.size() - 1);
                merged.add(new Turn(turn.fromCoach(), previous.text() + "\n\n" + turn.text()));
            } else {
                merged.add(turn);
            }
        }
        return merged;
    }
}

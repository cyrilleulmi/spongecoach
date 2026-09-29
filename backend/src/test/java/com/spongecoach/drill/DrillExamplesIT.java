package com.spongecoach.drill;

import com.spongecoach.domain.SketchRelation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interprets every example in {@code docs/drills/examples} with the real Claude API, the way the
 * app does (ADR-0019): photos downscaled like the browser does, the tags and relation the coach
 * would pick, the scripted coach answers when it asks, one retry for an unplayable script. Writes a
 * report per example to {@code build/drill-examples} to compare with its {@code expected.md}.
 *
 * <p>Costs money, so it only runs through {@code gradlew drillExamples} (one example:
 * {@code -Pexample=bresil}), never in {@code test}. Needs {@code ANTHROPIC_API_KEY}, from the
 * environment or {@code backend/.env}.
 */
@Tag("drill-examples")
class DrillExamplesIT {

    private static final Path EXAMPLES = Path.of("../docs/drills/examples");
    private static final Path REPORTS = Path.of("build/drill-examples");
    private static final int MAX_ROUNDS = 3;

    @Test
    void interpretsTheExamples() throws Exception {
        DrillInterpreter interpreter = new AnthropicDrillInterpreter(apiKey(), "claude-opus-5");
        Files.createDirectories(REPORTS);
        String only = System.getProperty("drill.example");
        List<Path> examples;
        try (Stream<Path> dirs = Files.list(EXAMPLES)) {
            examples = dirs.filter(Files::isDirectory)
                    .filter(dir -> only == null || dir.getFileName().toString().equals(only))
                    .sorted()
                    .toList();
        }
        assertTrue(!examples.isEmpty(), "no examples found");

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<String>> results = new ArrayList<>();
        for (Path example : examples) {
            results.add(pool.submit(() -> run(interpreter, example)));
        }
        List<String> failures = new ArrayList<>();
        for (Future<String> result : results) {
            String outcome = result.get();
            System.out.println(outcome);
            if (!outcome.contains("READY")) {
                failures.add(outcome);
            }
        }
        pool.shutdown();
        assertTrue(failures.isEmpty(), "examples that did not end READY: " + failures);
    }

    private static String run(DrillInterpreter interpreter, Path example) throws IOException {
        String name = example.getFileName().toString();
        String expected = Files.readString(example.resolve("expected.md"));
        List<DrillInterpreter.Sketch> sketches = sketches(example);
        for (DrillInterpreter.Sketch sketch : sketches) {
            Files.write(REPORTS.resolve(name + "-" + sketch.position() + ".jpg"), sketch.jpeg());
        }
        List<String> tags = DrillExampleSeeder.tags(expected);
        SketchRelation relation = DrillExampleSeeder.relation(expected);
        String coachAnswers = section(expected, "Coach answers");

        StringBuilder report = new StringBuilder("# " + name + "\n\n")
                .append("Tags: ").append(tags).append(" · Relation: ").append(relation)
                .append(" · Photos: ").append(sketches.size()).append("\n\n");
        List<DrillInterpreter.Turn> turns = new ArrayList<>();
        Interpretation answer = null;
        String outcome;
        try {
            for (int round = 1; round <= MAX_ROUNDS; round++) {
                answer = interpreter.interpret(new DrillInterpreter.Input(name, sketches, tags, relation, turns));
                report.append("## Round ").append(round).append(": ").append(answer.status()).append("\n\n")
                        .append("> ").append(answer.message()).append("\n\n");
                if (round == 1) {
                    appendReadings(report, answer);
                }
                if (answer.status() == Interpretation.Status.READY) {
                    List<String> errors = DrillScriptValidator.validate(answer.script(), sketches.size());
                    if (errors.isEmpty()) {
                        break;
                    }
                    report.append("Unplayable: ").append(errors).append("\n\n");
                    turns.add(new DrillInterpreter.Turn(false, DrillJson.write(answer)));
                    turns.add(new DrillInterpreter.Turn(true, "Das Skript ist so nicht abspielbar:\n- "
                            + String.join("\n- ", errors) + "\n\nBitte korrigiere diese Punkte."));
                    continue;
                }
                for (Interpretation.Question question : answer.questions()) {
                    report.append("- **").append(question.id()).append("** ").append(question.text())
                            .append(question.options().isEmpty() ? "" : " " + question.options()).append("\n");
                }
                String reply = coachAnswers.isBlank()
                        ? "Rate einfach: entscheide alle noch offenen Punkte selbst."
                        : coachAnswers + "\n\nWas damit nicht beantwortet ist, entscheide selbst.";
                report.append("\nCoach: ").append(reply.replace("\n", " ")).append("\n\n");
                turns.add(new DrillInterpreter.Turn(false, DrillJson.write(answer)));
                turns.add(new DrillInterpreter.Turn(true, reply));
            }
            boolean playable = answer.status() == Interpretation.Status.READY
                    && DrillScriptValidator.validate(answer.script(), sketches.size()).isEmpty();
            outcome = name + ": " + (playable ? "READY" : "NOT READY (" + answer.status() + ")");
            appendScript(report, answer.script());
        } catch (DrillUnavailableException e) {
            outcome = name + ": FAILED " + e.getMessage();
            report.append("FAILED: ").append(e.getMessage()).append("\n");
        }
        Files.writeString(REPORTS.resolve(name + ".md"), report);
        if (answer != null) {
            Files.writeString(REPORTS.resolve(name + ".json"), DrillJson.write(answer));
        }
        return outcome;
    }

    private static void appendReadings(StringBuilder report, Interpretation answer) {
        for (Interpretation.Reading reading : answer.readings()) {
            report.append("Photo ").append(reading.sketch()).append(" (").append(reading.boardSide()).append(", ")
                    .append(reading.orientation()).append("): ");
            List<String> symbols = new ArrayList<>();
            for (Interpretation.Symbol symbol : reading.symbols()) {
                symbols.add(symbol.kind() + (symbol.label().isBlank() ? "" : " '" + symbol.label() + "'")
                        + " " + symbol.color() + " → " + symbol.meaning()
                        + String.format(" (%.1f)", symbol.confidence()));
            }
            report.append(String.join("; ", symbols)).append("\n\n");
        }
    }

    private static void appendScript(StringBuilder report, DrillScript script) {
        report.append("## Script\n\n");
        for (DrillScript.Stage stage : script.stages()) {
            report.append("### ").append(stage.id()).append(" ").append(stage.name())
                    .append(" (").append(stage.area()).append(", photos ").append(stage.sketches())
                    .append(", ").append(stage.repetition().mode())
                    .append(stage.repetition().mirrored() ? " mirrored" : "").append(")\n\n");
            for (DrillScript.Actor actor : stage.actors()) {
                report.append(String.format("- actor %s %s %s side %s at (%.1f, %.1f)%s%n", actor.id(), actor.label(),
                        actor.kind(), actor.side(), actor.start().x(), actor.start().y(), actor.hasBall() ? " with ball" : ""));
            }
            for (DrillScript.Part part : stage.parts()) {
                report.append("- part ").append(part.id()).append(" ").append(part.name()).append(" = ").append(part.actorId())
                        .append(part.nextPartId().isBlank() ? "" : " → next " + part.nextPartId()).append("\n");
            }
            for (DrillScript.Prop prop : stage.props()) {
                report.append(String.format("- prop %s at (%.1f, %.1f)%n", prop.kind(), prop.position().x(), prop.position().y()));
            }
            for (DrillScript.Step step : stage.steps()) {
                DrillScript.Point end = step.path().isEmpty() ? null : step.path().get(step.path().size() - 1);
                report.append("- step ").append(step.id()).append(" [").append(step.label()).append("] ")
                        .append(step.partId()).append(" ").append(step.type())
                        .append(step.targetPartId().isBlank() ? "" : " → " + step.targetPartId())
                        .append(end == null ? "" : String.format(" to (%.1f, %.1f)", end.x(), end.y()))
                        .append(step.after().isBlank() ? " at start" : " after " + step.after() + " " + step.afterEdge())
                        .append(step.delay() > 0 ? " +" + step.delay() + "s" : "")
                        .append(" photo ").append(step.sketch()).append("\n");
            }
            report.append("\n");
        }
        report.append("Assumptions:\n");
        for (String assumption : script.assumptions()) {
            report.append("- ").append(assumption).append("\n");
        }
    }

    /** The photos, downscaled the way the browser does before upload. */
    private static List<DrillInterpreter.Sketch> sketches(Path example) throws IOException {
        List<Path> photos;
        try (Stream<Path> files = Files.list(example)) {
            photos = files.filter(file -> file.toString().endsWith(".jpg")).sorted().toList();
        }
        List<DrillInterpreter.Sketch> sketches = new ArrayList<>();
        for (int i = 0; i < photos.size(); i++) {
            sketches.add(new DrillInterpreter.Sketch(i + 1, SketchImages.uprightJpeg(Files.readAllBytes(photos.get(i))), null));
        }
        return sketches;
    }

    /** The body of a {@code ## heading} section, or empty. */
    private static String section(String markdown, String heading) {
        Matcher matcher = Pattern.compile("(?ms)^## " + Pattern.quote(heading) + "\\s*\\n(.*?)(?=^## |\\z)").matcher(markdown);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String apiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        try {
            Path env = Path.of(".env");
            if (Files.exists(env)) {
                for (String line : Files.readAllLines(env)) {
                    if (line.startsWith("ANTHROPIC_API_KEY=")) {
                        return line.substring("ANTHROPIC_API_KEY=".length()).trim();
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new IllegalStateException("ANTHROPIC_API_KEY is not set, neither in the environment nor in backend/.env");
    }
}

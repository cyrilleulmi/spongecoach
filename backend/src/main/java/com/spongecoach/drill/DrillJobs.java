package com.spongecoach.drill;

import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillMessage;
import com.spongecoach.domain.DrillMessageAuthor;
import com.spongecoach.domain.DrillScriptSource;
import com.spongecoach.domain.DrillScriptVersion;
import com.spongecoach.domain.DrillStatus;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Runs the interpreter off the request thread (ADR-0019). A request marks its Drill PENDING and
 * calls {@link #startAfterCommit}; the job reads the Drill and its conversation, asks the
 * interpreter, checks the script, and stores the answer as NEEDS_INPUT, READY or FAILED.
 *
 * <p>Jobs live in memory only. A Drill still PENDING at startup belonged to a job that died with
 * the last process, so it is marked FAILED rather than left to spin forever.
 */
@ApplicationScoped
public class DrillJobs {

    static final String RESTARTED = "Abgebrochen, weil der Server neu gestartet wurde. Bitte erneut versuchen.";

    @Inject
    DrillInterpreter interpreter;

    @Inject
    TransactionSynchronizationRegistry transactions;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    void onStart(@Observes StartupEvent event) {
        QuarkusTransaction.requiringNew().run(() -> {
            for (Drill drill : Drill.listPending()) {
                fail(drill, RESTARTED);
            }
        });
    }

    void onStop(@Observes ShutdownEvent event) {
        executor.shutdownNow();
    }

    /**
     * Runs the interpreter for this Drill once the caller's transaction has committed, so the job
     * sees the PENDING status and any message the request just added.
     */
    public void startAfterCommit(UUID drillId) {
        transactions.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {
                if (status == Status.STATUS_COMMITTED) {
                    executor.submit(() -> run(drillId));
                }
            }
        });
    }

    void run(UUID drillId) {
        try {
            DrillInterpreter.Input input = QuarkusTransaction.requiringNew().call(() -> input(drillId));
            Set<Integer> sketchPositions =
                    input.sketches().stream().map(DrillInterpreter.Sketch::position).collect(Collectors.toSet());
            Interpretation answer = interpreter.interpret(input);
            List<String> errors = check(answer, sketchPositions);
            if (!errors.isEmpty()) {
                // One retry, appended to the conversation so the interpreter sees its own answer
                // and exactly what was wrong with it.
                List<DrillInterpreter.Turn> turns = new ArrayList<>(input.turns());
                turns.add(new DrillInterpreter.Turn(false, DrillJson.write(answer)));
                turns.add(new DrillInterpreter.Turn(true, retryRequest(errors)));
                answer = interpreter.interpret(new DrillInterpreter.Input(
                        input.drillName(), input.sketches(), input.tags(), input.relation(), turns));
                errors = check(answer, sketchPositions);
            }
            if (!errors.isEmpty()) {
                throw new DrillUnavailableException(
                        "Claudes Skript war auch nach einer Korrektur ungültig: " + String.join("; ", errors));
            }
            Interpretation result = answer;
            QuarkusTransaction.requiringNew().run(() -> store(drillId, result));
        } catch (DrillUnavailableException e) {
            Log.warnf("drill %s: %s", drillId, e.getMessage());
            failQuietly(drillId, e.getMessage());
        } catch (RuntimeException e) {
            Log.errorf(e, "drill %s: interpreter job failed", drillId);
            failQuietly(drillId, "Unerwarteter Fehler beim Interpretieren der Skizzen.");
        }
    }

    private DrillInterpreter.Input input(UUID drillId) {
        Drill drill = Drill.findById(drillId);
        List<DrillInterpreter.Sketch> sketches = drill.activeSketches().stream()
                .map(sketch -> new DrillInterpreter.Sketch(sketch.position, sketch.image, sketch.note))
                .toList();
        List<String> tags = drill.tags.stream().map(tag -> tag.name).toList();
        return new DrillInterpreter.Input(drill.name, sketches, tags, drill.sketchRelation, turns(drill));
    }

    /**
     * The conversation after the upload. An interpreter reply is shown back as the answer it gave;
     * a coach turn after a hand edit also carries the edited script, so the interpreter builds on it.
     */
    private static List<DrillInterpreter.Turn> turns(Drill drill) {
        List<DrillInterpreter.Turn> turns = new ArrayList<>();
        Integer lastSeenVersion = null;
        for (DrillMessage message : drill.messages) {
            if (message.author == DrillMessageAuthor.INTERPRETER) {
                turns.add(new DrillInterpreter.Turn(false, replay(drill, message)));
                if (message.scriptVersion != null) {
                    lastSeenVersion = message.scriptVersion;
                }
                continue;
            }
            StringBuilder text = new StringBuilder();
            if (drill.currentVersion != null && !drill.currentVersion.equals(lastSeenVersion)
                    && message == lastCoachMessage(drill)) {
                DrillScriptVersion current = DrillScriptVersion.find(drill.id, drill.currentVersion);
                text.append("Ich habe das Skript von Hand geändert. Aktuelle Version:\n")
                        .append(current.script)
                        .append("\n\n");
                lastSeenVersion = drill.currentVersion;
            }
            text.append(coachText(message));
            turns.add(new DrillInterpreter.Turn(true, text.toString()));
        }
        return turns;
    }

    private static DrillMessage lastCoachMessage(Drill drill) {
        DrillMessage last = null;
        for (DrillMessage message : drill.messages) {
            if (message.author == DrillMessageAuthor.COACH) {
                last = message;
            }
        }
        return last;
    }

    private static String coachText(DrillMessage message) {
        if (message.answers == null) {
            return message.content;
        }
        return "Antworten auf deine Rückfragen:\n" + message.answers
                + (message.content == null || message.content.isBlank() ? "" : "\n\n" + message.content);
    }

    /** The interpreter's earlier answer, rebuilt from what was stored; deterministic, so the prefix stays cacheable. */
    private static String replay(Drill drill, DrillMessage message) {
        String script = message.scriptVersion == null
                ? "{\"stages\":[],\"assumptions\":[]}"
                : DrillScriptVersion.find(drill.id, message.scriptVersion).script;
        return "{\"status\":\"" + (message.scriptVersion == null ? "NEEDS_INPUT" : "READY") + "\""
                + ",\"message\":" + DrillJson.write(message.content == null ? "" : message.content)
                + ",\"questions\":" + (message.questions == null ? "[]" : message.questions)
                + ",\"script\":" + script + "}";
    }

    private static List<String> check(Interpretation answer, Set<Integer> sketchPositions) {
        if (answer.status() == Interpretation.Status.NEEDS_INPUT) {
            return answer.questions() == null || answer.questions().isEmpty()
                    ? List.of("NEEDS_INPUT without any question")
                    : List.of();
        }
        return DrillScriptValidator.validate(answer.script(), sketchPositions);
    }

    private static String retryRequest(List<String> errors) {
        return "Das Skript ist so nicht abspielbar:\n- " + String.join("\n- ", errors)
                + "\n\nBitte korrigiere diese Punkte und gib das vollständige Skript zurück.";
    }

    private static void store(UUID drillId, Interpretation answer) {
        Drill drill = Drill.findById(drillId);
        Instant now = Instant.now();
        for (Interpretation.Reading reading : answer.readings() == null ? List.<Interpretation.Reading>of() : answer.readings()) {
            drill.sketches.stream()
                    .filter(sketch -> sketch.position == reading.sketch())
                    .findFirst()
                    .ifPresent(sketch -> sketch.reading = DrillJson.write(reading));
        }

        DrillMessage reply = new DrillMessage();
        reply.id = UUID.randomUUID();
        reply.drill = drill;
        reply.position = drill.messages.size() + 1;
        reply.author = DrillMessageAuthor.INTERPRETER;
        reply.content = answer.message();
        reply.createdAt = now;

        if (answer.status() == Interpretation.Status.READY) {
            DrillScriptVersion version = newVersion(drill, answer.script(), DrillScriptSource.AI, answer.changeSummary());
            reply.scriptVersion = version.version;
            drill.status = DrillStatus.READY;
        } else {
            reply.questions = DrillJson.write(answer.questions());
            drill.status = DrillStatus.NEEDS_INPUT;
        }
        reply.persist();
        drill.messages.add(reply);
        drill.error = null;
        drill.updatedAt = now;
    }

    /** Appends a script version and makes it the Drill's current one. */
    public static DrillScriptVersion newVersion(Drill drill, DrillScript script, DrillScriptSource source, String changeSummary) {
        DrillScriptVersion version = new DrillScriptVersion();
        version.id = UUID.randomUUID();
        version.drill = drill;
        version.version = nextVersion(drill.id);
        version.source = source;
        version.script = DrillJson.write(script);
        version.changeSummary = changeSummary == null || changeSummary.isBlank() ? null : changeSummary;
        version.createdAt = Instant.now();
        version.persist();
        drill.currentVersion = version.version;
        return version;
    }

    private static int nextVersion(UUID drillId) {
        List<DrillScriptVersion> versions = DrillScriptVersion.listForDrill(drillId);
        return versions.isEmpty() ? 1 : versions.get(0).version + 1;
    }

    private static void failQuietly(UUID drillId, String error) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                Drill drill = Drill.findById(drillId);
                if (drill != null) {
                    fail(drill, error);
                }
            });
        } catch (RuntimeException e) {
            Log.errorf(e, "drill %s: could not record the failure", drillId);
        }
    }

    private static void fail(Drill drill, String error) {
        drill.status = DrillStatus.FAILED;
        drill.error = error;
        drill.updatedAt = Instant.now();
    }
}

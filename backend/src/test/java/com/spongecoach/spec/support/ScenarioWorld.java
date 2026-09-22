package com.spongecoach.spec.support;

import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.response.Response;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The state one scenario builds up as its steps run: what the spoken fixture names refer to, the
 * response the last {@code When} produced, and how to undo it all afterwards.
 *
 * <p>Scenarios name fixtures the way {@code CONTEXT.md} does — the Line "Kiwi", the Skill
 * "Passgenauigkeit". Those names are not unique in the database, which every scenario in the run
 * shares, so a {@code Given} creates the row under a uniquified name and registers it here against
 * the spoken one. Every later step resolves through {@link #id} or {@link #actualName}, so the
 * feature file stays readable while the rows stay isolated.
 *
 * <p>{@code @ScenarioScope} means one instance per scenario, destroyed when it finishes, shared by
 * every step class the scenario touches.
 */
@ScenarioScope
public class ScenarioWorld {

    private final Map<Kind, Map<String, UUID>> idsByName = new EnumMap<>(Kind.class);
    private final Map<Kind, Map<String, String>> actualNamesByName = new EnumMap<>(Kind.class);
    private final Map<Kind, String> mostRecent = new EnumMap<>(Kind.class);
    private final Deque<Runnable> cleanups = new ArrayDeque<>();
    private final Map<String, Object> notes = new HashMap<>();

    private Response response;

    // --- naming -----------------------------------------------------------------

    /** A collision-free variant of a spoken name, for the row actually written. */
    public String uniquify(String spokenName) {
        return spokenName + " " + UUID.randomUUID();
    }

    /** Remembers that the spoken name refers to this row, and makes it the kind's current subject. */
    public void register(Kind kind, String spokenName, UUID id, String actualName) {
        idsByName.computeIfAbsent(kind, k -> new HashMap<>()).put(spokenName, id);
        actualNamesByName.computeIfAbsent(kind, k -> new HashMap<>()).put(spokenName, actualName);
        mostRecent.put(kind, spokenName);
    }

    public UUID id(Kind kind, String spokenName) {
        UUID id = idsByName.getOrDefault(kind, Map.of()).get(spokenName);
        if (id == null) {
            throw new IllegalStateException(
                    "no " + kind + " named \"" + spokenName + "\" in this scenario — is a Given missing?");
        }
        return id;
    }

    public String actualName(Kind kind, String spokenName) {
        String name = actualNamesByName.getOrDefault(kind, Map.of()).get(spokenName);
        if (name == null) {
            throw new IllegalStateException(
                    "no " + kind + " named \"" + spokenName + "\" in this scenario — is a Given missing?");
        }
        return name;
    }

    public boolean knows(Kind kind, String spokenName) {
        return idsByName.getOrDefault(kind, Map.of()).containsKey(spokenName);
    }

    /** What "it" refers to: the last fixture of this kind the scenario named. */
    public UUID current(Kind kind) {
        String spokenName = mostRecent.get(kind);
        if (spokenName == null) {
            throw new IllegalStateException("the scenario has not named a " + kind + " yet");
        }
        return id(kind, spokenName);
    }

    public String currentName(Kind kind) {
        String spokenName = mostRecent.get(kind);
        if (spokenName == null) {
            throw new IllegalStateException("the scenario has not named a " + kind + " yet");
        }
        return actualName(kind, spokenName);
    }

    public boolean has(Kind kind) {
        return mostRecent.containsKey(kind);
    }

    // --- the last When ----------------------------------------------------------

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response response() {
        if (response == null) {
            throw new IllegalStateException("no request has been made yet — is a When missing?");
        }
        return response;
    }

    // --- scratch space ----------------------------------------------------------

    /** Somewhere for a step to leave a value the next step needs (a chosen color, a datetime). */
    public void note(String key, Object value) {
        notes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T recall(String key) {
        if (!notes.containsKey(key)) {
            throw new IllegalStateException("nothing noted under \"" + key + "\" in this scenario");
        }
        return (T) notes.get(key);
    }

    // --- teardown ---------------------------------------------------------------

    /** Registers an undo, run after the scenario in reverse order. */
    public void onCleanup(Runnable cleanup) {
        cleanups.push(cleanup);
    }

    public void runCleanups() {
        while (!cleanups.isEmpty()) {
            try {
                cleanups.pop().run();
            } catch (RuntimeException ignored) {
                // A scenario that failed part-way may have left a row the undo cannot find. Keep
                // unwinding: a cleanup that cannot run must not mask the real failure, nor stop
                // the remaining rows being removed.
            }
        }
    }
}

package com.spongecoach.drill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Reads and writes the Drill documents. Strict on purpose: an unknown field in a script is a mistake
 * (the model's or the editor's), not something to drop silently.
 */
public final class DrillJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

    /**
     * For the interpreter's answers: an extra field is dropped, a missing one left empty. The
     * validator then decides whether what's left is playable (ADR-0019).
     */
    private static final ObjectMapper LENIENT = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private DrillJson() {
    }

    public static <T> T readLenient(String json, Class<T> type) throws JsonProcessingException {
        return LENIENT.readValue(json, type);
    }

    public static <T> T read(String json, Class<T> type) throws JsonProcessingException {
        return MAPPER.readValue(json, type);
    }

    public static <T> T convert(JsonNode node, Class<T> type) throws JsonProcessingException {
        return MAPPER.treeToValue(node, type);
    }

    /** For handing stored JSON back out untouched; null stays null. */
    public static JsonNode tree(String json) {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored drill JSON is not valid JSON", e);
        }
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not write drill JSON", e);
        }
    }

    public static String resource(String path) {
        try (InputStream in = DrillJson.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + path);
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}

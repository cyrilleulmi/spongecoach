package com.spongecoach.drill;

/**
 * The interpreter could not produce an answer: no API key, the API failed or refused, or the answer
 * was unusable. Jobs turn it into a FAILED Drill whose {@code error} is this message, so write it for
 * the coach, in German.
 */
public class DrillUnavailableException extends RuntimeException {

    public DrillUnavailableException(String message) {
        super(message);
    }

    public DrillUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

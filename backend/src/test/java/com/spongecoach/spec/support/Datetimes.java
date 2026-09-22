package com.spongecoach.spec.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Translates between the datetimes scenarios write — {@code "2026-08-05 18:00"} — and the two
 * machine forms: what the API accepts, and what it echoes back (always with seconds, unlike
 * {@link LocalDateTime#toString()}).
 */
public final class Datetimes {

    private static final DateTimeFormatter SPOKEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ECHOED = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Datetimes() {
    }

    public static LocalDateTime parse(String spoken) {
        return LocalDateTime.parse(spoken, SPOKEN);
    }

    /** How the API echoes a datetime back, for an equality assertion. */
    public static String echoed(String spoken) {
        return parse(spoken).format(ECHOED);
    }

    public static String echoedFrom(LocalDateTime datetime) {
        return datetime.format(ECHOED);
    }
}

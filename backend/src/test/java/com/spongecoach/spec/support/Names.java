package com.spongecoach.spec.support;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the quoted name lists scenarios write in prose — {@code "Carmela", "Debi" and "Gina"} — so
 * one step definition covers a list of any length instead of one per arity.
 */
public final class Names {

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private Names() {
    }

    public static List<String> in(String prose) {
        Matcher matcher = QUOTED.matcher(prose);
        return matcher.results().map(result -> result.group(1)).toList();
    }
}

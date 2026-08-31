package com.udderlywet.osrsstrategist;

import java.util.*;

/** Bundled player-facing copy; conditional strategy logic remains in Java. */
final class PlayerText
{
    private static final Map<String, String> VALUES = load();

    private PlayerText() { }

    static String get(String id)
    {
        String value = VALUES.get(id);
        if (value == null) throw new IllegalArgumentException(
                "Missing bundled player text: " + id);
        return value;
    }

    private static Map<String, String> load()
    {
        Map<String, String> values = new HashMap<>();
        for (Entry entry : BundledCatalogLoader.array(
                "/content/player-text.json", Entry[].class))
            values.put(entry.id, entry.text);
        return Collections.unmodifiableMap(values);
    }

    private static final class Entry
    {
        private String id;
        private String text;
    }
}

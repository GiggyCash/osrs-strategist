package com.udderlywet.osrsstrategist;

/** Bundled player-facing copy; conditional strategy logic remains in Java. */
final class Text
{
    private static final String[] VALUES = BundledCatalogLoader.array(
            "/content/player-text.json", String[].class);

    private Text() { }

    static String get(int id)
    {
        if (id < 0 || id >= VALUES.length || VALUES[id] == null)
            throw new IllegalArgumentException(
                "Missing bundled player text: " + id);
        return VALUES[id];
    }
}

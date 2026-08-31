package compass;

import java.util.Locale;

/** Canonical local identifiers shared by catalogs, evidence, and planners. */
final class Names
{
    private Names() { }

    static String lower(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String text(String value)
    {
        return lower(value).replaceAll("\\s+", " ");
    }

    static String words(String value)
    {
        return lower(value).replace('\u2019', '\'')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    static String slug(String value)
    {
        return words(value).replace(' ', '-');
    }

    static String actionKey(String value)
    {
        return lower(value).replace('-', '_').replace(' ', '_');
    }

    static String actionText(String value)
    {
        return text(value).replace('_', ' ').replace('-', ' ');
    }
}

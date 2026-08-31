package compass;

import java.util.*;

/** Static safety and presentation allow/deny lists bundled for review. */
final class PolicyLists
{
    static final PolicyLists DATA = BundledCatalogLoader.array(
            Text.get(1908), PolicyLists[].class)[0];
    String[] one_defence_safe;
    String[] level_three_safe;
    String[] prayer_skiller_extra;
    String[] free_to_play_quests;
    String[] generic_titles;
    String[] generic_actions;
    String[] generic_locations;
    String[] unresolved_supplies;

    static Set<String> normalizedSet(String[] values)
    {
        Set<String> result = new HashSet<>();
        if (values != null)
            for (String value : values) result.add(normalize(value));
        return Collections.unmodifiableSet(result);
    }

    static List<String> list(String[] values)
    {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("\\s+", " ");
    }
}

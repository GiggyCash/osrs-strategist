package compass;

import java.time.LocalDate;
import java.util.*;

/**
 * Narrow corrections for verified live changes newer than the pinned RuneLite
 * skill-calculator data. Announced changes never enter this map.
 */
public final class CurrentLiveSkillActionOverrides
{
    private static final LocalDate VALIDATION_DATE = LocalDate.of(2026, 8, 25);
    private static final Map<String, Integer> LEVELS;
    private static final Map<String, Float> XP;
    private static final Set<String> UNSAFE_STALE_XP;

    static
    {
        Map<String, Integer> levels = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1652), VALIDATION_DATE))
            levels.put(Text.get(201), 77);
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1653), VALIDATION_DATE))
            levels.put(Text.get(202), 87);
        LEVELS = Collections.unmodifiableMap(levels);

        Map<String, Float> xp = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1654), VALIDATION_DATE))
        {
            xp.put(Text.get(1655), 112f);
            xp.put(Text.get(1656), 168f);
            xp.put(Text.get(1657), 224f);
            xp.put(Text.get(1658), 280f);
            xp.put(Text.get(1659), 369f);
            xp.put(Text.get(1660), 480f);
            xp.put(Text.get(1661), 612f);
            xp.put(Text.get(1662), 969f);
            xp.put(Text.get(1663), 1200f);
        }
        XP = Collections.unmodifiableMap(xp);

        Set<String> stale = new LinkedHashSet<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1664), VALIDATION_DATE))
        {
            stale.add(Text.get(203));
            stale.add(Text.get(204));
        }
        if (CurrentLiveContentChanges.mayAffectPlanning(
                Text.get(1665), VALIDATION_DATE))
        {
            stale.add(Text.get(1666));
            stale.add(Text.get(1667));
            stale.add(Text.get(1668));
            stale.add(Text.get(1669));
            stale.add(Text.get(1670));
        }
        UNSAFE_STALE_XP = Collections.unmodifiableSet(stale);
    }

    private CurrentLiveSkillActionOverrides() { }

    public static int level(String actionId, int upstreamLevel)
    {
        return LEVELS.getOrDefault(actionId, upstreamLevel);
    }

    public static Map<String, Integer> levelOverrides()
    {
        return LEVELS;
    }

    public static float xp(String actionId, float upstreamXp)
    {
        if (UNSAFE_STALE_XP.contains(actionId)) return 0f;
        return XP.getOrDefault(actionId, upstreamXp);
    }

    public static Map<String, Float> xpOverrides() { return XP; }
    public static Set<String> suppressedStaleXp() { return UNSAFE_STALE_XP; }
}

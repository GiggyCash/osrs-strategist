package com.udderlywet.osrsstrategist;

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
                "2026-08-12-sepulchre-floor-4", VALIDATION_DATE))
            levels.put("runelite:agility:hallowed_sepulchre_floor_4", 77);
        if (CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-sepulchre-floor-5", VALIDATION_DATE))
            levels.put("runelite:agility:hallowed_sepulchre_floor_5", 87);
        LEVELS = Collections.unmodifiableMap(levels);

        Map<String, Float> xp = new LinkedHashMap<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-19-birdhouse-xp", VALIDATION_DATE))
        {
            xp.put("runelite:hunter:regular_bird_house", 112f);
            xp.put("runelite:hunter:oak_bird_house", 168f);
            xp.put("runelite:hunter:willow_bird_house", 224f);
            xp.put("runelite:hunter:teak_bird_house", 280f);
            xp.put("runelite:hunter:maple_bird_house", 369f);
            xp.put("runelite:hunter:mahogany_bird_house", 480f);
            xp.put("runelite:hunter:yew_bird_house", 612f);
            xp.put("runelite:hunter:magic_bird_house", 969f);
            xp.put("runelite:hunter:redwood_bird_house", 1200f);
        }
        XP = Collections.unmodifiableMap(xp);

        Set<String> stale = new LinkedHashSet<>();
        if (CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-12-colossal-wyrm-courses", VALIDATION_DATE))
        {
            stale.add("runelite:agility:colossal_wyrm_basic_course");
            stale.add("runelite:agility:colossal_wyrm_advanced_course");
        }
        if (CurrentLiveContentChanges.mayAffectPlanning(
                "2026-08-19-hunter-methods", VALIDATION_DATE))
        {
            stale.add("runelite:hunter:wild_kebbit");
            stale.add("runelite:hunter:barb_tailed_kebbit");
            stale.add("runelite:hunter:prickly_kebbit");
            stale.add("runelite:hunter:sabre_toothed_kebbit");
            stale.add("runelite:hunter:pyre_fox");
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

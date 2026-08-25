package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Narrow corrections for verified live changes newer than the pinned RuneLite
 * skill-calculator data. Announced changes never enter this map.
 */
public final class CurrentLiveSkillActionOverrides
{
    private static final LocalDate VALIDATION_DATE = LocalDate.of(2026, 8, 25);
    private static final Map<String, Integer> LEVELS;

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
}

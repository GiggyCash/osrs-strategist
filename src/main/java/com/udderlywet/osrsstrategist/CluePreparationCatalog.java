package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;

/** Tier-aware baseline preparation for clue scroll sessions. */
@Singleton
public class CluePreparationCatalog
{
    private final Map<String, List<String>> byTier = new HashMap<>();

    public CluePreparationCatalog()
    {
        byTier.put("beginner", list("Spade when the step calls for it", "Basic teleports/run energy", "Step-required item/emote equipment"));
        byTier.put("easy", list("Spade", "Fast regional teleports", "Emote equipment", "Small food reserve for combat steps"));
        byTier.put("medium", list("Spade", "Broad teleport coverage", "Emote equipment", "Food", "Coordinate/navigation supplies when required"));
        byTier.put("hard", list("Spade", "Broad teleport coverage", "Combat gear", "Food", "Antipoison where route requires it", "Emote equipment/STASH checks"));
        byTier.put("elite", list("Spade", "High-coverage teleports", "Combat gear", "Food and prayer restore", "Puzzle/step requirements", "STASH checks"));
        byTier.put("master", list("Spade", "High-coverage teleports", "Strong combat setup", "Food/prayer restore", "Emote equipment/STASH checks", "Coordinate and special-step supplies"));
    }

    public List<String> preparationFor(String clueType)
    {
        if (clueType == null) return Collections.singletonList("Inspect the clue step before preparing supplies");
        String key = clueType.toLowerCase(Locale.ROOT).replace(" clue", "").trim();
        return byTier.getOrDefault(key,
                Collections.singletonList("Inspect the observed clue step before preparing supplies"));
    }

    private static List<String> list(String... values)
    {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
}

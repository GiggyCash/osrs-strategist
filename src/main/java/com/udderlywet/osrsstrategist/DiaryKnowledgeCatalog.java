package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Canonical Achievement Diary regions and reward-progression weights. */
@Singleton
public class DiaryKnowledgeCatalog
{
    private static final List<String> REGIONS = Collections.unmodifiableList(Arrays.asList(
            "ardougne",
            "desert",
            "falador",
            "fremennik",
            "kandarin",
            "karamja",
            "kourend & kebos",
            "lumbridge & draynor",
            "morytania",
            "varrock",
            "western provinces",
            "wilderness"
    ));

    public List<String> regions()
    {
        return REGIONS;
    }

    public double scoreForProgress(int complete, int total)
    {
        if (total <= 0 || complete >= total) return 0.0;
        double ratio = Math.max(0.0, Math.min(1.0, complete / (double) total));
        // Near-complete regions should become increasingly attractive because
        // finishing a diary tier/region unlocks permanent account utility.
        return 7.0 + ratio * 13.0;
    }
}

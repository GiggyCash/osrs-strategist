package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Small guaranteed-safe F2P baseline catalog.
 *
 * <p>This catalog exists for level bands where the richer curated catalog may
 * not yet have a method. It is intentionally conservative: a baseline method
 * must be genuinely usable in F2P and must expose concrete requirements instead
 * of allowing the UI to fall back to a vague "check needed" message.</p>
 */
@Singleton
public class F2pBaselineMethodCatalog
{
    private final List<CuratedTrainingMethod> runecraftMethods;

    public F2pBaselineMethodCatalog()
    {
        List<CuratedTrainingMethod> values = new ArrayList<>();
        values.add(runecraft("runecraft_f2p_air", 1, 1,
                "Craft air runes", "Use rune essence at the Air Altar. A talisman or tiara is required unless altar access has already been proven.",
                "Air talisman/tiara or verified Air Altar access"));
        values.add(runecraft("runecraft_f2p_mind", 2, 4,
                "Craft mind runes", "Use rune essence at the Mind Altar once level 2 Runecraft is reached.",
                "Mind talisman/tiara or verified Mind Altar access"));
        values.add(runecraft("runecraft_f2p_water", 5, 8,
                "Craft water runes", "Use rune essence at the Water Altar once level 5 Runecraft is reached.",
                "Water talisman/tiara or verified Water Altar access"));
        values.add(runecraft("runecraft_f2p_earth", 9, 13,
                "Craft earth runes", "Use rune essence at the Earth Altar once level 9 Runecraft is reached.",
                "Earth talisman/tiara or verified Earth Altar access"));
        values.add(runecraft("runecraft_f2p_fire", 14, 19,
                "Craft fire runes", "Use rune essence at the Fire Altar once level 14 Runecraft is reached.",
                "Fire talisman/tiara or verified Fire Altar access"));
        values.add(runecraft("runecraft_f2p_body", 20, 99,
                "Craft body runes", "Use rune essence at the Body Altar, or compare against another unlocked F2P rune when its route is more useful for the account.",
                "Body talisman/tiara or verified Body Altar access"));
        runecraftMethods = Collections.unmodifiableList(values);
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        return skill == Skill.RUNECRAFT
                ? runecraftMethods
                : Collections.emptyList();
    }

    private static CuratedTrainingMethod runecraft(
            String id,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            String altarRequirement)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                Skill.RUNECRAFT,
                minLevel,
                maxLevel,
                name,
                instructions,
                13.0,
                15.0,
                11.0,
                AttentionLevel.MODERATE,
                10,
                3,
                Arrays.asList("Rune essence", altarRequirement),
                RecommendationConfidence.CHECK_NEEDED,
                false,
                false,
                false
        );
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.VERY_LOW,
                RiskLevel.NONE,
                true,
                true,
                true,
                true,
                Collections.singletonList("f2p-baseline")
        );
        return new CuratedTrainingMethod(method, metadata);
    }
}

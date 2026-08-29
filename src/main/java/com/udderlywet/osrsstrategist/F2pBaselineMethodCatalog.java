package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Guaranteed-safe F2P baseline catalog.
 *
 * <p>The richer catalog should win whenever it has a better route. These lower
 * scored records exist so an F2P character never falls through a level-band
 * hole and gets a members method or an empty recommendation simply because a
 * specialist route has not been selected yet.</p>
 */
@Singleton
public class F2pBaselineMethodCatalog
{
    private final Map<Skill, List<CuratedTrainingMethod>> bySkill =
            new EnumMap<>(Skill.class);

    public F2pBaselineMethodCatalog()
    {
        combat();
        gathering();
        production();
        runecraft();
        utilityCombat();

        for (Map.Entry<Skill, List<CuratedTrainingMethod>> entry : bySkill.entrySet())
        {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        List<CuratedTrainingMethod> methods = bySkill.get(skill);
        return methods == null ? Collections.emptyList() : methods;
    }

    private void combat()
    {
        baseline(Skill.ATTACK, "attack_f2p_baseline", 1, 99,
                "F2P melee Attack",
                "Edgeville Monastery: fight monks with an observed F2P melee weapon on Accurate style and ask a monk to heal you when needed.",
                AttentionLevel.MODERATE);
        baseline(Skill.STRENGTH, "strength_f2p_baseline", 1, 99,
                "F2P melee Strength",
                "Edgeville Monastery: fight monks with an observed F2P melee weapon on Aggressive style and ask a monk to heal you when needed.",
                AttentionLevel.MODERATE);
        baseline(Skill.DEFENCE, "defence_f2p_baseline", 1, 99,
                "F2P defensive melee",
                "Edgeville Monastery: fight monks with an observed F2P melee weapon on Defensive style and ask a monk to heal you when needed.",
                AttentionLevel.MODERATE);
        baseline(Skill.RANGED, "ranged_f2p_baseline", 1, 99,
                "F2P bow training",
                "Edgeville Monastery: attack monks on Rapid style with the observed shortbow and matching arrows; ask a monk to heal you when needed.",
                AttentionLevel.MODERATE);
        baseline(Skill.PRAYER, "prayer_f2p_bones", 1, 99,
                "Bury F2P bones",
                "Lumbridge Castle bank: withdraw the bone type named in DO, bury the full inventory, bank, and repeat.",
                AttentionLevel.MODERATE);
    }

    private void gathering()
    {
        baseline(Skill.MINING, "mining_f2p_copper_tin", 1, 14,
                "Mine copper or tin",
                "East Lumbridge Swamp mine: bring the observed pickaxe, mine copper, drop the ore when full, and repeat until 15 Mining.",
                AttentionLevel.MODERATE);
        baseline(Skill.MINING, "mining_f2p_iron_baseline", 15, 99,
                "Mine iron ore",
                "Varrock East mine: bring the observed pickaxe, mine iron, drop the ore when full, and repeat.",
                AttentionLevel.ACTIVE);

        baseline(Skill.FISHING, "fishing_f2p_shrimps", 1, 19,
                "Net shrimps",
                "Lumbridge Swamp fishing spots beside the Fishing tutor: net shrimp, drop the catch when full, and repeat until 20 Fishing.",
                AttentionLevel.LOW);
        baseline(Skill.FISHING, "fishing_f2p_fly_baseline", 20, 99,
                "Fly-fish trout and salmon",
                "Barbarian Village river: bring a fly fishing rod and feathers, catch trout and salmon, drop the fish when full, and repeat.",
                AttentionLevel.MODERATE);

        baseline(Skill.WOODCUTTING, "woodcutting_f2p_trees", 1, 14,
                "Cut regular trees",
                "Trees west of Lumbridge Castle: bring the observed axe, cut regular logs, drop them when full, and repeat until 15 Woodcutting.",
                AttentionLevel.LOW);
        baseline(Skill.WOODCUTTING, "woodcutting_f2p_oaks", 15, 29,
                "Cut oak trees",
                "Oak trees east of Draynor Village bank: bring the observed axe, cut oaks, bank the logs, and repeat until 30 Woodcutting.",
                AttentionLevel.LOW);
        baseline(Skill.WOODCUTTING, "woodcutting_f2p_willows_baseline", 30, 99,
                "Cut willow trees",
                "Willow trees south of Draynor Village bank: bring the observed axe, cut willows, bank the logs, and repeat.",
                AttentionLevel.LOW);
    }

    private void production()
    {
        baseline(Skill.COOKING, "cooking_f2p_fish_baseline", 1, 99,
                "Cook F2P fish",
                "Al Kharid bank and range: withdraw one inventory of raw fish, cook it on the range immediately north of the bank, bank, and repeat.",
                AttentionLevel.LOW);

        baseline(Skill.FIREMAKING, "firemaking_f2p_logs", 1, 99,
                "Burn F2P logs",
                "Grand Exchange south-east corner: withdraw the log type named in DO and a tinderbox, burn east-to-west rows, bank, and repeat.",
                AttentionLevel.MODERATE);

        baseline(Skill.SMITHING, "smithing_f2p_bronze", 1, 14,
                "Smith bronze items",
                "Varrock West Bank: keep a hammer, withdraw bronze bars, use the anvils immediately south, smith the item named in DO, bank, and repeat.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_iron", 15, 29,
                "Smith iron items",
                "Varrock West Bank: keep a hammer, withdraw iron bars, use the anvils immediately south, smith the item named in DO, bank, and repeat.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_steel", 30, 47,
                "Smith steel items",
                "Varrock West Bank: keep a hammer, withdraw steel bars, use the anvils immediately south, smith the item named in DO, bank, and repeat.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_platebody_baseline", 48, 99,
                "Smith F2P platebodies",
                "Varrock West Bank: keep a hammer, withdraw five bars per trip, smith the platebody named in DO at the anvils immediately south, bank, and repeat.",
                AttentionLevel.MODERATE);

        baseline(Skill.CRAFTING, "crafting_f2p_leather_gloves", 1, 7,
                "Craft leather gloves",
                "Use a needle and thread on leather to make leather gloves until a stronger F2P Crafting action unlocks.",
                AttentionLevel.MODERATE);
        baseline(Skill.CRAFTING, "crafting_f2p_gold_amulets", 8, 22,
                "Craft gold amulets",
                "Al Kharid furnace: carry an amulet mould, smelt observed gold bars into gold amulets, bank immediately south, and repeat.",
                AttentionLevel.MODERATE);
        baseline(Skill.CRAFTING, "crafting_f2p_tiaras", 23, 99,
                "Craft silver tiaras",
                "Al Kharid furnace: carry a tiara mould, smelt observed silver bars into tiaras, bank immediately south, and repeat.",
                AttentionLevel.MODERATE);
    }

    private void runecraft()
    {
        rune("runecraft_f2p_air", 1, 1,
                "Craft air runes",
                "Air Altar southwest of Falador. Bank in Falador, run to the altar, craft, repeat.",
                "Air talisman or air tiara");
        rune("runecraft_f2p_mind", 2, 4,
                "Craft mind runes",
                "Mind Altar north of Falador between Ice Mountain and Goblin Village. Bank in Falador, run north, craft, repeat.",
                "Mind talisman or mind tiara");
        rune("runecraft_f2p_water", 5, 8,
                "Craft water runes",
                "Water Altar in Lumbridge Swamp. Bank in Draynor Village, run southeast to the altar, craft, repeat.",
                "Water talisman or water tiara");
        rune("runecraft_f2p_earth", 9, 13,
                "Craft earth runes",
                "Earth Altar northeast of Varrock, south of the Lumber Yard. Bank at Varrock East, run to the altar, craft, repeat.",
                "Earth talisman or earth tiara");
        rune("runecraft_f2p_fire", 14, 19,
                "Craft fire runes",
                "Fire Altar north of Al Kharid, west of Emir's Arena. Bank in Al Kharid, run to the altar, craft, repeat.",
                "Fire talisman or fire tiara");
        rune("runecraft_f2p_body", 20, 99,
                "Craft body runes",
                "Body Altar south of Edgeville Monastery. Bank in Edgeville, run to the altar, craft, repeat.",
                "Body talisman or body tiara");
    }

    private void utilityCombat()
    {
        // Hitpoints is deliberately absent. Compass never directly targets
        // Hitpoints because it should rise incidentally from a legal combat plan.
    }

    private void baseline(
            Skill skill,
            String id,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            AttentionLevel attention)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                skill,
                minLevel,
                maxLevel,
                name,
                instructions,
                5.0,
                6.0,
                7.0,
                attention,
                10,
                1,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED,
                false,
                false,
                false);
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.LOW,
                RiskLevel.NONE,
                true,
                true,
                true,
                true,
                Collections.singletonList("f2p-baseline"));
        bySkill.computeIfAbsent(skill, ignored -> new ArrayList<>())
                .add(new CuratedTrainingMethod(method, metadata));
    }

    private void rune(
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
                5.0,
                6.0,
                7.0,
                AttentionLevel.MODERATE,
                10,
                3,
                java.util.Arrays.asList("Rune or pure essence", altarRequirement),
                RecommendationConfidence.CHECK_NEEDED,
                false,
                false,
                false);
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                TrainingIntensity.BALANCED,
                MethodCostTier.VERY_LOW,
                RiskLevel.NONE,
                true,
                true,
                true,
                true,
                Collections.singletonList("f2p-baseline"));
        bySkill.computeIfAbsent(Skill.RUNECRAFT, ignored -> new ArrayList<>())
                .add(new CuratedTrainingMethod(method, metadata));
    }
}

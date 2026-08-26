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
                "Use the best legal F2P melee weapon you own on a nearby low-risk monster and select an Attack-training style.",
                AttentionLevel.MODERATE);
        baseline(Skill.STRENGTH, "strength_f2p_baseline", 1, 99,
                "F2P melee Strength",
                "Use the best legal F2P melee weapon you own on a nearby low-risk monster and select a Strength-training style.",
                AttentionLevel.MODERATE);
        baseline(Skill.DEFENCE, "defence_f2p_baseline", 1, 99,
                "F2P defensive melee",
                "Use the best legal F2P melee weapon you own and a defensive training style on a low-risk monster that respects the account build.",
                AttentionLevel.MODERATE);
        baseline(Skill.RANGED, "ranged_f2p_baseline", 1, 99,
                "F2P bow training",
                "Use the best F2P shortbow and arrows your Ranged level permits on a safe low-defence target.",
                AttentionLevel.MODERATE);
        baseline(Skill.MAGIC, "magic_f2p_baseline", 1, 99,
                "F2P spell training",
                "Use the best sensible F2P combat or utility spell that preserves the account build and fits the available rune supply.",
                AttentionLevel.MODERATE);
        baseline(Skill.PRAYER, "prayer_f2p_bones", 1, 99,
                "Bury F2P bones",
                "Bury the best practical F2P bones you can obtain without breaking the account's combat restrictions.",
                AttentionLevel.MODERATE);
    }

    private void gathering()
    {
        baseline(Skill.MINING, "mining_f2p_copper_tin", 1, 14,
                "Mine copper or tin",
                "Bring any usable pickaxe and mine copper or tin at a reachable F2P mine until iron becomes available at 15 Mining.",
                AttentionLevel.MODERATE);
        baseline(Skill.MINING, "mining_f2p_iron_baseline", 15, 99,
                "Mine iron ore",
                "Bring your best usable F2P pickaxe and mine iron at a convenient three-rock or banked location.",
                AttentionLevel.ACTIVE);

        baseline(Skill.FISHING, "fishing_f2p_shrimps", 1, 19,
                "Net shrimps",
                "Use a small fishing net at a reachable F2P net spot. Move to fly fishing from 20 Fishing when that route fits the session.",
                AttentionLevel.LOW);
        baseline(Skill.FISHING, "fishing_f2p_fly_baseline", 20, 99,
                "Fly-fish trout and salmon",
                "Bring a fly fishing rod and feathers and fish trout and salmon at a convenient F2P river spot.",
                AttentionLevel.MODERATE);

        baseline(Skill.WOODCUTTING, "woodcutting_f2p_trees", 1, 14,
                "Cut regular trees",
                "Bring any usable axe and cut regular trees until oak trees unlock at 15 Woodcutting.",
                AttentionLevel.LOW);
        baseline(Skill.WOODCUTTING, "woodcutting_f2p_oaks", 15, 29,
                "Cut oak trees",
                "Bring your best usable F2P axe and cut oak trees until willows unlock at 30 Woodcutting.",
                AttentionLevel.LOW);
        baseline(Skill.WOODCUTTING, "woodcutting_f2p_willows_baseline", 30, 99,
                "Cut willow trees",
                "Bring your best usable F2P axe and cut willows at a convenient banked or drop-training location.",
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
                "Use a tinderbox to burn the best sensible F2P log tier your level and supply support.",
                AttentionLevel.MODERATE);

        baseline(Skill.SMITHING, "smithing_f2p_bronze", 1, 14,
                "Smith bronze items",
                "Use bronze bars on a F2P anvil and make the highest practical bronze item unlocked at your level.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_iron", 15, 29,
                "Smith iron items",
                "Use iron bars on a F2P anvil and make the highest practical iron item unlocked at your level.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_steel", 30, 47,
                "Smith steel items",
                "Use steel bars on a F2P anvil and make the highest practical steel item unlocked at your level.",
                AttentionLevel.MODERATE);
        baseline(Skill.SMITHING, "smithing_f2p_platebody_baseline", 48, 99,
                "Smith F2P platebodies",
                "Smith the best practical F2P platebody tier supported by your Smithing level and bar supply.",
                AttentionLevel.MODERATE);

        baseline(Skill.CRAFTING, "crafting_f2p_leather_gloves", 1, 7,
                "Craft leather gloves",
                "Use a needle and thread on leather to make leather gloves until a stronger F2P Crafting action unlocks.",
                AttentionLevel.MODERATE);
        baseline(Skill.CRAFTING, "crafting_f2p_gold_amulets", 8, 22,
                "Craft gold amulets",
                "Smelt gold bars with an amulet mould at a F2P furnace when gold supply is practical.",
                AttentionLevel.MODERATE);
        baseline(Skill.CRAFTING, "crafting_f2p_tiaras", 23, 99,
                "Craft silver tiaras",
                "Use silver bars with a tiara mould at a F2P furnace for a simple repeatable Crafting route.",
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

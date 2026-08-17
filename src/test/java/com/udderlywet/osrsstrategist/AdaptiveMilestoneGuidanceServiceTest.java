package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdaptiveMilestoneGuidanceServiceTest
{
    @Test
    public void highAlchemyCountsEveryModeledRuneAndMainShortfall()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(0, MembershipStatus.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Arrays.asList(
                        item(561, "Nature rune", 10),
                        item(554, "Fire rune", 50)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();

        RecommendationGuidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("High Level Alchemy"));
        assertTrue(guidance.getSupplies().contains("Nature rune"));
        assertTrue(guidance.getSupplies().contains("Fire rune"));
        assertTrue(guidance.getSupplies().contains("Grand Exchange"));
    }

    @Test
    public void fireStaffRemovesFireRuneShortfallFromHighAlchemy()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(0, MembershipStatus.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Arrays.asList(
                        item(561, "Nature rune", 5000),
                        item(1387, "Staff of fire", 1)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();

        RecommendationGuidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("0 fire runes"));
        assertFalse(guidance.getSupplies().contains("buy 5 fire"));
    }

    @Test
    public void uimIgnoresNormalBankWhenCalculatingShortfall()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(2, MembershipStatus.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Arrays.asList(
                        item(561, "Nature rune", 100000),
                        item(554, "Fire rune", 100000)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();

        RecommendationGuidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Normal bank state is ignored for UIM"));
        assertTrue(guidance.getSupplies().contains("Acquire"));
    }

    @Test
    public void gimCountsObservedGroupStorageWhenEnabled()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.HERBLORE, "prayer_potion", "Prayer potion", 38, 87.5f));
        AccountSnapshot account = account(4, MembershipStatus.P2P,
                Skill.HERBLORE, 38, Experience.getXpForLevel(38));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .groupStorage(new GroupStorageSnapshot(true, Arrays.asList(
                        item(257, "Ranarr weed", 100),
                        item(231, "Snape grass", 100),
                        item(227, "Vial of water", 100))))
                .build();

        RecommendationGuidance guidance = service.build(
                data, Skill.HERBLORE, 38, 40,
                plan("herblore_prayer_potions", Skill.HERBLORE), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Verified:"));
        assertTrue(guidance.getSupplies().contains("Ranarr weed"));
        assertTrue(guidance.getSupplies().contains("Snape grass"));
        assertTrue(guidance.getSupplies().contains("Vial of water"));
    }

    @Test
    public void fullAnglerOutfitReducesExactFishingActionCount()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.FISHING, "salmon", "Salmon", 30, 70));
        int currentXp = Experience.getXpForLevel(70);
        int targetXp = Experience.getXpForLevel(80);
        int xpNeeded = targetXp - currentXp;
        int expectedWithOutfit = (int) Math.ceil(xpNeeded / (70.0 * 1.025));

        AccountSnapshot account = account(0, MembershipStatus.P2P,
                Skill.FISHING, 70, currentXp);
        EquipmentSnapshot equipment = new EquipmentSnapshot(Arrays.asList(
                item(1, "Angler hat", 1),
                item(2, "Angler top", 1),
                item(3, "Angler waders", 1),
                item(4, "Angler boots", 1)));
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .equipment(equipment)
                .build();

        RecommendationGuidance guidance = service.build(
                data, Skill.FISHING, 70, 80,
                plan("fishing_f2p_fly", Skill.FISHING), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains(
                "about " + expectedWithOutfit + " fish caught"));
        assertTrue(guidance.getNote().contains("full Angler/Spirit Angler outfit"));
    }

    private static AdaptiveMilestoneGuidanceService serviceWith(
            RuneLiteSkillActionDefinition... definitions)
    {
        final List<RuneLiteSkillActionDefinition> actions = Arrays.asList(definitions);
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override
            public List<RuneLiteSkillActionDefinition> actionsFor(Skill skill)
            {
                java.util.ArrayList<RuneLiteSkillActionDefinition> result =
                        new java.util.ArrayList<>();
                for (RuneLiteSkillActionDefinition action : actions)
                {
                    if (action.getSkill() == skill) result.add(action);
                }
                return result;
            }
        };
        return new AdaptiveMilestoneGuidanceService(
                catalog,
                new MethodExecutionProfileCatalog(),
                new SkillingXpModifierService());
    }

    private static RuneLiteSkillActionDefinition action(
            Skill skill, String id, String name, int level, float xp)
    {
        return new RuneLiteSkillActionDefinition(
                skill,
                "runelite:" + skill.name().toLowerCase() + ":" + id,
                name,
                level,
                xp,
                null,
                MembershipStatus.P2P,
                -1);
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id, skill, 1, 99, id,
                "Use the modeled method.",
                10, 10, 10, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new TrainingPlan(method, "test",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList());
    }

    private static AccountSnapshot account(
            int typeCode,
            MembershipStatus membership,
            Skill trainedSkill,
            int level,
            int trainedXp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, Experience.getXpForLevel(99));
        }
        levels.put(trainedSkill, level);
        xp.put(trainedSkill, trainedXp);
        return new AccountSnapshot(
                "Guidance Test",
                typeCode,
                typeCode == 0 ? "Main" : "Restricted",
                membership,
                1,
                2200,
                0L,
                levels,
                xp);
    }

    private static ItemStackSnapshot item(int id, String name, int quantity)
    {
        return new ItemStackSnapshot(id, name, quantity);
    }
}

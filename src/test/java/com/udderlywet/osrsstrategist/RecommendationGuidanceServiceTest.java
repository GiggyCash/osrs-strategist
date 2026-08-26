package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecommendationGuidanceServiceTest
{
    private final RecommendationGuidanceService service =
            new RecommendationGuidanceService();

    @Test
    public void mainAccountGetsExactGrandExchangeShortfall()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 17, Experience.getXpForLevel(17)))
                .bank(bank(new ItemStackSnapshot(335, "Raw trout", 20)))
                .inventory(inventory(new ItemStackSnapshot(335, "Raw trout", 5)))
                .quests(completedCooksAssistant())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                fishPlan()
        );

        assertTrue(guidance.getAction().contains("20 successful cooks"));
        assertTrue(guidance.getSupplies().contains("about 50 raw trout"));
        assertTrue(guidance.getSupplies().contains("Verified: 25 raw trout"));
        assertTrue(guidance.getSupplies().contains("Buy 25 raw trout"));
        assertTrue(guidance.getSupplies().contains("Grand Exchange"));
        assertTrue(guidance.getLocation().contains("Lumbridge Castle range"));
    }

    @Test
    public void enoughVerifiedTroutDoesNotTellPlayerToBuyMore()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 17, Experience.getXpForLevel(17)))
                .bank(bank(new ItemStackSnapshot(335, "Raw trout", 60)))
                .inventory(inventory())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                fishPlan()
        );

        assertTrue(guidance.getSupplies().contains("already have enough"));
        assertFalse(guidance.getSupplies().contains(" Buy "));
    }

    @Test
    public void unopenedBankIsUnknownNotEmpty()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 17, Experience.getXpForLevel(17)))
                .inventory(inventory(new ItemStackSnapshot(335, "Raw trout", 4)))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                fishPlan()
        );

        assertTrue(guidance.getSupplies().contains("Open your bank once"));
        assertFalse(guidance.getSupplies().contains("Buy 50"));
    }

    @Test
    public void ironAccountSourcesMissingFishInsteadOfUsingGrandExchange()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(1, 17, Experience.getXpForLevel(17)))
                .bank(bank(new ItemStackSnapshot(335, "Raw trout", 20)))
                .inventory(inventory())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                fishPlan()
        );

        assertTrue(guidance.getSupplies().toLowerCase()
                .contains("source 30 raw trout"));
        assertTrue(guidance.getSupplies().contains("Barbarian Village"));
        assertFalse(guidance.getSupplies().contains("Grand Exchange"));
    }

    @Test
    public void partialLevelProgressUsesExactCurrentExperience()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 19, 4000))
                .bank(bank())
                .inventory(inventory())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                19,
                20,
                fishPlan()
        );

        assertTrue(guidance.getAction().contains("7 successful cooks"));
        assertTrue(guidance.getSupplies().contains("about 18 raw trout"));
        assertTrue(guidance.getSupplies().contains("Buy 18 raw trout"));
    }

    @Test
    public void levelTwentyPlanStagesPikeThenSalmonToThirty()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 20, Experience.getXpForLevel(20)))
                .bank(bank(
                        new ItemStackSnapshot(349, "Raw pike", 8),
                        new ItemStackSnapshot(331, "Raw salmon", 5)
                ))
                .inventory(inventory())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                20,
                30,
                fishPlan()
        );

        assertTrue(guidance.getAction().contains("pike to level 25"));
        assertTrue(guidance.getAction().contains("43 successful cooks"));
        assertTrue(guidance.getAction().contains("salmon to level 30"));
        assertTrue(guidance.getAction().contains("62 successful cooks"));
        assertTrue(guidance.getSupplies().contains("about 108 raw pike"));
        assertTrue(guidance.getSupplies().contains("about 155 raw salmon"));
        assertTrue(guidance.getSupplies().contains("Buy 100 raw pike and 150 raw salmon"));
    }

    private static TrainingPlan fishPlan()
    {
        TrainingMethod method = new TrainingMethod(
                "cooking_f2p_fish",
                Skill.COOKING,
                1,
                99,
                "Cook fish",
                "Generic catalog text should be replaced by account guidance.",
                10,
                10,
                10,
                AttentionLevel.LOW,
                20,
                2,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED
        );
        return new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList()
        );
    }

    private static AccountSnapshot account(
            int typeCode,
            int cookingLevel,
            int cookingXp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, 0);
        }
        levels.put(Skill.COOKING, cookingLevel);
        xp.put(Skill.COOKING, cookingXp);

        return new AccountSnapshot(
                "Guidance Test",
                typeCode,
                typeCode == 0 ? "Main" : "Ironman",
                MembershipStatus.F2P,
                1,
                2200,
                cookingXp,
                levels,
                xp
        );
    }

    private static BankSnapshot bank(ItemStackSnapshot... items)
    {
        return new BankSnapshot(
                items == null || items.length == 0
                        ? Collections.emptyList()
                        : Arrays.asList(items),
                System.currentTimeMillis()
        );
    }

    private static InventorySnapshot inventory(ItemStackSnapshot... items)
    {
        return new InventorySnapshot(
                items == null || items.length == 0
                        ? Collections.emptyList()
                        : Arrays.asList(items)
        );
    }

    private static QuestSnapshot completedCooksAssistant()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Cook's Assistant", QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
    }
}

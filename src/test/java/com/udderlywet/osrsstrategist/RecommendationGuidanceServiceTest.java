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
                        account(0, Experience.getXpForLevel(17)))
                .bank(bank(20))
                .inventory(inventory(5))
                .quests(completedCooksAssistant())
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                troutPlan()
        );

        assertTrue(guidance.getAction().contains("20 successful trout cooks"));
        assertTrue(guidance.getSupplies().contains("about 50 raw trout"));
        assertTrue(guidance.getSupplies().contains("25 verified"));
        assertTrue(guidance.getSupplies().contains("buy 25 raw trout"));
        assertTrue(guidance.getSupplies().contains("Grand Exchange"));
        assertTrue(guidance.getLocation().contains("Lumbridge Castle range"));
    }

    @Test
    public void enoughVerifiedTroutDoesNotTellPlayerToBuyMore()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, Experience.getXpForLevel(17)))
                .bank(bank(60))
                .inventory(inventory(0))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                troutPlan()
        );

        assertTrue(guidance.getSupplies().contains("already have enough"));
        assertFalse(guidance.getSupplies().contains("buy "));
    }

    @Test
    public void unopenedBankIsUnknownNotEmpty()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, Experience.getXpForLevel(17)))
                .inventory(inventory(4))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                troutPlan()
        );

        assertTrue(guidance.getSupplies().contains("Open your bank once"));
        assertFalse(guidance.getSupplies().contains("buy 50"));
    }

    @Test
    public void ironAccountSourcesMissingFishInsteadOfUsingGrandExchange()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(1, Experience.getXpForLevel(17)))
                .bank(bank(20))
                .inventory(inventory(0))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                17,
                20,
                troutPlan()
        );

        assertTrue(guidance.getSupplies().contains("source 30 more raw trout"));
        assertFalse(guidance.getSupplies().contains("Grand Exchange"));
    }

    @Test
    public void partialLevelProgressUsesExactCurrentExperience()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(0, 4000))
                .bank(bank(0))
                .inventory(inventory(0))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.COOKING,
                19,
                20,
                troutPlan()
        );

        assertTrue(guidance.getAction().contains("7 successful trout cooks"));
        assertTrue(guidance.getSupplies().contains("about 18 raw trout"));
        assertTrue(guidance.getSupplies().contains("buy 18 raw trout"));
    }

    private static TrainingPlan troutPlan()
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

    private static AccountSnapshot account(int typeCode, int cookingXp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, 0);
        }
        levels.put(Skill.COOKING, cookingXp >= 3973 ? 19 : 17);
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

    private static BankSnapshot bank(int rawTrout)
    {
        return new BankSnapshot(
                rawTrout <= 0
                        ? Collections.emptyList()
                        : Arrays.asList(new ItemStackSnapshot(
                                335,
                                "Raw trout",
                                rawTrout
                        )),
                System.currentTimeMillis()
        );
    }

    private static InventorySnapshot inventory(int rawTrout)
    {
        return new InventorySnapshot(
                rawTrout <= 0
                        ? Collections.emptyList()
                        : Arrays.asList(new ItemStackSnapshot(
                                335,
                                "Raw trout",
                                rawTrout
                        ))
        );
    }

    private static QuestSnapshot completedCooksAssistant()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Cook's Assistant", QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
    }
}

package compass;

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
            TestFixtures.recommendationGuidanceService();

    @Test
    public void unknownPriceAndCashCannotBecomePurchaseGuidance()
    {
        GameData data = GameData.builder(account(0, 17, Experience.getXpForLevel(17)))
                .bank(bank()).inventory(inventory()).build();
        org.junit.Assert.assertNull(service.build(data, Skill.COOKING, 17, 20, fishPlan(), false));
    }

    @Test
    public void enoughVerifiedTroutDoesNotTellPlayerToBuyMore()
    {
        GameData data = GameData.builder(account(0, 17, Experience.getXpForLevel(17)))
                .bank(bank(new ItemState(335, "Raw trout", 60))).inventory(inventory()).build();
        Guidance guidance = service.build(data, Skill.COOKING, 17, 20, fishPlan(), false);
        assertTrue(guidance.supplies.contains("Withdraw observed bank stock: 4 Raw trout"));
        assertFalse(guidance.supplies.contains("Buy"));
    }

    @Test
    public void carriedBatchDoesNotRequireOpeningAnUnknownBank()
    {
        GameData data = GameData.builder(account(0, 17, Experience.getXpForLevel(17)))
                .inventory(inventory(new ItemState(335, "Raw trout", 4))).build();
        Guidance guidance = service.build(data, Skill.COOKING, 17, 20, fishPlan(), false);
        assertTrue(guidance.supplies.contains("already carried: 4 Raw trout"));
        assertFalse(guidance.supplies.contains("Buy"));
        assertTrue(guidance.getAction().contains("when this batch is used"));
    }

    @Test
    public void ironUsesObservedStockButDoesNotInventAcquisition()
    {
        GameData stocked = GameData.builder(account(1, 17, Experience.getXpForLevel(17)))
                .bank(bank(new ItemState(335, "Raw trout", 20))).inventory(inventory()).build();
        Guidance guidance = service.build(stocked, Skill.COOKING, 17, 20, fishPlan(), false);
        assertTrue(guidance.supplies.contains("Withdraw observed bank stock"));
        assertFalse(guidance.supplies.contains("Grand Exchange"));
        GameData missing = GameData.builder(account(1, 17, Experience.getXpForLevel(17)))
                .bank(bank()).inventory(inventory()).build();
        org.junit.Assert.assertNull(service.build(missing, Skill.COOKING, 17, 20, fishPlan(), false));
    }

    @Test
    public void partialLevelProgressUsesExactCurrentExperience()
    {
        GameData data = GameData.builder(account(0, 19, 4000))
                .inventory(inventory(new ItemState(335, "Raw trout", 4))).build();
        Guidance guidance = service.build(data, Skill.COOKING, 19, 20, fishPlan(), false);
        assertTrue(guidance.getAction().contains("470 Cooking XP remaining"));
        assertTrue(guidance.getAction().contains("Stop at level 20"));
        assertFalse(guidance.supplies.contains("Buy"));
    }

    @Test
    public void levelTwentyReviewsAtSalmonUnlockWithoutSpendingLockedInputs()
    {
        GameData data = GameData.builder(account(0, 20, Experience.getXpForLevel(20)))
                .bank(bank(new ItemState(349, "Raw pike", 8), new ItemState(331, "Raw salmon", 50)))
                .inventory(inventory()).build();
        Guidance guidance = service.build(data, Skill.COOKING, 20, 30, fishPlan(), false);
        assertTrue(guidance.getAction().contains("Raw pike"));
        assertTrue(guidance.getAction().contains("Stop at level 25"));
        assertFalse(guidance.supplies.contains("salmon"));
        GameData unlocked = GameData.builder(account(0, 25, Experience.getXpForLevel(25)))
                .bank(bank(new ItemState(331, "Raw salmon", 50))).inventory(inventory()).build();
        assertTrue(service.build(unlocked, Skill.COOKING, 25, 30, fishPlan(), false).getAction().contains("Raw salmon"));
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
                Confidence.VERIFIED
        );
        return new TrainingPlan(
                method,
                "test",
                Confidence.VERIFIED,
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

        return new AccountSnapshot("Guidance Test", 0L, typeCode, typeCode == 0 ? "Main" : "Ironman", Membership.F2P, 1, 2200, cookingXp, levels, xp);
    }

    private static ItemsState bank(ItemState... items)
    {
        return new ItemsState(
                items == null || items.length == 0
                        ? Collections.emptyList()
                        : Arrays.asList(items),
                System.currentTimeMillis()
        );
    }

    private static ItemsState inventory(ItemState... items)
    {
        java.util.List<ItemState> slots = new java.util.ArrayList<>();
        for (ItemState item : items)
            for (int n = 0; n < item.quantity; n++)
                slots.add(new ItemState(item.itemId, item.name, 1));
        return new ItemsState(slots, true);
    }
    private static QuestSnapshot completedCooksAssistant()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Cook's Assistant", QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
    }
}

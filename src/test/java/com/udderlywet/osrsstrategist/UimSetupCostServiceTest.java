package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UimSetupCostServiceTest
{
    private final UimSetupCostService service = new UimSetupCostService();

    @Test
    public void fullInventoryMakesHeavySetupWorseThanLightSetup()
    {
        StrategyContext context = context(fullInventoryData(false));
        Recommendation light = recommendation("skill:fishing", 2, "Fish nearby.");
        Recommendation heavy = recommendation("skill:construction", 15, "Rebuild a large setup.");

        assertTrue(service.score(light, context) > service.score(heavy, context));
    }

    @Test
    public void dangerousGauntletRouteIsStronglyPenalizedWithDeathStorage()
    {
        StrategyContext context = context(fullInventoryData(true));
        Recommendation safe = recommendation("skill:fishing", 2, "Fish nearby.");
        Recommendation dangerous = new Recommendation(
                "upgrade:bowfa",
                "Hunt the Enhanced crystal weapon seed",
                "Run the Corrupted Gauntlet.",
                100.0,
                null,
                RecommendationConfidence.VERIFIED,
                0,
                0,
                new RecommendationGuidance(
                        "Run the Corrupted Gauntlet.",
                        "The activity supplies its own temporary equipment.",
                        "Prifddinas.",
                        "A dangerous death can threaten current UIM death storage."));

        assertTrue(service.score(safe, context) - service.score(dangerous, context) >= 30.0);
    }

    @Test
    public void explicitUimJustInTimeRouteGetsPracticalityCredit()
    {
        StrategyContext context = context(fullInventoryData(false));
        Recommendation generic = recommendation("skill:herblore", 4,
                "Acquire supplies normally.");
        Recommendation uim = new Recommendation(
                "skill:herblore",
                "Train Herblore",
                "UIM route",
                40.0,
                null,
                RecommendationConfidence.VERIFIED,
                70,
                80,
                new RecommendationGuidance(
                        "Process the herbs.",
                        "Acquire materials just in time and preserve the current inventory setup for UIM.",
                        "Nearest practical loop.",
                        "UIM-aware route."));

        assertTrue(service.score(uim, context) > service.score(generic, context));
    }

    private static Recommendation recommendation(String id, int setup, String supplies)
    {
        TrainingMethod method = new TrainingMethod(
                id + ":method", Skill.FISHING, 1, 99,
                "Test method", "Do it.",
                10, 10, 10, AttentionLevel.LOW,
                20, setup, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        TrainingPlan plan = new TrainingPlan(
                method, "test", RecommendationConfidence.VERIFIED,
                Collections.emptyList());
        return new Recommendation(
                id, "Test", "Test", 40.0, plan,
                RecommendationConfidence.VERIFIED, 70, 80,
                new RecommendationGuidance(
                        "Do the route.", supplies, "Safe location.", "Safe."));
    }

    private static StrategyContext context(StrategyDataBundle data)
    {
        return new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX,
                true, false, false, new PreferenceProfile());
    }

    private static StrategyDataBundle fullInventoryData(boolean deathStorage)
    {
        List<ItemStackSnapshot> inventory = new ArrayList<>();
        for (int i = 0; i < 26; i++)
        {
            inventory.add(new ItemStackSnapshot(1000 + i, "Item " + i, 1));
        }

        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        if (deathStorage)
        {
            states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
            contents.put(StorageCapability.DEATH_STORAGE,
                    Collections.singletonList(
                            new ItemStackSnapshot(2000, "Stored valuable", 1)));
        }

        return StrategyDataBundle.builder(account())
                .inventory(new InventorySnapshot(inventory))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .storage(new StorageSnapshot(states, contents))
                .build();
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 80 : 70;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        return new AccountSnapshot(
                "UIM Test", 2, "Ultimate Ironman",
                MembershipStatus.P2P, 1, total, totalXp, levels, xp);
    }
}

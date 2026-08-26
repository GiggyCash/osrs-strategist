package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupStoragePlanningTest
{
    @Test
    public void recentSharedPickaxeChangesSetupGuidanceOnlyWhenEnabled()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .groupStorage(new GroupStorageSnapshot(true,
                        Collections.singletonList(new ItemStackSnapshot(
                                ItemID.BRONZE_PICKAXE,
                                "Bronze pickaxe", 1))))
                .build();
        RecommendationEngine engine = new RecommendationEngine(selector());
        Recommendation disabled = find(engine.recommendAll(data,
                StrategyMode.RELAXED, SessionIntent.AFK, false, false,
                new PreferenceProfile()), "skill:mining");
        Recommendation enabled = find(engine.recommendAll(data,
                StrategyMode.RELAXED, SessionIntent.AFK, true, false,
                new PreferenceProfile()), "skill:mining");

        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        assertTrue(policy.canLeadQueue(disabled));
        assertTrue(policy.canLeadQueue(enabled));
        assertTrue(enabled.getGuidance().getSupplies()
                .contains("Bronze pickaxe"));
        assertTrue(disabled.getGuidance().getSupplies()
                .contains("Mining tutor"));
    }

    @Test
    public void staleSharedPickaxeUsesConcreteAcquisitionInsteadOfOwnership()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .groupStorage(new GroupStorageSnapshot(true,
                        Collections.singletonList(new ItemStackSnapshot(
                                ItemID.BRONZE_PICKAXE,
                                "Bronze pickaxe", 1)),
                        System.currentTimeMillis()
                                - GroupStorageSnapshot.FRESH_FOR_MILLIS - 1L))
                .build();
        Recommendation mining = find(new RecommendationEngine(selector())
                .recommendAll(data, StrategyMode.RELAXED, SessionIntent.AFK,
                        true, false, new PreferenceProfile()), "skill:mining");

        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(mining));
        assertTrue(mining.getGuidance().getSupplies()
                .contains("Mining tutor"));
        assertFalse(mining.getGuidance().getSupplies()
                .contains("Bring your Bronze pickaxe"));
    }

    private static TrainingMethodSelector selector()
    {
        return new TrainingMethodSelector(new TrainingMethodDatabase(),
                new RequirementEvidenceEngine(
                        new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                        new AgilityAccessEvaluator(new AgilityCourseCatalog())),
                new ExpandedTrainingMethodCatalog(),
                new F2pBaselineMethodCatalog(), new TrainingMethodPolicy());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 45);
            xp.put(skill, Experience.getXpForLevel(45));
        }
        return new AccountSnapshot("GIM", 445L, 4, "GROUP_IRONMAN",
                MembershipStatus.P2P, 1, 45 * Skill.values().length,
                0L, levels, xp);
    }

    private static Recommendation find(
            List<Recommendation> recommendations, String id)
    {
        for (Recommendation recommendation : recommendations)
            if (id.equals(recommendation.getId())) return recommendation;
        throw new AssertionError("Missing " + id);
    }
}

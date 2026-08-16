package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UimResourceRoutingTest
{
    @Test
    public void uimReadinessIgnoresNormalBankAndUsesVerifiedStorageContents()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.STASH, CapabilityState.VERIFIED);

        Map<StorageCapability, java.util.List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.STASH, Collections.singletonList(
                new ItemStackSnapshot(100, "Thing", 2)));

        StrategyDataBundle data = StrategyDataBundle.builder(uim())
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(100, "Thing", 99)), 1L))
                .storage(new StorageSnapshot(states, contents))
                .build();

        RequirementCheck check = new ResourceReadinessService().evaluate(
                data, new ResourceRequirement("thing", "Thing", 2, 100));
        assertEquals(RequirementState.VERIFIED, check.getState());
    }

    @Test
    public void uimAcquisitionCanUseObservedSafeStorageButNotAssumedStorage()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.STASH, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.STASH, Collections.singletonList(
                new ItemStackSnapshot(100, "Thing", 2)));

        StrategyContext context = context(new StorageSnapshot(states, contents));
        ResourceAcquisitionPlan plan = new ResourceAcquisitionPlanner().plan(
                context, new ResourceNeed(100, "Thing", 2));

        assertEquals(AcquisitionSource.VERIFIED_STORAGE, plan.getSource());
        assertEquals(RecommendationConfidence.VERIFIED, plan.getConfidence());
    }

    @Test
    public void deathpileResourceStillRequiresRiskCheck()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATHPILE, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATHPILE, Collections.singletonList(
                new ItemStackSnapshot(100, "Thing", 2)));

        ResourceAcquisitionPlan plan = new ResourceAcquisitionPlanner().plan(
                context(new StorageSnapshot(states, contents)),
                new ResourceNeed(100, "Thing", 2));

        assertEquals(AcquisitionSource.VERIFIED_STORAGE, plan.getSource());
        assertEquals(RecommendationConfidence.CHECK_NEEDED, plan.getConfidence());
    }

    private static StrategyContext context(StorageSnapshot storage)
    {
        StrategyDataBundle data = StrategyDataBundle.builder(uim())
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .storage(storage)
                .build();
        return new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot uim()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "UimTester", 2, "Ultimate Ironman",
                MembershipStatus.P2P, 1, 1, 0L, levels, xp);
    }
}

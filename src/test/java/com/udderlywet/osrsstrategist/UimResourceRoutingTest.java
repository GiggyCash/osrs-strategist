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
    public void uimReadinessIgnoresNormalBankAndUsesVerifiedSafeStorageContents()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(uim())
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(100, "Thing", 99)), 1L))
                .storage(storage(StorageCapability.STASH, 2))
                .build();

        RequirementCheck check = new ResourceReadinessService().evaluate(
                data, new ResourceRequirement("thing", "Thing", 2, 100));
        assertEquals(RequirementState.VERIFIED, check.getState());
    }

    @Test
    public void deathBasedOrLootingBagStorageDoesNotBecomeReadyWithoutAccessCheck()
    {
        for (StorageCapability capability : new StorageCapability[] {
                StorageCapability.LOOTING_BAG,
                StorageCapability.DEATH_STORAGE,
                StorageCapability.DEATHPILE})
        {
            StrategyDataBundle data = StrategyDataBundle.builder(uim())
                    .inventory(new InventorySnapshot(Collections.emptyList()))
                    .storage(storage(capability, 2))
                    .build();
            RequirementCheck check = new ResourceReadinessService().evaluate(
                    data, new ResourceRequirement("thing", "Thing", 2, 100));
            assertEquals(capability.name(), RequirementState.CHECK_NEEDED,
                    check.getState());
        }
    }

    @Test
    public void uimAcquisitionCanUseObservedSafeStorageButNotAssumedStorage()
    {
        ResourceAcquisitionPlan plan = new ResourceAcquisitionPlanner().plan(
                context(storage(StorageCapability.STASH, 2)),
                new ResourceNeed(100, "Thing", 2));

        assertEquals(AcquisitionSource.VERIFIED_STORAGE, plan.getSource());
        assertEquals(RecommendationConfidence.VERIFIED, plan.getConfidence());
    }

    @Test
    public void restrictedStorageResourceStillRequiresPreconditionCheck()
    {
        for (StorageCapability capability : new StorageCapability[] {
                StorageCapability.LOOTING_BAG,
                StorageCapability.DEATH_STORAGE,
                StorageCapability.DEATHPILE})
        {
            ResourceAcquisitionPlan plan = new ResourceAcquisitionPlanner().plan(
                    context(storage(capability, 2)),
                    new ResourceNeed(100, "Thing", 2));
            assertEquals(capability.name(), AcquisitionSource.VERIFIED_STORAGE,
                    plan.getSource());
            assertEquals(capability.name(),
                    RecommendationConfidence.CHECK_NEEDED,
                    plan.getConfidence());
        }
    }

    private static StorageSnapshot storage(
            StorageCapability capability,
            int quantity)
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(capability, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(capability, Collections.singletonList(
                new ItemStackSnapshot(100, "Thing", quantity)));
        return new StorageSnapshot(states, contents);
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

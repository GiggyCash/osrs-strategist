package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Verifies the basic Main/Iron/GIM/UIM sourcing guardrails.
 */
public class ResourceAcquisitionPlannerTest
{
    private final ResourceAcquisitionPlanner planner =
            new ResourceAcquisitionPlanner();
    private final ResourceNeed planks =
            new ResourceNeed(960, "Plank", 10);

    @Test
    public void mainWithoutOwnedSupplyFallsBackToGeCheck()
    {
        AcquisitionPlan plan = planner.plan(
                context(AccountMode.MAIN,
                        new ItemsState(Collections.emptyList()),
                        new ItemsState(Collections.emptyList(), 1L),
                        null, true),
                planks
        );

        assertEquals(AcquisitionSource.GRAND_EXCHANGE, plan.getSource());
        assertEquals(
                Confidence.CHECK_NEEDED,
                plan.getConfidence()
        );
    }

    @Test
    public void ironWithoutOwnedSupplyRequiresSelfSource()
    {
        AcquisitionPlan plan = planner.plan(
                context(AccountMode.IRONMAN,
                        new ItemsState(Collections.emptyList()),
                        new ItemsState(Collections.emptyList(), 1L),
                        null, true),
                planks
        );

        assertEquals(AcquisitionSource.SELF_SOURCE, plan.getSource());
    }

    @Test
    public void uimNeverTreatsNormalBankAsAcquisitionRoute()
    {
        ItemsState bank = new ItemsState(
                Arrays.asList(new ItemState(960, "Plank", 100)),
                1L
        );

        AcquisitionPlan plan = planner.plan(
                context(AccountMode.ULTIMATE_IRONMAN,
                        new ItemsState(Collections.emptyList()),
                        bank, null, true),
                planks
        );

        assertEquals(AcquisitionSource.SELF_SOURCE, plan.getSource());
    }

    @Test
    public void gimUsesObservedGroupStorageOnlyWhenEnabled()
    {
        ItemsState group = new ItemsState(
                true,
                Arrays.asList(new ItemState(960, "Plank", 25))
        );

        AcquisitionPlan enabled = planner.plan(
                context(AccountMode.GROUP_IRONMAN,
                        new ItemsState(Collections.emptyList()),
                        new ItemsState(Collections.emptyList(), 1L),
                        group, true),
                planks
        );
        AcquisitionPlan disabled = planner.plan(
                context(AccountMode.GROUP_IRONMAN,
                        new ItemsState(Collections.emptyList()),
                        new ItemsState(Collections.emptyList(), 1L),
                        group, false),
                planks
        );

        assertEquals(AcquisitionSource.GROUP_STORAGE, enabled.getSource());
        assertEquals(
                Confidence.VERIFIED,
                enabled.getConfidence()
        );
        assertEquals(AcquisitionSource.SELF_SOURCE, disabled.getSource());
    }

    @Test
    public void unknownContainersNeverBecomeMainIronOrGimShortfalls()
    {
        assertEquals(AcquisitionSource.CHECK_NEEDED, planner.plan(
                context(AccountMode.MAIN, null, null, null, false), planks)
                .getSource());
        assertEquals(AcquisitionSource.CHECK_NEEDED, planner.plan(
                context(AccountMode.IRONMAN, null, null, null, false), planks)
                .getSource());
        assertEquals(AcquisitionSource.CHECK_NEEDED, planner.plan(
                context(AccountMode.GROUP_IRONMAN,
                        new ItemsState(Collections.emptyList()),
                        new ItemsState(Collections.emptyList(), 1L),
                        ItemsState.unknown(), true), planks)
                .getSource());
    }

    @Test
    public void uimMergesDuplicateResourcesAcrossVerifiedSafeStorage()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.POH_STORAGE, CapabilityState.VERIFIED);
        states.put(StorageCapability.STASH, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.POH_STORAGE, Collections.singletonList(
                new ItemState(960, "Plank", 4)));
        contents.put(StorageCapability.STASH, Collections.singletonList(
                new ItemState(960, "Plank", 3)));
        StrategyContext uim = context(GameData.builder(
                        account(AccountMode.ULTIMATE_IRONMAN))
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(960, "Plank", 3))))
                .storage(new StorageSnapshot(states, contents)).build());

        AcquisitionPlan plan = planner.plan(uim, planks);

        assertEquals(AcquisitionSource.VERIFIED_STORAGE, plan.getSource());
        assertEquals(10, plan.getConfirmedQuantity());
        assertEquals(Confidence.VERIFIED,
                plan.getConfidence());
    }

    @Test
    public void mergedRestrictedStorageStillRequiresExplicitRetrieval()
    {
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.POH_STORAGE, CapabilityState.VERIFIED);
        states.put(StorageCapability.LOOTING_BAG, CapabilityState.VERIFIED);
        Map<StorageCapability, java.util.List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.POH_STORAGE, Collections.singletonList(
                new ItemState(960, "Plank", 3)));
        contents.put(StorageCapability.LOOTING_BAG, Collections.singletonList(
                new ItemState(960, "Plank", 4)));
        StrategyContext uim = context(GameData.builder(
                        account(AccountMode.ULTIMATE_IRONMAN))
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(960, "Plank", 3))))
                .storage(new StorageSnapshot(states, contents)).build());

        AcquisitionPlan plan = planner.plan(uim, planks);

        assertEquals(AcquisitionSource.VERIFIED_STORAGE, plan.getSource());
        assertEquals(Confidence.CHECK_NEEDED,
                plan.getConfidence());
    }

    private static StrategyContext context(
            AccountMode mode,
            ItemsState inventory,
            ItemsState bank,
            ItemsState group,
            boolean useGroupStorage)
    {
        GameData data = GameData
                .builder(account(mode))
                .inventory(inventory)
                .bank(bank)
                .groupStorage(group)
                .build();

        return new StrategyContext(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL,
                GoalType.MAX,
                useGroupStorage,
                false,
                new PreferenceProfile()
        );
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.MAX, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(AccountMode mode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> experience = new EnumMap<>(Skill.class);

        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            experience.put(skill, 0);
        }

        return new AccountSnapshot(
                "Test",
                typeCode(mode),
                mode.name(),
                24,
                0L,
                levels,
                experience
        );
    }

    private static int typeCode(AccountMode mode)
    {
        switch (mode)
        {
            case MAIN: return 0;
            case IRONMAN: return 1;
            case ULTIMATE_IRONMAN: return 2;
            case HARDCORE_IRONMAN: return 3;
            case GROUP_IRONMAN: return 4;
            case HARDCORE_GROUP_IRONMAN: return 5;
            case UNRANKED_GROUP_IRONMAN: return 6;
            default: return -1;
        }
    }
}

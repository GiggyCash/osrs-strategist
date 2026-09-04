package compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class AccountStrategicPriorityServiceTest
{
    private final AccountStrategicPriorityService service =
            new AccountStrategicPriorityService();

    @Test
    public void profileIsExhaustiveAndMethodIdentityFree()
    {
        AccountPriorities profile = service.assess(
                AccountMode.MAIN, data(AccountMode.MAIN, 10, null), false);

        assertEquals(AccountDimension.values().length,
                profile.getPriorities().size());
        for (AccountPriority priority
                : profile.getPriorities().values())
        {
            assertTrue(priority.getReason().length() > 10);
        }
    }

    @Test
    public void identicalMainAndIronStateDivergesOnEconomicProperties()
    {
        AccountPriorities main = service.assess(
                AccountMode.MAIN, data(AccountMode.MAIN, 10, null), false);
        AccountPriorities iron = service.assess(
                AccountMode.IRONMAN,
                data(AccountMode.IRONMAN, 10, null), false);

        assertEquals(Priority.LOW, main.priorityOf(
                AccountDimension.SELF_SOURCING_BURDEN));
        assertEquals(Priority.HIGH, iron.priorityOf(
                AccountDimension.SELF_SOURCING_BURDEN));
        assertEquals(Priority.LOW, main.priorityOf(
                AccountDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY));
        assertEquals(Priority.HIGH, iron.priorityOf(
                AccountDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY));
        assertEquals(Capability.VERIFIED, main.get(
                AccountDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertEquals(Capability.BLOCKED, iron.get(
                AccountDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertTrue(iron.priorityOf(AccountDimension.POH_VALUE)
                .isAtLeast(main.priorityOf(
                        AccountDimension.POH_VALUE)));
    }

    @Test
    public void uimInventoryAndSetupPropertiesAreCritical()
    {
        AccountPriorities uim = service.assess(
                AccountMode.ULTIMATE_IRONMAN,
                data(AccountMode.ULTIMATE_IRONMAN, 25, null), false);

        assertEquals(Priority.CRITICAL, uim.priorityOf(
                AccountDimension.INVENTORY_PRESSURE));
        assertEquals(Priority.CRITICAL, uim.priorityOf(
                AccountDimension.BANK_AVAILABILITY));
        assertEquals(Capability.BLOCKED, uim.get(
                AccountDimension.BANK_AVAILABILITY)
                .getCapabilityState());
        assertEquals(Priority.CRITICAL, uim.priorityOf(
                AccountDimension.STORAGE_VALUE));
        assertEquals(Priority.CRITICAL, uim.priorityOf(
                AccountDimension.SETUP_COST_SENSITIVITY));
        assertEquals(Priority.CRITICAL, uim.priorityOf(
                AccountDimension.STORABLE_EQUIPMENT_VALUE));
    }

    @Test
    public void unobservedUimSlotsRemainCheckNeeded()
    {
        GameData data = GameData.builder(account(
                        AccountMode.ULTIMATE_IRONMAN,
                        Membership.P2P, 70))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        AccountPriority value = service.assess(
                AccountMode.ULTIMATE_IRONMAN, data, false).get(
                AccountDimension.INVENTORY_PRESSURE);

        assertEquals(Confidence.CHECK_NEEDED,
                value.getConfidence());
        assertTrue(value.getReason().contains("not observed"));
    }

    @Test
    public void hardcoreRiskIsHigherThanOrdinaryIronRisk()
    {
        AccountPriorities iron = service.assess(
                AccountMode.IRONMAN,
                data(AccountMode.IRONMAN, 10, null), false);
        AccountPriorities hardcore = service.assess(
                AccountMode.HARDCORE_IRONMAN,
                data(AccountMode.HARDCORE_IRONMAN, 10, null), false);

        assertEquals(Priority.MODERATE, iron.priorityOf(
                AccountDimension.DEATH_RISK_SENSITIVITY));
        assertEquals(Priority.CRITICAL, hardcore.priorityOf(
                AccountDimension.DEATH_RISK_SENSITIVITY));
    }

    @Test
    public void groupBenefitsRequireFreshEnabledObservedStorage()
    {
        long now = System.currentTimeMillis();
        ItemsState fresh = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        995, "Coins", 10)), now);
        ItemsState stale = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        995, "Coins", 10)), now - 10L * 60L * 1000L);

        AccountPriorities usable = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, fresh), true);
        AccountPriorities disabled = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, fresh), false);
        AccountPriorities expired = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, stale), true);

        assertEquals(Priority.HIGH, usable.priorityOf(
                AccountDimension.SHARED_RESOURCE_VALUE));
        assertEquals(Priority.HIGH, usable.priorityOf(
                AccountDimension.DUPLICATE_GRIND_PENALTY));
        assertEquals(Priority.NONE, disabled.priorityOf(
                AccountDimension.SHARED_RESOURCE_VALUE));
        assertEquals(Priority.NONE, expired.priorityOf(
                AccountDimension.DUPLICATE_GRIND_PENALTY));
        assertEquals(Confidence.CHECK_NEEDED,
                usable.get(AccountDimension.SHARED_INFRASTRUCTURE_VALUE)
                        .getConfidence());
        assertEquals(Capability.UNKNOWN, usable.get(
                AccountDimension.SHARED_INFRASTRUCTURE_VALUE)
                .getCapabilityState());
    }

    @Test
    public void unknownModeFailsClosedOnEconomicCapabilities()
    {
        AccountPriorities unknown = service.assess(
                AccountMode.UNKNOWN, null, true);

        assertEquals(Priority.CRITICAL, unknown.priorityOf(
                AccountDimension.GRAND_EXCHANGE_AVAILABILITY));
        assertEquals(Confidence.CHECK_NEEDED,
                unknown.get(AccountDimension.BANK_AVAILABILITY)
                        .getConfidence());
        assertEquals(Priority.NONE, unknown.priorityOf(
                AccountDimension.SHARED_RESOURCE_VALUE));
        assertEquals(Capability.UNKNOWN, unknown.get(
                AccountDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertEquals(AccountDimensionRole.CAPABILITY_GATE,
                AccountDimension.GRAND_EXCHANGE_AVAILABILITY
                        .getRole());
        assertEquals(AccountDimensionRole.BURDEN_WEIGHT,
                AccountDimension.DEATH_RISK_SENSITIVITY.getRole());
    }

    private static GameData data(AccountMode mode, int occupied,
            ItemsState groupStorage)
    {
        List<ItemState> items = new ArrayList<>();
        for (int i = 0; i < occupied; i++)
            items.add(new ItemState(10_000 + i, "Item " + i, 1, i));
        return GameData.builder(account(mode, Membership.P2P, 70))
                .inventory(new ItemsState(items))
                .groupStorage(groupStorage)
                .build();
    }

    private static AccountSnapshot account(AccountMode mode,
            Membership membership, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Priority", 1L, mode.ordinal(),
                mode.name(), membership, membership == Membership.P2P
                        ? 1 : 0, level * Skill.values().length, 0L, levels, xp);
    }
}

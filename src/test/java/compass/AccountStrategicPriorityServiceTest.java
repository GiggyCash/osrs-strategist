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
        AccountStrategicPriorityProfile profile = service.assess(
                AccountMode.MAIN, data(AccountMode.MAIN, 10, null), false);

        assertEquals(AccountStrategicDimension.values().length,
                profile.getPriorities().size());
        for (AccountStrategicPriority priority
                : profile.getPriorities().values())
        {
            assertTrue(priority.getReason().length() > 10);
        }
    }

    @Test
    public void identicalMainAndIronStateDivergesOnEconomicProperties()
    {
        AccountStrategicPriorityProfile main = service.assess(
                AccountMode.MAIN, data(AccountMode.MAIN, 10, null), false);
        AccountStrategicPriorityProfile iron = service.assess(
                AccountMode.IRONMAN,
                data(AccountMode.IRONMAN, 10, null), false);

        assertEquals(StrategicPriority.LOW, main.priorityOf(
                AccountStrategicDimension.SELF_SOURCING_BURDEN));
        assertEquals(StrategicPriority.HIGH, iron.priorityOf(
                AccountStrategicDimension.SELF_SOURCING_BURDEN));
        assertEquals(StrategicPriority.LOW, main.priorityOf(
                AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY));
        assertEquals(StrategicPriority.HIGH, iron.priorityOf(
                AccountStrategicDimension.CONSUMABLE_REPLACEMENT_DIFFICULTY));
        assertEquals(CapabilityState.VERIFIED, main.get(
                AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertEquals(CapabilityState.BLOCKED, iron.get(
                AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertTrue(iron.priorityOf(AccountStrategicDimension.POH_VALUE)
                .isAtLeast(main.priorityOf(
                        AccountStrategicDimension.POH_VALUE)));
    }

    @Test
    public void uimInventoryAndSetupPropertiesAreCritical()
    {
        AccountStrategicPriorityProfile uim = service.assess(
                AccountMode.ULTIMATE_IRONMAN,
                data(AccountMode.ULTIMATE_IRONMAN, 25, null), false);

        assertEquals(StrategicPriority.CRITICAL, uim.priorityOf(
                AccountStrategicDimension.INVENTORY_PRESSURE));
        assertEquals(StrategicPriority.CRITICAL, uim.priorityOf(
                AccountStrategicDimension.BANK_AVAILABILITY));
        assertEquals(CapabilityState.BLOCKED, uim.get(
                AccountStrategicDimension.BANK_AVAILABILITY)
                .getCapabilityState());
        assertEquals(StrategicPriority.CRITICAL, uim.priorityOf(
                AccountStrategicDimension.STORAGE_VALUE));
        assertEquals(StrategicPriority.CRITICAL, uim.priorityOf(
                AccountStrategicDimension.SETUP_COST_SENSITIVITY));
        assertEquals(StrategicPriority.CRITICAL, uim.priorityOf(
                AccountStrategicDimension.STORABLE_EQUIPMENT_VALUE));
    }

    @Test
    public void unobservedUimSlotsRemainCheckNeeded()
    {
        GameData data = GameData.builder(account(
                        AccountMode.ULTIMATE_IRONMAN,
                        MembershipStatus.P2P, 70))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        AccountStrategicPriority value = service.assess(
                AccountMode.ULTIMATE_IRONMAN, data, false).get(
                AccountStrategicDimension.INVENTORY_PRESSURE);

        assertEquals(Confidence.CHECK_NEEDED,
                value.getConfidence());
        assertTrue(value.getReason().contains("not observed"));
    }

    @Test
    public void hardcoreRiskIsHigherThanOrdinaryIronRisk()
    {
        AccountStrategicPriorityProfile iron = service.assess(
                AccountMode.IRONMAN,
                data(AccountMode.IRONMAN, 10, null), false);
        AccountStrategicPriorityProfile hardcore = service.assess(
                AccountMode.HARDCORE_IRONMAN,
                data(AccountMode.HARDCORE_IRONMAN, 10, null), false);

        assertEquals(StrategicPriority.MODERATE, iron.priorityOf(
                AccountStrategicDimension.DEATH_RISK_SENSITIVITY));
        assertEquals(StrategicPriority.CRITICAL, hardcore.priorityOf(
                AccountStrategicDimension.DEATH_RISK_SENSITIVITY));
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

        AccountStrategicPriorityProfile usable = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, fresh), true);
        AccountStrategicPriorityProfile disabled = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, fresh), false);
        AccountStrategicPriorityProfile expired = service.assess(
                AccountMode.GROUP_IRONMAN,
                data(AccountMode.GROUP_IRONMAN, 10, stale), true);

        assertEquals(StrategicPriority.HIGH, usable.priorityOf(
                AccountStrategicDimension.SHARED_RESOURCE_VALUE));
        assertEquals(StrategicPriority.HIGH, usable.priorityOf(
                AccountStrategicDimension.DUPLICATE_GRIND_PENALTY));
        assertEquals(StrategicPriority.NONE, disabled.priorityOf(
                AccountStrategicDimension.SHARED_RESOURCE_VALUE));
        assertEquals(StrategicPriority.NONE, expired.priorityOf(
                AccountStrategicDimension.DUPLICATE_GRIND_PENALTY));
        assertEquals(Confidence.CHECK_NEEDED,
                usable.get(AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE)
                        .getConfidence());
        assertEquals(CapabilityState.UNKNOWN, usable.get(
                AccountStrategicDimension.SHARED_INFRASTRUCTURE_VALUE)
                .getCapabilityState());
    }

    @Test
    public void unknownModeFailsClosedOnEconomicCapabilities()
    {
        AccountStrategicPriorityProfile unknown = service.assess(
                AccountMode.UNKNOWN, null, true);

        assertEquals(StrategicPriority.CRITICAL, unknown.priorityOf(
                AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY));
        assertEquals(Confidence.CHECK_NEEDED,
                unknown.get(AccountStrategicDimension.BANK_AVAILABILITY)
                        .getConfidence());
        assertEquals(StrategicPriority.NONE, unknown.priorityOf(
                AccountStrategicDimension.SHARED_RESOURCE_VALUE));
        assertEquals(CapabilityState.UNKNOWN, unknown.get(
                AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY)
                .getCapabilityState());
        assertEquals(AccountStrategicDimensionRole.CAPABILITY_GATE,
                AccountStrategicDimension.GRAND_EXCHANGE_AVAILABILITY
                        .getRole());
        assertEquals(AccountStrategicDimensionRole.BURDEN_WEIGHT,
                AccountStrategicDimension.DEATH_RISK_SENSITIVITY.getRole());
    }

    private static GameData data(AccountMode mode, int occupied,
            ItemsState groupStorage)
    {
        List<ItemState> items = new ArrayList<>();
        for (int i = 0; i < occupied; i++)
            items.add(new ItemState(10_000 + i, "Item " + i, 1, i));
        return GameData.builder(account(mode, MembershipStatus.P2P, 70))
                .inventory(new ItemsState(items))
                .groupStorage(groupStorage)
                .build();
    }

    private static AccountSnapshot account(AccountMode mode,
            MembershipStatus membership, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Priority", 1L, mode.ordinal(),
                mode.name(), membership, membership == MembershipStatus.P2P
                        ? 1 : 0, level * Skill.values().length, 0L, levels, xp);
    }
}

package compass;

import static org.junit.Assert.assertEquals;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;

/** Covers the focused account policy consumed by infrastructure planning. */
public class AccountStrategicPriorityServiceTest
{
    private Priority priority(StrategyContext context, InfraBenefit benefit)
    {
        return InfrastructureUnlockValueService.priorityOf(context, benefit);
    }

    @Test
    public void mainAndIronDivergeOnSelfSourcingBenefits()
    {
        assertEquals(Priority.LOW, priority(AccountMode.MAIN,
                InfraBenefit.SELF_SUFFICIENCY, 10));
        assertEquals(Priority.HIGH, priority(AccountMode.IRONMAN,
                InfraBenefit.SELF_SUFFICIENCY, 10));
        assertEquals(Priority.LOW, priority(AccountMode.MAIN,
                InfraBenefit.RESOURCE_SUSTAINABILITY, 10));
        assertEquals(Priority.HIGH, priority(AccountMode.IRONMAN,
                InfraBenefit.RESOURCE_SUSTAINABILITY, 10));
    }

    @Test
    public void uimInventoryStorageAndSetupBenefitsAreCritical()
    {
        StrategyContext context = context(AccountMode.ULTIMATE_IRONMAN, 25);
        assertEquals(Priority.CRITICAL,
                priority(context, InfraBenefit.INVENTORY_RELIEF));
        assertEquals(Priority.CRITICAL,
                priority(context, InfraBenefit.STORAGE));
        assertEquals(Priority.CRITICAL,
                priority(context, InfraBenefit.SETUP_REUSE));
        assertEquals(Priority.CRITICAL,
                priority(context, InfraBenefit.STORABLE_EQUIPMENT));
    }

    @Test
    public void uimInventoryPressureUsesObservedSlots()
    {
        assertEquals(Priority.HIGH, priority(AccountMode.ULTIMATE_IRONMAN,
                InfraBenefit.INVENTORY_RELIEF, 10));
        assertEquals(Priority.CRITICAL, priority(AccountMode.ULTIMATE_IRONMAN,
                InfraBenefit.INVENTORY_RELIEF, 25));
    }

    @Test
    public void hardcoreRiskExceedsOrdinaryIronRisk()
    {
        assertEquals(Priority.MODERATE, priority(AccountMode.IRONMAN,
                InfraBenefit.RISK_REDUCTION, 10));
        assertEquals(Priority.CRITICAL, priority(AccountMode.HARDCORE_IRONMAN,
                InfraBenefit.RISK_REDUCTION, 10));
    }

    @Test
    public void unknownModeFailsClosedForSelfSourcingOnly()
    {
        assertEquals(Priority.CRITICAL,
                priority(null, InfraBenefit.SELF_SUFFICIENCY));
        assertEquals(Priority.NONE,
                priority(null, InfraBenefit.POH_PLATFORM));
    }

    private Priority priority(AccountMode mode, InfraBenefit benefit,
            int occupied)
    {
        return priority(context(mode, occupied), benefit);
    }

    private static StrategyContext context(AccountMode mode, int occupied)
    {
        List<ItemState> items = new ArrayList<>();
        for (int i = 0; i < occupied; i++)
            items.add(new ItemState(10_000 + i, "Item " + i, 1, i));
        GameData data = GameData.builder(account(mode))
                .inventory(new ItemsState(items)).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(AccountMode mode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Priority", 1L, mode.ordinal(), mode.name(),
                Membership.P2P, 1, 70 * levels.size(), 0L, levels, xp);
    }
}

package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

/** Behavioral coverage for resource scoring after the DTO pipeline was collapsed. */
public class SustainableResourceValueServiceTest
{
    @Test
    public void ownedConsumablesHaveDifferentReplacementCostByMode()
    {
        int main = MethodResourceValueService.resourceAdjustment(
                context(0, bank("Cannonballs", 500)), "Cannonballs", 100, 1, true);
        int iron = MethodResourceValueService.resourceAdjustment(
                context(1, bank("Cannonballs", 500)), "Cannonballs", 100, 1, true);
        assertTrue(main > iron);
    }

    @Test
    public void unopenedBankIsUnknownInsteadOfEmpty()
    {
        assertEquals(-2, MethodResourceValueService.resourceAdjustment(
                context(1, null), "Prayer potion", 4, 4, false));
    }

    @Test
    public void uimNeverCountsConventionalBankAsReadySupply()
    {
        int uim = MethodResourceValueService.resourceAdjustment(
                context(2, bank("Prayer potion", 100)), "Prayer potion", 4, 4, false);
        int main = MethodResourceValueService.resourceAdjustment(
                context(0, bank("Prayer potion", 100)), "Prayer potion", 4, 4, false);
        assertTrue(uim < main);
    }

    @Test
    public void reservedResourceHasHigherOpportunityCost()
    {
        StrategyContext context = context(1, bank("Law rune", 100));
        assertTrue(MethodResourceValueService.resourceAdjustment(
                context, "Law rune", 10, 1, false)
                > MethodResourceValueService.resourceAdjustment(
                context, "Law rune", 10, 7, false));
    }

    private static ItemsState bank(String name, int quantity)
    {
        return new ItemsState(Collections.singletonList(
                new ItemState(1, name, quantity)), 1L);
    }

    private static StrategyContext context(int type, ItemsState bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 70); xp.put(skill, 0); }
        AccountSnapshot account = new AccountSnapshot("Resource", 88L, type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        GameData.Builder data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()));
        if (bank != null) data.bank(bank);
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }
}

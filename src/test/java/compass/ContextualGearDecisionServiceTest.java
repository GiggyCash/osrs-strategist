package compass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

/** Regression coverage for the contextual choices now owned by the provider. */
public class ContextualGearDecisionServiceTest
{
    @Test
    public void recommendationUsesObservedOwnershipAndEncounterContext()
    {
        Recommendation recommendation = candidate(context(1,
                new ItemsState(Arrays.asList(
                        new ItemState(1, "Crystal body", 1)), 1L)));

        assertTrue(recommendation.getReason().contains("Bowfa"));
        assertFalse(recommendation.getReason().contains("universal BIS"));
        assertTrue(recommendation.getGuidance().getSupplies()
                .contains("Crystal body"));
    }

    @Test
    public void unopenedBankDoesNotTurnCompoundSlotProseIntoMissingItems()
    {
        Recommendation recommendation = candidate(context(0, null));

        assertTrue(recommendation.getGuidance().getAction()
                .contains("Open the bank"));
        assertFalse(GearCandidateProvider.isExactOwnershipTarget(
                "Dragon/Avernic defender"));
        assertFalse(GearCandidateProvider.isExactOwnershipTarget(
                "Amulet of torture or rancour"));
    }

    @Test
    public void ironAvailableNowUsesSelfSourceAndMainRequiresEconomics()
    {
        ItemsState observed = new ItemsState(Collections.emptyList(), 1L);

        String iron = candidate(context(1, observed)).getGuidance().getAction();
        String main = candidate(context(0, observed)).getGuidance().getAction();
        assertTrue(iron, iron.contains("self-source"));
        assertTrue(main, main.contains("live price"));
    }

    private static Recommendation candidate(StrategyContext context)
    {
        return new GearCandidateProvider(new GearProgressionCatalog())
                .candidates(context).stream().findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static StrategyContext context(int type, ItemsState bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 80);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Gear", 301L, type,
                AccountMode.fromTypeCode(type).name(), Membership.P2P,
                1, 80 * Skill.values().length, 0L, levels, xp);
        GameData.Builder data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .quests(new QuestSnapshot(Collections.emptyMap()));
        if (bank != null) data.bank(bank);
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.GEAR_TARGET, false, false,
                new PreferenceProfile());
    }
}

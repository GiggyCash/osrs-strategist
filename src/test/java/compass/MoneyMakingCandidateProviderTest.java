package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoneyMakingCandidateProviderTest
{
    private final MoneyMakingCandidateProvider provider =
            new MoneyMakingCandidateProvider(new MoneyMakingCatalog());

    @Test
    public void ironCashPressureProducesOneExecutablePyramidLoop()
    {
        List<Recommendation> candidates = provider.candidates(
                context(1, 60));

        assertEquals(1, candidates.size());
        Recommendation candidate = candidates.get(0);
        assertEquals("money:agility-pyramid", candidate.getId());
        assertEquals(Confidence.VERIFIED,
                candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction().contains("10,000 coins"));
        assertTrue(candidate.getGuidance().getSupplies()
                .contains("four waterskin(4)s"));
        assertTrue(candidate.getGuidance().getLocation().contains("Nardah"));
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(candidate));
    }

    @Test
    public void pyramidStaysHiddenBeforeBreakpointAndForHardcore()
    {
        assertFalse(provider.candidates(context(1, 59)).stream().anyMatch(
                value -> "money:agility-pyramid".equals(value.getId())));
        assertFalse(provider.candidates(context(3, 70)).stream().anyMatch(
                value -> "money:agility-pyramid".equals(value.getId())));
    }

    private static StrategyContext context(int type, int agility)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.AGILITY ? agility : 60;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        AccountSnapshot account = new AccountSnapshot("Money test", type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P,
                1, total, totalXp, levels, xp);
        GameData data = GameData.builder(account)
                .economy(new AccountEconomySnapshot(10_000L, 10_000L,
                        Confidence.VERIFIED))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, false,
                new PreferenceProfile());
    }
}

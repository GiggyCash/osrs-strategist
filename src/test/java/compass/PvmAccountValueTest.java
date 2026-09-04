package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmAccountValueTest
{
    @Test
    public void unrelatedPreparationDoesNotCompeteJustBecauseBossExists()
    {
        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put("pvm:zulrah", preparation("pvm:zulrah"));
        readiness.put("pvm:the_gauntlet", preparation("pvm:the_gauntlet"));

        assertTrue(new PvmCandidateProvider().candidates(context(readiness,
                GoalType.BASE_70S, null)).isEmpty());
    }

    @Test
    public void goalPromotesCuratedPreparationButNotOtherBosses()
    {
        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put("pvm:zulrah", preparation("pvm:zulrah"));
        readiness.put("pvm:the_gauntlet", preparation("pvm:the_gauntlet"));
        java.util.List<Recommendation> result = new PvmCandidateProvider()
                .candidates(context(readiness, GoalType.BOWFA, null));

        assertTrue(contains(result, "Gauntlet"));
        assertFalse(contains(result, "Zulrah"));
    }

    @Test
    public void matchingSlayerTaskMakesBossPreparationRelevant()
    {
        Map<String, PvmReadiness> readiness = Collections.singletonMap(
                "pvm:kraken", preparation("pvm:kraken"));
        SlayerSnapshot task = TestFixtures.slayerSnapshot("Cave krakens", 100,
                "Duradel", 0, Confidence.VERIFIED);
        assertTrue(contains(new PvmCandidateProvider().candidates(
                context(readiness, GoalType.SLAYER_85, task)), "Kraken"));
    }

    private static PvmReadiness preparation(String id)
    {
        return new PvmReadiness(id, false, Confidence.CHECK_NEEDED,
                Collections.singletonList("Equip the required combat setup"));
    }

    private static StrategyContext context(Map<String, PvmReadiness> readiness,
            GoalType goal, SlayerSnapshot slayer)
    {
        GameData data = GameData.builder(account())
                .pvm(new PvmSnapshot(readiness)).slayer(slayer).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, goal,
                false, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 90); xp.put(skill, 0); }
        return new AccountSnapshot("Value", 0L, 0, "Main", Membership.P2P, 1, 2000, 0L, levels, xp);
    }

    private static boolean contains(java.util.List<Recommendation> values,
            String text)
    {
        return values.stream().anyMatch(value -> value.getTitle().contains(text));
    }
}

package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AgilityAccessEvaluatorTest
{
    private final AgilityAccessEvaluator evaluator =
            new AgilityAccessEvaluator(new AgilityCourseCatalog());

    @Test
    public void questStateChangesBestCourseInsteadOfLeavingGenericCheck()
    {
        GameData withoutQuest = data(45, Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Varrock rooftop",
                evaluator.bestStandardCourse(withoutQuest).getDisplayName());

        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Priest in Peril", QuestStatus.COMPLETE);
        GameData withQuest = data(45, quests, Collections.emptyMap());
        assertEquals("Canifis rooftop",
                evaluator.bestStandardCourse(withQuest).getDisplayName());
    }

    @Test
    public void directRegionObservationProvesQuestGatedCourse()
    {
        Map<String, Long> memory = new HashMap<>();
        memory.put("region.13878", 1L);
        GameData data = data(45, Collections.emptyMap(), memory);
        assertEquals("Canifis rooftop",
                evaluator.bestStandardCourse(data).getDisplayName());
    }

    private static GameData data(
            int agility,
            Map<String, QuestStatus> quests,
            Map<String, Long> memory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.AGILITY, agility);
        AccountSnapshot account = new AccountSnapshot("Tester", 0L, 0, "Main", Membership.P2P, 1, agility, 0L, levels, xp);
        return GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .accessMemory(new AccessMemorySnapshot(memory))
                .build();
    }
}

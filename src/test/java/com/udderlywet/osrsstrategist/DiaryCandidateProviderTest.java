package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiaryCandidateProviderTest
{
    @Test
    public void asksForOneObservablePageInsteadOfManualTaskGuessing()
    {
        StrategyCandidate candidate = new DiaryCandidateProvider()
                .candidates(context(snapshot(Collections.emptyMap())))
                .get(0);

        assertTrue(candidate.getId().startsWith("verify:diary:"));
        assertTrue(candidate.getGuidance().getAction()
                .contains("leave it open until Compass refreshes"));
    }

    @Test
    public void selectsObservedIncompleteTaskOnlyWhenRequirementsAreMet()
    {
        DiaryTaskDefinition task = new DiaryTaskCatalog()
                .forTier("Ardougne", DiaryTier.EASY).get(1);
        StrategyCandidate candidate = new DiaryCandidateProvider()
                .candidates(context(snapshot(Collections.singletonMap(
                        task.getId(), false)))).get(0);

        assertEquals(task.getId(), candidate.getId());
        assertEquals(task.getTask(), candidate.getGuidance().getAction());
        assertEquals(RecommendationConfidence.VERIFIED,
                candidate.getConfidence());
    }

    private static DiarySnapshot snapshot(Map<String, Boolean> tasks)
    {
        Map<String, Integer> completed = Collections.singletonMap("Ardougne", 0);
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();
        EnumMap<DiaryTier, Boolean> region = new EnumMap<>(DiaryTier.class);
        region.put(DiaryTier.EASY, false);
        tiers.put("Ardougne", region);
        return new DiarySnapshot(completed, Collections.emptyMap(), tiers, tasks);
    }

    private static StrategyContext context(DiarySnapshot snapshot)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, 13_034_431);
        }
        AccountSnapshot account = new AccountSnapshot("Diary", 1L, 0,
                "NORMAL", MembershipStatus.P2P, 1, 2277, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .diaries(snapshot)
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.DIARY_CAPE, true, false, false,
                new PreferenceProfile());
    }
}

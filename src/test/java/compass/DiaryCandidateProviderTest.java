package compass;

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
        Recommendation candidate = new DiaryCandidateProvider()
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
        Recommendation candidate = new DiaryCandidateProvider()
                .candidates(context(snapshot(Collections.singletonMap(
                        task.getId(), false)))).get(0);

        assertEquals(task.getId(), candidate.getId());
        assertEquals(task.getTask(), candidate.getGuidance().getAction());
        assertEquals(Confidence.VERIFIED,
                candidate.getConfidence());
    }

    @Test
    public void questPointTaskUsesObservedQuestPointTotal()
    {
        DiaryTaskDefinition task = new DiaryTaskCatalog()
                .forTier("Varrock", DiaryTier.MEDIUM).stream()
                .filter(value -> value.getTask().contains("Champions' Guild"))
                .findFirst().orElseThrow(AssertionError::new);
        DiarySnapshot snapshot = snapshot("Varrock", DiaryTier.MEDIUM,
                task.getId());

        Recommendation ready = new DiaryCandidateProvider()
                .candidates(context(snapshot, 32)).get(0);
        assertEquals(task.getId(), ready.getId());
        assertTrue(ready.getGuidance().getSupplies().contains("32 quest points"));

        assertTrue(new DiaryCandidateProvider()
                .candidates(context(snapshot, 31)).isEmpty());
        assertTrue(new DiaryCandidateProvider()
                .candidates(context(snapshot, null)).isEmpty());
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

    private static DiarySnapshot snapshot(String region, DiaryTier tier,
            String incompleteTask)
    {
        Map<String, Integer> completed = Collections.singletonMap(region, 0);
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();
        EnumMap<DiaryTier, Boolean> regionTiers = new EnumMap<>(DiaryTier.class);
        for (DiaryTier value : DiaryTier.values())
            regionTiers.put(value, value.ordinal() < tier.ordinal());
        regionTiers.put(tier, false);
        tiers.put(region, regionTiers);
        return new DiarySnapshot(completed, Collections.emptyMap(), tiers,
                Collections.singletonMap(incompleteTask, false));
    }

    private static StrategyContext context(DiarySnapshot snapshot)
    {
        return context(snapshot, null);
    }

    private static StrategyContext context(DiarySnapshot snapshot,
            Integer questPoints)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, 13_034_431);
        }
        AccountSnapshot account = new AccountSnapshot("Diary", 1L, 0,
                "NORMAL", Membership.P2P, 1, 2277, 0L, levels, xp);
        GameData.Builder builder = GameData.builder(account)
                .diaries(snapshot)
                .quests(new QuestSnapshot(Collections.emptyMap()));
        if (questPoints != null)
            builder.slayer(TestFixtures.slayerSnapshot(null, 0, null, null, 0,
                    null, questPoints, null, null, Confidence.CHECK_NEEDED));
        GameData data = builder.build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.DIARY_CAPE, true, false, false,
                new PreferenceProfile());
    }
}

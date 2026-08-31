package compass;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressHistoryTest
{
    private final Gson gson = new Gson();

    @Test
    public void codecRoundTripsCompactProgressWithoutRawEvents()
    {
        ProgressHistory history = populatedHistory();
        String json = ProgressHistoryCodec.encode(gson, history);
        ProgressHistory decoded = ProgressHistoryCodec.decode(gson, json);

        assertEquals(1, decoded.getSessions().size());
        assertEquals(250L,
                decoded.getSessions().get(0).getTotalXpGained());
        assertEquals(Integer.valueOf(250), decoded.getSessions().get(0)
                .getXpBySkill().get(Skill.FISHING));
        assertEquals(1, decoded.getMilestones().size());
        assertEquals(1, decoded.getBuckets().size());
        assertEquals(1, decoded.getSessions().get(0).getMilestones().size());
        assertFalse(json.contains("rateIntervals"));
    }

    @Test
    public void versionOneHistoryStillLoadsWithoutInventedSessionMilestones()
    {
        ProgressHistory decoded = ProgressHistoryCodec.decode(gson,
                "{\"schemaVersion\":1,\"sessions\":[{"
                        + "\"startedAtMillis\":1,\"endedAtMillis\":2,"
                        + "\"activeDurationMillis\":1,\"totalXpGained\":1,"
                        + "\"levelsGained\":0,\"xpBySkill\":{\"MINING\":1}}],"
                        + "\"milestones\":[],\"buckets\":[]}");

        assertEquals(1, decoded.getSessions().size());
        assertTrue(decoded.getSessions().get(0).getMilestones().isEmpty());
    }

    @Test
    public void persistenceSortsAndMergesDuplicateTimelineEntries()
    {
        ProgressHistory history = new ProgressHistory();
        ProgressSessionSummary duplicate = new ProgressSessionSummary(
                20, 30, 10, 5, 0,
                Collections.singletonMap(Skill.MINING, 5));
        List<ProgressTimeBucket> buckets = new ArrayList<>();
        buckets.add(new ProgressTimeBucket(20,
                Collections.singletonMap(Skill.MINING, 3)));
        buckets.add(new ProgressTimeBucket(10,
                Collections.singletonMap(Skill.FISHING, 2)));
        buckets.add(new ProgressTimeBucket(20,
                Collections.singletonMap(Skill.MINING, 4)));
        ProgressMilestone milestone = new ProgressMilestone("quest:test",
                ProgressMilestoneType.QUEST, "Test complete", null, null, 20);

        history.replaceAll(java.util.Arrays.asList(duplicate, duplicate),
                java.util.Arrays.asList(milestone, milestone), buckets);

        assertEquals(1, history.getSessions().size());
        assertEquals(1, history.getMilestones().size());
        assertEquals(2, history.getBuckets().size());
        assertEquals(10L, history.getBuckets().get(0).getStartedAtMillis());
        assertEquals(7, history.getBuckets().get(1).getTotalXp());
    }

    @Test
    public void checkpointDoesNotCloseOrMutateLiveHistory()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.reset(1L);
        service.record(Skill.MINING, 100, 2, 1L);
        service.record(Skill.MINING, 200, 3, 2L);
        ProgressHistory live = new ProgressHistory();

        ProgressHistory checkpoint = live.checkpoint(service.snapshot(2L));

        assertTrue(live.getSessions().isEmpty());
        assertEquals(1, checkpoint.getSessions().size());
        assertEquals(100L,
                checkpoint.getSessions().get(0).getTotalXpGained());
    }

    @Test
    public void corruptOrUnknownPersistenceFailsClosed()
    {
        assertTrue(ProgressHistoryCodec.decode(gson, "not json")
                .getSessions().isEmpty());
        assertTrue(ProgressHistoryCodec.decode(gson,
                "{\"schemaVersion\":99,\"sessions\":[{}]}")
                .getSessions().isEmpty());
        assertTrue(ProgressHistoryCodec.decode(gson,
                "{\"schemaVersion\":1,\"sessions\":[{"
                        + "\"startedAtMillis\":10,\"endedAtMillis\":1,"
                        + "\"activeDurationMillis\":99,"
                        + "\"totalXpGained\":-5,\"levelsGained\":-1}],"
                        + "\"milestones\":[],\"buckets\":[]}")
                .getSessions().isEmpty());
    }

    @Test
    public void archiveBoundsAllPersistentCollections()
    {
        ProgressHistory history = new ProgressHistory();
        List<ProgressSessionSummary> sessions = new ArrayList<>();
        List<ProgressMilestone> milestones = new ArrayList<>();
        List<ProgressTimeBucket> buckets = new ArrayList<>();
        for (int index = 0; index < 400; index++)
        {
            sessions.add(new ProgressSessionSummary(index, index + 1,
                    1, 1, 0, Collections.singletonMap(Skill.MINING, 1)));
            milestones.add(new ProgressMilestone("m:" + index,
                    ProgressMilestoneType.PLAN_STEP, "Step " + index,
                    null, null, index));
            buckets.add(new ProgressTimeBucket(index,
                    Collections.singletonMap(Skill.MINING, 1)));
        }
        history.replaceAll(sessions, milestones, buckets);

        assertEquals(ProgressHistory.MAX_SESSIONS,
                history.getSessions().size());
        assertEquals(ProgressHistory.MAX_MILESTONES,
                history.getMilestones().size());
        assertEquals(ProgressHistory.MAX_BUCKETS,
                history.getBuckets().size());
        assertEquals(370L, history.getSessions().get(0).getStartedAtMillis());
    }

    @Test
    public void storeUsesActiveRuneLiteProfileAndNeverLeaksAcrossCharacters()
    {
        FakeProfileConfiguration config = new FakeProfileConfiguration();
        AccountProfileStore store = new AccountProfileStore(config, gson);

        config.active = "alice";
        store.saveProgress(populatedHistory());
        config.active = "bob";
        assertTrue(store.loadProgress().getSessions().isEmpty());
        store.saveProgress(new ProgressHistory());
        config.active = "alice";
        assertEquals(1, store.loadProgress().getSessions().size());

        config.active = null;
        store.saveProgress(populatedHistory());
        assertEquals(2, config.values.size());
    }

    private ProgressHistory populatedHistory()
    {
        ProgressAnalyticsService service = new ProgressAnalyticsService();
        service.reset(1_000L);
        service.record(Skill.FISHING, 1_000, 10, 1_000L);
        service.record(Skill.FISHING, 1_250, 12, 2_000L);
        service.recordMilestone(new ProgressMilestone("quest:test",
                ProgressMilestoneType.QUEST, "Test quest complete", null,
                "goal:test", 2_000L));
        ProgressHistory history = new ProgressHistory();
        history.archive(service.snapshot(2_000L));
        return history;
    }

    private static final class FakeProfileConfiguration
            implements AccountProfileStore.ProfileConfiguration
    {
        private final Map<String, String> values = new HashMap<>();
        private String active;

        public String activeProfileKey() { return active; }
        public String get(String group, String key)
        {
            return values.get(active + ":" + group + ":" + key);
        }
        public void set(String group, String key, String value)
        {
            values.put(active + ":" + group + ":" + key, value);
        }
        public void unset(String group, String key)
        {
            values.remove(active + ":" + group + ":" + key);
        }
    }
}

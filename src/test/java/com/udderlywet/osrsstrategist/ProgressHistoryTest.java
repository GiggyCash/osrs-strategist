package com.udderlywet.osrsstrategist;

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
        assertFalse(json.contains("rateIntervals"));
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
        AccountProgressHistoryStore store =
                new AccountProgressHistoryStore(config, gson);

        config.active = "alice";
        store.save(populatedHistory());
        config.active = "bob";
        assertTrue(store.load().getSessions().isEmpty());
        store.save(new ProgressHistory());
        config.active = "alice";
        assertEquals(1, store.load().getSessions().size());

        config.active = null;
        store.save(populatedHistory());
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
            implements AccountProgressHistoryStore.ProfileConfiguration
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

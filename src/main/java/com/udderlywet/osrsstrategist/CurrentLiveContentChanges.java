package com.udderlywet.osrsstrategist;

import java.time.LocalDate;
import java.util.*;

/**
 * Reviewed, recommendation-relevant live changes that are easy to regress.
 * Announced changes remain separate and must never alter runtime planning.
 */
public final class CurrentLiveContentChanges
{
    public enum Status
    {
        LIVE_CURRENT,
        ANNOUNCED_NOT_LIVE,
        UNKNOWN,
        REMOVED_SUPERSEDED
    }

    public static final class Entry
    {
        private final String id;
        private final LocalDate effectiveDate;
        private final Status status;
        private final String behavior;
        private final String source;

        private Entry(String id, LocalDate effectiveDate, Status status,
                String behavior, String source)
        {
            this.id = id;
            this.effectiveDate = effectiveDate;
            this.status = status;
            this.behavior = behavior;
            this.source = source;
        }

        public String getId() { return id; }
        public LocalDate getEffectiveDate() { return effectiveDate; }
        public Status getStatus() { return status; }
        public String getBehavior() { return behavior; }
        public String getSource() { return source; }
    }

    private static final String OFFICIAL = PlayerText.get("CLCC1");
    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry("2026-08-12-sepulchre-floor-4", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC2"), OFFICIAL),
            new Entry("2026-08-12-sepulchre-floor-5", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC3"), OFFICIAL),
            new Entry("2026-08-12-colossal-wyrm-courses", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC4"), OFFICIAL),
            new Entry("2026-08-12-agility-shortcuts", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC5"), OFFICIAL),
            new Entry("2026-08-12-bonfire-tending", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC6"), OFFICIAL),
            new Entry("2026-08-19-birdhouse-nests", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC7"), OFFICIAL),
            new Entry("2026-08-19-birdhouse-xp", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC8"), OFFICIAL),
            new Entry("2026-08-19-hunter-methods", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC9"), OFFICIAL),
            new Entry("2026-08-19-skilling-reward-shops", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    PlayerText.get("CLCC10"), OFFICIAL),
            new Entry("2026-09-02-sweep-up-follow-up", LocalDate.of(2026, 9, 2),
                    Status.ANNOUNCED_NOT_LIVE,
                    PlayerText.get("CLCC11"), OFFICIAL)
    ));

    private CurrentLiveContentChanges() { }

    public static List<Entry> all() { return ENTRIES; }

    public static boolean mayAffectPlanning(String id, LocalDate validationDate)
    {
        for (Entry entry : ENTRIES)
            if (entry.id.equals(id))
                return entry.status == Status.LIVE_CURRENT
                        && !entry.effectiveDate.isAfter(validationDate);
        return false;
    }
}

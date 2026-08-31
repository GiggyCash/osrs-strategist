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

    private static final String OFFICIAL = "Official Old School RuneScape game update";
    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry("2026-08-12-sepulchre-floor-4", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    "Hallowed Sepulchre floor 4 requires 77 Agility and permits boosts.", OFFICIAL),
            new Entry("2026-08-12-sepulchre-floor-5", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    "Hallowed Sepulchre floor 5 requires 87 Agility and permits boosts.", OFFICIAL),
            new Entry("2026-08-12-colossal-wyrm-courses", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    "Colossal Wyrm courses take longer and award proportionally more XP, bone shards and termites, reducing input intensity without a claimed exact new action value.", OFFICIAL),
            new Entry("2026-08-12-agility-shortcuts", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    "New shortcuts and barehanded replacements use their live Agility levels; diary tasks still require grapple use where specified.", OFFICIAL),
            new Entry("2026-08-12-bonfire-tending", LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    "Bonfires support continuous Tend-to interaction and conditional Make-X behavior.", OFFICIAL),
            new Entry("2026-08-19-birdhouse-nests", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    "Birdhouses use five nest rolls with tripled success chance; do not retain old exact yield assumptions.", OFFICIAL),
            new Entry("2026-08-19-birdhouse-xp", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    "Birdhouse XP is reduced by tier except Redwood, so pre-update RuneLite values must be overridden.", OFFICIAL),
            new Entry("2026-08-19-hunter-methods", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    "Falconry, Aerial Fishing, deadfalls, trap limits and Hunter Rumours use the live Sweep-Up behavior.", OFFICIAL),
            new Entry("2026-08-19-skilling-reward-shops", LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    "GOTR and Tempoross offer newly deterministic currency purchases while currency acquisition remains variable.", OFFICIAL),
            new Entry("2026-09-02-sweep-up-follow-up", LocalDate.of(2026, 9, 2),
                    Status.ANNOUNCED_NOT_LIVE,
                    "The remaining announced Sweep-Up changes are excluded until verified live.", OFFICIAL)
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

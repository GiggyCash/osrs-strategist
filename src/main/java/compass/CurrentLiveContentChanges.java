package compass;
import static compass.Text.get;

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
        final String id;
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

    private static final String OFFICIAL = get(189);
    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry(get(1652), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(192), OFFICIAL),
            new Entry(get(1653), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(193), OFFICIAL),
            new Entry(get(1664), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(194), OFFICIAL),
            new Entry(get(1685), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(195), OFFICIAL),
            new Entry(get(1686), LocalDate.of(2026, 8, 12),
                    Status.LIVE_CURRENT,
                    get(196), OFFICIAL),
            new Entry(get(1687), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(197), OFFICIAL),
            new Entry(get(1654), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(198), OFFICIAL),
            new Entry(get(1665), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(199), OFFICIAL),
            new Entry(get(1688), LocalDate.of(2026, 8, 19),
                    Status.LIVE_CURRENT,
                    get(190), OFFICIAL),
            new Entry(get(1689), LocalDate.of(2026, 9, 2),
                    Status.ANNOUNCED_NOT_LIVE,
                    get(191), OFFICIAL)
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

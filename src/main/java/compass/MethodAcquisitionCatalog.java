package compass;

import java.time.LocalDate;
import java.util.*;
import net.runelite.api.Skill;

/** Only reviewed, unrestricted pickups can currently prove a self-source step. */
final class MethodAcquisitionCatalog
{
    enum Kind { GROUND_PICKUP }
    enum Access { UNRESTRICTED }

    static final class Source
    {
        int itemId;
        String name, location, action, source, reviewed;
        long sourceRevision;
        Boolean membersOnly;
        Kind kind;
        Access access;
        RiskLevel risk;
        List<Skill> experienceSkills;
    }

    private final Map<Integer, Source> sources = new HashMap<>();

    MethodAcquisitionCatalog()
    {
        this(BundledCatalogLoader.array("/content/catalogs/method-acquisition.json", Source[].class));
    }

    MethodAcquisitionCatalog(Source[] definitions)
    {
        if (definitions == null) throw new IllegalStateException("Missing acquisition contracts");
        for (Source source : definitions)
        {
            // Other source kinds need their own access, XP/build and resource evaluator.
            if (source == null || source.itemId <= 0 || !text(source.name)
                    || !text(source.location) || !text(source.action) || source.membersOnly == null
                    || source.kind != Kind.GROUND_PICKUP || source.access != Access.UNRESTRICTED
                    || source.risk != RiskLevel.NONE || source.experienceSkills == null
                    || !source.experienceSkills.isEmpty() || source.sourceRevision <= 0
                    || source.source == null || !source.source.startsWith("https://oldschool.runescape.wiki/w/")
                    || source.reviewed == null)
                throw new IllegalStateException("Incomplete or unsupported acquisition contract");
            LocalDate.parse(source.reviewed);
            if (sources.put(source.itemId, source) != null)
                throw new IllegalStateException("Duplicate acquisition contract: " + source.itemId);
        }
    }

    MethodPreparation.Step step(MethodInput need, Membership membership)
    {
        Source source = sources.get(need.itemId);
        if (source == null || !source.name.equalsIgnoreCase(need.name)
                || source.membersOnly && membership != Membership.P2P) return null;
        return new MethodPreparation.Step(MethodPreparation.Kind.ACQUIRE, need, RequirementState.VERIFIED,
                "Collect " + need.quantity + " " + need.name + " at " + source.location + ". "
                        + source.action + " If the spawn is absent, wait for it to reappear; "
                        + "recheck your carried quantity after each pickup. No spawn timing is assumed.");
    }

    private static boolean text(String value) { return value != null && !value.trim().isEmpty(); }
}

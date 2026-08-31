package compass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Hiscore-backed encounter census with an explicit readiness disposition. */
@Singleton
public class PvmCoverageManifest
{
    public static final String PROVENANCE = "RuneLite HiscoreSkill BOSS enum 1.12.35; audited 2026-08-18";
    private final List<ContentCoverageEntry> entries;

    public PvmCoverageManifest()
    {
        PvmActivityCatalog activities = new PvmActivityCatalog();
        PvmEvidenceProfileCatalog evidence = new PvmEvidenceProfileCatalog();
        PvmPreparationProfileCatalog preparation = new PvmPreparationProfileCatalog();
        List<ContentCoverageEntry> values = new ArrayList<>();
        for (PvmActivityDefinition activity : activities.all())
        {
            boolean verified = evidence.forActivity(activity.getId()) != null;
            boolean partial = activities.hasCuratedReadinessProfile(activity.getId())
                    || preparation.forActivity(activity.getId()) != null;
            ContentCoverageState state = verified ? ContentCoverageState.STRUCTURED
                    : partial ? ContentCoverageState.PARTIAL_PREPARATION
                    : ContentCoverageState.CONSERVATIVE_FAIL_CLOSED;
            String reason = verified
                    ? "All required locally modeled readiness evidence must pass before this encounter can lead."
                    : partial
                    ? "A conservative readiness floor can produce preparation, but incomplete encounter evidence cannot become VERIFIED."
                    : "The encounter identity and risk class are known, but no complete local readiness model exists; it remains fail-closed.";
            values.add(new ContentCoverageEntry(activity.getId(), activity.getName(),
                    state, reason, PROVENANCE));
        }
        entries = Collections.unmodifiableList(values);
    }

    public List<ContentCoverageEntry> all() { return entries; }
}

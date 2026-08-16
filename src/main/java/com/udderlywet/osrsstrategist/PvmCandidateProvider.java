package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Makes explicitly verified/realistic PvM assessments eligible for DO NEXT. */
@Singleton
public class PvmCandidateProvider implements StrategyCandidateProvider
{
    private final PvmActivityCatalog catalog;

    @Inject
    public PvmCandidateProvider(PvmActivityCatalog catalog)
    {
        this.catalog = catalog;
    }

    public PvmCandidateProvider()
    {
        this(new PvmActivityCatalog());
    }

    @Override
    public String getId()
    {
        return "pvm-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getPvm() == null)
        {
            return result;
        }

        AccountMode mode = context.getAccountMode();
        PreferenceProfile preferences = context.getPreferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.getData().getPvm().getReadinessByActivity().entrySet())
        {
            PvmReadiness readiness = entry.getValue();
            if (readiness == null || !readiness.isRealisticallyReady()) continue;

            PvmActivityDefinition definition = catalog.match(entry.getKey());
            if (definition != null)
            {
                if (definition.isWilderness() && !context.isAllowWildernessMethods()) continue;
                if ((mode == AccountMode.HARDCORE_IRONMAN
                        || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                        && !definition.isHardcoreSafeByDefault())
                {
                    continue;
                }
            }
            else if (mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
            {
                // Unknown PvM is not safe enough to recommend to a Hardcore.
                continue;
            }

            String normalizedKey = entry.getKey().startsWith("pvm:")
                    ? entry.getKey().substring(4) : entry.getKey();
            String id = "pvm:" + normalizedKey;
            if (preferences.isOnCooldown(id)) continue;
            double score = 48.0 + preferences.weightFor(id) * 10.0;
            if (definition != null)
            {
                if (definition.isRaid()) score += 4.0;
                if (definition.getRiskLevel() == RiskLevel.HIGH
                        && AccountModePolicy.isRiskSensitive(mode)) score -= 8.0;
            }

            String title = definition == null ? entry.getKey() : definition.getName();
            result.add(new StrategyCandidate(
                    id,
                    "Do " + title,
                    "Combat stats, equipment, supplies, access, and the activity-specific readiness assessment say this is realistically ready.",
                    score,
                    readiness.getConfidence()
            ));
        }
        return result;
    }
}

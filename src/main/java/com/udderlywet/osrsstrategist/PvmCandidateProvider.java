package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/** Makes verified or near-ready PvM assessments eligible for DO NEXT. */
@Singleton
public class PvmCandidateProvider implements StrategyCandidateProvider
{
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

        PreferenceProfile preferences = context.getPreferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.getData().getPvm().getReadinessByActivity().entrySet())
        {
            PvmReadiness readiness = entry.getValue();
            if (readiness == null) continue;

            String id = "pvm:" + entry.getKey();
            if (preferences.isOnCooldown(id)) continue;

            if (readiness.isRealisticallyReady())
            {
                double score = 48.0 + preferences.weightFor(id) * 10.0;
                result.add(new StrategyCandidate(
                        id,
                        "Do " + entry.getKey(),
                        "Stats, access, gear and supplies have passed the active readiness checks for this PvM activity.",
                        score,
                        readiness.getConfidence()));
                continue;
            }

            // If the only remaining issue is the final loadout/mechanics proof,
            // surface preparation instead of pretending the boss is ready.
            if (readiness.getMissingRequirements().size() == 1
                    && readiness.getMissingRequirements().get(0)
                    .startsWith("Verify practical gear"))
            {
                double score = 22.0 + preferences.weightFor(id) * 10.0;
                result.add(new StrategyCandidate(
                        id,
                        "Prepare for " + entry.getKey(),
                        readiness.getMissingRequirements().get(0),
                        score,
                        RecommendationConfidence.CHECK_NEEDED));
            }
        }
        return result;
    }
}

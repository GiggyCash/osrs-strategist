package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/** Makes explicitly verified/realistic PvM assessments eligible for DO NEXT. */
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
            if (readiness == null || !readiness.isRealisticallyReady()) continue;

            String id = "pvm:" + entry.getKey();
            if (preferences.isOnCooldown(id)) continue;
            double score = 48.0 + preferences.weightFor(id) * 10.0;

            result.add(new StrategyCandidate(
                    id,
                    "Do " + entry.getKey(),
                    "The account has an explicit realistic-readiness assessment for this PvM activity.",
                    score,
                    readiness.getConfidence()
            ));
        }
        return result;
    }
}

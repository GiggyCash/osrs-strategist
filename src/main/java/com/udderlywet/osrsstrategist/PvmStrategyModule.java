package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Surfaces PvM content only when a readiness analyzer has produced an explicit
 * assessment. This prevents the generic skill engine from blindly recommending
 * bosses based on combat level alone.
 */
@Singleton
public class PvmStrategyModule implements StrategyModule
{
    @Override
    public String getId()
    {
        return "pvm";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        StrategyDataBundle data = context.getData();

        if (data == null || data.getPvm() == null)
        {
            return signals;
        }

        for (Map.Entry<String, PvmReadiness> entry
                : data.getPvm().getReadinessByActivity().entrySet())
        {
            PvmReadiness readiness = entry.getValue();

            if (readiness == null || !readiness.isRealisticallyReady())
            {
                continue;
            }

            signals.add(
                    new StrategySignal(
                            "pvm:" + entry.getKey(),
                            StrategySignalCategory.PVM,
                            "PvM ready: " + entry.getKey(),
                            5.0,
                            readiness.getConfidence()
                    )
            );
        }

        return signals;
    }
}

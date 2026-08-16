package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Connects the remaining typed account snapshots to the shared reasoning bus.
 * Exact recommendations still require verified game-data definitions, but no
 * major progression domain needs to bypass StrategyEngine as coverage grows.
 */
@Singleton
public class AccountSystemsStrategyModule implements StrategyModule
{
    @Override
    public String getId()
    {
        return "account-systems";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        if (context == null || context.getData() == null)
        {
            return signals;
        }

        StrategyDataBundle data = context.getData();

        SlayerSnapshot slayer = data.getSlayer();
        if (slayer != null && slayer.hasTask())
        {
            signals.add(signal(
                    "systems:slayer-task",
                    StrategySignalCategory.SLAYER,
                    "Active Slayer task observed: " + slayer.getTaskName()
                            + " (" + slayer.getRemaining() + " remaining)",
                    2.0,
                    slayer.getConfidence()
            ));
        }

        SailingSnapshot sailing = data.getSailing();
        if (sailing != null
                && (!sailing.getVerifiedPorts().isEmpty()
                || !sailing.getVerifiedActivities().isEmpty()))
        {
            signals.add(signal(
                    "systems:sailing",
                    StrategySignalCategory.SAILING,
                    "Sailing state observed: "
                            + sailing.getVerifiedPorts().size() + " ports and "
                            + sailing.getVerifiedActivities().size()
                            + " activities verified",
                    1.0,
                    sailing.getConfidence()
            ));
        }

        MinigameSnapshot minigames = data.getMinigames();
        if (minigames != null && !minigames.getUnlocked().isEmpty())
        {
            signals.add(signal(
                    "systems:minigames",
                    StrategySignalCategory.MINIGAME,
                    "Observed minigame unlocks: "
                            + minigames.getUnlocked().size(),
                    1.0,
                    RecommendationConfidence.VERIFIED
            ));
        }

        TransportSnapshot transport = data.getTransport();
        if (transport != null && !transport.getVerifiedRoutes().isEmpty())
        {
            signals.add(signal(
                    "systems:transport",
                    StrategySignalCategory.TRANSPORT,
                    "Verified transport routes: "
                            + transport.getVerifiedRoutes().size(),
                    1.0,
                    RecommendationConfidence.VERIFIED
            ));
        }

        PohSnapshot poh = data.getPoh();
        if (poh != null && poh.getHouseAccess() == CapabilityState.VERIFIED)
        {
            signals.add(signal(
                    "systems:poh",
                    StrategySignalCategory.POH,
                    "Player-owned house access verified",
                    1.0,
                    RecommendationConfidence.VERIFIED
            ));
        }

        AccountEconomySnapshot economy = data.getEconomy();
        if (economy != null)
        {
            signals.add(signal(
                    "systems:economy",
                    StrategySignalCategory.ECONOMY,
                    "Economy snapshot available for affordability and opportunity-cost planning",
                    0.0,
                    economy.getConfidence()
            ));
        }

        return signals;
    }

    private static StrategySignal signal(
            String id,
            StrategySignalCategory category,
            String summary,
            double score,
            RecommendationConfidence confidence)
    {
        return new StrategySignal(id, category, summary, score, confidence);
    }
}

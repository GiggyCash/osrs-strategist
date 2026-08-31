package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Converts a verified clue observation into a strategy signal.
 *
 * <p>The signal grows slowly with clue age. Membership eligibility is checked
 * here as well as on the visible clue surfaces so an unavailable clue cannot
 * indirectly boost another recommendation while the character is F2P.</p>
 */
@Singleton
public class ClueStrategyModule implements StrategyModule
{
    @Override
    public String getId()
    {
        return "clue";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        StrategyDataBundle data = context.getData();

        if (data == null || data.getClue() == null)
        {
            return signals;
        }

        ClueSnapshot clue = data.getClue();

        if (!clue.isCluePresent())
        {
            return signals;
        }

        ClueTier tier = ClueTier.fromText(clue.getClueType());
        AccountSnapshot account = data.getAccount();
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.getMembershipStatus();
        if (!tier.isAvailableFor(membership))
        {
            return signals;
        }

        long ageMillis = Math.max(
                0L,
                System.currentTimeMillis()
                        - clue.getFirstSeenAtMillis()
        );

        double ageHours = ageMillis / 3_600_000.0;
        double score = Math.min(8.0, 2.0 + ageHours * 0.25);

        signals.add(
                new StrategySignal(
                        "clue:pending",
                        StrategySignalCategory.CLUE,
                        "Pending "
                                + (clue.getClueType() == null
                                ? "clue"
                                : clue.getClueType() + " clue"),
                        score,
                        clue.getConfidence()
                )
        );

        return signals;
    }
}

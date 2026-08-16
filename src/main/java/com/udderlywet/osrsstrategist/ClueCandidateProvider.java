package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/** Lets an observed clue become actual DO NEXT work without making it spammy. */
@Singleton
public class ClueCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "clue-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null) return result;
        ClueSnapshot clue = context.getData().getClue();
        if (clue == null || !clue.isCluePresent()) return result;

        ClueTier tier = ClueTier.fromText(clue.getClueType());
        String id = "clue:pending:" + tier.name().toLowerCase();
        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences.isOnCooldown(id)
                || preferences.isOnCooldown("clue:pending")) return result;

        long age = Math.max(0L,
                System.currentTimeMillis() - clue.getFirstSeenAtMillis());
        double ageHours = age / 3_600_000.0;
        double score = 39.0
                + tier.getPriorityBonus()
                + Math.min(15.0, ageHours * 0.5)
                + preferences.weightFor(id) * 10.0;

        if (context.isCollectionistMode()) score += 6.0;
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN) score += 2.0;

        String type = tier == ClueTier.UNKNOWN
                ? "clue"
                : tier.name().toLowerCase() + " clue";
        StringBuilder reason = new StringBuilder();
        reason.append("Clears the pending ").append(type)
                .append(" slot and can advance Collection Log progress. ")
                .append("Before starting, Strategist should check the current step, required equipment, spade, teleports, food/combat needs, and any observed STASH state.");
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            reason.append(" UIM routing also checks inventory pressure and only counts STASH/POH/other storage when the relevant capability and contents are verified.");
        }
        if (context.getAccountMode() == AccountMode.HARDCORE_IRONMAN
                || context.getAccountMode() == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            reason.append(" Hardcore accounts must verify the clue step is not a Wilderness or otherwise unsafe step before it can become Ready.");
        }

        result.add(new StrategyCandidate(
                id,
                "Complete " + type,
                reason.toString(),
                score,
                RecommendationConfidence.CHECK_NEEDED
        ));
        return result;
    }
}

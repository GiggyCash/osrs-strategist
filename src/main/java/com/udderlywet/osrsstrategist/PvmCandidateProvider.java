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
    public String getId() { return "pvm-candidates"; }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getPvm() == null) return result;

        AccountMode mode = context.getAccountMode();
        AccountSnapshot account = context.getData().getAccount();
        PreferenceProfile preferences = context.getPreferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.getData().getPvm().getReadinessByActivity().entrySet())
        {
            PvmReadiness readiness = entry.getValue();
            if (readiness == null) continue;
            if (readiness.getConfidence() == RecommendationConfidence.BLOCKED) continue;

            PvmActivityDefinition definition = catalog.match(entry.getKey());
            if (definition != null)
            {
                if (account == null || !ContentAccessRules.isContentAvailable(
                        account.getMembershipStatus(), definition.isFreeToPlay())) continue;
                if (definition.isWilderness() && !context.isAllowWildernessMethods()) continue;
                if ((mode == AccountMode.HARDCORE_IRONMAN
                        || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                        && !definition.isHardcoreSafeByDefault()) continue;
            }
            else continue; // Unknown/future metadata cannot prove beta readiness.

            String normalizedKey = entry.getKey().startsWith("pvm:")
                    ? entry.getKey().substring(4) : entry.getKey();
            String id = "pvm:" + normalizedKey;
            if (preferences.isOnCooldown(id)) continue;
            boolean ready = readiness.isReadyForRecommendation();
            boolean relevant = progressionRelevant(definition, context);
            // A generic hiscore identity is not a reason to boss. Preparation
            // competes globally only when a goal/task makes the encounter
            // relevant and a curated readiness floor can name concrete work.
            if (!ready && (!catalog.hasCuratedReadinessProfile(definition.getId())
                    || !relevant)) continue;

            double score = (ready ? 48.0 : 32.0)
                    + preferences.weightFor(id) * 10.0;
            if (relevant) score += 14.0;
            if (definition != null)
            {
                if (definition.isRaid()) score += 4.0;
                if (definition.getRiskLevel() == RiskLevel.HIGH
                        && AccountModePolicy.isRiskSensitive(mode)) score -= 8.0;
            }

            String title = definition == null ? entry.getKey() : definition.getName();
            String missing = readiness.getMissingRequirements().isEmpty()
                    ? "" : String.join("; ", readiness.getMissingRequirements());
            if (!ready && missing.trim().isEmpty()) continue;
            RecommendationGuidance guidance = ready
                    ? new RecommendationGuidance(
                            "Attempt " + title + " using the currently equipped and carried setup.",
                            "Your equipped weapon, loadout, and minimum carried supplies are ready.",
                            "Use only the verified non-Wilderness access route for this encounter.",
                            "This is conservative readiness, not a universal BIS claim. Stop and reassess if the live setup changes.")
                    : new RecommendationGuidance(
                            "Prepare the missing PvM evidence before attempting " + title + ": " + missing + ".",
                            missing,
                            "Verify the encounter access route and prepare outside the encounter.",
                            "Finish this preparation before attempting the encounter.");
            result.add(new StrategyCandidate(
                    id,
                    "Do " + title,
                    ready
                            ? "Observed equipped gear, carried supplies, access and conservative activity checks are ready."
                            : "The encounter is not ready; complete the listed preparation before attempting it.",
                    score,
                    ready ? RecommendationConfidence.VERIFIED
                            : RecommendationConfidence.CHECK_NEEDED,
                    guidance,
                    CandidateSafetyEvidence.potentiallyIrreversible(
                            definition.isFreeToPlay())
            ));
        }
        return result;
    }

    private static boolean progressionRelevant(PvmActivityDefinition definition,
            StrategyContext context)
    {
        if (definition == null || context == null) return false;
        String id = definition.getId();
        GoalType goal = context.getActiveGoal();
        if (goal == GoalType.BOWFA)
            return id.contains("gauntlet");
        if (goal == GoalType.INFERNAL_CAPE)
            return id.endsWith("tztok_jad") || id.endsWith("tzkal_zuk");
        if (goal == GoalType.RAID_READY)
            return definition.isRaid();
        if (goal == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
            return catalogChallengeEncounter(id);

        SlayerSnapshot slayer = context.getData() == null
                ? null : context.getData().getSlayer();
        if (slayer == null || !slayer.hasTask()) return false;
        String task = normalize(slayer.getTaskName());
        String boss = normalize(definition.getName());
        return boss.contains(task) || task.contains(boss)
                || (id.endsWith("kraken") && task.contains("kraken"))
                || (id.endsWith("cerberus") && task.contains("hellhound"))
                || (id.endsWith("alchemical_hydra") && task.contains("hydra"))
                || (id.endsWith("araxxor") && task.contains("araxyte"));
    }

    private static boolean catalogChallengeEncounter(String id)
    {
        return id != null && (id.contains("gauntlet") || id.contains("raid")
                || id.contains("tombs_of_amascut") || id.contains("chambers_of_xeric")
                || id.contains("theatre_of_blood") || id.endsWith("tzkal_zuk")
                || id.endsWith("sol_heredit") || id.endsWith("nex"));
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}

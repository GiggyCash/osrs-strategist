package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Makes explicitly verified/realistic PvM assessments eligible for DO NEXT. */
@Singleton
public class PvmCandidateProvider implements CandidateProvider
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
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().pvm() == null) return result;

        AccountMode mode = context.accountMode();
        AccountSnapshot account = context.data().account();
        PreferenceProfile preferences = context.preferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.data().pvm().getReadinessByActivity().entrySet())
        {
            PvmReadiness readiness = entry.getValue();
            if (readiness == null) continue;
            if (readiness.getConfidence() == Confidence.BLOCKED) continue;

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
            Guidance guidance = ready
                    ? readyGuidance(definition, title)
                    : new Guidance(
                            Text.get(417) + title + ": " + missing + ".",
                            missing,
                            Text.get(428),
                            Text.get(433));
            result.add(new Recommendation(
                    id,
                    "Do " + title,
                    ready
                            ? Text.get(434)
                            : Text.get(435),
                    score,
                    ready ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    guidance,
                    SafetyEvidence.potentiallyIrreversible(
                            definition.isFreeToPlay())
            ));
        }
        return result;
    }

    private static Guidance readyGuidance(
            PvmActivityDefinition definition, String title)
    {
        if (definition != null && "pvm:tztok_jad".equals(definition.getId()))
        {
            return new Guidance(
                    Text.get(436),
                    Text.get(437),
                    Text.get(438),
                    Text.get(439));
        }
        if (definition != null && "pvm:obor".equals(definition.getId()))
            return simpleReadyGuidance(title,
                    Text.get(418),
                    Text.get(419),
                    Text.get(420));
        if (definition != null && "pvm:bryophyta".equals(definition.getId()))
            return simpleReadyGuidance(title,
                    Text.get(421),
                    Text.get(422),
                    Text.get(423));
        if (definition != null && "pvm:scurrius".equals(definition.getId()))
            return simpleReadyGuidance(title,
                    Text.get(424),
                    Text.get(1338),
                    Text.get(425));
        return new Guidance(
                "Attempt " + title + Text.get(426),
                Text.get(427),
                Text.get(429),
                Text.get(430));
    }

    private static Guidance simpleReadyGuidance(String title,
            String action, String location, String supplies)
    {
        return new Guidance(action, supplies, location,
                Text.get(431)
                        + title + Text.get(432));
    }

    private static boolean progressionRelevant(PvmActivityDefinition definition,
            StrategyContext context)
    {
        if (definition == null || context == null) return false;
        String id = definition.getId();
        GoalType goal = context.getActiveGoal();
        if (goal == GoalType.BOWFA)
            return id.contains("gauntlet");
        if (goal == GoalType.FIRE_CAPE)
            return id.endsWith("tztok_jad");
        if (goal == GoalType.INFERNAL_CAPE)
            return id.endsWith("tztok_jad") || id.endsWith("tzkal_zuk");
        if (goal == GoalType.RAID_READY)
            return definition.isRaid();
        if (goal == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
            return catalogChallengeEncounter(id);

        SlayerSnapshot slayer = context.data() == null
                ? null : context.data().slayer();
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

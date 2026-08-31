package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class RecommendationEngine
{
    private final TrainingMethodSelector trainingMethodSelector;
    private final RecommendationGuidanceService guidanceService;
    private final CombatGuidanceService combatGuidanceService;
    private final SlayerGuidanceService slayerGuidanceService;
    private final SailingGuidanceService sailingGuidanceService;
    private final SkillBreakpointService breakpointService;
    private final AdaptiveActionSelector actionResolver;

    @Inject
    public RecommendationEngine(
            TrainingMethodSelector trainingMethodSelector,
            RecommendationGuidanceService guidanceService,
            CombatGuidanceService combatGuidanceService,
            SlayerGuidanceService slayerGuidanceService,
            SailingGuidanceService sailingGuidanceService,
            SkillBreakpointService breakpointService,
            AdaptiveActionSelector actionResolver)
    {
        this.trainingMethodSelector = trainingMethodSelector;
        this.guidanceService = guidanceService;
        this.combatGuidanceService = combatGuidanceService;
        this.slayerGuidanceService = slayerGuidanceService;
        this.sailingGuidanceService = sailingGuidanceService;
        this.breakpointService = breakpointService == null
                ? new SkillBreakpointService() : breakpointService;
        this.actionResolver = actionResolver == null
                ? new AdaptiveActionSelector() : actionResolver;
    }

    public RecommendationEngine(
            TrainingMethodSelector trainingMethodSelector,
            RecommendationGuidanceService guidanceService)
    {
        this(trainingMethodSelector, guidanceService,
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    public RecommendationEngine(TrainingMethodSelector trainingMethodSelector)
    {
        this(trainingMethodSelector, new RecommendationGuidanceService(),
                new CombatGuidanceService(), new SlayerGuidanceService(),
                new SailingGuidanceService(), new SkillBreakpointService(),
                new AdaptiveActionSelector());
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            PreferenceProfile preferenceProfile)
    {
        return recommend(GameData.builder(snapshot).build(),
                strategyMode, SessionIntent.PICK_FOR_ME, true, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            AccountSnapshot snapshot,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(GameData.builder(snapshot).build(),
                strategyMode, sessionIntent, true, false, preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            PreferenceProfile preferenceProfile)
    {
        return recommend(data, strategyMode, sessionIntent, true, false,
                preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return recommend(data, strategyMode, sessionIntent, true,
                allowWildernessMethods, preferenceProfile);
    }

    public List<Recommendation> recommend(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return topThree(recommendAll(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, GoalType.AUTOMATIC,
                preferenceProfile));
    }

    /** Full skill candidate pool for the global strategy queue. Do not trim here. */
    public List<Recommendation> recommendAll(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            PreferenceProfile preferenceProfile)
    {
        return recommendAllInternal(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, GoalType.AUTOMATIC,
                preferenceProfile);
    }

    public List<Recommendation> recommendAll(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            GoalType activeGoal,
            PreferenceProfile preferenceProfile)
    {
        // Focused queue tests historically override the public six-argument
        // method with a synthetic pool. Preserve that extension seam when no
        // production selector exists instead of entering the concrete skill
        // generator with a null dependency.
        if (trainingMethodSelector == null)
            return recommendAll(data, strategyMode, sessionIntent,
                    useGroupStorage, allowWildernessMethods,
                    preferenceProfile);
        return recommendAllInternal(data, strategyMode, sessionIntent,
                useGroupStorage, allowWildernessMethods, activeGoal,
                preferenceProfile);
    }

    private List<Recommendation> recommendAllInternal(
            GameData data,
            StrategyMode strategyMode,
            SessionIntent sessionIntent,
            boolean useGroupStorage,
            boolean allowWildernessMethods,
            GoalType activeGoal,
            PreferenceProfile preferenceProfile)
    {
        List<Recommendation> recommendations = new ArrayList<>();
        if (trainingMethodSelector == null || data == null
                || data.account() == null) return recommendations;
        var snapshot = data.account();
        PreferenceProfile safePreferences = preferenceProfile == null
                ? new PreferenceProfile() : preferenceProfile;
        StrategyContext context = new StrategyContext(data, strategyMode,
                sessionIntent, QuestTolerance.NORMAL, activeGoal,
                useGroupStorage, false, allowWildernessMethods,
                safePreferences);

        for (Skill skill : Skill.values())
        {
            var level = snapshot.getSkillLevel(skill);
            if (level >= 99 || skill == Skill.HITPOINTS) continue;
            if (!ContentAccessRules.isSkillAvailable(skill,
                    snapshot.getMembershipStatus())) continue;
            if (!AccountBuildPolicy.allowsSkill(snapshot, skill)) continue;

            var activityId = "skill:" + skill.name().toLowerCase();
            if (safePreferences.isOnCooldown(activityId)) continue;

            SkillBreakpoint breakpoint = breakpointService.next(
                    skill, level, context);
            var target = breakpoint.getLevel();
            TrainingPlan trainingPlan = null;
            Guidance guidance = null;
            TrainingPlan highestRankedPlan = null;
            for (TrainingPlan candidate : trainingMethodSelector.rankedCandidates(
                    data, skill, level, strategyMode, sessionIntent,
                    allowWildernessMethods, useGroupStorage))
            {
                if (highestRankedPlan == null) highestRankedPlan = candidate;
                Guidance candidateGuidance = buildGuidance(
                        data, skill, level,
                        actionResolver.resolve(candidate, level, target),
                        candidate, sessionIntent,
                        useGroupStorage);
                if (candidateGuidance != null
                        && candidate.getStrategyProfile() != null)
                {
                    candidateGuidance = candidateGuidance.withBankingBehavior(
                            candidate.getStrategyProfile()
                                    .getBankingBehavior());
                }
                // Some activities can only be rendered truthfully once live
                // resources or state identify a concrete execution loop. A
                // higher-scoring but unrenderable route must not consume the
                // skill's only candidate and hide a ready lower-ranked route.
                if (candidateGuidance == null) continue;
                trainingPlan = candidate.withCurrentStageTargetLevel(
                        actionResolver.resolve(candidate, level, target));
                guidance = candidateGuidance;
                break;
            }
            // Keep the historical diagnostic candidate when this engine was
            // constructed without a renderer capable of any method in the
            // skill. The final actionability boundary still prevents it from
            // leading DO NEXT; this also preserves focused selector callers.
            if (trainingPlan == null) trainingPlan = highestRankedPlan;
            if (trainingPlan == null || trainingPlan.getMethod() == null) continue;

            var score = baseScore(level, breakpoint);
            score += safePreferences.weightFor(activityId) * 10.0;
            score += safePreferences.timedScoreAdjustmentFor(activityId);
            score += milestoneMomentum(level, target);

            var primaryReason = trainingPlan.getWhyThisMethod();
            if (primaryReason == null || primaryReason.trim().isEmpty())
                primaryReason = breakpoint.getLabel() + ".";
            Recommendation recommendation = new Recommendation(
                    activityId,
                    "Train " + skill.getName() + " to " + target,
                    primaryReason,
                    score,
                    trainingPlan,
                    trainingPlan.getConfidence(),
                    level,
                    target,
                    guidance,
                    SafetyEvidence.skill(
                            ContentAccessRules.isMethodAvailable(
                                    trainingPlan.getMethod(), MembershipStatus.F2P),
                            skill));
            recommendation = recommendation.withStrategicValue(
                    StrategicValue.builder()
                            .unlockValue(breakpoint.strategicValue())
                            .evidence(breakpoint.getEvidenceId())
                            .build());
            recommendations.add(recommendation);
        }

        recommendations.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        return recommendations;
    }

    private Guidance buildGuidance(
            GameData data,
            Skill skill,
            int level,
            int target,
            TrainingPlan trainingPlan,
            SessionIntent sessionIntent,
            boolean useGroupStorage)
    {
        Guidance guidance = combatGuidanceService == null
                ? null : combatGuidanceService.build(
                        data, skill, level, target, trainingPlan,
                        sessionIntent, useGroupStorage);

        if (guidance == null && skill == Skill.SLAYER
                && slayerGuidanceService != null)
        {
            guidance = slayerGuidanceService.build(
                    data, level, target, useGroupStorage);
        }

        if (guidance == null && skill == Skill.SAILING
                && sailingGuidanceService != null)
        {
            guidance = sailingGuidanceService.build(
                    data, level, target, trainingPlan);
        }

        if (guidance == null && guidanceService != null)
        {
            guidance = guidanceService.build(
                    data, skill, level, target, trainingPlan,
                    useGroupStorage);
        }
        return guidance;
    }

    private static List<Recommendation> topThree(
            List<Recommendation> recommendations)
    {
        if (recommendations == null || recommendations.isEmpty())
            return new ArrayList<>();
        if (recommendations.size() <= 3)
            return new ArrayList<>(recommendations);
        return new ArrayList<>(recommendations.subList(0, 3));
    }

    private double baseScore(int level, SkillBreakpoint breakpoint)
    {
        var distance = Math.max(1, breakpoint.getLevel() - level);
        double proximity = distance <= 1 ? 12.0
                : distance <= 3 ? 7.0 : distance <= 7 ? 3.0 : 0.0;
        return 24.0 + proximity + breakpoint.strategicValue() * 20.0;
    }

    private double milestoneMomentum(int level, int target)
    {
        var remaining = target - level;
        if (remaining <= 1) return 8.0;
        if (remaining <= 3) return 4.0;
        return 0.0;
    }

}

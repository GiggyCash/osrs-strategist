package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class TrainingMethodSelector
{
    private final TrainingMethodDatabase database;
    private final RequirementEvidenceEngine requirementEvidenceEngine;
    private final ExpandedTrainingMethodCatalog expandedCatalog;
    private final F2pBaselineMethodCatalog f2pBaselineCatalog;
    private final TrainingMethodPolicy methodPolicy;
    private final MethodStrategyKnowledgeCatalog strategyCatalog;
    private final MethodStrategyService strategyService;

    @Inject
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine,
            ExpandedTrainingMethodCatalog expandedCatalog,
            F2pBaselineMethodCatalog f2pBaselineCatalog,
            TrainingMethodPolicy methodPolicy,
            MethodStrategyKnowledgeCatalog strategyCatalog,
            MethodStrategyService strategyService)
    {
        this.database = database;
        this.requirementEvidenceEngine = requirementEvidenceEngine;
        this.expandedCatalog = expandedCatalog;
        this.f2pBaselineCatalog = f2pBaselineCatalog;
        this.methodPolicy = methodPolicy;
        this.strategyCatalog = strategyCatalog == null
                ? new MethodStrategyKnowledgeCatalog() : strategyCatalog;
        this.strategyService = strategyService == null
                ? new MethodStrategyService() : strategyService;
    }

    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine,
            ExpandedTrainingMethodCatalog expandedCatalog,
            F2pBaselineMethodCatalog f2pBaselineCatalog,
            TrainingMethodPolicy methodPolicy)
    {
        this(database, requirementEvidenceEngine, expandedCatalog,
                f2pBaselineCatalog, methodPolicy,
                new MethodStrategyKnowledgeCatalog(),
                new MethodStrategyService());
    }

    /** Compatibility constructor keeps focused legacy-selector tests isolated. */
    public TrainingMethodSelector(
            TrainingMethodDatabase database,
            RequirementEvidenceEngine requirementEvidenceEngine)
    {
        this(database, requirementEvidenceEngine, null, null,
                new TrainingMethodPolicy(),
                new MethodStrategyKnowledgeCatalog(),
                new MethodStrategyService());
    }

    public TrainingMethodSelector(TrainingMethodDatabase database)
    {
        this(database, null);
    }

    public TrainingPlan select(Skill skill, int currentLevel,
            StrategyMode strategyMode, SessionIntent sessionIntent)
    {
        return select(null, skill, currentLevel, strategyMode, sessionIntent, false);
    }

    public TrainingPlan select(GameData data, Skill skill, int currentLevel,
            StrategyMode strategyMode, SessionIntent sessionIntent)
    {
        return select(data, skill, currentLevel, strategyMode, sessionIntent, false);
    }

    public TrainingPlan select(GameData data, Skill skill, int currentLevel,
            StrategyMode strategyMode, SessionIntent sessionIntent,
            boolean allowWildernessMethods)
    {
        return select(data, skill, currentLevel, strategyMode, sessionIntent,
                allowWildernessMethods, false);
    }

    public TrainingPlan select(GameData data, Skill skill, int currentLevel,
            StrategyMode strategyMode, SessionIntent sessionIntent,
            boolean allowWildernessMethods, boolean useGroupStorage)
    {
        List<TrainingPlan> ranked = rankedCandidates(data, skill, currentLevel,
                strategyMode, sessionIntent, allowWildernessMethods,
                useGroupStorage);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    /** Ranked legal methods, exposed package-wide for decision-tournament tests. */
    List<TrainingPlan> rankedCandidates(GameData data, Skill skill,
            int currentLevel, StrategyMode strategyMode,
            SessionIntent sessionIntent, boolean allowWildernessMethods,
            boolean useGroupStorage)
    {
        List<CuratedTrainingMethod> methods = candidates(data, skill);
        MembershipStatus membershipStatus = membershipStatus(data);
        List<ScoredPlan> ranked = new ArrayList<>();
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN : AccountMode.fromTypeCode(
                        data.account().getAccountTypeCode());

        for (CuratedTrainingMethod candidate : methods)
        {
            TrainingMethod method = candidate.getMethod();
            TrainingMethodMetadata metadata = candidate.getMetadata();
            MethodStrategyProfile strategyProfile = strategyCatalog.profileFor(
                    method, metadata, mode);
            MethodStrategyAssessment strategyAssessment = strategyService.assess(
                    data, strategyProfile);
            if (!method.supportsLevel(currentLevel)
                    || !ContentAccessRules.isMethodAvailable(method, membershipStatus)
                    || method.getConfidence() == Confidence.BLOCKED
                    || !methodPolicy.isAllowed(data, method, metadata, allowWildernessMethods)
                    || !strategyAssessment.isViable())
            {
                continue;
            }

            List<RequirementCheck> checks = requirementEvidenceEngine == null
                    ? Collections.emptyList()
                    : useGroupStorage
                            ? requirementEvidenceEngine.evaluate(
                                    data, method, true)
                            : requirementEvidenceEngine.evaluate(data, method);
            Confidence confidence = assessConfidence(method, checks);
            if (confidence == Confidence.BLOCKED) continue;

            // A skill gets one selected method. Never let a higher-scoring
            // method with unknown access consume that slot and hide a simpler
            // route the player can actually begin. Hard-gated methods can
            // return once their quest/access evidence is observed.
            TrainingPlan assessed = new TrainingPlan(method, "", confidence,
                    checks, strategyProfile);
            boolean hardRequirementUnknown =
                    RequirementActionability.hasHardUnresolvedRequirement(
                            assessed);

            double score = method.scoreFor(strategyMode, sessionIntent)
                    + methodPolicy.scoreAdjustment(data, metadata, strategyMode, sessionIntent)
                    + strategyAssessment.getScoreAdjustment()
                    + readinessAdjustment(data, checks, sessionIntent);
            // Retain a hard-gated plan as diagnostic/secondary information when
            // every route is unknown, but it must lose to any executable route
            // regardless of their normal efficiency scores.
            if (hardRequirementUnknown) score -= 10_000.0;
            ranked.add(new ScoredPlan(new TrainingPlan(method,
                    buildExplanation(method, metadata, strategyMode,
                            sessionIntent, data, strategyAssessment), confidence,
                    checks, strategyProfile), score));
        }

        ranked.sort(Comparator.comparingDouble(ScoredPlan::getScore).reversed());
        List<TrainingPlan> plans = new ArrayList<>();
        for (ScoredPlan candidate : ranked) plans.add(candidate.plan);
        return Collections.unmodifiableList(plans);
    }

    private static double readinessAdjustment(GameData data,
            List<RequirementCheck> checks, SessionIntent sessionIntent)
    {
        if (checks == null || checks.isEmpty()) return 0.0;
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().getAccountTypeCode());
        boolean fullyReady = true;
        double missingResourcePenalty = 0.0;
        for (RequirementCheck check : checks)
        {
            if (check == null)
            {
                fullyReady = false;
                continue;
            }
            if (check.getState() != RequirementState.VERIFIED)
                fullyReady = false;
            if (check.getState() == RequirementState.CHECK_NEEDED
                    && RequirementActionability.isPreparationRequirement(check))
            {
                double penalty = mode.isIronLike() ? 14.0 : 10.0;
                if (sessionIntent == SessionIntent.QUICK_20_MIN) penalty += 4.0;
                if (sessionIntent == SessionIntent.LONG_SESSION)
                    penalty -= mode.isIronLike() ? 5.0 : 8.0;
                missingResourcePenalty += penalty;
            }
        }
        // Readiness is plan-level: a requirement-free method and a method with
        // every check verified get the same benefit. The number of evidence
        // rows never adds score, so splitting one fact into several checks
        // cannot change the winner.
        return (fullyReady ? 10.0 : 0.0) - missingResourcePenalty;
    }

    private static final class ScoredPlan
    {
        private final TrainingPlan plan;
        private final double score;

        private ScoredPlan(TrainingPlan plan, double score)
        {
            this.plan = plan;
            this.score = score;
        }

        private double getScore() { return score; }
    }

    private List<CuratedTrainingMethod> candidates(GameData data, Skill skill)
    {
        List<CuratedTrainingMethod> candidates = new ArrayList<>();
        MembershipStatus membership = membershipStatus(data);

        if (expandedCatalog == null)
        {
            for (TrainingMethod method : database.methodsFor(skill))
            {
                candidates.add(new CuratedTrainingMethod(method,
                        TrainingMethodMetadata.legacy(method)));
            }
            return candidates;
        }

        // In production, legacy methods predate route-level F2P metadata. F2P
        // therefore uses only catalogs whose membership compatibility is explicit.
        if (membership == MembershipStatus.P2P)
        {
            for (TrainingMethod method : database.methodsFor(skill))
            {
                // These legacy catch-alls delegate the meaningful method choice
                // back to the player and can outrank their concrete successors.
                if (method.delegatesMethodChoice()) continue;
                candidates.add(new CuratedTrainingMethod(method,
                        TrainingMethodMetadata.legacy(method)));
            }
        }

        candidates.addAll(expandedCatalog.methodsFor(skill));

        // Basic F2P routes are also valid on members worlds. Keep them available
        // to P2P accounts as concrete low-level Runecraft routes and as a safe
        // fallback when a higher-level members method is not currently usable.
        if (f2pBaselineCatalog != null)
        {
            candidates.addAll(f2pBaselineCatalog.methodsFor(skill));
        }
        // Several routes exist in both the protected legacy catalog and the
        // richer expanded catalog. A duplicate must never masquerade as the
        // runner-up in Other Good Options or method-tournament diagnostics.
        // Prefer the later expanded definition, preserving original order.
        Map<String, CuratedTrainingMethod> unique = new LinkedHashMap<>();
        for (CuratedTrainingMethod candidate : candidates)
        {
            String id = candidate.getMethod().getId();
            if (unique.containsKey(id)) unique.remove(id);
            unique.put(id, candidate);
        }
        return new ArrayList<>(unique.values());
    }

    private static MembershipStatus membershipStatus(GameData data)
    {
        if (data == null || data.account() == null) return MembershipStatus.UNKNOWN;
        return data.account().getMembershipStatus();
    }

    private Confidence assessConfidence(
            TrainingMethod method, List<RequirementCheck> checks)
    {
        if (method.getConfidence() == Confidence.BLOCKED)
            return Confidence.BLOCKED;
        if (checks != null && !checks.isEmpty())
        {
            boolean hasUnknown = false;
            for (RequirementCheck check : checks)
            {
                if (check.getState() == RequirementState.BLOCKED)
                    return Confidence.BLOCKED;
                if (check.getState() == RequirementState.CHECK_NEEDED) hasUnknown = true;
            }
            return hasUnknown ? Confidence.CHECK_NEEDED
                    : Confidence.VERIFIED;
        }
        if (method.getRequirements().isEmpty()
                && method.getConfidence() == Confidence.VERIFIED)
            return Confidence.VERIFIED;
        return Confidence.CHECK_NEEDED;
    }

    private String buildExplanation(TrainingMethod method,
            TrainingMethodMetadata metadata, StrategyMode strategyMode,
            SessionIntent sessionIntent, GameData data,
            MethodStrategyAssessment strategyAssessment)
    {
        StringBuilder reason = new StringBuilder();
        if (strategyAssessment != null
                && strategyAssessment.getExplanation() != null
                && !strategyAssessment.getExplanation().trim().isEmpty())
        {
            reason.append(strategyAssessment.getExplanation().trim());
        }
        else
        {
            reason.append(Text.get(1279))
                    .append(pretty(strategyMode.name())).append(" play.");
        }
        if (sessionIntent != SessionIntent.PICK_FOR_ME)
            reason.append(" It also fits ").append(pretty(sessionIntent.name()))
                    .append(" sessions.");
        if (method.isWilderness())
            reason.append(Text.get(898));
        return reason.toString();
    }

    private static String pretty(String value)
    {
        if (value == null || value.isEmpty()) return "Unknown";
        String lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

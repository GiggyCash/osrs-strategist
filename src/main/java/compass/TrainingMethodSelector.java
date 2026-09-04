package compass;
import static java.util.Collections.*;
import lombok.*;

import static compass.Text.get;

import java.util.*;
import javax.inject.*;
import net.runelite.api.Skill;

@Singleton
public class TrainingMethodSelector
{
    private final TrainingMethodCatalog catalog;
    private final RequirementEvidenceEngine requirementEvidenceEngine;
    private final TrainingMethodPolicy methodPolicy;
    private final MethodStrategyKnowledgeCatalog strategyCatalog;
    private final MethodStrategyService strategyService;

    @Inject
    public TrainingMethodSelector(
            TrainingMethodCatalog catalog,
            RequirementEvidenceEngine requirementEvidenceEngine,
            TrainingMethodPolicy methodPolicy,
            MethodStrategyKnowledgeCatalog strategyCatalog,
            MethodStrategyService strategyService)
    {
        this.catalog = catalog;
        this.requirementEvidenceEngine = requirementEvidenceEngine;
        this.methodPolicy = methodPolicy;
        this.strategyCatalog = strategyCatalog == null
                ? new MethodStrategyKnowledgeCatalog() : strategyCatalog;
        this.strategyService = strategyService == null
                ? new MethodStrategyService() : strategyService;
    }
   TrainingMethodSelector(TrainingMethodCatalog catalog,
            RequirementEvidenceEngine evidence, TrainingMethodCatalog ignoredCurated,
            TrainingMethodCatalog ignoredF2p, TrainingMethodPolicy policy)
    {
        this(new TrainingMethodCatalog(), evidence, policy, new MethodStrategyKnowledgeCatalog(),
                new MethodStrategyService());
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
        var methods = candidates(data, skill);
        var membershipStatus = membershipStatus(data);
        List<ScoredPlan> ranked = new ArrayList<>();
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN : AccountMode.fromTypeCode(
                        data.account().modeCode());

        for (CuratedTrainingMethod candidate : methods)
        {
            var method = candidate.method();
            var metadata = candidate.getMetadata();
            MethodStrategyProfile strategyProfile = strategyCatalog.profileFor(
                    method, metadata, mode);
            MethodStrategyAssessment strategyAssessment = strategyService.assess(
                    data, strategyProfile);
            if (!method.supportsLevel(currentLevel)
                    || !ContentAccessRules.isMethodAvailable(method, membershipStatus)
                    || method.confidence == Confidence.BLOCKED
                    || !methodPolicy.isAllowed(data, method, metadata, allowWildernessMethods)
                    || !strategyAssessment.isViable())
            {
                continue;
            }

            List<EvidenceCheck> checks = requirementEvidenceEngine == null
                    ? emptyList()
                    : useGroupStorage
                            ? requirementEvidenceEngine.evaluate(
                                    data, method, true)
                            : requirementEvidenceEngine.evaluate(data, method);
            var confidence = assessConfidence(method, checks);
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
        return unmodifiableList(plans);
    }

    private static double readinessAdjustment(GameData data,
            List<EvidenceCheck> checks, SessionIntent sessionIntent)
    {
        if (checks == null || checks.isEmpty()) return 0.0;
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().modeCode());
        var fullyReady = true;
        var missingResourcePenalty = 0.0;
        for (EvidenceCheck check : checks)
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
                var penalty = mode.isIronLike() ? 14.0 : 10.0;
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

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)

    private static final class ScoredPlan
    {
        private final TrainingPlan plan;
        @Getter
        private final double score;
    }

    private List<CuratedTrainingMethod> candidates(GameData data, Skill skill)
    {
        List<CuratedTrainingMethod> candidates = new ArrayList<>();
        var membership = membershipStatus(data);

        if (catalog.legacyOnly())
        {
            for (TrainingMethod method : catalog.legacyFor(skill))
                candidates.add(new CuratedTrainingMethod(method,
                        TrainingMethodMetadata.legacy(method)));
            return candidates;
        }

        // In production, legacy methods predate route-level F2P metadata. F2P
        // therefore uses only catalogs whose membership compatibility is explicit.
        if (membership == Membership.P2P)
        {
            for (TrainingMethod method : catalog.legacyFor(skill))
            {
                // These legacy catch-alls delegate the meaningful method choice
                // back to the player and can outrank their concrete successors.
                if (method.delegatesMethodChoice()) continue;
                candidates.add(new CuratedTrainingMethod(method,
                        TrainingMethodMetadata.legacy(method)));
            }
        }

        candidates.addAll(catalog.curatedFor(skill));

        // Basic F2P routes are also valid on members worlds. Keep them available
        // to P2P accounts as concrete low-level Runecraft routes and as a safe
        // fallback when a higher-level members method is not currently usable.
        candidates.addAll(catalog.f2pFor(skill));
        // Several routes exist in both the protected legacy catalog and the
        // richer expanded catalog. A duplicate must never masquerade as the
        // runner-up in Other Good Options or method-tournament diagnostics.
        // Prefer the later expanded definition, preserving original order.
        Map<String, CuratedTrainingMethod> unique = new LinkedHashMap<>();
        for (CuratedTrainingMethod candidate : candidates)
        {
            var id = candidate.method().id;
            if (unique.containsKey(id)) unique.remove(id);
            unique.put(id, candidate);
        }
        return new ArrayList<>(unique.values());
    }

    private static Membership membershipStatus(GameData data)
    {
        if (data == null || data.account() == null) return Membership.UNKNOWN;
        return data.account().membership();
    }

    private Confidence assessConfidence(
            TrainingMethod method, List<EvidenceCheck> checks)
    {
        if (method.confidence == Confidence.BLOCKED)
            return Confidence.BLOCKED;
        if (checks != null && !checks.isEmpty())
        {
            var hasUnknown = false;
            for (EvidenceCheck check : checks)
            {
                if (check.getState() == RequirementState.BLOCKED)
                    return Confidence.BLOCKED;
                if (check.getState() == RequirementState.CHECK_NEEDED) hasUnknown = true;
            }
            return hasUnknown ? Confidence.CHECK_NEEDED
                    : Confidence.VERIFIED;
        }
        if (method.requirements.isEmpty()
                && method.confidence == Confidence.VERIFIED)
            return Confidence.VERIFIED;
        return Confidence.CHECK_NEEDED;
    }

    private String buildExplanation(TrainingMethod method,
            TrainingMethodMetadata metadata, StrategyMode strategyMode,
            SessionIntent sessionIntent, GameData data,
            MethodStrategyAssessment strategyAssessment)
    {
        var reason = new StringBuilder();
        if (strategyAssessment != null
                && strategyAssessment.getExplanation() != null
                && !strategyAssessment.getExplanation().trim().isEmpty())
        {
            reason.append(strategyAssessment.getExplanation().trim());
        }
        else
        {
            reason.append(get(1279))
                    .append(pretty(strategyMode.name())).append(" play.");
        }
        if (sessionIntent != SessionIntent.PICK_FOR_ME)
            reason.append(" It also fits ").append(pretty(sessionIntent.name()))
                    .append(" sessions.");
        if (method.wilderness)
            reason.append(get(898));
        return reason.toString();
    }

    private static String pretty(String value)
    {
        if (value == null || value.isEmpty()) return "Unknown";
        var lower = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

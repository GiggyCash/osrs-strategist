package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Turns static method requirements into account-specific evidence checks.
 * Dedicated evaluators replace generic Check First rows one system at a time.
 *
 * <p>The order is deliberate. Domain-specific evaluators (Farming, Agility,
 * Runecraft) have the richest semantic knowledge and run first. The general
 * {@link MethodReadinessCatalog} then handles stable item-driven requirements
 * for common methods. Any fact that is still not observable stays explicit and
 * unresolved rather than being guessed.</p>
 */
@Singleton
public class RequirementEvidenceEngine
{
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final AgilityAccessEvaluator agilityAccessEvaluator;
    private final FarmingSupplyCatalog farmingSupplyCatalog;
    private final RunecraftSupplyCatalog runecraftSupplyCatalog;
    private final MethodReadinessCatalog methodReadinessCatalog;
    private final ResourceReadinessService resourceReadinessService;

    @Inject
    public RequirementEvidenceEngine(
            FarmingAccessEvaluator farmingAccessEvaluator,
            AgilityAccessEvaluator agilityAccessEvaluator,
            FarmingSupplyCatalog farmingSupplyCatalog,
            RunecraftSupplyCatalog runecraftSupplyCatalog,
            MethodReadinessCatalog methodReadinessCatalog,
            ResourceReadinessService resourceReadinessService)
    {
        this.farmingAccessEvaluator = farmingAccessEvaluator;
        this.agilityAccessEvaluator = agilityAccessEvaluator;
        this.farmingSupplyCatalog = farmingSupplyCatalog;
        this.runecraftSupplyCatalog = runecraftSupplyCatalog;
        this.methodReadinessCatalog = methodReadinessCatalog;
        this.resourceReadinessService = resourceReadinessService;
    }

    /** Compatibility constructor retained for focused tests. */
    public RequirementEvidenceEngine(
            FarmingAccessEvaluator farmingAccessEvaluator,
            AgilityAccessEvaluator agilityAccessEvaluator)
    {
        this(
                farmingAccessEvaluator,
                agilityAccessEvaluator,
                new FarmingSupplyCatalog(),
                new RunecraftSupplyCatalog(),
                new MethodReadinessCatalog(),
                new ResourceReadinessService()
        );
    }

    /** Compatibility constructor retained for older focused tests. */
    public RequirementEvidenceEngine(FarmingAccessEvaluator farmingAccessEvaluator)
    {
        this(farmingAccessEvaluator, null);
    }

    public List<RequirementCheck> evaluate(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        if (method == null) return checks;

        if (method.getSkill() == Skill.FARMING)
        {
            return evaluateFarming(data, method);
        }
        if (method.getSkill() == Skill.AGILITY && agilityAccessEvaluator != null)
        {
            return evaluateAgility(data, method);
        }
        if (method.getSkill() == Skill.RUNECRAFT
                && runecraftSupplyCatalog.supports(method.getId()))
        {
            return evaluateRunecraft(data, method);
        }

        MethodReadinessProfile profile = methodReadinessCatalog == null
                ? null : methodReadinessCatalog.forMethod(method.getId());
        if (profile != null)
        {
            return evaluateProfile(data, profile);
        }

        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    /**
     * Conventional F2P altar routes are resource-driven. The player does not
     * need to manually confirm them once Strategist has observed the essence and
     * matching talisman/tiara in equipment, inventory, bank, or safe
     * account-specific storage.
     */
    private List<RequirementCheck> evaluateRunecraft(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        checks.add(resourceReadinessService.evaluate(
                data,
                runecraftSupplyCatalog.runeEssence()
        ));
        ResourceRequirement entry = runecraftSupplyCatalog.altarEntryFor(method.getId());
        if (entry != null)
        {
            checks.add(resourceReadinessService.evaluate(data, entry));
        }
        return checks;
    }

    /**
     * Evaluates all automatically provable item families, then appends only the
     * account facts that still need a richer reader (quest/access/location/etc.).
     */
    private List<RequirementCheck> evaluateProfile(
            StrategyDataBundle data,
            MethodReadinessProfile profile)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        for (NamedResourceRequirement requirement
                : profile.getItemRequirements())
        {
            checks.add(resourceReadinessService.evaluate(data, requirement));
        }
        int index = 0;
        for (String unresolved : profile.getOtherChecks())
        {
            checks.add(new RequirementCheck(
                    "method:" + profile.getMethodId() + ":check:"
                            + index++ + ":" + slug(unresolved),
                    unresolved,
                    RequirementState.CHECK_NEEDED,
                    "Strategist cannot prove this requirement from the current live readers yet."
            ));
        }
        return checks;
    }

    private List<RequirementCheck> evaluateAgility(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        if ("agility_wilderness".equals(method.getId()))
        {
            checks.add(agilityAccessEvaluator.wildernessCourseCheck(data));
            checks.add(new RequirementCheck(
                    "agility:wilderness_risk",
                    "Wilderness risk accepted",
                    RequirementState.VERIFIED,
                    "This method only reaches the evaluator when Wilderness methods are enabled."
            ));
            return checks;
        }

        AgilityCourseDefinition course =
                agilityAccessEvaluator.bestStandardCourse(data);
        checks.add(agilityAccessEvaluator.courseCheck(data, course));
        return checks;
    }

    private List<RequirementCheck> evaluateFarming(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        AccountSnapshot account = data == null ? null : data.getAccount();
        FarmingSnapshot farming = data == null ? null : data.getFarming();
        int level = account == null ? 1 : account.getSkillLevel(Skill.FARMING);

        if ("farming_early".equals(method.getId()))
        {
            String patch = farmingAccessEvaluator.firstReachablePatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:reachable_patch",
                    "Reachable Farming patch",
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? "Quest/access checks and observed-area memory have not proven a patch yet."
                            : patch + " is available from quest/access evidence."
            ));
            checks.add(new RequirementCheck(
                    "farming:supplies",
                    "Seeds and farming tools",
                    RequirementState.CHECK_NEEDED,
                    "The generic early-Farming seed catalog is not complete yet; Strategist will not guess a usable seed."
            ));
            return checks;
        }

        if ("farming_herbs".equals(method.getId()))
        {
            checks.add(new RequirementCheck(
                    "farming:level_9",
                    "9 Farming",
                    level >= 9
                            ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED,
                    "Current Farming level is " + level + "."
            ));

            String patch = farmingAccessEvaluator.firstReachableHerbPatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:herb_patch",
                    "Reachable herb patch",
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? "No herb patch has been proven by quest/access checks or prior observation yet."
                            : patch + " is available from quest/access evidence."
            ));

            checks.add(resourceReadinessService.evaluate(
                    data,
                    farmingSupplyCatalog.herbSeedsForLevel(level)
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.rake(),
                    "rake",
                    "Rake was previously verified in Tool Leprechaun storage."
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.dibber(),
                    "dibber",
                    "Seed dibber was previously verified in Tool Leprechaun storage."
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.spade(),
                    "spade",
                    "Spade was previously verified in Tool Leprechaun storage."
            ));
            return checks;
        }

        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    private RequirementCheck toolCheck(
            StrategyDataBundle data,
            FarmingSnapshot farming,
            ResourceRequirement requirement,
            String toolId,
            String leprechaunEvidence)
    {
        CapabilityState stored = farming == null
                ? CapabilityState.UNKNOWN
                : farming.leprechaunToolState(toolId);
        return resourceReadinessService.evaluate(
                data,
                requirement,
                stored,
                leprechaunEvidence
        );
    }

    private RequirementCheck generic(String requirement)
    {
        return new RequirementCheck(
                "generic:" + requirement,
                requirement,
                RequirementState.CHECK_NEEDED,
                "Strategist has not observed enough account state to prove this yet."
        );
    }

    private static String slug(String value)
    {
        if (value == null) return "unknown";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}

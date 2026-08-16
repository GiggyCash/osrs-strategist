package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Turns static method requirements into account-specific evidence checks.
 *
 * <p>The first focused implementation handles Farming access deeply because it
 * is a good example of Strategist's intended behavior: cross-check quests,
 * remember places previously reached, and only ask the player about facts that
 * cannot yet be proven automatically.</p>
 */
@Singleton
public class RequirementEvidenceEngine
{
    private final FarmingAccessEvaluator farmingAccessEvaluator;

    @Inject
    public RequirementEvidenceEngine(
            FarmingAccessEvaluator farmingAccessEvaluator)
    {
        this.farmingAccessEvaluator = farmingAccessEvaluator;
    }

    public List<RequirementCheck> evaluate(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();

        if (method == null)
        {
            return checks;
        }

        if (method.getSkill() == Skill.FARMING)
        {
            return evaluateFarming(data, method);
        }

        // Until a system-specific evaluator exists, preserve the exact static
        // requirement as an explicit Check Needed instead of pretending it was
        // verified merely because the method exists in the catalog.
        for (String requirement : method.getRequirements())
        {
            checks.add(new RequirementCheck(
                    "generic:" + requirement,
                    requirement,
                    RequirementState.CHECK_NEEDED,
                    "Strategist has not observed enough account state to prove this yet."
            ));
        }

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
                    "Inventory/bank supply matching is not complete yet."
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
                    level >= 9
                            ? "Current Farming level is " + level + "."
                            : "Current Farming level is " + level + "."
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

            checks.add(new RequirementCheck(
                    "farming:herb_seed",
                    "Herb seeds",
                    RequirementState.CHECK_NEEDED,
                    "Seed quantities have not been matched against inventory/bank state yet."
            ));

            boolean knownToolState = farming != null
                    && !farming.getLeprechaunTools().isEmpty();
            checks.add(new RequirementCheck(
                    "farming:tools",
                    "Farming tools / Tool Leprechaun",
                    knownToolState
                            ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    knownToolState
                            ? "Stored tool state has been observed."
                            : "Tool Leprechaun contents have not been observed yet."
            ));
            return checks;
        }

        for (String requirement : method.getRequirements())
        {
            checks.add(new RequirementCheck(
                    "farming:" + requirement,
                    requirement,
                    RequirementState.CHECK_NEEDED,
                    "This Farming requirement still needs a dedicated verifier."
            ));
        }
        return checks;
    }
}

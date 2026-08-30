package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Turns verified POH absence into one concrete build or verification action. */
@Singleton
public class InfrastructureCandidateProvider implements StrategyCandidateProvider
{
    private final InfrastructureMilestoneCatalog catalog;
    private final InfrastructureUnlockValueService values;
    private final UimRecurringPressureService recurringPressure;

    @Inject
    public InfrastructureCandidateProvider(
            InfrastructureMilestoneCatalog catalog,
            InfrastructureUnlockValueService values,
            UimRecurringPressureService recurringPressure)
    {
        this.catalog = catalog;
        this.values = values;
        this.recurringPressure = recurringPressure == null
                ? new UimRecurringPressureService() : recurringPressure;
    }

    public InfrastructureCandidateProvider(
            InfrastructureMilestoneCatalog catalog,
            InfrastructureUnlockValueService values)
    {
        this(catalog, values, new UimRecurringPressureService());
    }

    @Override
    public String getId()
    {
        return "infrastructure-candidates";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null) return result;
        AccountSnapshot account = context.getData().getAccount();
        if (account.getMembershipStatus() != MembershipStatus.P2P) return result;

        if (context.getData().getPoh() == null)
        {
            result.add(verificationCandidate(context));
            return result;
        }

        UimRecurringPressureAssessment pressure =
                recurringPressure.observe(context);

        for (InfrastructureMilestoneDefinition definition : catalog.all())
        {
            InfrastructureValueAssessment assessment = values.assess(
                    definition.getId(), context);
            if (!assessment.canRecommendAcquisition()) continue;
            result.add(buildCandidate(definition, assessment, context,
                    pressure));
        }
        return result;
    }

    private static StrategyCandidate verificationCandidate(
            StrategyContext context)
    {
        double modeValue = context.getAccountMode()
                == AccountMode.ULTIMATE_IRONMAN ? 0.9
                : AccountModePolicy.requiresSelfSourcing(
                        context.getAccountMode()) ? 0.55 : 0.25;
        return new StrategyCandidate(
                "verify:poh-build-mode",
                "Verify your own POH",
                "A single ownership-proven scan lets Compass remember personal storage, restoration, and transport without counting a public or teammate house.",
                34.0 + modeValue * 12.0,
                RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        "At a house portal, choose Build mode. If the game says you do not own a house, buy one from the Varrock Estate agent for 1,000 coins, then enter in Build mode.",
                        "1,000 coins only if you do not own a house",
                        "Varrock Estate agent, then your POH",
                        "Compass records the complete personal-house scan automatically."),
                CandidateSafetyEvidence.harmless(false),
                RecommendationStrategicValue.builder()
                        .infrastructureValue(modeValue)
                        .accountModeFit(modeValue)
                        .evidence("runelite:poh-building-mode")
                        .build());
    }

    private static StrategyCandidate buildCandidate(
            InfrastructureMilestoneDefinition definition,
            InfrastructureValueAssessment assessment,
            StrategyContext context,
            UimRecurringPressureAssessment pressure)
    {
        double utility = assessment.getStrategicValue().ordinal()
                / (double) StrategicPriority.CRITICAL.ordinal();
        double score = 31.0 + utility * 26.0;
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
            score += 8.0;
        else if (AccountModePolicy.requiresSelfSourcing(context.getAccountMode()))
            score += 3.0;
        if (context.getSessionIntent() == SessionIntent.QUICK_20_MIN)
            score -= expensiveSetup(definition.getId()) ? 12.0 : 3.0;
        if (context.getSessionIntent() == SessionIntent.AFK) score -= 8.0;
        if (context.getStrategyMode() == StrategyMode.EFFICIENT) score += 2.0;
        boolean recurringRelief = pressure != null && pressure.isRepeated()
                && (definition.getBenefits().containsKey(
                        InfrastructureBenefit.INVENTORY_RELIEF)
                || definition.getBenefits().containsKey(
                        InfrastructureBenefit.STORAGE));
        if (recurringRelief) score += 12.0;

        String modeReason = context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                ? " It reduces UIM inventory pressure, travel, or future setup churn without counting conventional bank storage."
                : AccountModePolicy.requiresSelfSourcing(context.getAccountMode())
                ? " Its reusable utility reduces future self-sourced travel or setup costs."
                : " Its repeat utility is weighed against tradeable and public-house substitutes; neither substitute is assumed available.";
        if (recurringRelief)
            modeReason += " Distinct observed inventory layouts have blocked "
                    + String.join(" and ", pressure.getBlockedFamilies())
                    + ", so this is recurring pressure rather than a one-off full inventory.";
        return new StrategyCandidate(
                "prepare:infrastructure:" + definition.getId(),
                "Build " + definition.getName(),
                "Your own-house scan proves this milestone is absent and its skill, quest, and POH prerequisites are complete."
                        + modeReason,
                score,
                RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        definition.getAction(), materials(definition.getId()),
                        "Your own POH in Build mode",
                        "Compass will detect the completed furniture automatically."),
                CandidateSafetyEvidence.skill(false, Skill.CONSTRUCTION),
                RecommendationStrategicValue.builder()
                        .infrastructureValue(utility)
                        .accountModeFit(context.getAccountMode()
                                == AccountMode.ULTIMATE_IRONMAN
                                ? utility : utility * 0.55)
                        .setupReuse(utility * 0.7)
                        .resourceFit(expensiveSetup(definition.getId())
                                ? -0.75 : -0.25)
                        .unlockValue(recurringRelief ? 0.8 : 0.0)
                        .evidence("infrastructure:" + definition.getId())
                        .evidence(recurringRelief
                                ? "uim:recurring-inventory-pressure" : null)
                        .build());
    }

    private static boolean expensiveSetup(String id)
    {
        return "poh-restoration-pool".equals(id)
                || "poh-portal-nexus".equals(id)
                || "poh-basic-jewellery-box".equals(id);
    }

    private static String materials(String id)
    {
        switch (id)
        {
            case "poh-costume-room": return "50,000 coins";
            case "poh-armour-case": return "Hammer, saw, 3 oak planks";
            case "poh-portal-chamber":
                return "100,000 coins, hammer, saw, 2 limestone bricks, 3 teak planks, 300 air runes, 100 fire runes, 100 law runes";
            case "poh-superior-garden": return "75,000 coins";
            case "poh-restoration-pool":
                return "Hammer, saw, 5 limestone bricks, 5 buckets of water, 1,000 soul runes, 1,000 body runes";
            case "poh-portal-nexus":
                return "200,000 coins, hammer, saw, 4 marble blocks";
            case "poh-spirit-tree":
                return "Filled watering can, spirit sapling";
            case "poh-basic-jewellery-box":
                return "Hammer, saw, bolt of cloth, steel bar, 3 games necklaces(8), 3 rings of dueling(8)";
            case "poh-fairy-ring":
                return "Filled watering can, 10 unnoted mushrooms, fairy enchantment";
            default: return null;
        }
    }
}

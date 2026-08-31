package com.udderlywet.osrsstrategist;

import java.util.*;
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
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
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

    private static Recommendation verificationCandidate(
            StrategyContext context)
    {
        double modeValue = context.getAccountMode()
                == AccountMode.ULTIMATE_IRONMAN ? 0.9
                : AccountModePolicy.requiresSelfSourcing(
                        context.getAccountMode()) ? 0.55 : 0.25;
        return new Recommendation(
                "verify:poh-build-mode",
                "Verify your own POH",
                Text.get(301),
                34.0 + modeValue * 12.0,
                RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        Text.get(309),
                        Text.get(310),
                        "Varrock Estate agent, then your POH",
                        Text.get(311)),
                CandidateSafetyEvidence.harmless(false),
                RecommendationStrategicValue.builder()
                        .infrastructureValue(modeValue)
                        .accountModeFit(modeValue)
                        .evidence("runelite:poh-building-mode")
                        .build());
    }

    private static Recommendation buildCandidate(
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
                ? Text.get(312)
                : AccountModePolicy.requiresSelfSourcing(context.getAccountMode())
                ? Text.get(313)
                : Text.get(314);
        if (recurringRelief)
            modeReason += Text.get(315)
                    + String.join(" and ", pressure.getBlockedFamilies())
                    + Text.get(316);
        return new Recommendation(
                "prepare:infrastructure:" + definition.getId(),
                "Build " + definition.getName(),
                Text.get(302)
                        + modeReason,
                score,
                RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        definition.getAction(), materials(definition.getId()),
                        "Your own POH in Build mode",
                        Text.get(303)),
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
                return Text.get(304);
            case "poh-superior-garden": return "75,000 coins";
            case "poh-restoration-pool":
                return Text.get(305);
            case "poh-portal-nexus":
                return Text.get(306);
            case "poh-spirit-tree":
                return "Filled watering can, spirit sapling";
            case "poh-basic-jewellery-box":
                return Text.get(307);
            case "poh-fairy-ring":
                return Text.get(308);
            default: return null;
        }
    }
}

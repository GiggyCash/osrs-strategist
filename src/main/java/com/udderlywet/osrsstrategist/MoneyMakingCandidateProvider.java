package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Surfaces money/resource work only when cash pressure or a gear goal makes it relevant. */
@Singleton
public class MoneyMakingCandidateProvider implements StrategyCandidateProvider
{
    private final MoneyMakingCatalog catalog;

    @Inject
    public MoneyMakingCandidateProvider(MoneyMakingCatalog catalog)
    {
        this.catalog = catalog;
    }

    @Override
    public String getId() { return "money-candidates"; }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null) return result;

        AccountSnapshot account = context.getData().getAccount();
        AccountEconomySnapshot economy = context.getData().getEconomy();
        boolean explicitGearNeed = context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY;
        boolean observedCashPressure = economy != null
                && economy.getConfidence() == RecommendationConfidence.VERIFIED
                && economy.getCoins() < 1_000_000L;
        if (!explicitGearNeed && !observedCashPressure) return result;

        AccountMode mode = context.getAccountMode();
        for (MoneyMakingDefinition method : catalog.forAccount(mode))
        {
            if (!ContentAccessRules.isContentAvailable(
                    account.getMembershipStatus(), method.isFreeToPlay())) continue;
            if (method.getPrimarySkill() != null
                    && account.getSkillLevel(method.getPrimarySkill()) < method.getMinimumLevel()) continue;
            if (method.isWilderness() && !context.isAllowWildernessMethods()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && (method.isWilderness()
                    || method.getRiskLevel() == RiskLevel.HIGH
                    || method.getRiskLevel() == RiskLevel.IRREVERSIBLE)) continue;

            String id = method.getId();
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;
            RecommendationGuidance guidance = guidanceFor(method, context);
            // A catalog identity is not a recommendation. Price-sensitive,
            // encounter-dependent, or access-dependent methods stay hidden
            // until Compass can publish one coherent executable loop.
            if (guidance == null) continue;
            double score = 25.0;
            if (observedCashPressure) score += 12.0;
            if (explicitGearNeed) score += 7.0;
            if (method.getRiskLevel() == RiskLevel.NONE) score += 4.0;
            if (method.getAttention() == AttentionLevel.AFK
                    && context.getSessionIntent() == SessionIntent.AFK) score += 8.0;
            if (method.getAttention() == AttentionLevel.LOW
                    && context.getStrategyMode() == StrategyMode.RELAXED) score += 5.0;
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            String priceNote = method.isRequiresLivePrices()
                    ? " Main-account profit ranking requires current prices before comparing GP/hour."
                    : "";
            result.add(new StrategyCandidate(
                    id,
                    "Make money: " + method.getName(),
                    method.getDescription() + priceNote,
                    score,
                    RecommendationConfidence.VERIFIED,
                    guidance,
                    safetyFor(method),
                    strategicValue(method, mode)
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static RecommendationGuidance guidanceFor(
            MoneyMakingDefinition method, StrategyContext context)
    {
        if (method == null || context == null) return null;
        if (!"money:agility-pyramid".equals(method.getId())) return null;
        AccountMode mode = context.getAccountMode();
        if (!mode.isIronLike()
                || mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN
                || context.getData().getAccount().getSkillLevel(
                        net.runelite.api.Skill.AGILITY) < 60)
        {
            return null;
        }
        return new RecommendationGuidance(
                "Climb the Agility Pyramid, take the pyramid top, sell it to Simon Templeton for 10,000 coins, and repeat.",
                "Buy four waterskin(4)s from the Pollnivneach general store and ten jugs of wine from Ali the Barman before each batch; restock before the final waterskin empties.",
                "Pollnivneach shops -> Agility Pyramid west of Nardah -> Simon Templeton beside the pyramid.",
                "Level 60 is the conservative cash breakpoint. Obstacle failures and damage vary, so Compass does not promise a lap rate or exact hourly coins.");
    }

    private static RecommendationStrategicValue strategicValue(
            MoneyMakingDefinition method, AccountMode mode)
    {
        if (method != null && "money:agility-pyramid".equals(method.getId())
                && mode != null && mode.isIronLike())
        {
            return RecommendationStrategicValue.builder()
                    .accountModeFit(0.8)
                    .resourceFit(0.75)
                    .riskBurden(0.3)
                    .evidence("wiki:agility-pyramid-fixed-top-value")
                    .evidence("wiki:ironman-agility-pyramid-cash")
                    .build();
        }
        return RecommendationStrategicValue.neutral();
    }

    private static CandidateSafetyEvidence safetyFor(MoneyMakingDefinition method)
    {
        if (method.getRiskLevel() == RiskLevel.HIGH
                || method.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            return CandidateSafetyEvidence.potentiallyIrreversible(
                    method.isFreeToPlay());
        if (method.getPrimarySkill() != null)
            return CandidateSafetyEvidence.skill(method.isFreeToPlay(),
                    method.getPrimarySkill());
        return CandidateSafetyEvidence.harmless(method.isFreeToPlay());
    }
}

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
        boolean f2p = account.getMembershipStatus() == MembershipStatus.F2P;
        for (MoneyMakingDefinition method : catalog.forAccount(mode))
        {
            if (f2p && !method.isFreeToPlay()) continue;
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
                    RecommendationConfidence.CHECK_NEEDED
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }
}

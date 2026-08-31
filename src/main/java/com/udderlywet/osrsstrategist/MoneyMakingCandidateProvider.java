package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Surfaces money/resource work only when cash pressure or a gear goal makes it relevant. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class MoneyMakingCandidateProvider implements CandidateProvider
{
    private final MoneyMakingCatalog catalog;

    @Override
    public String getId() { return "money-candidates"; }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null) return result;

        var account = context.data().account();
        var economy = context.data().economy();
        boolean explicitGearNeed = context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY;
        boolean observedCashPressure = economy != null
                && economy.getConfidence() == Confidence.VERIFIED
                && economy.getCoins() < 1_000_000L;
        if (!explicitGearNeed && !observedCashPressure) return result;

        var mode = context.accountMode();
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

            var id = method.getId();
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var guidance = guidanceFor(method, context);
            // A catalog identity is not a recommendation. Price-sensitive,
            // encounter-dependent, or access-dependent methods stay hidden
            // until Compass can publish one coherent executable loop.
            if (guidance == null) continue;
            var score = 25.0;
            if (observedCashPressure) score += 12.0;
            if (explicitGearNeed) score += 7.0;
            if (method.getRiskLevel() == RiskLevel.NONE) score += 4.0;
            if (method.getAttention() == AttentionLevel.AFK
                    && context.getSessionIntent() == SessionIntent.AFK) score += 8.0;
            if (method.getAttention() == AttentionLevel.LOW
                    && context.getStrategyMode() == StrategyMode.RELAXED) score += 5.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            String priceNote = method.isRequiresLivePrices()
                    ? Text.get(378)
                    : "";
            result.add(new Recommendation(
                    id,
                    "Make money: " + method.getName(),
                    method.getDescription() + priceNote,
                    score,
                    Confidence.VERIFIED,
                    guidance,
                    safetyFor(method),
                    strategicValue(method, mode)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static Guidance guidanceFor(
            MoneyMakingDefinition method, StrategyContext context)
    {
        if (method == null || context == null) return null;
        if (!"money:agility-pyramid".equals(method.getId())) return null;
        var mode = context.accountMode();
        if (!mode.isIronLike()
                || mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN
                || context.data().account().getSkillLevel(
                        net.runelite.api.Skill.AGILITY) < 60)
        {
            return null;
        }
        return new Guidance(
                Text.get(379),
                Text.get(380),
                Text.get(381),
                Text.get(382));
    }

    private static StrategicValue strategicValue(
            MoneyMakingDefinition method, AccountMode mode)
    {
        if (method != null && "money:agility-pyramid".equals(method.getId())
                && mode != null && mode.isIronLike())
        {
            return StrategicValue.builder()
                    .accountModeFit(0.8)
                    .resourceFit(0.75)
                    .riskBurden(0.3)
                    .evidence("wiki:agility-pyramid-fixed-top-value")
                    .evidence("wiki:ironman-agility-pyramid-cash")
                    .build();
        }
        return StrategicValue.neutral();
    }

    private static SafetyEvidence safetyFor(MoneyMakingDefinition method)
    {
        if (method.getRiskLevel() == RiskLevel.HIGH
                || method.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            return SafetyEvidence.potentiallyIrreversible(
                    method.isFreeToPlay());
        if (method.getPrimarySkill() != null)
            return SafetyEvidence.skill(method.isFreeToPlay(),
                    method.getPrimarySkill());
        return SafetyEvidence.harmless(method.isFreeToPlay());
    }
}

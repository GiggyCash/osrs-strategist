package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Picks the concrete RuneLite action inside a curated method.
 *
 * <p>For mains, the curated route and action XP dominate because missing
 * tradeable inputs can normally be purchased. For Iron-style accounts, a
 * slightly lower-tier action that is already supplied can beat a theoretically
 * faster action that would require a large detour to self-source materials.</p>
 */
@Singleton
public class AdaptiveActionSelector
{
    private final MethodInputResolver inputResolver;

    @Inject
    public AdaptiveActionSelector(MethodInputResolver inputResolver)
    {
        this.inputResolver = inputResolver;
    }

    public AdaptiveActionSelector()
    {
        this(new MethodInputResolver());
    }

    public RuneLiteSkillActionDefinition select(
            StrategyDataBundle data,
            MethodExecutionProfile profile,
            List<RuneLiteSkillActionDefinition> actions,
            int currentLevel,
            MembershipStatus membership,
            int currentXp,
            int targetXp,
            double xpMultiplier,
            boolean useGroupStorage)
    {
        if (profile == null || actions == null || actions.isEmpty()) return null;
        AccountMode mode = data == null || data.getAccount() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.getAccount().getAccountTypeCode());
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
        boolean storageKnown = mode == AccountMode.ULTIMATE_IRONMAN
                || data != null && data.getBank() != null;

        RuneLiteSkillActionDefinition best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (RuneLiteSkillActionDefinition action : actions)
        {
            if (action == null || action.getXp() <= 0
                    || action.getLevel() > currentLevel)
            {
                continue;
            }
            if (membership == MembershipStatus.F2P
                    && action.getMembership() == MembershipStatus.P2P)
            {
                continue;
            }
            if (!matches(action, profile.getActionTerms())) continue;

            double xpPerAction = action.getXp() * Math.max(1.0, xpMultiplier);
            int actionsNeeded = divideRoundUp(
                    Math.max(0, targetXp - currentXp), xpPerAction);
            List<ResolvedMethodInput> needs = inputResolver.resolve(
                    profile, action, actionsNeeded);

            // Base ranking remains tied to the actual action inside the already
            // selected strategic method. Logarithmic XP weighting prevents a
            // large per-action XP number from overwhelming account readiness.
            double score = Math.log1p(xpPerAction) * 8.0
                    + action.getLevel() * 0.03;

            if (storageKnown && !needs.isEmpty())
            {
                double coverage = materialCoverage(items, needs);
                if (mode == AccountMode.ULTIMATE_IRONMAN)
                {
                    score += coverage * 120.0;
                    if (coverage >= 0.999) score += 35.0;
                    else score -= (1.0 - coverage) * 45.0;
                }
                else if (mode.isIronLike())
                {
                    score += coverage * 85.0;
                    if (coverage >= 0.999) score += 25.0;
                    else score -= (1.0 - coverage) * 30.0;
                }
                else
                {
                    // Mains can buy missing tradeables, so owned materials are
                    // a convenience rather than a hard route preference.
                    score += coverage * 4.0;
                }
            }

            if (best == null || score > bestScore)
            {
                best = action;
                bestScore = score;
            }
        }
        return best;
    }

    /** Compatibility/simple selector used when no account-resource state exists. */
    public RuneLiteSkillActionDefinition selectSimple(
            List<RuneLiteSkillActionDefinition> actions,
            MethodExecutionProfile profile,
            int currentLevel,
            MembershipStatus membership)
    {
        RuneLiteSkillActionDefinition best = null;
        for (RuneLiteSkillActionDefinition action : actions)
        {
            if (action == null || action.getLevel() > currentLevel) continue;
            if (membership == MembershipStatus.F2P
                    && action.getMembership() == MembershipStatus.P2P) continue;
            if (!matches(action, profile.getActionTerms())) continue;
            if (best == null
                    || action.getLevel() > best.getLevel()
                    || action.getLevel() == best.getLevel()
                    && action.getXp() > best.getXp())
            {
                best = action;
            }
        }
        return best;
    }

    private static double materialCoverage(
            ObservedItemIndex items,
            List<ResolvedMethodInput> needs)
    {
        if (needs == null || needs.isEmpty()) return 1.0;
        double total = 0.0;
        int counted = 0;
        for (ResolvedMethodInput need : needs)
        {
            if (need == null || need.getQuantity() <= 0) continue;
            int owned = items.quantity(need.getName());
            total += Math.min(1.0, owned / (double) need.getQuantity());
            counted++;
        }
        return counted == 0 ? 1.0 : total / counted;
    }

    private static boolean matches(
            RuneLiteSkillActionDefinition action,
            List<String> terms)
    {
        if (terms == null || terms.isEmpty()) return false;
        String haystack = normalize(action.getId()) + " "
                + normalize(action.getName()) + " "
                + normalize(action.getCategory());
        for (String term : terms)
        {
            if (haystack.contains(normalize(term))) return true;
        }
        return false;
    }

    private static int divideRoundUp(int numerator, double denominator)
    {
        if (numerator <= 0) return 0;
        return (int) Math.ceil(numerator / denominator);
    }

    private static String normalize(String value)
    {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}

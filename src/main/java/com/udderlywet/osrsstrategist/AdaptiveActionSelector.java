package com.udderlywet.osrsstrategist;

import java.util.*;
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

    public ActionDef select(
            GameData data,
            MethodProfile profile,
            List<ActionDef> actions,
            int currentLevel,
            MembershipStatus membership,
            int currentXp,
            int targetXp,
            double xpMultiplier,
            boolean useGroupStorage)
    {
        if (profile == null || actions == null || actions.isEmpty()) return null;
        AccountMode mode = data == null || data.account() == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(data.account().getAccountTypeCode());
        ItemIndex items = new ItemIndex(data, useGroupStorage);
        boolean storageKnown = items.resourceContainersObserved();

        ActionDef best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (ActionDef action : actions)
        {
            if (action == null || action.getXp() <= 0
                    || action.getLevel() > currentLevel
                    || !membershipAllowed(action.getMembership(), membership))
            {
                continue;
            }
            if (!matches(action, profile.getActionTerms())) continue;

            double xpPerAction = action.getXp() * Math.max(1.0, xpMultiplier);
            int actionsNeeded = divideRoundUp(
                    Math.max(0, targetXp - currentXp), xpPerAction);
            List<MethodInput> needs = inputResolver.resolve(
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
    public ActionDef selectSimple(
            List<ActionDef> actions,
            MethodProfile profile,
            int currentLevel,
            MembershipStatus membership)
    {
        ActionDef best = null;
        for (ActionDef action : actions)
        {
            if (action == null || action.getLevel() > currentLevel
                    || !membershipAllowed(action.getMembership(), membership))
            {
                continue;
            }
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

    private static boolean membershipAllowed(
            MembershipStatus actionMembership,
            MembershipStatus accountMembership)
    {
        if (actionMembership == null || actionMembership == MembershipStatus.UNKNOWN)
        {
            return false;
        }
        if (accountMembership == MembershipStatus.P2P)
        {
            return actionMembership == MembershipStatus.F2P
                    || actionMembership == MembershipStatus.P2P;
        }

        // F2P and UNKNOWN fail closed to actions RuneLite explicitly identifies
        // as F2P. This mirrors ContentAccessRules and prevents transient reads
        // from leaking member actions into an F2P route.
        return actionMembership == MembershipStatus.F2P;
    }

    private static double materialCoverage(
            ItemIndex items,
            List<MethodInput> needs)
    {
        if (needs == null || needs.isEmpty()) return 1.0;
        double total = 0.0;
        int counted = 0;
        for (MethodInput need : needs)
        {
            if (need == null || need.getQuantity() <= 0) continue;
            int owned = items.quantity(need.getName());
            total += Math.min(1.0, owned / (double) need.getQuantity());
            counted++;
        }
        return counted == 0 ? 1.0 : total / counted;
    }

    private static boolean matches(
            ActionDef action,
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

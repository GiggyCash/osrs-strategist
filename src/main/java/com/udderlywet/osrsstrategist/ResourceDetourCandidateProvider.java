package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Cross-skill detours that solve a real resource shortage while advancing a
 * second useful goal.
 *
 * <p>A detour must beat the direct acquisition route on more than novelty. This
 * provider therefore keeps scores modest unless the account is an Iron-style
 * account, the shortage is observed, and the detour also progresses a skill the
 * account still needs. VERIFIED detours include concrete guidance so they can
 * legitimately compete for DO NEXT instead of existing as decorative options.</p>
 */
@Singleton
public class ResourceDetourCandidateProvider
        implements CandidateProvider
{
    @Override
    public String getId()
    {
        return "resource-detours";
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return result;
        }

        AccountSnapshot account = context.data().account();
        if (!ContentAccessRules.hasVerifiedMembership(
                account.getMembershipStatus())) return result;
        AccountMode mode = context.accountMode();
        if (!mode.isIronLike() || mode == AccountMode.ULTIMATE_IRONMAN)
        {
            return result;
        }

        ItemIndex items = new ItemIndex(
                context.data(), context.isUseGroupStorage());
        if (!items.resourceContainersObserved()) return result;

        plankDetours(context, account, items, result);
        result.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        return result;
    }

    private static void plankDetours(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!constructionRelevant(context.getActiveGoal())) return;
        int construction = account.getSkillLevel(Skill.CONSTRUCTION);
        if (construction >= 70) return;

        int planks = items.quantity(
                "Plank", "Oak plank", "Teak plank", "Mahogany plank");
        if (planks >= 150) return;

        int fishing = account.getSkillLevel(Skill.FISHING);
        if (fishing >= 35 && fishing < 80)
        {
            String id = "detour:tempoross-planks";
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                double score = 27.0;
                if (construction < 50) score += 5.0;
                if (fishing < 70) score += 5.0;
                if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        Text.get(599),
                        "Only " + planks + Text.get(602),
                        Text.get(603),
                        Text.get(604)
                );
                result.add(new Recommendation(
                        id,
                        Text.get(1296),
                        "Only " + planks + Text.get(605),
                        score,
                        Confidence.VERIFIED,
                        guidance,
                        SafetyEvidence.skill(false, Skill.FISHING)));
            }
        }

        int firemaking = account.getSkillLevel(Skill.FIREMAKING);
        int logs = items.quantity(
                "Logs", "Oak logs", "Willow logs", "Maple logs",
                "Yew logs", "Teak logs", "Mahogany logs");
        if (firemaking >= 50 && firemaking < 80 && logs < 100
                && account.getMembershipStatus() == MembershipStatus.P2P
                && context.accountMode() != AccountMode.HARDCORE_IRONMAN
                && context.accountMode() != AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            String id = "detour:wintertodt-logs";
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                double score = 20.0;
                if (account.getSkillLevel(Skill.WOODCUTTING) < 60) score += 2.0;
                if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        Text.get(606),
                        "Only " + logs + " useful logs and " + planks + Text.get(607),
                        Text.get(608),
                        Text.get(609)
                );
                result.add(new Recommendation(
                        id,
                        Text.get(600),
                        Text.get(601),
                        score,
                        Confidence.VERIFIED,
                        guidance,
                        SafetyEvidence.skill(false, Skill.FIREMAKING)));
            }
        }
    }

    private static boolean constructionRelevant(GoalType goal)
    {
        if (goal == null) return true;
        switch (goal)
        {
            case AUTOMATIC:
            case MAX:
            case QUEST_CAPE:
            case BARROWS_GLOVES:
            case PRIFDDINAS:
            case DIARY_CAPE:
            case RAID_READY:
            case TOTAL_2000:
            case BASE_70S:
            case CUSTOM:
                return true;
            default:
                return false;
        }
    }
}

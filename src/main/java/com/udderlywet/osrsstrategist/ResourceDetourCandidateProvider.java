package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Cross-skill detours that solve a real resource shortage while advancing a
 * second useful goal.
 *
 * <p>A detour must beat the direct acquisition route on more than novelty. This
 * provider therefore keeps scores modest unless the account is an Iron-style
 * account, the shortage is observed, and the detour also progresses a skill the
 * account still needs.</p>
 */
@Singleton
public class ResourceDetourCandidateProvider
        implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "resource-detours";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null)
        {
            return result;
        }

        AccountSnapshot account = context.getData().getAccount();
        AccountMode mode = context.getAccountMode();
        if (!mode.isIronLike() || mode == AccountMode.ULTIMATE_IRONMAN)
        {
            return result;
        }

        ObservedItemIndex items = new ObservedItemIndex(
                context.getData(), context.isUseGroupStorage());
        if (!items.bankObserved()) return result;

        plankDetours(context, account, items, result);
        result.sort(Comparator.comparingDouble(
                StrategyCandidate::getScore).reversed());
        return result;
    }

    private static void plankDetours(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
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
            if (!context.getPreferenceProfile().isOnCooldown(id))
            {
                double score = 27.0;
                if (construction < 50) score += 5.0;
                if (fishing < 70) score += 5.0;
                if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.getPreferenceProfile().weightFor(id) * 10.0;

                result.add(new StrategyCandidate(
                        id,
                        "Tempoross for Fishing + early planks",
                        "Only " + planks + " planks are currently observed. Tempoross can advance Fishing while its reward pool supplies some normal and oak planks. "
                                + "Treat this as a cross-skill supply detour, not a bulk Construction plank method; switch to direct log/sawmill routes when larger plank volumes are needed.",
                        score,
                        RecommendationConfidence.VERIFIED));
            }
        }

        int firemaking = account.getSkillLevel(Skill.FIREMAKING);
        int logs = items.quantity(
                "Logs", "Oak logs", "Willow logs", "Maple logs",
                "Yew logs", "Teak logs", "Mahogany logs");
        if (firemaking >= 50 && firemaking < 80 && logs < 100
                && account.getMembershipStatus() == MembershipStatus.P2P
                && context.getAccountMode() != AccountMode.HARDCORE_IRONMAN
                && context.getAccountMode() != AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            String id = "detour:wintertodt-logs";
            if (!context.getPreferenceProfile().isOnCooldown(id))
            {
                double score = 20.0;
                if (account.getSkillLevel(Skill.WOODCUTTING) < 60) score += 2.0;
                if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.getPreferenceProfile().weightFor(id) * 10.0;

                result.add(new StrategyCandidate(
                        id,
                        "Wintertodt for Firemaking + log supply",
                        "Your observed log/plank stock is low. Wintertodt can progress Firemaking and provide logs from supply crates, which can later be converted into planks. "
                                + "This is secondary to direct plank sourcing and should only win when the Firemaking progress is useful too.",
                        score,
                        RecommendationConfidence.VERIFIED));
            }
        }
    }

    private static boolean constructionRelevant(GoalType goal)
    {
        if (goal == null) return true;
        switch (goal)
        {
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

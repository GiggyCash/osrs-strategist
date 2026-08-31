package compass;
import static compass.Text.get;

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

        var account = context.data().account();
        if (!ContentAccessRules.hasVerifiedMembership(
                account.membership())) return result;
        var mode = context.accountMode();
        if (!mode.isIronLike() || mode == AccountMode.ULTIMATE_IRONMAN)
        {
            return result;
        }

        ItemIndex items = new ItemIndex(
                context.data(), context.usesGroupStorage());
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
        if (!constructionRelevant(context.goal())) return;
        var construction = account.level(Skill.CONSTRUCTION);
        if (construction >= 70) return;

        int planks = items.quantity(
                "Plank", "Oak plank", "Teak plank", "Mahogany plank");
        if (planks >= 150) return;

        var fishing = account.level(Skill.FISHING);
        if (fishing >= 35 && fishing < 80)
        {
            var id = "detour:tempoross-planks";
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                var score = 27.0;
                if (construction < 50) score += 5.0;
                if (fishing < 70) score += 5.0;
                if (context.intent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        get(599),
                        "Only " + planks + get(602),
                        get(603),
                        get(604)
                );
                result.add(new Recommendation(
                        id,
                        get(1296),
                        "Only " + planks + get(605),
                        score,
                        Confidence.VERIFIED,
                        guidance,
                        SafetyEvidence.skill(false, Skill.FISHING)));
            }
        }

        var firemaking = account.level(Skill.FIREMAKING);
        int logs = items.quantity(
                "Logs", "Oak logs", "Willow logs", "Maple logs",
                "Yew logs", "Teak logs", "Mahogany logs");
        if (firemaking >= 50 && firemaking < 80 && logs < 100
                && account.membership() == MembershipStatus.P2P
                && context.accountMode() != AccountMode.HARDCORE_IRONMAN
                && context.accountMode() != AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            var id = "detour:wintertodt-logs";
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                var score = 20.0;
                if (account.level(Skill.WOODCUTTING) < 60) score += 2.0;
                if (context.intent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        get(606),
                        "Only " + logs + " useful logs and " + planks + get(607),
                        get(608),
                        get(609)
                );
                result.add(new Recommendation(
                        id,
                        get(600),
                        get(601),
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

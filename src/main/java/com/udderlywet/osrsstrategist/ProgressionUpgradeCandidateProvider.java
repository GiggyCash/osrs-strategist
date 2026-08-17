package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * High-value account upgrades that should sometimes interrupt raw skill XP.
 *
 * <p>This provider is intentionally narrower than a universal BIS table. It
 * covers upgrades where the route and account-state decision are clear enough
 * to compete with ordinary training. Encounter-specific BIS remains in the gear
 * system and will grow independently.</p>
 */
@Singleton
public class ProgressionUpgradeCandidateProvider
        implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "progression-upgrades";
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

        StrategyDataBundle data = context.getData();
        AccountSnapshot account = data.getAccount();
        ObservedItemIndex items = new ObservedItemIndex(
                data, context.isUseGroupStorage());

        fighterTorso(context, account, items, result);
        abyssalWhip(context, account, items, result);
        dragonDefender(context, account, items, result);
        anglerOutfit(context, account, items, result);

        result.sort(Comparator.comparingDouble(
                StrategyCandidate::getScore).reversed());
        return result;
    }

    private static void fighterTorso(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() == MembershipStatus.F2P) return;
        if (account.getSkillLevel(Skill.DEFENCE) < 40) return;

        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        boolean defencePure = build == RestrictedBuildType.DEFENCE_PURE;
        if (!defencePure && Math.max(account.getSkillLevel(Skill.ATTACK),
                account.getSkillLevel(Skill.STRENGTH)) < 40)
        {
            return;
        }

        if (build == RestrictedBuildType.SKILLER
                || build == RestrictedBuildType.F2P_SKILLER
                || build == RestrictedBuildType.PRAYER_SKILLER
                || build == RestrictedBuildType.TEN_HITPOINTS)
        {
            return;
        }

        if (!ownershipCanBeJudged(account, items)) return;
        if (items.has(
                "Fighter torso", "Fighter torso (l)",
                "Bandos chestplate", "Blood moon chestplate",
                "Torva platebody", "Torva platebody (damaged)"))
        {
            return;
        }

        String id = "upgrade:fighter-torso";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;

        AccountMode mode = context.getAccountMode();
        double score = mode.isIronLike() ? 48.0 : 37.0;
        if (defencePure) score += 8.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 10.0;
        }
        score += context.getPreferenceProfile().weightFor(id) * 10.0;

        String route = defencePure
                ? "Barbarian Assault is legal for this Defence pure if the Attacker role is played on a Defence-training style only. Earn 375 honour in each role, defeat the Penance Queen once, and buy the torso without intentionally gaining Attack, Strength, Ranged, or Magic XP."
                : "Barbarian Assault is a strong torso upgrade for this melee account. Earn 375 honour points in each role and defeat the Penance Queen once, then buy the torso.";

        result.add(new StrategyCandidate(
                id,
                "Get a Fighter torso",
                route + " Strategist will stop offering this when it observes the torso or a stronger chest upgrade.",
                score,
                RecommendationConfidence.VERIFIED));
    }

    private static void abyssalWhip(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() == MembershipStatus.F2P) return;
        if (account.getSkillLevel(Skill.ATTACK) < 70) return;
        if (!AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)) return;
        if (!ownershipCanBeJudged(account, items)) return;

        if (items.has(
                "Abyssal whip", "Abyssal whip (or)", "Abyssal tentacle",
                "Blade of saeldor", "Blade of saeldor (c)",
                "Ghrazi rapier", "Osmumten's fang",
                "Soulreaper axe", "Scythe of vitur"))
        {
            return;
        }

        String id = "upgrade:abyssal-whip";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        int slayer = account.getSkillLevel(Skill.SLAYER);
        double score;
        String title;
        String reason;
        RecommendationConfidence confidence;

        if (mode.usesGrandExchange())
        {
            score = 41.0;
            title = "Get an Abyssal whip";
            reason = "You have 70 Attack and no observed whip-or-better general melee weapon. "
                    + "Buy an Abyssal whip if the live price fits your verified cash budget. "
                    + "Strategist should verify GP before turning this into an exact purchase instruction.";
            confidence = RecommendationConfidence.CHECK_NEEDED;
        }
        else if (slayer >= 85)
        {
            score = 49.0;
            title = "Get an Abyssal whip";
            reason = "You have 85 Slayer and can self-source the whip from abyssal demons. "
                    + "Kill them on a safe account-appropriate route until the whip drops; keep Wilderness methods disabled unless explicitly allowed.";
            confidence = RecommendationConfidence.VERIFIED;
        }
        else
        {
            if (context.getActiveGoal() != GoalType.MAX
                    && context.getActiveGoal() != GoalType.SLAYER_85
                    && context.getActiveGoal() != GoalType.GEAR_TARGET
                    && context.getActiveGoal() != GoalType.RAID_READY)
            {
                return;
            }
            int remaining = 85 - slayer;
            score = Math.max(24.0, 42.0 - remaining * 0.8);
            title = "Work toward 85 Slayer for a whip";
            reason = "This Iron-style account cannot buy the upgrade. Abyssal demons unlock at 85 Slayer, so Slayer progression is the acquisition route instead of the Grand Exchange.";
            confidence = RecommendationConfidence.VERIFIED;
        }

        score += context.getPreferenceProfile().weightFor(id) * 10.0;
        result.add(new StrategyCandidate(id, title, reason, score, confidence));
    }

    private static void dragonDefender(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() == MembershipStatus.F2P) return;
        if (!ownershipCanBeJudged(account, items)) return;

        // The Warriors' Guild can be entered from Attack + Strength, but the
        // Dragon defender itself requires 60 Defence to equip. Never suggest a
        // technically obtainable but unusable defender to a pure.
        if (account.getSkillLevel(Skill.DEFENCE) < 60) return;
        if (!AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                || !AccountBuildPolicy.allowsSkill(account, Skill.STRENGTH))
        {
            return;
        }

        int attack = account.getSkillLevel(Skill.ATTACK);
        int strength = account.getSkillLevel(Skill.STRENGTH);
        if (attack + strength < 130) return;

        if (items.has("Dragon defender", "Dragon defender (t)",
                "Avernic defender", "Avernic defender (l)"))
        {
            return;
        }

        String id = "upgrade:dragon-defender";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        double score = 45.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 8.0;
        }
        score += context.getPreferenceProfile().weightFor(id) * 10.0;
        result.add(new StrategyCandidate(
                id,
                "Get a Dragon defender",
                "Your Attack + Strength meet the Warriors' Guild requirement, you have 60 Defence to equip the reward, and no Dragon/Avernic defender is observed. "
                        + "Enter the Warriors' Guild, work through defender tiers in the Cyclops rooms, then obtain the Dragon defender from the basement Cyclopes.",
                score,
                RecommendationConfidence.VERIFIED));
    }

    private static void anglerOutfit(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() == MembershipStatus.F2P) return;
        int fishing = account.getSkillLevel(Skill.FISHING);
        if (fishing < 15) return;
        if (!ownershipCanBeJudged(account, items)) return;

        int pieces = anglerPieces(items);
        if (pieces >= 4) return;

        String id = "upgrade:angler-outfit";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;

        int currentXp = account.getSkillExperience(Skill.FISHING);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(fishing);
        int remainingXp = Math.max(0,
                Experience.getXpForLevel(99) - currentXp);

        double score = 16.0;
        if (context.isCollectionistMode()) score += 30.0;
        if (fishing >= 82) score += 17.0;
        if (context.getActiveGoal() == GoalType.MAX && remainingXp >= 5_000_000)
            score += 12.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET) score += 5.0;
        score += pieces * 2.0;
        score += context.getPreferenceProfile().weightFor(id) * 10.0;

        if (score < 25.0) return;
        result.add(new StrategyCandidate(
                id,
                "Finish the Angler outfit (" + pieces + "/4)",
                "Fishing Trawler can fill the missing Angler pieces. The full set gives a 2.5% Fishing XP bonus and is also needed for minnow access. "
                        + "Strategist keeps this below direct Fishing when the remaining XP savings do not justify the detour.",
                score,
                RecommendationConfidence.VERIFIED));
    }

    private static int anglerPieces(ObservedItemIndex items)
    {
        int pieces = 0;
        if (items.has("Angler hat", "Spirit angler headband")) pieces++;
        if (items.has("Angler top", "Spirit angler top")) pieces++;
        if (items.has("Angler waders", "Spirit angler waders")) pieces++;
        if (items.has("Angler boots", "Spirit angler boots")) pieces++;
        return pieces;
    }

    private static boolean ownershipCanBeJudged(
            AccountSnapshot account,
            ObservedItemIndex items)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        return mode == AccountMode.ULTIMATE_IRONMAN || items.bankObserved();
    }
}

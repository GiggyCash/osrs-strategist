package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Chooses a useful permanent Slayer reward without consuming the skip reserve. */
@Singleton
public class SlayerRewardAdvisor
{
    private static final int SKIP_RESERVE = SlayerPointEconomy.SKIP_COST;

    public SlayerRewardAdvice recommend(StrategyContext context,
            SlayerSnapshot slayer)
    {
        if (context == null || slayer == null || slayer.getRewards() == null)
            return null;
        List<SlayerRewardAdvice> candidates = new ArrayList<>();
        AccountSnapshot account = context.getData().getAccount();
        int slayerLevel = account.getSkillLevel(Skill.SLAYER);
        int crafting = account.getSkillLevel(Skill.CRAFTING);
        int fletching = account.getSkillLevel(Skill.FLETCHING);

        add(candidates, slayer, SlayerReward.BIGGER_AND_BADDER,
                slayerLevel >= 5, 100.0,
                "Superior creatures add durable Slayer XP and unique-drop value across future eligible tasks.");
        add(candidates, slayer, SlayerReward.MALEVOLENT_MASQUERADE,
                crafting >= 55, context.getAccountMode().isIronLike() ? 82.0 : 74.0,
                "The Slayer helmet combines observed task protections with the black-mask task bonus once the components are obtained.");
        add(candidates, slayer, SlayerReward.BROADER_FLETCHING,
                context.getAccountMode().isIronLike() && fletching >= 52,
                78.0,
                "Self-made broad ammunition is a permanent ranged-supply route for an Iron account.");
        add(candidates, slayer, SlayerReward.RING_BLING,
                crafting >= 75 && context.getAccountMode().isIronLike(),
                context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                        ? 76.0 : 68.0,
                "Crafted Slayer rings provide reusable task travel without relying on the Grand Exchange.");
        add(candidates, slayer, SlayerReward.TASK_STORAGE,
                context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                        || context.getSessionIntent() == SessionIntent.QUICK_20_MIN,
                context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                        ? 72.0 : 62.0,
                "Task Storage can preserve a valuable assignment when the current inventory or session cannot support it.");
        add(candidates, slayer, SlayerReward.HOT_STUFF,
                context.getActiveGoal() == GoalType.FIRE_CAPE
                        || context.getActiveGoal() == GoalType.INFERNAL_CAPE,
                70.0,
                "On-task Fight Caves or Inferno attempts align the selected cape goal with Slayer experience and helmet value.");
        add(candidates, slayer, SlayerReward.LIKE_A_BOSS,
                context.getActiveGoal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS
                        || context.getActiveGoal() == GoalType.GEAR_TARGET,
                66.0,
                "Boss assignments can align future tasks with the selected PvM objective; access requirements remain independently checked.");

        boolean longXpSession = context.getSessionIntent()
                == SessionIntent.LONG_SESSION
                && (context.getActiveGoal() == GoalType.SLAYER_85
                    || context.getActiveGoal() == GoalType.MAX);
        add(candidates, slayer, SlayerReward.EXTEND_DUST_DEVILS,
                longXpSession && slayerLevel >= 65, 64.0,
                "Longer dust-devil assignments preserve a high-value multitarget Magic setup during a long Slayer or Max session.");
        add(candidates, slayer, SlayerReward.EXTEND_NECHRYAELS,
                longXpSession && slayerLevel >= 80, 65.0,
                "Longer nechryael assignments preserve a high-value multitarget Magic setup during a long Slayer or Max session.");
        add(candidates, slayer, SlayerReward.EXTEND_ABYSSAL_DEMONS,
                longXpSession && slayerLevel >= 85, 61.0,
                "Longer abyssal-demon assignments fit the selected long Slayer progression goal without changing task legality.");
        add(candidates, slayer, SlayerReward.EXTEND_BLOODVELDS,
                context.getAccountMode().isIronLike()
                        && context.getSessionIntent() != SessionIntent.QUICK_20_MIN
                        && slayerLevel >= 50,
                58.0,
                "Longer bloodveld assignments can retain an Iron account's useful drop and combat-training setup when session time supports it.");
        add(candidates, slayer, SlayerReward.EXTEND_GARGOYLES,
                context.getAccountMode().isIronLike()
                        && context.getSessionIntent() != SessionIntent.QUICK_20_MIN
                        && slayerLevel >= 75,
                57.0,
                "Longer gargoyle assignments can retain a profitable low-attention setup for an Iron account, but do not outrank core permanent unlocks.");
        add(candidates, slayer, SlayerReward.EXTEND_KRAKEN,
                slayerLevel >= 87
                        && (context.getActiveGoal() == GoalType.GEAR_TARGET
                            || context.getStrategyMode() == StrategyMode.RELAXED),
                56.0,
                "Longer cave-kraken assignments suit a verified gear objective or low-attention strategy after higher-value permanent unlocks.");

        return candidates.stream().max(Comparator.comparingDouble(
                SlayerRewardAdvice::getScore)).orElse(null);
    }

    private static void add(List<SlayerRewardAdvice> candidates,
            SlayerSnapshot slayer, SlayerReward reward, boolean eligible,
            double score, String reason)
    {
        if (!eligible
                || slayer.getRewards().stateOf(reward) != CapabilityState.BLOCKED
                || slayer.getPoints() < reward.getPointCost() + SKIP_RESERVE)
            return;
        candidates.add(new SlayerRewardAdvice(reward, score, reason));
    }
}

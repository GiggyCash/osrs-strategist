package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
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
        AccountSnapshot account = context.data().account();
        int slayerLevel = account.getSkillLevel(Skill.SLAYER);
        int crafting = account.getSkillLevel(Skill.CRAFTING);
        int fletching = account.getSkillLevel(Skill.FLETCHING);

        add(candidates, slayer, SlayerReward.BIGGER_AND_BADDER,
                slayerLevel >= 5, 100.0,
                get(792));
        add(candidates, slayer, SlayerReward.MALEVOLENT_MASQUERADE,
                crafting >= 55, context.accountMode().isIronLike() ? 82.0 : 74.0,
                get(797));
        add(candidates, slayer, SlayerReward.BROADER_FLETCHING,
                context.accountMode().isIronLike() && fletching >= 52,
                78.0,
                get(798));
        add(candidates, slayer, SlayerReward.RING_BLING,
                crafting >= 75 && context.accountMode().isIronLike(),
                context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                        ? 76.0 : 68.0,
                get(799));
        add(candidates, slayer, SlayerReward.TASK_STORAGE,
                context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                        || context.getSessionIntent() == SessionIntent.QUICK_20_MIN,
                context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                        ? 72.0 : 62.0,
                get(800));
        add(candidates, slayer, SlayerReward.HOT_STUFF,
                context.getActiveGoal() == GoalType.FIRE_CAPE
                        || context.getActiveGoal() == GoalType.INFERNAL_CAPE,
                70.0,
                get(801));
        add(candidates, slayer, SlayerReward.LIKE_A_BOSS,
                context.getActiveGoal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS
                        || context.getActiveGoal() == GoalType.GEAR_TARGET,
                66.0,
                get(802));

        boolean longXpSession = context.getSessionIntent()
                == SessionIntent.LONG_SESSION
                && (context.getActiveGoal() == GoalType.SLAYER_85
                    || context.getActiveGoal() == GoalType.MAX);
        add(candidates, slayer, SlayerReward.EXTEND_DUST_DEVILS,
                longXpSession && slayerLevel >= 65, 64.0,
                get(803));
        add(candidates, slayer, SlayerReward.EXTEND_NECHRYAELS,
                longXpSession && slayerLevel >= 80, 65.0,
                get(804));
        add(candidates, slayer, SlayerReward.EXTEND_ABYSSAL_DEMONS,
                longXpSession && slayerLevel >= 85, 61.0,
                get(793));
        add(candidates, slayer, SlayerReward.EXTEND_BLOODVELDS,
                context.accountMode().isIronLike()
                        && context.getSessionIntent() != SessionIntent.QUICK_20_MIN
                        && slayerLevel >= 50,
                58.0,
                get(794));
        add(candidates, slayer, SlayerReward.EXTEND_GARGOYLES,
                context.accountMode().isIronLike()
                        && context.getSessionIntent() != SessionIntent.QUICK_20_MIN
                        && slayerLevel >= 75,
                57.0,
                get(795));
        add(candidates, slayer, SlayerReward.EXTEND_KRAKEN,
                slayerLevel >= 87
                        && (context.getActiveGoal() == GoalType.GEAR_TARGET
                            || context.getStrategyMode() == StrategyMode.RELAXED),
                56.0,
                get(796));

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

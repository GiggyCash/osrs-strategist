package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Sailing-specific progression planner.
 *
 * <p>Sailing contains one-off charting XP, variable port tasks, salvaging, and
 * fixed-completion Barracuda Trials. Only the fixed trial completion XP is
 * reduced to an exact repeat count. Other routes receive concrete unlock/tool
 * guidance instead of fake precision.</p>
 */
@Singleton
public class SailingGuidanceService
{
    public RecommendationGuidance build(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.getAccount() == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }
        AccountSnapshot account = data.getAccount();
        if (account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return null;
        }

        String id = plan.getMethod().getId() == null
                ? "" : plan.getMethod().getId();
        int currentXp = account.getSkillExperience(Skill.SAILING);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        if (id.startsWith("sailing_barracuda_"))
        {
            return barracudaGuidance(data, id, targetLevel, xpNeeded);
        }
        if ("sailing_salvage_small".equals(id))
        {
            return salvageGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if ("sailing_courier".equals(id))
        {
            return courierGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if ("sailing_deep_sea_trawling".equals(id))
        {
            return new RecommendationGuidance(
                    "Use Deep Sea Trawling until you gain " + format(xpNeeded)
                            + " Sailing XP toward level " + targetLevel + ".",
                    Text.get(732),
                    Text.get(743),
                    Text.get(754)
            );
        }

        return chartingGuidance(data, currentLevel, targetLevel, xpNeeded);
    }

    private static RecommendationGuidance barracudaGuidance(
            StrategyDataBundle data,
            String methodId,
            int targetLevel,
            int xpNeeded)
    {
        Trial trial;
        if (methodId.contains("gwenith"))
        {
            trial = new Trial(
                    "The Gwenith Glide",
                    16050,
                    Text.get(765),
                    Text.get(776));
        }
        else if (methodId.contains("jubbly"))
        {
            trial = new Trial(
                    "The Jubbly Jive",
                    6200,
                    Text.get(787),
                    Text.get(788));
        }
        else
        {
            trial = new Trial(
                    "The Tempor Tantrum",
                    1250,
                    Text.get(789),
                    Text.get(790));
        }

        int marlinCompletions = divideRoundUp(xpNeeded, trial.marlinXp);
        String action = Text.get(733)
                + trial.name + Text.get(734)
                + marlinCompletions + " Marlin completion"
                + (marlinCompletions == 1 ? "" : "s")
                + " at " + format(trial.marlinXp)
                + " XP each covers the remaining " + format(xpNeeded)
                + " XP to level " + targetLevel + ".";

        String supplies = trial.requirements;
        String note = Text.get(735);
        return new RecommendationGuidance(
                action, supplies, trial.location, note);
    }

    private static RecommendationGuidance salvageGuidance(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = Text.get(736)
                + format(xpNeeded) + " XP toward level " + targetLevel + ".";
        String supplies = Text.get(737);
        String where = Text.get(738);
        String note = Text.get(739);
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance courierGuidance(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = Text.get(740)
                + format(xpNeeded) + " Sailing XP to level " + targetLevel + ".";
        String supplies = Text.get(741);
        String where = Text.get(742);
        String note = Text.get(744);
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance chartingGuidance(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        QuestSnapshot quests = data.getQuests();
        if (!complete(quests, "Pandemonium"))
        {
            return new RecommendationGuidance(
                    Text.get(745),
                    Text.get(746),
                    Text.get(747),
                    Text.get(748)
            );
        }

        StringBuilder action = new StringBuilder();
        action.append(Text.get(749))
                .append(format(xpNeeded)).append(" Sailing XP to level ")
                .append(targetLevel).append(".");

        StringBuilder supplies = new StringBuilder(Text.get(750));
        if (currentLevel >= 12 && !complete(quests, "Prying Times"))
        {
            supplies.append(Text.get(751));
        }
        if (currentLevel >= 22 && !complete(quests, "Current Affairs"))
        {
            supplies.append(Text.get(752));
        }
        if (currentLevel >= 15)
        {
            AccountEconomySnapshot economy = data.getEconomy();
            if (economy != null
                    && economy.getConfidence() == RecommendationConfidence.VERIFIED)
            {
                if (economy.getCoins() >= 15000)
                {
                    supplies.append(Text.get(753));
                }
                else
                {
                    supplies.append(" You are ")
                            .append(format(15000 - economy.getCoins()))
                            .append(Text.get(755));
                }
            }
            else
            {
                supplies.append(Text.get(756));
            }
        }

        String where = Text.get(757);
        String note = Text.get(758);
        return new RecommendationGuidance(
                action.toString(), supplies.toString(), where, note);
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static final class Trial
    {
        private final String name;
        private final int marlinXp;
        private final String requirements;
        private final String location;

        private Trial(String name, int marlinXp, String requirements, String location)
        {
            this.name = name;
            this.marlinXp = marlinXp;
            this.requirements = requirements;
            this.location = location;
        }
    }
}

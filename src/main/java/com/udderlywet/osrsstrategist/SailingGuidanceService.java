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
    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }
        AccountSnapshot account = data.account();
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
            return new Guidance(
                    Text.get(1370) + format(xpNeeded)
                            + Text.get(1371) + targetLevel + ".",
                    Text.get(732),
                    Text.get(743),
                    Text.get(754)
            );
        }

        return chartingGuidance(data, currentLevel, targetLevel, xpNeeded);
    }

    private static Guidance barracudaGuidance(
            GameData data,
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
                    Text.get(1372),
                    1250,
                    Text.get(789),
                    Text.get(790));
        }

        int marlinCompletions = divideRoundUp(xpNeeded, trial.marlinXp);
        String action = Text.get(733)
                + trial.name + Text.get(734)
                + marlinCompletions + Text.get(1373)
                + (marlinCompletions == 1 ? "" : "s")
                + " at " + format(trial.marlinXp)
                + Text.get(1374) + format(xpNeeded)
                + " XP to level " + targetLevel + ".";

        String supplies = trial.requirements;
        String note = Text.get(735);
        return new Guidance(
                action, supplies, trial.location, note);
    }

    private static Guidance salvageGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = Text.get(736)
                + format(xpNeeded) + " XP toward level " + targetLevel + ".";
        String supplies = Text.get(737);
        String where = Text.get(738);
        String note = Text.get(739);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance courierGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = Text.get(740)
                + format(xpNeeded) + Text.get(1375) + targetLevel + ".";
        String supplies = Text.get(741);
        String where = Text.get(742);
        String note = Text.get(744);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance chartingGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        QuestSnapshot quests = data.quests();
        if (!complete(quests, "Pandemonium"))
        {
            return new Guidance(
                    Text.get(745),
                    Text.get(746),
                    Text.get(747),
                    Text.get(748)
            );
        }

        StringBuilder action = new StringBuilder();
        action.append(Text.get(749))
                .append(format(xpNeeded)).append(Text.get(1375))
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
            AccountEconomySnapshot economy = data.economy();
            if (economy != null
                    && economy.getConfidence() == Confidence.VERIFIED)
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
        return new Guidance(
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

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
                    "Bring a safe boat/trawling setup and enough repair supplies for the trip. Per-catch Sailing XP depends on the live trawling loop, so no exact catch count is shown.",
                    "Use the highest safe Deep Sea Trawling route your verified boat and Sailing/Fishing levels support.",
                    "Deep Sea Trawling is a Fishing/Sailing hybrid focused more on valuable fish than maximum Sailing XP. Use it when the hybrid rewards fit the account better than Barracuda Trials."
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
                    "Use a skiff with at least an adamant keel and complete Regicide before attempting the high-level trial.",
                    "Gwenith Glide starts from its Barracuda Trial location in the western/northern Sailing progression area.");
        }
        else if (methodId.contains("jubbly"))
        {
            trial = new Trial(
                    "The Jubbly Jive",
                    6200,
                    "Use a skiff with at least a mithril helm and an inoculation station.",
                    "Jubbly Jive is the level-55 Barracuda Trial in Backwater.");
        }
        else
        {
            trial = new Trial(
                    "The Tempor Tantrum",
                    1250,
                    "Use a skiff with at least an iron helm, oak masts, and linen sails.",
                    "Tempor Tantrum starts near The Storm Tempor.");
        }

        int marlinCompletions = divideRoundUp(xpNeeded, trial.marlinXp);
        String action = "Aim for Marlin-rank completions of " + trial.name
                + ". After one-time rank bonuses are already claimed, about "
                + marlinCompletions + " Marlin completion"
                + (marlinCompletions == 1 ? "" : "s")
                + " at " + format(trial.marlinXp)
                + " XP each covers the remaining " + format(xpNeeded)
                + " XP to level " + targetLevel + ".";

        String supplies = trial.requirements;
        String note = "The one-time rank bonuses for Swordfish, Shark, and Marlin add extra XP the first time they are claimed, so an account with unclaimed bonuses will finish sooner than this repeat-only count. Boat speed and player execution affect XP per hour, not the listed Marlin completion XP.";
        return new RecommendationGuidance(
                action, supplies, trial.location, note);
    }

    private static RecommendationGuidance salvageGuidance(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = "Salvage the highest safe shipwreck tier unlocked at Sailing "
                + currentLevel + " until you gain " + format(xpNeeded)
                + " XP toward level " + targetLevel + ". Sort salvage when the resources matter; discard it only when pure Sailing XP matters more.";
        String supplies = currentLevel >= 15
                ? "Use a skiff with a cargo hold and a salvaging hook. If you do not own a skiff yet, buy one for 15,000 coins before committing to this route."
                : "Reach 15 Sailing before using the skiff + salvaging-hook route.";
        String where = "Use the highest shipwreck tier your Sailing level and boat can safely reach, with a nearby salvaging station when you want the resources.";
        String note = "Salvaging XP depends on the shipwreck tier and interaction loop, so Strategist reports exact XP remaining but does not create a false universal salvage count. Iron-style accounts get extra value from sorting the resources instead of dropping salvage.";
        return new RecommendationGuidance(action, supplies, where, note);
    }

    private static RecommendationGuidance courierGuidance(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        String action = "Take courier Port Tasks between ports you already have unlocked and stack compatible pickups/deliveries. You need "
                + format(xpNeeded) + " Sailing XP to level " + targetLevel + ".";
        String supplies = "Use your current verified boat. At 15 Sailing, a skiff costs 15,000 coins and unlocks the practical salvaging setup; do not buy it early if the account cannot afford it.";
        String where = "Use notice boards at verified ports and prefer tasks that can be chained along the same route instead of dead-heading the boat between jobs.";
        String note = "Port Task XP varies with the assignment, so Strategist keeps the exact remaining XP but waits for a concrete live task before showing a task count.";
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
                    "Complete Pandemonium to unlock Sailing training.",
                    "Keep the starter boat/tools from the Sailing tutorial progression.",
                    "Start Pandemonium from its normal quest start and finish the Sailing tutorial sequence.",
                    "Sailing is members-only and normal training begins after Pandemonium. The route refreshes automatically when the quest state changes."
            );
        }

        StringBuilder action = new StringBuilder();
        action.append("Complete every reachable uncompleted sea-charting objective. You need ")
                .append(format(xpNeeded)).append(" Sailing XP to level ")
                .append(targetLevel).append(".");

        StringBuilder supplies = new StringBuilder("Use the Captain's log and charting tools you have unlocked.");
        if (currentLevel >= 12 && !complete(quests, "Prying Times"))
        {
            supplies.append(" Complete Prying Times for the crowbar so more charting steps become available.");
        }
        if (currentLevel >= 22 && !complete(quests, "Current Affairs"))
        {
            supplies.append(" Complete Current Affairs for the current duck to unlock more charting locations.");
        }
        if (currentLevel >= 15)
        {
            AccountEconomySnapshot economy = data.getEconomy();
            if (economy != null
                    && economy.getConfidence() == RecommendationConfidence.VERIFIED)
            {
                if (economy.getCoins() >= 15000)
                {
                    supplies.append(" You have enough verified cash for the 15,000-coin skiff when salvaging becomes the better session fit.");
                }
                else
                {
                    supplies.append(" You are ")
                            .append(format(15000 - economy.getCoins()))
                            .append(" coins short of the 15,000-coin skiff; do not route into skiff-dependent salvaging yet.");
                }
            }
            else
            {
                supplies.append(" A skiff costs 15,000 coins; Strategist needs verified cash before treating that purchase as ready.");
            }
        }

        String where = "Work through charting tasks in reachable sea regions and claim each region's completion XP before sailing long distances solely for repeatable training.";
        String note = "Sea charting is one-time account progress. Because Strategist does not yet read every individual Captain's-log checkbox, it will not invent a number of charts remaining. At 30 Sailing, Barracuda Trials become the fast repeatable route and can be converted into exact completion counts.";
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

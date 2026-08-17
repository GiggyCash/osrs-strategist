package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Account-aware Slayer task guidance without inventing task-specific DPS. */
@Singleton
public class SlayerGuidanceService
{
    public RecommendationGuidance build(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel)
    {
        if (data == null || data.getAccount() == null) return null;
        AccountSnapshot account = data.getAccount();
        if (!AccountBuildPolicy.allowsSkill(account, Skill.SLAYER)) return null;
        if (account.getMembershipStatus() == MembershipStatus.F2P) return null;

        int currentXp = account.getSkillExperience(Skill.SLAYER);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        SlayerSnapshot slayer = data.getSlayer();
        if (slayer != null && slayer.hasTask())
        {
            String master = slayer.getMasterName() == null
                    || slayer.getMasterName().trim().isEmpty()
                    ? "your current Slayer master"
                    : slayer.getMasterName();
            String action = "Finish your current " + slayer.getTaskName()
                    + " assignment: " + slayer.getRemaining()
                    + " kills remain. You need " + format(xpNeeded)
                    + " Slayer XP to level " + targetLevel + ".";
            String supplies = "Use the task's required Slayer protection/item first, then choose the strongest build-legal combat style and gear you can sustain. Task-specific loadout and supply quantities should only become exact after Strategist resolves this monster's mechanics.";
            String where = "Continue the assignment from " + master
                    + ". If the task has multiple locations, prefer the safest reachable non-Wilderness location unless Wilderness methods are explicitly enabled.";
            String note = "The remaining kill count is live task evidence. Slayer XP per kill depends on the assigned monster's Hitpoints and variants, so Strategist does not convert the milestone into a fake universal kill count.";
            return new RecommendationGuidance(action, supplies, where, note);
        }

        SlayerMasterChoice master = bestMaster(account, data.getQuests());
        String action = "Get a new Slayer assignment from " + master.name
                + ". You need " + format(xpNeeded)
                + " Slayer XP to level " + targetLevel + ".";
        String supplies = "Do not pre-buy a task loadout before the assignment is known. Once Strategist observes the task, it can rank the required protection item, combat style, location, food, prayer, and ammunition/runes for that specific monster.";
        String note = master.reason + " Wilderness Slayer is intentionally excluded from automatic master selection because its risk must be explicitly enabled.";
        return new RecommendationGuidance(action, supplies, master.location, note);
    }

    private static SlayerMasterChoice bestMaster(
            AccountSnapshot account,
            QuestSnapshot quests)
    {
        int combat = combatLevel(account);
        int slayer = account.getSkillLevel(Skill.SLAYER);

        if (combat >= 100 && slayer >= 50 && complete(quests, "Shilo Village"))
        {
            return new SlayerMasterChoice(
                    "Duradel/Kuradal",
                    "Shilo Village",
                    "Highest standard master available at 100 combat and 50 Slayer after Shilo Village.");
        }
        if (combat >= 85)
        {
            return new SlayerMasterChoice(
                    "Nieve/Steve",
                    "Tree Gnome Stronghold",
                    "High-level master available from 85 combat.");
        }
        if (combat >= 75)
        {
            return new SlayerMasterChoice(
                    "Konar quo Maten",
                    "Mount Karuulm",
                    "Available from 75 combat; assignments require her specified location.");
        }
        if (combat >= 70 && complete(quests, "Lost City"))
        {
            return new SlayerMasterChoice(
                    "Chaeldar",
                    "Zanaris",
                    "Available from 70 combat after Lost City.");
        }
        if (combat >= 40)
        {
            return new SlayerMasterChoice(
                    "Vannaka",
                    "Edgeville Dungeon",
                    "Available from 40 combat without adding a Wilderness requirement.");
        }
        if (combat >= 20 && complete(quests, "Priest in Peril"))
        {
            return new SlayerMasterChoice(
                    "Mazchna/Achtryn",
                    "Canifis",
                    "Available from 20 combat after Priest in Peril.");
        }
        return new SlayerMasterChoice(
                "Turael/Aya",
                "Burthorpe",
                "No combat-level requirement; this is the safe baseline when a higher master is not verified.");
    }

    /** Mirrors the standard OSRS combat-level formula closely enough for gates. */
    static int combatLevel(AccountSnapshot account)
    {
        double base = 0.25 * (
                account.getSkillLevel(Skill.DEFENCE)
                        + account.getSkillLevel(Skill.HITPOINTS)
                        + Math.floor(account.getSkillLevel(Skill.PRAYER) / 2.0));
        double melee = 0.325 * (
                account.getSkillLevel(Skill.ATTACK)
                        + account.getSkillLevel(Skill.STRENGTH));
        double ranged = 0.325 * Math.floor(
                account.getSkillLevel(Skill.RANGED) * 1.5);
        double magic = 0.325 * Math.floor(
                account.getSkillLevel(Skill.MAGIC) * 1.5);
        return (int) Math.floor(base + Math.max(melee, Math.max(ranged, magic)));
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static final class SlayerMasterChoice
    {
        private final String name;
        private final String location;
        private final String reason;

        private SlayerMasterChoice(String name, String location, String reason)
        {
            this.name = name;
            this.location = location;
            this.reason = reason;
        }
    }
}

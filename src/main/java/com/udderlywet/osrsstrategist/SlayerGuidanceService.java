package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Account-aware Slayer task guidance without inventing task-specific DPS. */
@Singleton
public class SlayerGuidanceService
{
    private final SlayerTaskProfileCatalog taskProfiles;

    @Inject
    public SlayerGuidanceService(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null
                ? new SlayerTaskProfileCatalog() : taskProfiles;
    }

    public SlayerGuidanceService()
    {
        this(new SlayerTaskProfileCatalog());
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel)
    {
        return build(data, currentLevel, targetLevel, true);
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            int currentLevel,
            int targetLevel,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null) return null;
        AccountSnapshot account = data.getAccount();
        if (!AccountBuildPolicy.allowsSkill(account, Skill.SLAYER)) return null;
        if (account.getMembershipStatus() != MembershipStatus.P2P) return null;

        int currentXp = account.getSkillExperience(Skill.SLAYER);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        SlayerSnapshot slayer = data.getSlayer();
        if (slayer != null && slayer.hasTask())
        {
            SlayerTaskProfile profile = taskProfiles.profileFor(slayer.getTaskName());
            ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
            String action = taskAction(slayer, profile, xpNeeded, targetLevel);
            String supplies = taskSupplies(account, items, profile);
            String where = taskLocation(slayer, profile);
            String note = taskNote(account, profile);
            return new RecommendationGuidance(action, supplies, where, note);
        }

        SlayerMasterChoice master = bestMaster(account, data.getQuests());
        String action = "Get a new Slayer assignment from " + master.name
                + ". You need " + format(xpNeeded)
                + " Slayer XP to level " + targetLevel + ".";
        String supplies = "Do not pre-buy a task loadout before the assignment is known. Get the assignment first, then check its required protection, legal damage options, location, and supplies against the account's observed gear and storage.";
        String note = master.reason + " Wilderness Slayer is intentionally excluded from automatic master selection because its risk must be explicitly enabled.";
        return new RecommendationGuidance(action, supplies, master.location, note);
    }

    private static String taskAction(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile,
            int xpNeeded,
            int targetLevel)
    {
        StringBuilder action = new StringBuilder();
        action.append("Finish your current ")
                .append(slayer.getTaskName())
                .append(" assignment: ")
                .append(slayer.getRemaining())
                .append(" kills remain. You need ")
                .append(format(xpNeeded))
                .append(" Slayer XP to level ")
                .append(targetLevel).append(".");
        if (profile != null && hasText(profile.getStyleGuidance()))
        {
            action.append(" ").append(profile.getStyleGuidance());
        }
        return action.toString();
    }

    private static String taskSupplies(
            AccountSnapshot account,
            ObservedItemIndex items,
            SlayerTaskProfile profile)
    {
        if (profile == null || profile.getRequiredProtection().isEmpty())
        {
            return "No catalogued mandatory Slayer item is known for this task. Use the strongest build-legal sustainable setup you own; food, prayer, ammunition and rune quantities remain account/gear dependent rather than a fake fixed inventory.";
        }

        List<String> required = profile.getRequiredProtection();
        String owned = firstOwned(items, required);
        if (owned != null)
        {
            return "Verified: you own " + owned
                    + ", which satisfies this task's catalogued protection/kill requirement. Keep it equipped or in inventory as the mechanic requires; then use the strongest build-legal sustainable gear around it.";
        }

        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        String choices = joinChoices(required);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            int restricted = restrictedOwned(items, required);
            if (restricted > 0)
            {
                return "You do not currently have a directly usable required task item. One is observed in retrieval-only UIM storage. Retrieve a legal option from: "
                        + choices
                        + ", but preserve the current inventory setup when the retrieval cost is worse than taking a different task route.";
            }
            return "Acquire one legal task item just in time before continuing: "
                    + choices
                    + ". Normal bank state is ignored for UIM; inaccessible stored items require a retrieval step.";
        }

        if (!items.bankObserved())
        {
            return "Open your bank once to verify the mandatory task item. Valid options include: "
                    + choices + ".";
        }

        if (mode.isIronLike())
        {
            return "No required task item is observed in usable storage. Self-source one legal option before continuing: "
                    + choices + ".";
        }
        return "No required task item is observed. Obtain one legal option before continuing: "
                + choices
                + ". For a Main, compare live price and observed cash when the selected option is tradeable.";
    }

    private static String taskLocation(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile)
    {
        if (hasText(slayer.getTaskLocation()))
        {
            return "Your live assignment specifies "
                    + slayer.getTaskLocation()
                    + ". Use that area unless the task state changes.";
        }
        if (profile != null && hasText(profile.getPreferredLocation()))
        {
            return profile.getPreferredLocation();
        }
        if (hasText(slayer.getMasterName()))
        {
            return "Continue the assignment from "
                    + slayer.getMasterName()
                    + ". If the task has multiple locations, prefer the safest reachable non-Wilderness option unless Wilderness methods are explicitly enabled.";
        }
        return "Use the safest reachable non-Wilderness location for this task unless the assignment itself specifies an area or Wilderness methods are explicitly enabled.";
    }

    private static String taskNote(AccountSnapshot account,
            SlayerTaskProfile profile)
    {
        String base = "The remaining count comes from the live assignment. Slayer XP per kill varies with the assigned monster and variant, so no fixed kills-to-level estimate is shown.";
        if (profile == null) return base;
        StringBuilder note = new StringBuilder();
        if (hasText(profile.getMechanicsNote()))
        {
            note.append(profile.getMechanicsNote()).append(" ");
        }
        if (profile.getMultiTargetMagicEligibility() == CapabilityState.VERIFIED)
            note.append("Multitarget Magic is supported for this task, but use it only when spellbook, runes, prayer, build, and the live location are ready. ");
        if (profile.getCannonEligibility() == CapabilityState.UNKNOWN)
            note.append("Cannon use is not confirmed for the live location; do not bring or place one yet. ");
        if (profile.isWildernessVariantKnown())
            note.append("A Wilderness variant exists, but the safe default remains non-Wilderness unless risk is explicitly enabled. ");
        if (AccountMode.fromTypeCode(account.getAccountTypeCode()).isIronLike()
                && !profile.getIronObjectives().isEmpty())
            note.append("Iron objective: ").append(String.join(", ",
                    profile.getIronObjectives())).append(". ");
        if (hasText(profile.getTaskDecisionGuidance()))
            note.append(profile.getTaskDecisionGuidance()).append(" ");
        return note.append(base).toString();
    }

    private static String firstOwned(
            ObservedItemIndex items,
            List<String> candidates)
    {
        for (String candidate : candidates)
        {
            if (items.has(candidate)) return candidate;
        }
        return null;
    }

    private static int restrictedOwned(
            ObservedItemIndex items,
            List<String> candidates)
    {
        int total = 0;
        for (String candidate : candidates)
        {
            total += items.restrictedQuantity(candidate);
        }
        return total;
    }

    private static String joinChoices(List<String> choices)
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < choices.size(); i++)
        {
            if (i > 0) text.append(i == choices.size() - 1 ? " or " : ", ");
            text.append(choices.get(i));
        }
        return text.toString();
    }

    private static SlayerMasterChoice bestMaster(
            AccountSnapshot account,
            QuestSnapshot quests)
    {
        int combat = combatLevel(account);
        int slayer = account.getSkillLevel(Skill.SLAYER);

        if (combat >= 100 && slayer >= 50 && complete(quests, "Shilo Village"))
            return new SlayerMasterChoice("Duradel/Kuradal", "Shilo Village",
                    "Highest standard master available at 100 combat and 50 Slayer after Shilo Village.");
        if (combat >= 85)
            return new SlayerMasterChoice("Nieve/Steve", "Tree Gnome Stronghold",
                    "High-level master available from 85 combat.");
        if (combat >= 75)
            return new SlayerMasterChoice("Konar quo Maten", "Mount Karuulm",
                    "Available from 75 combat; assignments require her specified location.");
        if (combat >= 70 && complete(quests, "Lost City"))
            return new SlayerMasterChoice("Chaeldar", "Zanaris",
                    "Available from 70 combat after Lost City.");
        if (combat >= 40)
            return new SlayerMasterChoice("Vannaka", "Edgeville Dungeon",
                    "Available from 40 combat without adding a Wilderness requirement.");
        if (combat >= 20 && complete(quests, "Priest in Peril"))
            return new SlayerMasterChoice("Mazchna/Achtryn", "Canifis",
                    "Available from 20 combat after Priest in Peril.");
        return new SlayerMasterChoice("Turael/Aya", "Burthorpe",
                "No combat-level requirement; this is the safe baseline when a higher master is not verified.");
    }

    /** Mirrors the standard OSRS combat-level formula closely enough for gates. */
    static int combatLevel(AccountSnapshot account)
    {
        double base = 0.25 * (account.getSkillLevel(Skill.DEFENCE)
                + account.getSkillLevel(Skill.HITPOINTS)
                + Math.floor(account.getSkillLevel(Skill.PRAYER) / 2.0));
        double melee = 0.325 * (account.getSkillLevel(Skill.ATTACK)
                + account.getSkillLevel(Skill.STRENGTH));
        double ranged = 0.325 * Math.floor(account.getSkillLevel(Skill.RANGED) * 1.5);
        double magic = 0.325 * Math.floor(account.getSkillLevel(Skill.MAGIC) * 1.5);
        return (int) Math.floor(base + Math.max(melee, Math.max(ranged, magic)));
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
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

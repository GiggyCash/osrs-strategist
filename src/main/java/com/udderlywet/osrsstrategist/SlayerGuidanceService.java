package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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

    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel)
    {
        return build(data, currentLevel, targetLevel, true);
    }

    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null) return null;
        AccountSnapshot account = data.account();
        if (!AccountBuildPolicy.allowsSkill(account, Skill.SLAYER)) return null;
        if (account.getMembershipStatus() != MembershipStatus.P2P) return null;

        int currentXp = account.getSkillExperience(Skill.SLAYER);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        SlayerSnapshot slayer = data.slayer();
        if (slayer != null && slayer.hasTask())
        {
            SlayerTaskProfile profile = taskProfiles.profileFor(slayer.getTaskName());
            ItemIndex items = new ItemIndex(data, useGroupStorage);
            String action = taskAction(slayer, profile, xpNeeded, targetLevel);
            String supplies = taskSupplies(account, items, profile);
            String where = taskLocation(slayer, profile);
            String note = taskNote(account, profile);
            return new Guidance(action, supplies, where, note);
        }

        SlayerMasterChoice master = bestMaster(account, data.quests());
        var action = get(1452) + master.name
                + ". You need " + format(xpNeeded)
                + get(1453) + targetLevel + ".";
        String supplies = get(759);
        var note = master.reason + get(760);
        return new Guidance(action, supplies, master.location, note);
    }

    private static String taskAction(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile,
            int xpNeeded,
            int targetLevel)
    {
        StringBuilder action = new StringBuilder();
        action.append(get(1454))
                .append(slayer.getTaskName())
                .append(" assignment: ")
                .append(slayer.getRemaining())
                .append(get(1455))
                .append(format(xpNeeded))
                .append(get(1453))
                .append(targetLevel).append(".");
        if (profile != null && hasText(profile.getStyleGuidance()))
        {
            action.append(" ").append(profile.getStyleGuidance());
        }
        return action.toString();
    }

    private static String taskSupplies(
            AccountSnapshot account,
            ItemIndex items,
            SlayerTaskProfile profile)
    {
        if (profile == null || profile.getRequiredProtection().isEmpty())
        {
            return get(761);
        }

        List<String> required = profile.getRequiredProtection();
        String owned = firstOwned(items, required);
        if (owned != null)
        {
            return get(1456) + owned
                    + get(762);
        }

        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        String choices = joinChoices(required);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            int restricted = restrictedOwned(items, required);
            if (restricted > 0)
            {
                return get(763)
                        + choices
                        + get(764);
            }
            return get(766)
                    + choices
                    + get(767);
        }

        if (!items.primaryOwnershipObserved())
        {
            return get(768)
                    + choices + ".";
        }

        if (mode.isIronLike())
        {
            return get(769)
                    + choices + ".";
        }
        return get(770)
                + choices
                + get(771);
    }

    private static String taskLocation(
            SlayerSnapshot slayer,
            SlayerTaskProfile profile)
    {
        if (hasText(slayer.getTaskLocation()))
        {
            return get(1457)
                    + slayer.getTaskLocation()
                    + get(772);
        }
        if (profile != null && hasText(profile.getPreferredLocation()))
        {
            return profile.getPreferredLocation();
        }
        if (hasText(slayer.getMasterName()))
        {
            return get(1458)
                    + slayer.getMasterName()
                    + get(773);
        }
        return get(774);
    }

    private static String taskNote(AccountSnapshot account,
            SlayerTaskProfile profile)
    {
        String base = get(775);
        if (profile == null) return base;
        StringBuilder note = new StringBuilder();
        if (hasText(profile.getMechanicsNote()))
        {
            note.append(profile.getMechanicsNote()).append(" ");
        }
        if (profile.getMultiTargetMagicEligibility() == CapabilityState.VERIFIED)
            note.append(get(777));
        if (profile.getCannonEligibility() == CapabilityState.UNKNOWN)
            note.append(get(778));
        if (profile.isWildernessVariantKnown())
            note.append(get(779));
        if (AccountMode.fromTypeCode(account.getAccountTypeCode()).isIronLike()
                && !profile.getIronObjectives().isEmpty())
            note.append("Iron objective: ").append(String.join(", ",
                    profile.getIronObjectives())).append(". ");
        if (hasText(profile.getTaskDecisionGuidance()))
            note.append(profile.getTaskDecisionGuidance()).append(" ");
        return note.append(base).toString();
    }

    private static String firstOwned(
            ItemIndex items,
            List<String> candidates)
    {
        for (String candidate : candidates)
        {
            if (items.has(candidate)) return candidate;
        }
        return null;
    }

    private static int restrictedOwned(
            ItemIndex items,
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
                    get(780));
        if (combat >= 85)
            return new SlayerMasterChoice("Nieve/Steve", get(1459),
                    get(781));
        if (combat >= 75)
            return new SlayerMasterChoice("Konar quo Maten", "Mount Karuulm",
                    get(782));
        if (combat >= 70 && complete(quests, "Lost City"))
            return new SlayerMasterChoice("Chaeldar", "Zanaris",
                    get(783));
        if (combat >= 40)
            return new SlayerMasterChoice("Vannaka", "Edgeville Dungeon",
                    get(784));
        if (combat >= 20 && complete(quests, "Priest in Peril"))
            return new SlayerMasterChoice("Mazchna/Achtryn", "Canifis",
                    get(785));
        return new SlayerMasterChoice("Turael/Aya", "Burthorpe",
                get(786));
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

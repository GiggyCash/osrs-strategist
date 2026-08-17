package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * Membership-level content gates that run before deeper account capability
 * checks. Unknown membership fails closed to F2P-safe content instead of
 * temporarily leaking members-only recommendations into the queue.
 */
public final class ContentAccessRules
{
    private static final Set<Skill> FREE_TO_PLAY_SKILLS = EnumSet.of(
            Skill.ATTACK,
            Skill.STRENGTH,
            Skill.DEFENCE,
            Skill.RANGED,
            Skill.PRAYER,
            Skill.MAGIC,
            Skill.RUNECRAFT,
            Skill.HITPOINTS,
            Skill.CRAFTING,
            Skill.MINING,
            Skill.SMITHING,
            Skill.FISHING,
            Skill.COOKING,
            Skill.FIREMAKING,
            Skill.WOODCUTTING
    );

    private static final Set<String> MEMBERS_ONLY_METHOD_IDS = Set.of(
            "runecraft_gotr",
            "mining_mlm",
            "smithing_foundry",
            "smithing_giants_foundry",
            "fishing_tempoross",
            "firemaking_wintertodt",
            "construction_homes",
            "construction_mahogany_homes",
            "herblore_mixology",
            "farming_tithe",
            "hunter_rumours",
            "hunter_herbiboar",
            "woodcutting_forestry",
            "thieving_pyramid",
            "thieving_varlamore"
    );

    private ContentAccessRules()
    {
    }

    public static boolean isSkillAvailable(
            Skill skill,
            MembershipStatus membershipStatus)
    {
        if (skill == null) return false;
        if (membershipStatus == MembershipStatus.P2P) return true;

        // F2P and UNKNOWN both use the F2P skill boundary. UNKNOWN is treated
        // conservatively until RuneLite gives Strategist verified membership.
        return FREE_TO_PLAY_SKILLS.contains(skill);
    }

    public static boolean isMethodAvailable(
            TrainingMethod method,
            MembershipStatus membershipStatus)
    {
        if (method == null || !isSkillAvailable(method.getSkill(), membershipStatus))
        {
            return false;
        }
        if (membershipStatus == MembershipStatus.P2P) return true;

        // F2P and UNKNOWN are intentionally identical here. A transient access
        // read may temporarily narrow a member to safe F2P routes, but can never
        // expose a members-only route to an F2P account.
        return !method.isMembersOnly()
                && !MEMBERS_ONLY_METHOD_IDS.contains(method.getId());
    }

    public static boolean isFreeToPlaySkill(Skill skill)
    {
        return skill != null && FREE_TO_PLAY_SKILLS.contains(skill);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * Membership-level content gates that can be evaluated before deeper account
 * capability checks. Method-specific requirements still run after this layer.
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

    private ContentAccessRules()
    {
    }

    public static boolean isSkillAvailable(
            Skill skill,
            MembershipStatus membershipStatus)
    {
        if (skill == null)
        {
            return false;
        }

        if (membershipStatus == MembershipStatus.F2P)
        {
            return FREE_TO_PLAY_SKILLS.contains(skill);
        }

        // P2P gets the complete skill set. UNKNOWN is intentionally permissive
        // so a transient read failure does not erase the recommendation queue.
        return true;
    }

    public static boolean isMethodAvailable(
            TrainingMethod method,
            MembershipStatus membershipStatus)
    {
        if (method == null)
        {
            return false;
        }

        if (!isSkillAvailable(method.getSkill(), membershipStatus))
        {
            return false;
        }

        return membershipStatus != MembershipStatus.F2P
                || !method.isMembersOnly();
    }
}

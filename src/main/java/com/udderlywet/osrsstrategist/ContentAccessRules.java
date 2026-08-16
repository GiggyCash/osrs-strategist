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

    /**
     * Existing catalog methods that live behind membership even though their
     * parent skill is available in F2P. New catalog entries should prefer the
     * TrainingMethod.membersOnly flag instead of extending this compatibility
     * set indefinitely.
     */
    private static final Set<String> MEMBERS_ONLY_METHOD_IDS = Set.of(
            "runecraft_gotr",
            "mining_mlm",
            "smithing_foundry",
            "fishing_tempoross",
            "firemaking_wintertodt"
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

        if (membershipStatus != MembershipStatus.F2P)
        {
            return true;
        }

        return !method.isMembersOnly()
                && !MEMBERS_ONLY_METHOD_IDS.contains(method.getId());
    }
}

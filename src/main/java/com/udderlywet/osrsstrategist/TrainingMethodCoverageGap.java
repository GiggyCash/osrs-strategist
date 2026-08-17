package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** One contiguous skill-level range with no catalog method for a membership mode. */
public final class TrainingMethodCoverageGap
{
    private final Skill skill;
    private final MembershipStatus membership;
    private final int startLevel;
    private final int endLevel;

    public TrainingMethodCoverageGap(
            Skill skill,
            MembershipStatus membership,
            int startLevel,
            int endLevel)
    {
        this.skill = skill;
        this.membership = membership;
        this.startLevel = startLevel;
        this.endLevel = endLevel;
    }

    public Skill getSkill() { return skill; }
    public MembershipStatus getMembership() { return membership; }
    public int getStartLevel() { return startLevel; }
    public int getEndLevel() { return endLevel; }

    @Override
    public String toString()
    {
        return skill.getName() + " " + startLevel + "-" + endLevel
                + " (" + membership + ")";
    }
}

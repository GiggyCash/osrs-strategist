package com.udderlywet.osrsstrategist;

import lombok.Getter;

import net.runelite.api.Skill;

/** One action exposed by RuneLite's maintained skill-calculator data. */
public final class RuneLiteSkillActionDefinition
{
    @Getter
    private final Skill skill;
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final int level;
    @Getter
    private final float xp;
    @Getter
    private final String category;
    @Getter
    private final MembershipStatus membership;
    @Getter
    private final int itemId;

    public RuneLiteSkillActionDefinition(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership)
    {
        this(skill, id, name, level, xp, category, membership, -1);
    }

    public RuneLiteSkillActionDefinition(Skill skill, String id, String name,
            int level, float xp, String category, MembershipStatus membership,
            int itemId)
    {
        this.skill = skill;
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.category = category;
        this.membership = membership == null ? MembershipStatus.UNKNOWN : membership;
        this.itemId = itemId;
    }

}

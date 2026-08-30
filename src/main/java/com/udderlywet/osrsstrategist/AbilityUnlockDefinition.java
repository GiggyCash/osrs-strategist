package com.udderlywet.osrsstrategist;

import lombok.Getter;

import net.runelite.api.Skill;

/** Verified prerequisites for a prayer, spell, or spellbook unlock. */
public final class AbilityUnlockDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final GoalNodeKind kind;
    @Getter
    private final String quest;
    @Getter
    private final Skill skill;
    @Getter
    private final int level;
    @Getter
    private final Skill secondarySkill;
    @Getter
    private final int secondaryLevel;
    @Getter
    private final String requiredItem;
    @Getter
    private final String encounterId;
    @Getter
    private final String accessCheck;

    AbilityUnlockDefinition(String id, String name, GoalNodeKind kind,
            String quest, Skill skill, int level, Skill secondarySkill,
            int secondaryLevel, String requiredItem, String encounterId,
            String accessCheck)
    {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.quest = quest;
        this.skill = skill;
        this.level = Math.max(0, level);
        this.secondarySkill = secondarySkill;
        this.secondaryLevel = Math.max(0, secondaryLevel);
        this.requiredItem = requiredItem;
        this.encounterId = encounterId;
        this.accessCheck = accessCheck;
    }

}

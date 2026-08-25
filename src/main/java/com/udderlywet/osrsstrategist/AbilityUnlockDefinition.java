package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Verified prerequisites for a prayer, spell, or spellbook unlock. */
public final class AbilityUnlockDefinition
{
    private final String id;
    private final String name;
    private final GoalNodeKind kind;
    private final String quest;
    private final Skill skill;
    private final int level;
    private final Skill secondarySkill;
    private final int secondaryLevel;
    private final String requiredItem;
    private final String encounterId;
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

    public String getId() { return id; }
    public String getName() { return name; }
    public GoalNodeKind getKind() { return kind; }
    public String getQuest() { return quest; }
    public Skill getSkill() { return skill; }
    public int getLevel() { return level; }
    public Skill getSecondarySkill() { return secondarySkill; }
    public int getSecondaryLevel() { return secondaryLevel; }
    public String getRequiredItem() { return requiredItem; }
    public String getEncounterId() { return encounterId; }
    public String getAccessCheck() { return accessCheck; }
}

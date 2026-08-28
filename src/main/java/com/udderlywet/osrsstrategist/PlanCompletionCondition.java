package com.udderlywet.osrsstrategist;

import java.util.Objects;
import net.runelite.api.Skill;

/** Observable completion rule for a strategic plan step. */
public final class PlanCompletionCondition
{
    public enum Kind
    {
        SKILL_LEVEL,
        QUEST_COMPLETE,
        NONE
    }

    private final Kind kind;
    private final Skill skill;
    private final int level;
    private final String quest;

    private PlanCompletionCondition(
            Kind kind, Skill skill, int level, String quest)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = Math.max(0, level);
        this.quest = quest;
    }

    public static PlanCompletionCondition skillLevel(Skill skill, int level)
    {
        if (skill == null || level < 1)
            throw new IllegalArgumentException("Skill target is required");
        return new PlanCompletionCondition(
                Kind.SKILL_LEVEL, skill, level, null);
    }

    public static PlanCompletionCondition questComplete(String quest)
    {
        if (quest == null || quest.trim().isEmpty())
            throw new IllegalArgumentException("Quest name is required");
        return new PlanCompletionCondition(
                Kind.QUEST_COMPLETE, null, 0, quest.trim());
    }

    public static PlanCompletionCondition none()
    {
        return new PlanCompletionCondition(Kind.NONE, null, 0, null);
    }

    public boolean isComplete(StrategyDataBundle data)
    {
        if (data == null) return false;
        if (kind == Kind.SKILL_LEVEL)
            return data.getAccount() != null
                    && data.getAccount().getSkillLevel(skill) >= level;
        if (kind == Kind.QUEST_COMPLETE)
            return data.getQuests() != null
                    && data.getQuests().statusOf(quest) == QuestStatus.COMPLETE;
        return false;
    }

    public Kind getKind() { return kind; }
    public Skill getSkill() { return skill; }
    public int getLevel() { return level; }
    public String getQuest() { return quest; }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PlanCompletionCondition)) return false;
        PlanCompletionCondition that = (PlanCompletionCondition) other;
        return level == that.level && kind == that.kind
                && skill == that.skill && Objects.equals(quest, that.quest);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(kind, skill, level, quest);
    }
}

package com.udderlywet.osrsstrategist;

import lombok.Getter;

import net.runelite.api.Skill;

@Getter
public final class DiaryTaskRequirement
{
    public enum Kind { SKILL, QUEST, COMBAT_LEVEL, QUEST_POINTS, ALTERNATIVE_CHECK }

    private final Kind kind;
    private final Skill skill;
    private final int level;
    private final String quest;
    private final boolean startedOnly;
    private final String check;

    private DiaryTaskRequirement(Kind kind, Skill skill, int level,
            String quest, boolean startedOnly, String check)
    {
        this.kind = kind;
        this.skill = skill;
        this.level = Math.max(0, level);
        this.quest = quest;
        this.startedOnly = startedOnly;
        this.check = check;
    }

    public static DiaryTaskRequirement skill(Skill skill, int level)
    {
        return new DiaryTaskRequirement(Kind.SKILL, skill, level,
                null, false, null);
    }

    public static DiaryTaskRequirement quest(String quest, boolean startedOnly)
    {
        return new DiaryTaskRequirement(Kind.QUEST, null, 0,
                quest, startedOnly, null);
    }

    public static DiaryTaskRequirement combat(int level)
    {
        return new DiaryTaskRequirement(Kind.COMBAT_LEVEL, null, level,
                null, false, null);
    }

    public static DiaryTaskRequirement questPoints(int points)
    {
        return new DiaryTaskRequirement(Kind.QUEST_POINTS, null, points,
                null, false, null);
    }

    public static DiaryTaskRequirement alternative(String check)
    {
        return new DiaryTaskRequirement(Kind.ALTERNATIVE_CHECK, null, 0,
                null, false, check);
    }

}

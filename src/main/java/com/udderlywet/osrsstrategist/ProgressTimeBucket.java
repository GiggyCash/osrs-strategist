package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Compact event-driven XP aggregation used by charts and persistence. */
public final class ProgressTimeBucket
{
    @Getter
    private final long startedAtMillis;
    @Getter
    private final Map<Skill, Integer> xpBySkill;

    ProgressTimeBucket(long startedAtMillis, Map<Skill, Integer> xpBySkill)
    {
        this.startedAtMillis = startedAtMillis;
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null) copy.putAll(xpBySkill);
        this.xpBySkill = Collections.unmodifiableMap(copy);
    }

    public int getTotalXp()
    {
        long total = 0L;
        for (Integer value : xpBySkill.values())
            total += value == null ? 0 : Math.max(0, value);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
}

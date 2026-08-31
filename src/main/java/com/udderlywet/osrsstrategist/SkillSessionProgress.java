package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.runelite.api.Skill;

/** Immutable per-skill progress for the current client session. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SkillSessionProgress
{
    private final Skill skill;
    private final int startingXp;
    private final int currentXp;
    private final int startingLevel;
    private final int currentLevel;
    private final XpRateEstimate rate;


    public int getXpGained() { return Math.max(0, currentXp - startingXp); }
    public int getLevelsGained()
    {
        return Math.max(0, currentLevel - startingLevel);
    }
}

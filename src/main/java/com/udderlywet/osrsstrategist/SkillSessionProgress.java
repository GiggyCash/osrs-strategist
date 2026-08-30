package com.udderlywet.osrsstrategist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.runelite.api.Skill;

/** Immutable per-skill progress for the current client session. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class SkillSessionProgress
{
    @Getter
    private final Skill skill;
    @Getter
    private final int startingXp;
    @Getter
    private final int currentXp;
    @Getter
    private final int startingLevel;
    @Getter
    private final int currentLevel;
    @Getter
    private final XpRateEstimate rate;


    public int getXpGained() { return Math.max(0, currentXp - startingXp); }
    public int getLevelsGained()
    {
        return Math.max(0, currentLevel - startingLevel);
    }
}

package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Immutable per-skill progress for the current client session. */
public final class SkillSessionProgress
{
    private final Skill skill;
    private final int startingXp;
    private final int currentXp;
    private final int startingLevel;
    private final int currentLevel;
    private final XpRateEstimate rate;

    SkillSessionProgress(
            Skill skill,
            int startingXp,
            int currentXp,
            int startingLevel,
            int currentLevel,
            XpRateEstimate rate)
    {
        this.skill = skill;
        this.startingXp = startingXp;
        this.currentXp = currentXp;
        this.startingLevel = startingLevel;
        this.currentLevel = currentLevel;
        this.rate = rate;
    }

    public Skill getSkill() { return skill; }
    public int getStartingXp() { return startingXp; }
    public int getCurrentXp() { return currentXp; }
    public int getXpGained() { return Math.max(0, currentXp - startingXp); }
    public int getStartingLevel() { return startingLevel; }
    public int getCurrentLevel() { return currentLevel; }
    public int getLevelsGained()
    {
        return Math.max(0, currentLevel - startingLevel);
    }
    public XpRateEstimate getRate() { return rate; }
}

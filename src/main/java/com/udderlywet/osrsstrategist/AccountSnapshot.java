package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

public final class AccountSnapshot
{
    private final String playerName;
    private final int accountTypeCode;
    private final String accountTypeName;
    private final int totalLevel;
    private final long totalExperience;

    private final Map<Skill, Integer> skillLevels;
    private final Map<Skill, Integer> skillExperience;

    public AccountSnapshot(
            String playerName,
            int accountTypeCode,
            String accountTypeName,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this.playerName = playerName;
        this.accountTypeCode = accountTypeCode;
        this.accountTypeName = accountTypeName;
        this.totalLevel = totalLevel;
        this.totalExperience = totalExperience;

        this.skillLevels = Collections.unmodifiableMap(
                new EnumMap<>(skillLevels)
        );

        this.skillExperience = Collections.unmodifiableMap(
                new EnumMap<>(skillExperience)
        );
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public int getAccountTypeCode()
    {
        return accountTypeCode;
    }

    public String getAccountTypeName()
    {
        return accountTypeName;
    }

    public int getTotalLevel()
    {
        return totalLevel;
    }

    public long getTotalExperience()
    {
        return totalExperience;
    }

    public Map<Skill, Integer> getSkillLevels()
    {
        return skillLevels;
    }

    public Map<Skill, Integer> getSkillExperience()
    {
        return skillExperience;
    }

    public int getSkillLevel(Skill skill)
    {
        return skillLevels.getOrDefault(skill, 1);
    }

    public int getSkillExperience(Skill skill)
    {
        return skillExperience.getOrDefault(skill, 0);
    }

    public int getTrackedSkillCount()
    {
        return skillLevels.size();
    }
}
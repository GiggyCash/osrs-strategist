package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

public final class AccountSnapshot
{
    private final String playerName;
    private final long accountHash;
    private final int accountTypeCode;
    private final String accountTypeName;
    private final MembershipStatus membershipStatus;
    private final int membershipCredit;
    private final int totalLevel;
    private final long totalExperience;

    private final Map<Skill, Integer> skillLevels;
    private final Map<Skill, Integer> skillExperience;

    /**
     * Compatibility constructor for tests and callers that do not yet supply
     * membership state.
     */
    public AccountSnapshot(
            String playerName,
            int accountTypeCode,
            String accountTypeName,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this(
                playerName,
                0L,
                accountTypeCode,
                accountTypeName,
                MembershipStatus.UNKNOWN,
                0,
                totalLevel,
                totalExperience,
                skillLevels,
                skillExperience
        );
    }

    public AccountSnapshot(
            String playerName,
            int accountTypeCode,
            String accountTypeName,
            MembershipStatus membershipStatus,
            int membershipCredit,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this(playerName, 0L, accountTypeCode, accountTypeName,
                membershipStatus, membershipCredit, totalLevel,
                totalExperience, skillLevels, skillExperience);
    }

    public AccountSnapshot(
            String playerName,
            long accountHash,
            int accountTypeCode,
            String accountTypeName,
            MembershipStatus membershipStatus,
            int membershipCredit,
            int totalLevel,
            long totalExperience,
            Map<Skill, Integer> skillLevels,
            Map<Skill, Integer> skillExperience)
    {
        this.playerName = playerName;
        this.accountHash = accountHash;
        this.accountTypeCode = accountTypeCode;
        this.accountTypeName = accountTypeName;
        this.membershipStatus = membershipStatus == null
                ? MembershipStatus.UNKNOWN
                : membershipStatus;
        this.membershipCredit = membershipCredit;
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

    /** Stable local character identity. Zero means RuneLite has not supplied it yet. */
    public long getAccountHash()
    {
        return accountHash;
    }

    public boolean hasStableAccountIdentity()
    {
        return accountHash != 0L;
    }

    public int getAccountTypeCode()
    {
        return accountTypeCode;
    }

    public String getAccountTypeName()
    {
        return accountTypeName;
    }

    public MembershipStatus getMembershipStatus()
    {
        return membershipStatus;
    }

    public int getMembershipCredit()
    {
        return membershipCredit;
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

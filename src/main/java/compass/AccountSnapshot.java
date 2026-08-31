package compass;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

public final class AccountSnapshot
{
    @Getter
    private final String playerName;
    private final long accountHash;
    @Getter
    private final int accountTypeCode;
    @Getter
    private final String accountTypeName;
    @Getter
    private final MembershipStatus membershipStatus;
    @Getter
    private final int membershipCredit;
    @Getter
    private final int totalLevel;
    @Getter
    private final long totalExperience;

    @Getter
    private final Map<Skill, Integer> skillLevels;
    @Getter
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


    /** Stable local character identity. Zero means RuneLite has not supplied it yet. */
    public long getAccountHash()
    {
        return accountHash;
    }

    public boolean hasStableAccountIdentity()
    {
        return accountHash != 0L;
    }









    public int getSkillLevel(Skill skill)
    {
        return skillLevels.getOrDefault(skill, 1);
    }

    int level(Skill skill) { return getSkillLevel(skill); }
    int xp(Skill skill) { return getSkillExperience(skill); }
    MembershipStatus membership() { return membershipStatus; }
    int modeCode() { return accountTypeCode; }

    public int getSkillExperience(Skill skill)
    {
        return skillExperience.getOrDefault(skill, 0);
    }

    public int getTrackedSkillCount()
    {
        return skillLevels.size();
    }
}

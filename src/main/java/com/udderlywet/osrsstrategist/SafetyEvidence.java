package com.udderlywet.osrsstrategist;

import lombok.Getter;

import net.runelite.api.Skill;

/** Structured access/build evidence consumed by the final recommendation gate. */
public final class SafetyEvidence
{
    public enum Access
    {
        F2P_SAFE,
        MEMBERS_ONLY,
        UNKNOWN
    }

    public enum BuildEffect
    {
        HARMLESS,
        SKILL_XP,
        VERIFIED_SAFE,
        POTENTIALLY_IRREVERSIBLE,
        UNKNOWN
    }

    @Getter
    private final Access access;
    @Getter
    private final BuildEffect buildEffect;
    @Getter
    private final Skill affectedSkill;
    @Getter
    private final boolean conventionalBankRequired;
    private final boolean unverifiedDangerousStorage;
    private final boolean invalidCurrentExecution;

    private SafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill, boolean conventionalBankRequired)
    {
        this(access, buildEffect, affectedSkill, conventionalBankRequired,
                false, false);
    }

    private SafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill, boolean conventionalBankRequired,
            boolean unverifiedDangerousStorage)
    {
        this(access, buildEffect, affectedSkill, conventionalBankRequired,
                unverifiedDangerousStorage, false);
    }

    private SafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill, boolean conventionalBankRequired,
            boolean unverifiedDangerousStorage,
            boolean invalidCurrentExecution)
    {
        this.access = access == null ? Access.UNKNOWN : access;
        this.buildEffect = buildEffect == null ? BuildEffect.UNKNOWN : buildEffect;
        this.affectedSkill = affectedSkill;
        this.conventionalBankRequired = conventionalBankRequired;
        this.unverifiedDangerousStorage = unverifiedDangerousStorage;
        this.invalidCurrentExecution = invalidCurrentExecution;
    }

    public static SafetyEvidence unknown()
    {
        return new SafetyEvidence(Access.UNKNOWN, BuildEffect.UNKNOWN,
                null, false);
    }

    public static SafetyEvidence harmless(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.HARMLESS, null, false);
    }

    public static SafetyEvidence skill(boolean freeToPlay, Skill skill)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.SKILL_XP, skill, false);
    }

    public static SafetyEvidence verifiedSafe(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.VERIFIED_SAFE, null, false);
    }

    public static SafetyEvidence potentiallyIrreversible(boolean freeToPlay)
    {
        return new SafetyEvidence(access(freeToPlay),
                BuildEffect.POTENTIALLY_IRREVERSIBLE, null, false);
    }

    public SafetyEvidence requiringConventionalBank()
    {
        return new SafetyEvidence(access, buildEffect,
                affectedSkill, true, unverifiedDangerousStorage,
                invalidCurrentExecution);
    }

    public SafetyEvidence withUnverifiedDangerousStorage()
    {
        return new SafetyEvidence(access, buildEffect,
                affectedSkill, conventionalBankRequired, true,
                invalidCurrentExecution);
    }

    public SafetyEvidence withInvalidCurrentExecution()
    {
        return new SafetyEvidence(access, buildEffect, affectedSkill,
                conventionalBankRequired, unverifiedDangerousStorage, true);
    }

    private static Access access(boolean freeToPlay)
    {
        return freeToPlay ? Access.F2P_SAFE : Access.MEMBERS_ONLY;
    }

    public boolean hasUnverifiedDangerousStorage()
    {
        return unverifiedDangerousStorage;
    }
    public boolean hasInvalidCurrentExecution()
    {
        return invalidCurrentExecution;
    }
}

package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Structured access/build evidence consumed by the final recommendation gate. */
public final class CandidateSafetyEvidence
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

    private final Access access;
    private final BuildEffect buildEffect;
    private final Skill affectedSkill;
    private final boolean conventionalBankRequired;
    private final boolean unverifiedDangerousStorage;

    private CandidateSafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill, boolean conventionalBankRequired)
    {
        this(access, buildEffect, affectedSkill, conventionalBankRequired,
                false);
    }

    private CandidateSafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill, boolean conventionalBankRequired,
            boolean unverifiedDangerousStorage)
    {
        this.access = access == null ? Access.UNKNOWN : access;
        this.buildEffect = buildEffect == null ? BuildEffect.UNKNOWN : buildEffect;
        this.affectedSkill = affectedSkill;
        this.conventionalBankRequired = conventionalBankRequired;
        this.unverifiedDangerousStorage = unverifiedDangerousStorage;
    }

    public static CandidateSafetyEvidence unknown()
    {
        return new CandidateSafetyEvidence(Access.UNKNOWN, BuildEffect.UNKNOWN,
                null, false);
    }

    public static CandidateSafetyEvidence harmless(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay),
                BuildEffect.HARMLESS, null, false);
    }

    public static CandidateSafetyEvidence skill(boolean freeToPlay, Skill skill)
    {
        return new CandidateSafetyEvidence(access(freeToPlay),
                BuildEffect.SKILL_XP, skill, false);
    }

    public static CandidateSafetyEvidence verifiedSafe(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay),
                BuildEffect.VERIFIED_SAFE, null, false);
    }

    public static CandidateSafetyEvidence potentiallyIrreversible(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay),
                BuildEffect.POTENTIALLY_IRREVERSIBLE, null, false);
    }

    public CandidateSafetyEvidence requiringConventionalBank()
    {
        return new CandidateSafetyEvidence(access, buildEffect,
                affectedSkill, true, unverifiedDangerousStorage);
    }

    public CandidateSafetyEvidence withUnverifiedDangerousStorage()
    {
        return new CandidateSafetyEvidence(access, buildEffect,
                affectedSkill, conventionalBankRequired, true);
    }

    private static Access access(boolean freeToPlay)
    {
        return freeToPlay ? Access.F2P_SAFE : Access.MEMBERS_ONLY;
    }

    public Access getAccess() { return access; }
    public BuildEffect getBuildEffect() { return buildEffect; }
    public Skill getAffectedSkill() { return affectedSkill; }
    public boolean isConventionalBankRequired()
    {
        return conventionalBankRequired;
    }
    public boolean hasUnverifiedDangerousStorage()
    {
        return unverifiedDangerousStorage;
    }
}

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

    private CandidateSafetyEvidence(Access access, BuildEffect buildEffect,
            Skill affectedSkill)
    {
        this.access = access == null ? Access.UNKNOWN : access;
        this.buildEffect = buildEffect == null ? BuildEffect.UNKNOWN : buildEffect;
        this.affectedSkill = affectedSkill;
    }

    public static CandidateSafetyEvidence unknown()
    {
        return new CandidateSafetyEvidence(Access.UNKNOWN, BuildEffect.UNKNOWN, null);
    }

    public static CandidateSafetyEvidence harmless(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay), BuildEffect.HARMLESS, null);
    }

    public static CandidateSafetyEvidence skill(boolean freeToPlay, Skill skill)
    {
        return new CandidateSafetyEvidence(access(freeToPlay), BuildEffect.SKILL_XP, skill);
    }

    public static CandidateSafetyEvidence verifiedSafe(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay), BuildEffect.VERIFIED_SAFE, null);
    }

    public static CandidateSafetyEvidence potentiallyIrreversible(boolean freeToPlay)
    {
        return new CandidateSafetyEvidence(access(freeToPlay),
                BuildEffect.POTENTIALLY_IRREVERSIBLE, null);
    }

    private static Access access(boolean freeToPlay)
    {
        return freeToPlay ? Access.F2P_SAFE : Access.MEMBERS_ONLY;
    }

    public Access getAccess() { return access; }
    public BuildEffect getBuildEffect() { return buildEffect; }
    public Skill getAffectedSkill() { return affectedSkill; }
}

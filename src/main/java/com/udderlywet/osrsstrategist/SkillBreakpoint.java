package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** One actual level target and the typed reason it matters. */
public final class SkillBreakpoint
{
    public enum Kind
    {
        GOAL_REQUIREMENT,
        INFRASTRUCTURE_UNLOCK,
        ABILITY_UNLOCK,
        TRAINING_ACTION_UNLOCK,
        MAX_TARGET,
        NEXT_LEVEL_FALLBACK
    }

    private final Skill skill;
    private final int level;
    private final String label;
    private final Kind kind;
    private final String evidenceId;

    public SkillBreakpoint(Skill skill, int level, String label,
            Kind kind, String evidenceId)
    {
        if (skill == null || level < 2 || label == null
                || label.trim().isEmpty() || kind == null)
            throw new IllegalArgumentException("A verified breakpoint is required");
        this.skill = skill;
        this.level = level;
        this.label = label.trim();
        this.kind = kind;
        this.evidenceId = evidenceId == null ? "" : evidenceId;
    }

    public Skill getSkill() { return skill; }
    public int getLevel() { return level; }
    public String getLabel() { return label; }
    public Kind getKind() { return kind; }
    public String getEvidenceId() { return evidenceId; }

    public double strategicValue()
    {
        switch (kind)
        {
            case GOAL_REQUIREMENT: return 1.0;
            case INFRASTRUCTURE_UNLOCK: return 0.8;
            case ABILITY_UNLOCK: return 0.65;
            case TRAINING_ACTION_UNLOCK: return 0.35;
            case MAX_TARGET: return 0.45;
            case NEXT_LEVEL_FALLBACK:
            default: return 0.05;
        }
    }
}

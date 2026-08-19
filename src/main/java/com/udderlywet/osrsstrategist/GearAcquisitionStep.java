package com.udderlywet.osrsstrategist;

/** One reusable edge in a gear acquisition chain. */
public final class GearAcquisitionStep
{
    public enum Kind { QUEST, SKILL, BOSS, MINIGAME, RESOURCE, SHOP, VERIFY }

    private final Kind kind;
    private final String target;
    private final String action;

    public GearAcquisitionStep(Kind kind, String target, String action)
    {
        this.kind = kind;
        this.target = target;
        this.action = action;
    }

    public Kind getKind() { return kind; }
    public String getTarget() { return target; }
    public String getAction() { return action; }
}

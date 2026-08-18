package com.udderlywet.osrsstrategist;

/** Reusable CLOG-style reward payload for skill and non-skill completions. */
public final class StrategistRewardNotification
{
    private final String id;
    private final String header;
    private final String left;
    private final String right;
    private final String footerLeft;
    private final String footerRight;

    public StrategistRewardNotification(
            String id,
            String header,
            String left,
            String right,
            String footerLeft,
            String footerRight)
    {
        this.id = id;
        this.header = header;
        this.left = left;
        this.right = right;
        this.footerLeft = footerLeft;
        this.footerRight = footerRight;
    }

    public static StrategistRewardNotification fromMilestone(
            MilestoneCompletion completion)
    {
        if (completion == null) return null;
        return new StrategistRewardNotification(
                completion.getActivityId(),
                "COMPASS MILESTONE",
                completion.getSkill().getName(),
                completion.getStartedAtLevel() + " → " + completion.getTargetLevel(),
                "Goal complete",
                "Next move ready"
        );
    }

    public String getId() { return id; }
    public String getHeader() { return header; }
    public String getLeft() { return left; }
    public String getRight() { return right; }
    public String getFooterLeft() { return footerLeft; }
    public String getFooterRight() { return footerRight; }
}

package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Reusable CLOG-style reward payload for skill and non-skill completions. */
@RequiredArgsConstructor
public final class StrategistRewardNotification
{
    @Getter
    private final String id;
    @Getter
    private final String header;
    @Getter
    private final String left;
    @Getter
    private final String right;
    @Getter
    private final String footerLeft;
    @Getter
    private final String footerRight;


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

}

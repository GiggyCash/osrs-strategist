package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Reusable CLOG-style reward payload for skill and non-skill completions. */
@Getter
@RequiredArgsConstructor
public final class StrategistRewardNotification
{
    private final String id;
    private final String header;
    private final String left;
    private final String right;
    private final String footerLeft;
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

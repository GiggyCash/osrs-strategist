package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.runelite.api.Skill;

/**
 * One detected completion event shown briefly in the sidebar.
 */
@RequiredArgsConstructor
public final class MilestoneCompletion
{
    @Getter
    private final String activityId;
    @Getter
    private final String title;
    @Getter
    private final Skill skill;
    @Getter
    private final int startedAtLevel;
    @Getter
    private final int targetLevel;






}

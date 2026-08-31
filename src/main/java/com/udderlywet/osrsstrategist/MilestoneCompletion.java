package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.runelite.api.Skill;

/**
 * One detected completion event shown briefly in the sidebar.
 */
@Getter
@RequiredArgsConstructor
public final class MilestoneCompletion
{
    private final String activityId;
    private final String title;
    private final Skill skill;
    private final int startedAtLevel;
    private final int targetLevel;






}

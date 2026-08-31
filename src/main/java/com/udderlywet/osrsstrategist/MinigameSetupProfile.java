package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Locally verifiable setup contract for a progression minigame. */
@Getter
@RequiredArgsConstructor
public final class MinigameSetupProfile
{
    private final String activityId;
    private final ItemRequirementExpression items;
    private final String location;
    private final String supplies;
    private final String instructions;


}

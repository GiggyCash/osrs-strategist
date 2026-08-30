package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Locally verifiable setup contract for a progression minigame. */
@RequiredArgsConstructor
public final class MinigameSetupProfile
{
    @Getter
    private final String activityId;
    @Getter
    private final ItemRequirementExpression items;
    @Getter
    private final String location;
    @Getter
    private final String supplies;
    @Getter
    private final String instructions;


}

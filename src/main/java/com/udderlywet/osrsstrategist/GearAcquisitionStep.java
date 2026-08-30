package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One reusable edge in a gear acquisition chain. */
@RequiredArgsConstructor
public final class GearAcquisitionStep
{
    public enum Kind { QUEST, SKILL, BOSS, MINIGAME, RESOURCE, SHOP, VERIFY }

    @Getter
    private final Kind kind;
    @Getter
    private final String target;
    @Getter
    private final String action;


}

package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One reusable edge in a gear acquisition chain. */
@Getter
@RequiredArgsConstructor
public final class GearAcquisitionStep
{
    public enum Kind { QUEST, SKILL, BOSS, MINIGAME, RESOURCE, SHOP, VERIFY }

    private final Kind kind;
    private final String target;
    private final String action;


}

package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Complete evidence contract for encounters simple enough to verify locally. */
@RequiredArgsConstructor
@Getter
public final class PvmEvidenceProfile
{
    private final String activityId;
    private final String weaponStyle;
    private final List<String> accessItems;
    private final int minimumFood;
    private final int minimumRestoration;


}

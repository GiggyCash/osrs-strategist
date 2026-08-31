package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Reviewable preparation evidence that may produce actions but never READY. */
@RequiredArgsConstructor
@Getter
public final class PvmPreparationProfile
{
    private final String activityId;
    private final String style;
    private final List<String> checks;
    private final String accountValue;
    private final String provenance;


}

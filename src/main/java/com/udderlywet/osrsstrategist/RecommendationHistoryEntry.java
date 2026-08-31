package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One per-character interaction/completion event retained for later learning. */
@Getter
@RequiredArgsConstructor
public final class RecommendationHistoryEntry
{
    private final String activityId;
    private final String title;
    private final RecommendationHistoryAction action;
    private final long occurredAtMillis;


}

package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** One per-character interaction/completion event retained for later learning. */
@RequiredArgsConstructor
public final class RecommendationHistoryEntry
{
    @Getter
    private final String activityId;
    @Getter
    private final String title;
    @Getter
    private final RecommendationHistoryAction action;
    @Getter
    private final long occurredAtMillis;


}

package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RestrictedBuildSuggestion
{
    @Getter
    private final RestrictedBuildType type;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final String evidence;


}

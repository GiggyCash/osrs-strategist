package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class RestrictedBuildSuggestion
{
    private final RestrictedBuildType type;
    private final RecommendationConfidence confidence;
    private final String evidence;


}

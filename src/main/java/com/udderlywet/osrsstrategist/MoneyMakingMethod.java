package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MoneyMakingMethod
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final long estimatedGpPerHour;
    @Getter
    private final RecommendationConfidence confidence;





}

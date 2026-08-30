package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AccountEconomySnapshot
{
    @Getter
    private final long coins;
    @Getter
    private final long estimatedBankValue;
    @Getter
    private final RecommendationConfidence confidence;




}

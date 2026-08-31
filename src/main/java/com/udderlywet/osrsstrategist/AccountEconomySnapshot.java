package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class AccountEconomySnapshot
{
    private final long coins;
    private final long estimatedBankValue;
    private final Confidence confidence;




}

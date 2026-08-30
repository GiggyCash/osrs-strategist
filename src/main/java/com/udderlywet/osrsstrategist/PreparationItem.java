package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PreparationItem
{
    @Getter
    private final String label;
    @Getter
    private final int required;
    @Getter
    private final int available;


    public boolean ready() { return available >= required; }
}

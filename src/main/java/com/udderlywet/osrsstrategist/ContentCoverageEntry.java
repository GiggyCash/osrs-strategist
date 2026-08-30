package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ContentCoverageEntry
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final ContentCoverageState state;
    @Getter
    private final String reason;
    @Getter
    private final String provenance;


}

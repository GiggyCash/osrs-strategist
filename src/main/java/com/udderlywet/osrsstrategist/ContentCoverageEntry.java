package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class ContentCoverageEntry
{
    private final String id;
    private final String name;
    private final ContentCoverageState state;
    private final String reason;
    private final String provenance;


}

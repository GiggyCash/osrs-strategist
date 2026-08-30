package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Provenance attached to structured game-data records. */
@RequiredArgsConstructor
public final class KnowledgeRecordMetadata
{
    @Getter
    private final String recordId;
    @Getter
    private final GameKnowledgeDomain domain;
    @Getter
    private final KnowledgeSource source;
    @Getter
    private final String sourceRevision;
    @Getter
    private final long verifiedAtMillis;
    @Getter
    private final boolean verifiedForPlanning;


}

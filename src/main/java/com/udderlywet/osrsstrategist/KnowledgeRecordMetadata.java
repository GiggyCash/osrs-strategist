package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Provenance attached to structured game-data records. */
@Getter
@RequiredArgsConstructor
public final class KnowledgeRecordMetadata
{
    private final String recordId;
    private final GameKnowledgeDomain domain;
    private final KnowledgeSource source;
    private final String sourceRevision;
    private final long verifiedAtMillis;
    private final boolean verifiedForPlanning;


}

package com.udderlywet.osrsstrategist;

/** Provenance attached to structured game-data records. */
public final class KnowledgeRecordMetadata
{
    private final String recordId;
    private final GameKnowledgeDomain domain;
    private final KnowledgeSource source;
    private final String sourceRevision;
    private final long verifiedAtMillis;
    private final boolean verifiedForPlanning;

    public KnowledgeRecordMetadata(
            String recordId,
            GameKnowledgeDomain domain,
            KnowledgeSource source,
            String sourceRevision,
            long verifiedAtMillis,
            boolean verifiedForPlanning)
    {
        this.recordId = recordId;
        this.domain = domain;
        this.source = source;
        this.sourceRevision = sourceRevision;
        this.verifiedAtMillis = verifiedAtMillis;
        this.verifiedForPlanning = verifiedForPlanning;
    }

    public String getRecordId() { return recordId; }
    public GameKnowledgeDomain getDomain() { return domain; }
    public KnowledgeSource getSource() { return source; }
    public String getSourceRevision() { return sourceRevision; }
    public long getVerifiedAtMillis() { return verifiedAtMillis; }
    public boolean isVerifiedForPlanning() { return verifiedForPlanning; }
}

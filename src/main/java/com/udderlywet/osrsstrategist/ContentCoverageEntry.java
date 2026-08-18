package com.udderlywet.osrsstrategist;

public final class ContentCoverageEntry
{
    private final String id;
    private final String name;
    private final ContentCoverageState state;
    private final String reason;
    private final String provenance;

    public ContentCoverageEntry(String id, String name,
            ContentCoverageState state, String reason, String provenance)
    {
        this.id = id;
        this.name = name;
        this.state = state;
        this.reason = reason;
        this.provenance = provenance;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ContentCoverageState getState() { return state; }
    public String getReason() { return reason; }
    public String getProvenance() { return provenance; }
}

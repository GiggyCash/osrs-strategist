package com.udderlywet.osrsstrategist;

import java.time.LocalDate;

/** Provenance for one reviewed strategic source family. */
public final class StrategySourceDefinition
{
    private final StrategySourceId id;
    private final String url;
    private final String subject;
    private final LocalDate reviewedDate;
    private final String revision;
    private final String license;

    public StrategySourceDefinition(StrategySourceId id, String url,
            String subject, LocalDate reviewedDate, String revision,
            String license)
    {
        this.id = id;
        this.url = url;
        this.subject = subject;
        this.reviewedDate = reviewedDate;
        this.revision = revision;
        this.license = license;
    }

    public StrategySourceId getId() { return id; }
    public String getUrl() { return url; }
    public String getSubject() { return subject; }
    public LocalDate getReviewedDate() { return reviewedDate; }
    public String getRevision() { return revision; }
    public String getLicense() { return license; }
}

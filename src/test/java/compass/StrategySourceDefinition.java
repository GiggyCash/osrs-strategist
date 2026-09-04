package compass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/** Provenance for one reviewed strategic source family. */
public final class StrategySourceDefinition
{
    private final Source id;
    private final String url;
    private final String subject;
    private final LocalDate reviewedDate;
    private final String revision;
    private final String license;
    private final List<String> derivedStrategyFamilies;

    public StrategySourceDefinition(Source id, String url,
            String subject, LocalDate reviewedDate, String revision,
            String license)
    {
        this(id, url, subject, reviewedDate, revision, license,
                Collections.emptyList());
    }

    public StrategySourceDefinition(Source id, String url,
            String subject, LocalDate reviewedDate, String revision,
            String license, List<String> derivedStrategyFamilies)
    {
        this.id = id;
        this.url = url;
        this.subject = subject;
        this.reviewedDate = reviewedDate;
        this.revision = revision;
        this.license = license;
        this.derivedStrategyFamilies = Collections.unmodifiableList(
                derivedStrategyFamilies == null ? new ArrayList<>()
                        : new ArrayList<>(derivedStrategyFamilies));
    }

    public Source getId()
    {
        return id;
    }

    public String getUrl()
    {
        return url;
    }

    public String getSubject()
    {
        return subject;
    }

    public LocalDate getReviewedDate()
    {
        return reviewedDate;
    }

    public String getRevision()
    {
        return revision;
    }

    public String getLicense()
    {
        return license;
    }

    public List<String> getDerivedStrategyFamilies()
    {
        return derivedStrategyFamilies;
    }

}

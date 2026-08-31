package compass;

import lombok.Getter;

/** One ordered, non-destructive step in a resource acquisition chain. */
@Getter
public final class ResourceAcquisitionStep
{
    private final AcquisitionSource source;
    private final String action;
    private final Confidence confidence;

    public ResourceAcquisitionStep(AcquisitionSource source, String action,
            Confidence confidence)
    {
        this.source = source;
        this.action = action;
        this.confidence = confidence == null
                ? Confidence.CHECK_NEEDED : confidence;
    }

}

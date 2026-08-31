package compass;

import lombok.Getter;

/** Actionability result for one fully identified quest. */
@Getter
public final class QuestResolution
{
    private final Confidence confidence;
    private final Guidance guidance;
    private final String reason;
    private final SafetyEvidence safetyEvidence;

    public QuestResolution(Confidence confidence,
            Guidance guidance, String reason)
    {
        this(confidence, guidance, reason, SafetyEvidence.unknown());
    }

    public QuestResolution(Confidence confidence,
            Guidance guidance, String reason,
            SafetyEvidence safetyEvidence)
    {
        this.confidence = confidence;
        this.guidance = guidance;
        this.reason = reason;
        this.safetyEvidence = safetyEvidence;
    }

}

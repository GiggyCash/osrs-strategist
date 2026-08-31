package compass;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** A concrete method paired with the strategy metadata needed to rank it safely. */
@Getter
@RequiredArgsConstructor
public final class CuratedTrainingMethod
{
    private final TrainingMethod method;

    TrainingMethod method() { return method; }
    private final TrainingMethodMetadata metadata;


}

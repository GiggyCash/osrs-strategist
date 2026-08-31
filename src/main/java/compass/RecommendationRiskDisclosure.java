package compass;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Prominent, typed warning for an unusual player-visible dangerous action. */
@Getter
@RequiredArgsConstructor
public final class RecommendationRiskDisclosure
{
    private final String heading;
    private final String message;
    private final boolean acknowledgementRequired;


    public static RecommendationRiskDisclosure deathStorage()
    {
        return new RecommendationRiskDisclosure("HIGH RISK",
                Text.get(702),
                true);
    }

}

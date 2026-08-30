package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Prominent, typed warning for an unusual player-visible dangerous action. */
@RequiredArgsConstructor
public final class RecommendationRiskDisclosure
{
    @Getter
    private final String heading;
    @Getter
    private final String message;
    @Getter
    private final boolean acknowledgementRequired;


    public static RecommendationRiskDisclosure deathStorage()
    {
        return new RecommendationRiskDisclosure("HIGH RISK",
                "This strategy uses death-based item storage. Incorrect execution or another unsafe death may permanently destroy stored items. Verify the exact retrieval rules before continuing. Use at your own risk; Gielinor Compass cannot recover or reimburse lost items.",
                true);
    }

}

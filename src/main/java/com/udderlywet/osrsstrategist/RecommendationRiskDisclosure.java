package com.udderlywet.osrsstrategist;

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
                "This strategy uses death-based item storage. Incorrect execution or another unsafe death may permanently destroy stored items. Verify the exact retrieval rules before continuing. Use at your own risk; Gielinor Compass cannot recover or reimburse lost items.",
                true);
    }

}

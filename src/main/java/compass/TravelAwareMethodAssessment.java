package compass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Travel evidence and bounded value for a selected concrete method location. */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TravelAwareMethodAssessment
{
    private final MethodLocationOption location;
    private final int travelBurden;
    private final int scoreAdjustment;
    private final boolean verifiedRouteUsed;
    private final String evidence;


}

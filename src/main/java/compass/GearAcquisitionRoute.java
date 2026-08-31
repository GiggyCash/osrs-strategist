package compass;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

/** Account-aware, multi-hop route for one meaningful gear target. */
@RequiredArgsConstructor
@Getter
public final class GearAcquisitionRoute
{
    private final String id;
    private final String itemName;
    private final CombatStyle style;
    private final boolean tradeable;
    private final List<GearAcquisitionStep> steps;
    private final String valueRule;
    private final String provenance;


}

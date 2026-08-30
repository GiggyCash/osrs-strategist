package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Account-aware, multi-hop route for one meaningful gear target. */
public final class GearAcquisitionRoute
{
    @Getter
    private final String id;
    @Getter
    private final String itemName;
    @Getter
    private final CombatStyle style;
    @Getter
    private final boolean tradeable;
    @Getter
    private final List<GearAcquisitionStep> steps;
    @Getter
    private final String valueRule;
    @Getter
    private final String provenance;

    public GearAcquisitionRoute(String id, String itemName, CombatStyle style,
            boolean tradeable, List<GearAcquisitionStep> steps,
            String valueRule, String provenance)
    {
        this.id = id;
        this.itemName = itemName;
        this.style = style;
        this.tradeable = tradeable;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.valueRule = valueRule;
        this.provenance = provenance;
    }

}

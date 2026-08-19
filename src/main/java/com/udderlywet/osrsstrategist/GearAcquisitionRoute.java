package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Account-aware, multi-hop route for one meaningful gear target. */
public final class GearAcquisitionRoute
{
    private final String id;
    private final String itemName;
    private final CombatStyle style;
    private final boolean tradeable;
    private final List<GearAcquisitionStep> steps;
    private final String valueRule;
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

    public String getId() { return id; }
    public String getItemName() { return itemName; }
    public CombatStyle getStyle() { return style; }
    public boolean isTradeable() { return tradeable; }
    public List<GearAcquisitionStep> getSteps() { return steps; }
    public String getValueRule() { return valueRule; }
    public String getProvenance() { return provenance; }
}

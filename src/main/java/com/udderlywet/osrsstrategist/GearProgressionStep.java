package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One practical equipment milestone, from early/budget through endgame. */
public final class GearProgressionStep
{
    private final GearRole role;
    private final int tier;
    private final String name;
    private final List<String> keyItems;
    private final String purpose;
    private final boolean endgame;

    public GearProgressionStep(GearRole role, int tier, String name,
            List<String> keyItems, String purpose, boolean endgame)
    {
        this.role = role;
        this.tier = tier;
        this.name = name;
        this.keyItems = Collections.unmodifiableList(new ArrayList<>(keyItems));
        this.purpose = purpose;
        this.endgame = endgame;
    }

    public GearRole getRole() { return role; }
    public int getTier() { return tier; }
    public String getName() { return name; }
    public List<String> getKeyItems() { return keyItems; }
    public String getPurpose() { return purpose; }
    public boolean isEndgame() { return endgame; }
}

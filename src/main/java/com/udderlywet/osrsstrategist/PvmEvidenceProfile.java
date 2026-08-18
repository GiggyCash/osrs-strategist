package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete evidence contract for encounters simple enough to verify locally. */
public final class PvmEvidenceProfile
{
    private final String activityId;
    private final String weaponStyle;
    private final List<String> accessItems;
    private final int minimumFood;
    private final int minimumRestoration;

    public PvmEvidenceProfile(String activityId, String weaponStyle,
            List<String> accessItems, int minimumFood, int minimumRestoration)
    {
        this.activityId = activityId;
        this.weaponStyle = weaponStyle;
        this.accessItems = Collections.unmodifiableList(accessItems == null
                ? new ArrayList<>() : new ArrayList<>(accessItems));
        this.minimumFood = Math.max(0, minimumFood);
        this.minimumRestoration = Math.max(0, minimumRestoration);
    }

    public String getActivityId() { return activityId; }
    public String getWeaponStyle() { return weaponStyle; }
    public List<String> getAccessItems() { return accessItems; }
    public int getMinimumFood() { return minimumFood; }
    public int getMinimumRestoration() { return minimumRestoration; }
}

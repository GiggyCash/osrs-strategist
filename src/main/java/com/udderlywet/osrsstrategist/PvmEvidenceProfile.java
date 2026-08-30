package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Complete evidence contract for encounters simple enough to verify locally. */
public final class PvmEvidenceProfile
{
    @Getter
    private final String activityId;
    @Getter
    private final String weaponStyle;
    @Getter
    private final List<String> accessItems;
    @Getter
    private final int minimumFood;
    @Getter
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

}

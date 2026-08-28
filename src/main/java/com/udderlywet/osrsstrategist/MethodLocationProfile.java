package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Data descriptor connecting a training method to legal concrete locations. */
public final class MethodLocationProfile
{
    private final String methodId;
    private final List<MethodLocationOption> locations;
    private final String sourceUrl;

    public MethodLocationProfile(String methodId,
            List<MethodLocationOption> locations, String sourceUrl)
    {
        this.methodId = methodId;
        this.locations = Collections.unmodifiableList(locations == null
                ? new ArrayList<>() : new ArrayList<>(locations));
        this.sourceUrl = sourceUrl;
    }

    public String getMethodId() { return methodId; }
    public List<MethodLocationOption> getLocations() { return locations; }
    public String getSourceUrl() { return sourceUrl; }
}

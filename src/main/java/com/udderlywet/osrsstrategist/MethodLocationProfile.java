package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Data descriptor connecting a training method to legal concrete locations. */
public final class MethodLocationProfile
{
    @Getter
    private final String methodId;
    @Getter
    private final List<MethodLocationOption> locations;
    @Getter
    private final String sourceUrl;

    public MethodLocationProfile(String methodId,
            List<MethodLocationOption> locations, String sourceUrl)
    {
        this.methodId = methodId;
        this.locations = Collections.unmodifiableList(locations == null
                ? new ArrayList<>() : new ArrayList<>(locations));
        this.sourceUrl = sourceUrl;
    }

}

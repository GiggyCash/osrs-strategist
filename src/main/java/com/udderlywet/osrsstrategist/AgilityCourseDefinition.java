package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AgilityCourseDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String displayName;
    @Getter
    private final int requiredLevel;
    @Getter
    private final int regionId;
    @Getter
    private final String requiredQuest;
    @Getter
    private final boolean wilderness;



    public String observationKey()
    {
        return "region." + regionId;
    }
}

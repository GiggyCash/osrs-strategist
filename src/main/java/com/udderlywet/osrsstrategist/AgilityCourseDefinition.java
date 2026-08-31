package com.udderlywet.osrsstrategist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class AgilityCourseDefinition
{
    private final String id;
    private final String displayName;
    private final int requiredLevel;
    private final int regionId;
    private final String requiredQuest;
    private final boolean wilderness;



    public String observationKey()
    {
        return "region." + regionId;
    }
}

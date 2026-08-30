package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

public final class ActivityDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String title;
    @Getter
    private final ActivityKind kind;
    @Getter
    private final List<String> requirements;

    public ActivityDefinition(
            String id,
            String title,
            ActivityKind kind,
            List<String> requirements)
    {
        this.id = id;
        this.title = title;
        this.kind = kind;
        this.requirements = Collections.unmodifiableList(
                new ArrayList<>(requirements)
        );
    }




}

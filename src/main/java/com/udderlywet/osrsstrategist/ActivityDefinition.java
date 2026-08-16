package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActivityDefinition
{
    private final String id;
    private final String title;
    private final ActivityKind kind;
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

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public ActivityKind getKind()
    {
        return kind;
    }

    public List<String> getRequirements()
    {
        return requirements;
    }
}

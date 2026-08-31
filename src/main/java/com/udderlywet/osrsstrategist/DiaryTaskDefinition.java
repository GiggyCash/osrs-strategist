package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
public final class DiaryTaskDefinition
{
    private final String region;
    private final DiaryTier tier;
    private final String task;
    private final List<DiaryTaskRequirement> requirements;

    DiaryTaskDefinition(String region, DiaryTier tier, String task,
            List<DiaryTaskRequirement> requirements)
    {
        this.region = region;
        this.tier = tier;
        this.task = task;
        this.requirements = Collections.unmodifiableList(
                new ArrayList<>(requirements));
    }

    public String getId()
    {
        return "diary-task:" + Names.slug(region) + ":"
                + tier.name().toLowerCase(Locale.ROOT) + ":" + Names.slug(task);
    }
    public boolean isTransportRelevant()
    {
        var value = task.toLowerCase(Locale.ROOT);
        return value.contains("teleport") || value.contains("travel")
                || value.contains("fairy ring") || value.contains("glider")
                || value.contains("balloon") || value.contains("boat")
                || value.contains("minecart");
    }
    public boolean isWilderness()
    {
        return "Wilderness".equals(region);
    }

}

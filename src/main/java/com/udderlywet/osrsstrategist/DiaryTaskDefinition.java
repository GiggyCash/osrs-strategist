package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
        return "diary-task:" + normalize(region) + ":"
                + tier.name().toLowerCase(Locale.ROOT) + ":" + normalize(task);
    }
    public String getRegion() { return region; }
    public DiaryTier getTier() { return tier; }
    public String getTask() { return task; }
    public List<DiaryTaskRequirement> getRequirements() { return requirements; }
    public boolean isTransportRelevant()
    {
        String value = task.toLowerCase(Locale.ROOT);
        return value.contains("teleport") || value.contains("travel")
                || value.contains("fairy ring") || value.contains("glider")
                || value.contains("balloon") || value.contains("boat")
                || value.contains("minecart");
    }
    public boolean isWilderness()
    {
        return "Wilderness".equals(region);
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

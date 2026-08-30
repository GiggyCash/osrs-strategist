package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Starter mapping from useful grinds to the reward objective they serve. */
@Singleton
public class ProgressionObjectiveCatalog
{
    private final List<ProgressionObjectiveDefinition> objectives = Arrays.asList(
            objective("objective:graceful", "Graceful outfit",
                    "agility_rooftop", ProgressionObjectiveType.OUTFIT),
            objective("objective:prospector", "Prospector outfit",
                    "mining_mlm", ProgressionObjectiveType.OUTFIT),
            objective("objective:raiments", "Raiments of the Eye progression",
                    "runecraft_gotr", ProgressionObjectiveType.OUTFIT),
            objective("objective:smiths-uniform", "Smiths' Uniform progression",
                    "smithing_foundry", ProgressionObjectiveType.OUTFIT),
            objective("objective:tempoross", "Tempoross reward progression",
                    "fishing_tempoross", ProgressionObjectiveType.COLLECTION_LOG),
            objective("objective:wintertodt", "Wintertodt reward progression",
                    "firemaking_wintertodt", ProgressionObjectiveType.COLLECTION_LOG)
    );

    public List<ProgressionObjectiveDefinition> all()
    {
        return Collections.unmodifiableList(objectives);
    }

    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        if (methodId == null) return null;
        for (ProgressionObjectiveDefinition objective : objectives)
        {
            if (methodId.equals(objective.getMethodId()))
            {
                return objective;
            }
        }
        return null;
    }

    private static ProgressionObjectiveDefinition objective(
            String id,
            String title,
            String methodId,
            ProgressionObjectiveType type)
    {
        return new ProgressionObjectiveDefinition(id, title, methodId, type);
    }
}

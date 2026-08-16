package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Decides whether a method is still serving a larger reward objective.
 *
 * <p>A method can advance several objectives simultaneously. We therefore keep
 * protecting it until every known objective tied to that route is proven
 * complete, rather than dropping protection as soon as the first catalog entry
 * is completed.</p>
 */
@Singleton
public class ProgressionObjectiveService
{
    private final ProgressionObjectiveCatalog catalog;

    @Inject
    public ProgressionObjectiveService(ProgressionObjectiveCatalog catalog)
    {
        this.catalog = catalog;
    }

    public ProgressionObjectiveDefinition activeObjective(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.getMethod() == null) return null;

        List<ProgressionObjectiveDefinition> objectives =
                catalog.objectivesForMethod(plan.getMethod().getId());
        for (ProgressionObjectiveDefinition objective : objectives)
        {
            if (collectionLog == null
                    || !collectionLog.isObjectiveComplete(objective.getId()))
            {
                return objective;
            }
        }
        return null;
    }

    public boolean shouldProtect(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.getMethod() == null) return false;

        List<ProgressionObjectiveDefinition> objectives =
                catalog.objectivesForMethod(plan.getMethod().getId());
        if (!objectives.isEmpty())
        {
            for (ProgressionObjectiveDefinition objective : objectives)
            {
                if (collectionLog == null
                        || !collectionLog.isObjectiveComplete(objective.getId()))
                {
                    return true;
                }
            }
            return false;
        }

        // Conservative fallback for a progression-protected method that has not
        // yet been migrated to explicit long-form objective records.
        return plan.getMethod().isProgressionProtected();
    }
}

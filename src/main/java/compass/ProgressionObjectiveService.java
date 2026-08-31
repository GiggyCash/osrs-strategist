package compass;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Decides whether a method is still serving a larger reward objective. */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProgressionObjectiveService
{
    private final ProgressionObjectiveCatalog catalog;

    public ProgressionObjectiveDefinition activeObjective(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.method() == null)
        {
            return null;
        }

        ProgressionObjectiveDefinition objective =
                catalog.forMethod(plan.method().getId());
        if (objective == null)
        {
            return null;
        }

        if (collectionLog != null
                && collectionLog.isObjectiveComplete(objective.getId()))
        {
            return null;
        }

        return objective;
    }

    public boolean shouldProtect(
            TrainingPlan plan,
            CollectionLogSnapshot collectionLog)
    {
        if (plan == null || plan.method() == null)
        {
            return false;
        }

        // Explicit catalog objectives take precedence. The method flag remains
        // a conservative fallback while collection-log readers are incomplete.
        ProgressionObjectiveDefinition objective =
                catalog.forMethod(plan.method().getId());
        if (objective != null)
        {
            return collectionLog == null
                    || !collectionLog.isObjectiveComplete(objective.getId());
        }

        return plan.method().isProgressionProtected();
    }
}

package compass;

import javax.inject.Singleton;

/** Persistent method objectives loaded from the bundled catalog. */
@Singleton
public class ProgressionObjectiveCatalog extends CatalogStore<ProgressionObjectiveDefinition>
{
    public ProgressionObjectiveCatalog() { super(Text.get(440), ProgressionObjectiveDefinition[].class); }
    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        return methodId == null ? null
                : find(value -> methodId.equals(value.getMethodId()));
    }
}

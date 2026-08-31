package compass;

import javax.inject.Singleton;

/** Verified course access data loaded from the bundled catalog. */
@Singleton
public class AgilityCourseCatalog extends CatalogStore<AgilityCourseDefinition>
{
    public AgilityCourseCatalog() { super(Text.get(1605), AgilityCourseDefinition[].class); }

    public AgilityCourseDefinition wildernessCourse()
    {
        return find(AgilityCourseDefinition::isWilderness);
    }

}

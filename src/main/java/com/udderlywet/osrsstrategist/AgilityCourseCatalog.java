package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Verified course access data loaded from the bundled catalog. */
@Singleton
public class AgilityCourseCatalog
{
    private final List<AgilityCourseDefinition> courses =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    "/content/catalogs/agility-courses.json",
                    AgilityCourseDefinition[].class)));

    public List<AgilityCourseDefinition> all()
    {
        return Collections.unmodifiableList(courses);
    }

    public AgilityCourseDefinition wildernessCourse()
    {
        for (AgilityCourseDefinition course : courses)
        {
            if (course.isWilderness())
            {
                return course;
            }
        }
        return null;
    }

}

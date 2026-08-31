package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/**
 * Starter course catalog aligned with RuneLite's current course regions.
 * Level and quest requirements remain Compass game data so the evaluator can
 * combine them with live account state and remembered region observations.
 */
@Singleton
public class AgilityCourseCatalog
{
    private final List<AgilityCourseDefinition> courses = Arrays.asList(
            course("gnome", "Gnome Stronghold course", 1, 9781, null, false),
            course("draynor", "Draynor Village rooftop", 10, 12338, null, false),
            course("al_kharid", "Al Kharid rooftop", 20, 13105, null, false),
            course("varrock", "Varrock rooftop", 30, 12853, null, false),
            course("canifis", "Canifis rooftop", 40, 13878, "Priest in Peril", false),
            course("falador", "Falador rooftop", 50, 12084, null, false),
            course("wilderness", "Wilderness Agility Course", 52, 11837, null, true),
            course("seers", "Seers' Village rooftop", 60, 10806, null, false),
            course("pollnivneach", "Pollnivneach rooftop", 70, 13358, null, false),
            course("prifddinas", "Prifddinas Agility Course", 75, 12895, "Song of the Elves", false),
            course("rellekka", "Rellekka rooftop", 80, 10553, null, false),
            course("ardougne", "Ardougne rooftop", 90, 10547, null, false)
    );

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

    private static AgilityCourseDefinition course(
            String id,
            String name,
            int level,
            int region,
            String quest,
            boolean wilderness)
    {
        return new AgilityCourseDefinition(
                id,
                name,
                level,
                region,
                quest,
                wilderness
        );
    }
}

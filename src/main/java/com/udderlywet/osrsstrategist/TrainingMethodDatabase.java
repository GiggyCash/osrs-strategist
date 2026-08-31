package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Compatibility training methods loaded from the required bundled catalog. */
@Singleton
public class TrainingMethodDatabase
{
    private static final String RESOURCE = Text.get(897);
    private final List<TrainingMethod> methods;

    public TrainingMethodDatabase()
    {
        methods = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, TrainingMethod[].class)));
        for (TrainingMethod method : methods)
            if (method.getId() == null || method.getSkill() == null)
                throw new IllegalStateException(Text.get(1132) + RESOURCE);
    }

    public List<TrainingMethod> methodsFor(Skill skill)
    {
        List<TrainingMethod> result = new ArrayList<>();
        for (TrainingMethod method : methods)
            if (method.getSkill() == skill) result.add(method);
        return Collections.unmodifiableList(result);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Explicitly safe F2P baseline methods loaded from the bundled catalog. */
@Singleton
public class F2pBaselineMethodCatalog
{
    private final Map<Skill, List<CuratedTrainingMethod>> bySkill = new EnumMap<>(Skill.class);

    public F2pBaselineMethodCatalog()
    {
        for (CuratedTrainingMethod value : BundledCatalogLoader.array(
                Text.get(217), CuratedTrainingMethod[].class))
        {
            var skill = value.getMethod().getSkill();
            bySkill.computeIfAbsent(skill, ignored -> new ArrayList<>()).add(value);
        }
        for (Map.Entry<Skill, List<CuratedTrainingMethod>> entry : bySkill.entrySet())
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        var methods = bySkill.get(skill);
        return methods == null ? Collections.emptyList() : methods;
    }
}

package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Curated training methods loaded from the required bundled catalog. */
@Singleton
public class ExpandedTrainingMethodCatalog
{
    public static final String PROVENANCE =
            Text.get(215);
    public static final String AUDITED_THROUGH = "2026-08-25";
    private static final String RESOURCE = Text.get(216);
    private final Map<Skill, List<CuratedTrainingMethod>> methods = new EnumMap<>(Skill.class);

    public ExpandedTrainingMethodCatalog()
    {
        for (Skill skill : Skill.values()) methods.put(skill, new ArrayList<>());
        for (CuratedTrainingMethod curated
                : BundledCatalogLoader.array(RESOURCE, CuratedTrainingMethod[].class))
        {
            if (curated.getMethod() == null || curated.getMetadata() == null
                    || curated.getMethod().getSkill() == null
                    || curated.getMethod().getId() == null)
                throw new IllegalStateException(Text.get(1132) + RESOURCE);
            methods.get(curated.getMethod().getSkill()).add(curated);
        }
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        List<CuratedTrainingMethod> list = methods.get(skill);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }
}

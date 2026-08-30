package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
                "/content/catalogs/f2p-baseline-methods.json", CuratedTrainingMethod[].class))
        {
            Skill skill = value.getMethod().getSkill();
            bySkill.computeIfAbsent(skill, ignored -> new ArrayList<>()).add(value);
        }
        for (Map.Entry<Skill, List<CuratedTrainingMethod>> entry : bySkill.entrySet())
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        List<CuratedTrainingMethod> methods = bySkill.get(skill);
        return methods == null ? Collections.emptyList() : methods;
    }
}

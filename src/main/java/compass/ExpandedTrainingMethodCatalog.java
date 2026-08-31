package compass;

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
            if (curated.method() == null || curated.getMetadata() == null
                    || curated.method().getSkill() == null
                    || curated.method().getId() == null)
                throw new IllegalStateException(Text.get(1132) + RESOURCE);
            methods.get(curated.method().getSkill()).add(curated);
        }
    }

    public List<CuratedTrainingMethod> methodsFor(Skill skill)
    {
        var list = methods.get(skill);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }
}

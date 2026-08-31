package compass;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Single indexed source for legacy, curated, and explicitly F2P training routes. */
@Singleton
public class TrainingMethodCatalog
{
    public static final String PROVENANCE = Text.get(215);
    public static final String AUDITED_THROUGH = "2026-08-25";

    private final Map<Skill, List<TrainingMethod>> legacy = new EnumMap<>(Skill.class);
    private final Map<Skill, List<CuratedTrainingMethod>> curated = new EnumMap<>(Skill.class);
    private final Map<Skill, List<CuratedTrainingMethod>> f2p = new EnumMap<>(Skill.class);

    public TrainingMethodCatalog()
    {
        for (TrainingMethod method : BundledCatalogLoader.array(
                Text.get(897), TrainingMethod[].class))
        {
            if (method.id == null || method.getSkill() == null)
                throw invalid(Text.get(897));
            legacy.computeIfAbsent(method.getSkill(), ignored -> new ArrayList<>()).add(method);
        }
        loadCurated(Text.get(216), curated, true);
        loadCurated(Text.get(217), f2p, false);
        freeze(legacy);
        freeze(curated);
        freeze(f2p);
    }

    public List<TrainingMethod> legacyFor(Skill skill)
    {
        return legacy.getOrDefault(skill, Collections.emptyList());
    }

    public List<CuratedTrainingMethod> curatedFor(Skill skill)
    {
        return curated.getOrDefault(skill, Collections.emptyList());
    }

    public List<CuratedTrainingMethod> f2pFor(Skill skill)
    {
        return f2p.getOrDefault(skill, Collections.emptyList());
    }

    boolean legacyOnly() { return false; }

    private static void loadCurated(String resource,
            Map<Skill, List<CuratedTrainingMethod>> target, boolean metadataRequired)
    {
        for (CuratedTrainingMethod value : BundledCatalogLoader.array(
                resource, CuratedTrainingMethod[].class))
        {
            TrainingMethod method = value == null ? null : value.method();
            if (method == null || method.getSkill() == null || method.id == null
                    || metadataRequired && value.getMetadata() == null)
                throw invalid(resource);
            target.computeIfAbsent(method.getSkill(), ignored -> new ArrayList<>()).add(value);
        }
    }

    private static <T> void freeze(Map<Skill, List<T>> values)
    {
        for (Map.Entry<Skill, List<T>> entry : values.entrySet())
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    private static IllegalStateException invalid(String resource)
    {
        return new IllegalStateException(Text.get(1132) + resource);
    }
}

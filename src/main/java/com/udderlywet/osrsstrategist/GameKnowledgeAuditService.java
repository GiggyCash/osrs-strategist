package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Central catalog-breadth audit.
 *
 * <p>This is a maintainability feature: adding a new game system is useful only
 * if future edits cannot silently delete large chunks of coverage. CI therefore
 * has one place to measure broad knowledge surfaces while domain-specific tests
 * continue to validate exact rules.</p>
 */
@Singleton
public class GameKnowledgeAuditService
{
    private final TrainingMethodDatabase legacyMethods;
    private final ExpandedTrainingMethodCatalog expandedMethods;
    private final F2pBaselineMethodCatalog f2pBaselineMethods;
    private final MinigameCatalog minigames;
    private final PvmActivityCatalog pvmActivities;
    private final ProgressionObjectiveCatalog objectives;
    private final MoneyMakingCatalog moneyMaking;
    private final ResourceSourceCatalog resourceSources;

    @Inject
    public GameKnowledgeAuditService(
            TrainingMethodDatabase legacyMethods,
            ExpandedTrainingMethodCatalog expandedMethods,
            F2pBaselineMethodCatalog f2pBaselineMethods,
            MinigameCatalog minigames,
            PvmActivityCatalog pvmActivities,
            ProgressionObjectiveCatalog objectives,
            MoneyMakingCatalog moneyMaking,
            ResourceSourceCatalog resourceSources)
    {
        this.legacyMethods = legacyMethods;
        this.expandedMethods = expandedMethods;
        this.f2pBaselineMethods = f2pBaselineMethods;
        this.minigames = minigames;
        this.pvmActivities = pvmActivities;
        this.objectives = objectives;
        this.moneyMaking = moneyMaking;
        this.resourceSources = resourceSources;
    }

    public GameKnowledgeAuditReport audit()
    {
        EnumMap<Skill, Integer> methodsPerSkill = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            Set<String> ids = new HashSet<>();
            for (TrainingMethod method : legacyMethods.methodsFor(skill))
                if (method != null && method.getId() != null) ids.add(method.getId());
            for (CuratedTrainingMethod method : expandedMethods.methodsFor(skill))
                if (method != null && method.getMethod() != null
                        && method.getMethod().getId() != null)
                    ids.add(method.getMethod().getId());
            for (CuratedTrainingMethod method : f2pBaselineMethods.methodsFor(skill))
                if (method != null && method.getMethod() != null
                        && method.getMethod().getId() != null)
                    ids.add(method.getMethod().getId());
            methodsPerSkill.put(skill, ids.size());
        }

        return new GameKnowledgeAuditReport(
                methodsPerSkill,
                minigames.all().size(),
                pvmActivities.all().size(),
                objectives.all().size(),
                moneyMaking.all().size(),
                resourceSources.all().size());
    }
}

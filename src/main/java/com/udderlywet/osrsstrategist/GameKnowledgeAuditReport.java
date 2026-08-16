package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * Snapshot of catalog breadth used by tests, maintainers, and future diagnostic
 * tooling. It intentionally reports counts rather than claiming that a count
 * equals exhaustive correctness.
 */
public final class GameKnowledgeAuditReport
{
    private final Map<Skill, Integer> trainingMethodsPerSkill;
    private final int minigames;
    private final int pvmActivities;
    private final int progressionObjectives;
    private final int moneyMakingMethods;
    private final int resourceSources;

    public GameKnowledgeAuditReport(
            Map<Skill, Integer> trainingMethodsPerSkill,
            int minigames,
            int pvmActivities,
            int progressionObjectives,
            int moneyMakingMethods,
            int resourceSources)
    {
        this.trainingMethodsPerSkill = Collections.unmodifiableMap(
                new EnumMap<>(trainingMethodsPerSkill));
        this.minigames = minigames;
        this.pvmActivities = pvmActivities;
        this.progressionObjectives = progressionObjectives;
        this.moneyMakingMethods = moneyMakingMethods;
        this.resourceSources = resourceSources;
    }

    public Map<Skill, Integer> getTrainingMethodsPerSkill()
    {
        return trainingMethodsPerSkill;
    }

    public int trainingMethodsFor(Skill skill)
    {
        return trainingMethodsPerSkill.getOrDefault(skill, 0);
    }

    public int getMinigames() { return minigames; }
    public int getPvmActivities() { return pvmActivities; }
    public int getProgressionObjectives() { return progressionObjectives; }
    public int getMoneyMakingMethods() { return moneyMakingMethods; }
    public int getResourceSources() { return resourceSources; }

    /**
     * Minimum breadth floor, not a declaration of finished content coverage.
     * Exact requirements/readiness are separately protected by no-guessing tests.
     */
    public boolean meetsFoundationBreadthFloor()
    {
        for (Skill skill : Skill.values())
        {
            if (trainingMethodsFor(skill) < 2) return false;
        }
        return minigames >= 45
                && pvmActivities >= 20
                && progressionObjectives >= 35
                && moneyMakingMethods >= 20
                && resourceSources >= 20;
    }
}

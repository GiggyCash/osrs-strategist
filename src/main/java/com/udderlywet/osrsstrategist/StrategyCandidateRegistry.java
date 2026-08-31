package com.udderlywet.osrsstrategist;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;

/** Registry for non-skill activities that may compete for the main queue. */
@Singleton
public class StrategyCandidateRegistry
{
    @Getter
    private final List<StrategyCandidateProvider> providers;

    @Inject
    public StrategyCandidateRegistry(
            ClueCandidateProvider clueProvider,
            PvmCandidateProvider pvmProvider,
            QuestCandidateProvider questProvider,
            DiaryCandidateProvider diaryProvider,
            CombatAchievementCandidateProvider combatAchievementProvider,
            InfrastructureCandidateProvider infrastructureProvider,
            GearCandidateProvider gearProvider,
            ProgressionUpgradeCandidateProvider progressionUpgradeProvider,
            ResourceDetourCandidateProvider resourceDetourProvider,
            SlayerCandidateProvider slayerProvider,
            MoneyMakingCandidateProvider moneyProvider,
            MinigameCandidateProvider minigameProvider,
            CollectionLogCandidateProvider collectionLogProvider)
    {
        this.providers = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(
                        clueProvider,
                        pvmProvider,
                        questProvider,
                        diaryProvider,
                        combatAchievementProvider,
                        infrastructureProvider,
                        progressionUpgradeProvider,
                        resourceDetourProvider,
                        slayerProvider,
                        gearProvider,
                        moneyProvider,
                        minigameProvider,
                        collectionLogProvider))
        );
    }

    /** Compatibility constructor for focused tests written before the expanded registry. */
    public StrategyCandidateRegistry(
            ClueCandidateProvider clueProvider,
            PvmCandidateProvider pvmProvider)
    {
        this.providers = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(clueProvider, pvmProvider))
        );
    }

    StrategyCandidateRegistry(List<StrategyCandidateProvider> providers)
    {
        this.providers = Collections.unmodifiableList(new ArrayList<>(
                providers == null ? Collections.emptyList() : providers));
    }

}

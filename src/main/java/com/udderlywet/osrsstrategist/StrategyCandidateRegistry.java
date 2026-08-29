package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Registry for non-skill activities that may compete for the main queue. */
@Singleton
public class StrategyCandidateRegistry
{
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

    public List<StrategyCandidateProvider> getProviders()
    {
        return providers;
    }
}

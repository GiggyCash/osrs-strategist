package com.udderlywet.osrsstrategist;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Getter;

/** Explicit registry showing every reasoning domain that contributes signals. */
@Singleton
public class StrategyModuleRegistry
{
    @Getter
    private final List<StrategyModule> modules;

    @Inject
    public StrategyModuleRegistry(
            GoalStrategyModule goalModule,
            AccountModeStrategyModule accountModeModule,
            UimStrategyModule uimModule,
            ClueStrategyModule clueModule,
            ProgressionStrategyModule progressionModule,
            AccountSystemsStrategyModule accountSystemsModule,
            PvmStrategyModule pvmModule)
    {
        modules = Collections.unmodifiableList(
                new ArrayList<>(
                        Arrays.asList(
                                goalModule,
                                accountModeModule,
                                uimModule,
                                clueModule,
                                progressionModule,
                                accountSystemsModule,
                                pvmModule
                        )
                )
        );
    }

}

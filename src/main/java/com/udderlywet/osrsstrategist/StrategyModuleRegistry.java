package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Explicit registry of strategy modules.
 *
 * <p>Using a small registry avoids hidden Guice multibinding magic while the
 * project is young. It also gives a new maintainer one obvious place to see
 * which reasoning subsystems participate in the final strategy.</p>
 */
@Singleton
public class StrategyModuleRegistry
{
    private final List<StrategyModule> modules;

    @Inject
    public StrategyModuleRegistry(
            GoalStrategyModule goalModule,
            AccountModeStrategyModule accountModeModule,
            ClueStrategyModule clueModule,
            ProgressionStrategyModule progressionModule,
            PvmStrategyModule pvmModule)
    {
        modules = Collections.unmodifiableList(
                new ArrayList<>(
                        Arrays.asList(
                                goalModule,
                                accountModeModule,
                                clueModule,
                                progressionModule,
                                pvmModule
                        )
                )
        );
    }

    public List<StrategyModule> getModules()
    {
        return modules;
    }
}

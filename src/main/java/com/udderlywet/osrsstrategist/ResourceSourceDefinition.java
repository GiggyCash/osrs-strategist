package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** A stable, human-readable acquisition family for common progression resources. */
@Getter
public final class ResourceSourceDefinition
{
    private final String id;
    private final List<String> nameTokens;
    private final String mainRoute;
    private final String ironRoute;
    private final String uimRoute;
    private final List<String> freeToPlayItemNames;
    private final String freeToPlayMainRoute;
    private final String freeToPlayIronRoute;
    private final String freeToPlayUimRoute;
    private final List<StrategySourceId> sourceIds;
    private final boolean wilderness;
    private final RiskLevel riskLevel;

    public ResourceSourceDefinition(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this(id, nameTokens, mainRoute, ironRoute, uimRoute,
                Collections.emptyList(), null, null, null,
                wilderness, riskLevel);
    }

    public ResourceSourceDefinition(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            List<String> freeToPlayItemNames, String freeToPlayMainRoute,
            String freeToPlayIronRoute, String freeToPlayUimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this.id = id;
        this.nameTokens = Collections.unmodifiableList(nameTokens == null
                ? new ArrayList<>() : new ArrayList<>(nameTokens));
        this.mainRoute = mainRoute;
        this.ironRoute = ironRoute;
        this.uimRoute = uimRoute;
        this.freeToPlayItemNames = Collections.unmodifiableList(
                freeToPlayItemNames == null ? new ArrayList<>()
                        : new ArrayList<>(freeToPlayItemNames));
        this.freeToPlayMainRoute = freeToPlayMainRoute;
        this.freeToPlayIronRoute = freeToPlayIronRoute;
        this.freeToPlayUimRoute = freeToPlayUimRoute;
        this.sourceIds = Collections.unmodifiableList(
                freeToPlayItemNames == null || freeToPlayItemNames.isEmpty()
                        ? Arrays.asList(StrategySourceId.GENERAL_SKILL_TRAINING,
                                StrategySourceId.IRONMAN_GENERAL,
                                StrategySourceId.UIM_GENERAL)
                        : Arrays.asList(StrategySourceId.GENERAL_SKILL_TRAINING,
                                StrategySourceId.IRONMAN_GENERAL,
                                StrategySourceId.F2P_IRONMAN_GENERAL,
                                StrategySourceId.UIM_GENERAL));
        this.wilderness = wilderness;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
    }

}

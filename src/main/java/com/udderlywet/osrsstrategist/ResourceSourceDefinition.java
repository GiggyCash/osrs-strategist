package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A stable, human-readable acquisition family for common progression resources. */
public final class ResourceSourceDefinition
{
    private final String id;
    private final List<String> nameTokens;
    private final String mainRoute;
    private final String ironRoute;
    private final String uimRoute;
    private final boolean wilderness;
    private final RiskLevel riskLevel;

    public ResourceSourceDefinition(String id, List<String> nameTokens,
            String mainRoute, String ironRoute, String uimRoute,
            boolean wilderness, RiskLevel riskLevel)
    {
        this.id = id;
        this.nameTokens = Collections.unmodifiableList(nameTokens == null
                ? new ArrayList<>() : new ArrayList<>(nameTokens));
        this.mainRoute = mainRoute;
        this.ironRoute = ironRoute;
        this.uimRoute = uimRoute;
        this.wilderness = wilderness;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
    }

    public String getId() { return id; }
    public List<String> getNameTokens() { return nameTokens; }
    public String getMainRoute() { return mainRoute; }
    public String getIronRoute() { return ironRoute; }
    public String getUimRoute() { return uimRoute; }
    public boolean isWilderness() { return wilderness; }
    public RiskLevel getRiskLevel() { return riskLevel; }
}

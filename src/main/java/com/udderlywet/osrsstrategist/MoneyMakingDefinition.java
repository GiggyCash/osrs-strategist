package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Skill;

/** Stable strategy metadata for a money/resource-producing activity. */
public final class MoneyMakingDefinition
{
    private final String id;
    private final String name;
    private final String description;
    private final Skill primarySkill;
    private final int minimumLevel;
    private final boolean freeToPlay;
    private final Set<AccountMode> supportedModes;
    private final RiskLevel riskLevel;
    private final AttentionLevel attention;
    private final boolean wilderness;
    private final boolean requiresLivePrices;

    public MoneyMakingDefinition(String id, String name, String description,
            Skill primarySkill, int minimumLevel, boolean freeToPlay,
            Set<AccountMode> supportedModes, RiskLevel riskLevel,
            AttentionLevel attention, boolean wilderness,
            boolean requiresLivePrices)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.primarySkill = primarySkill;
        this.minimumLevel = Math.max(1, minimumLevel);
        this.freeToPlay = freeToPlay;
        this.supportedModes = supportedModes == null || supportedModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supportedModes));
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.wilderness = wilderness;
        this.requiresLivePrices = requiresLivePrices;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Skill getPrimarySkill() { return primarySkill; }
    public int getMinimumLevel() { return minimumLevel; }
    public boolean isFreeToPlay() { return freeToPlay; }
    public Set<AccountMode> getSupportedModes() { return supportedModes; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public AttentionLevel getAttention() { return attention; }
    public boolean isWilderness() { return wilderness; }
    public boolean isRequiresLivePrices() { return requiresLivePrices; }

    public boolean supports(AccountMode mode)
    {
        return mode != null && supportedModes.contains(mode);
    }
}

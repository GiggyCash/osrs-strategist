package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Stable strategy metadata for a money/resource-producing activity. */
public final class MoneyMakingDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final Skill primarySkill;
    @Getter
    private final int minimumLevel;
    @Getter
    private final boolean freeToPlay;
    @Getter
    private final Set<AccountMode> supportedModes;
    @Getter
    private final RiskLevel riskLevel;
    @Getter
    private final AttentionLevel attention;
    @Getter
    private final boolean wilderness;
    @Getter
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


    public boolean supports(AccountMode mode)
    {
        return mode != null && supportedModes.contains(mode);
    }
}

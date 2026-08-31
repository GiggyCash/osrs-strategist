package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

public final class MinigameDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final Skill primarySkill;
    @Getter
    private final int minimumLevel;
    @Getter
    private final boolean freeToPlay;
    @Getter
    private final RiskLevel riskLevel;
    @Getter
    private final AttentionLevel attention;
    @Getter
    private final Set<AccountMode> supportedModes;
    @Getter
    private final String rewardFocus;
    @Getter
    private final boolean combatActivity;

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus)
    {
        this(id, name, primarySkill, minimumLevel, freeToPlay, riskLevel,
                attention, supportedModes, rewardFocus, false);
    }

    public MinigameDefinition(String id, String name, Skill primarySkill,
            int minimumLevel, boolean freeToPlay, RiskLevel riskLevel,
            AttentionLevel attention, Set<AccountMode> supportedModes,
            String rewardFocus, boolean combatActivity)
    {
        this.id = id;
        this.name = name;
        this.primarySkill = primarySkill;
        this.minimumLevel = Math.max(1, minimumLevel);
        this.freeToPlay = freeToPlay;
        this.riskLevel = riskLevel == null ? RiskLevel.NONE : riskLevel;
        this.attention = attention == null ? AttentionLevel.MODERATE : attention;
        this.supportedModes = supportedModes == null || supportedModes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(supportedModes));
        this.rewardFocus = rewardFocus;
        this.combatActivity = combatActivity;
    }

    public boolean supports(AccountMode mode) { return supportedModes.contains(mode); }
}

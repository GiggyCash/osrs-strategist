package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Stable strategy metadata for a money/resource-producing activity. */
@RequiredArgsConstructor
@Getter
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



    public boolean supports(AccountMode mode)
    {
        return mode != null && supportedModes.contains(mode);
    }
}

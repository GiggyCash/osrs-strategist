package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** One reusable transport system; destinations are fan-out evidence, not score hacks. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TransportDefinition
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final TransportCategory category;
    @Getter
    private final boolean membersOnly;
    @Getter
    private final String quest;
    private final boolean questStartSuffices;
    @Getter
    private final Skill skill;
    @Getter
    private final int level;
    @Getter
    private final String itemOrAccessCheck;
    @Getter
    private final String pohFurniture;
    @Getter
    private final boolean wilderness;
    @Getter
    private final List<String> uses;


    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public int getFanOut() { return uses.size(); }
}

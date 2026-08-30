package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

import net.runelite.api.Skill;

/** One reusable transport system; destinations are fan-out evidence, not score hacks. */
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

    TransportDefinition(String id, String name, TransportCategory category,
            boolean membersOnly, String quest, boolean questStartSuffices,
            Skill skill, int level, String itemOrAccessCheck,
            String pohFurniture, boolean wilderness, List<String> uses)
    {
        this.id = id;
        this.name = name;
        this.category = category;
        this.membersOnly = membersOnly;
        this.quest = quest;
        this.questStartSuffices = questStartSuffices;
        this.skill = skill;
        this.level = Math.max(0, level);
        this.itemOrAccessCheck = itemOrAccessCheck;
        this.pohFurniture = pohFurniture;
        this.wilderness = wilderness;
        this.uses = Collections.unmodifiableList(new ArrayList<>(uses));
    }

    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public int getFanOut() { return uses.size(); }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;

/** One reusable transport system; destinations are fan-out evidence, not score hacks. */
public final class TransportDefinition
{
    private final String id;
    private final String name;
    private final TransportCategory category;
    private final boolean membersOnly;
    private final String quest;
    private final boolean questStartSuffices;
    private final Skill skill;
    private final int level;
    private final String itemOrAccessCheck;
    private final String pohFurniture;
    private final boolean wilderness;
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

    public String getId() { return id; }
    public String getName() { return name; }
    public TransportCategory getCategory() { return category; }
    public boolean isMembersOnly() { return membersOnly; }
    public String getQuest() { return quest; }
    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public Skill getSkill() { return skill; }
    public int getLevel() { return level; }
    public String getItemOrAccessCheck() { return itemOrAccessCheck; }
    public String getPohFurniture() { return pohFurniture; }
    public boolean isWilderness() { return wilderness; }
    public List<String> getUses() { return uses; }
    public int getFanOut() { return uses.size(); }
}

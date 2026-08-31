package com.udderlywet.osrsstrategist;

import java.util.*;
import net.runelite.api.Skill;

/** Verified quest requirements and progression effects used by the local planner. */
public final class QuestDefinition
{
    private final String name;
    private final boolean freeToPlay;
    private final List<String> prerequisites;
    private final Map<Skill, Integer> skillRequirements;
    private final List<QuestItemRequirement> itemRequirements;
    private final ItemRequirementExpression itemRequirementExpression;
    private final int questPointsRequired;
    private final List<String> accessChecks;
    private final String startLocation;
    private final List<String> unlocks;
    private final Map<Skill, Integer> rewardXp;
    private final List<String> fieldUncertainties;

    public QuestDefinition(String name, boolean freeToPlay,
            List<String> prerequisites, Map<Skill, Integer> skillRequirements,
            List<QuestItemRequirement> itemRequirements, int questPointsRequired,
            List<String> accessChecks, String startLocation, List<String> unlocks,
            Map<Skill, Integer> rewardXp)
    {
        this(name, freeToPlay, prerequisites, skillRequirements, itemRequirements,
                null, questPointsRequired, accessChecks, startLocation, unlocks,
                rewardXp, Collections.emptyList());
    }

    public QuestDefinition(String name, boolean freeToPlay,
            List<String> prerequisites, Map<Skill, Integer> skillRequirements,
            List<QuestItemRequirement> itemRequirements,
            ItemRequirementExpression itemRequirementExpression,
            int questPointsRequired, List<String> accessChecks,
            String startLocation, List<String> unlocks,
            Map<Skill, Integer> rewardXp)
    {
        this(name, freeToPlay, prerequisites, skillRequirements, itemRequirements,
                itemRequirementExpression, questPointsRequired, accessChecks,
                startLocation, unlocks, rewardXp, Collections.emptyList());
    }

    public QuestDefinition(String name, boolean freeToPlay,
            List<String> prerequisites, Map<Skill, Integer> skillRequirements,
            List<QuestItemRequirement> itemRequirements,
            ItemRequirementExpression itemRequirementExpression,
            int questPointsRequired, List<String> accessChecks,
            String startLocation, List<String> unlocks,
            Map<Skill, Integer> rewardXp, List<String> fieldUncertainties)
    {
        this.name = name;
        this.freeToPlay = freeToPlay;
        this.prerequisites = immutable(prerequisites);
        EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
        if (skillRequirements != null) skills.putAll(skillRequirements);
        this.skillRequirements = Collections.unmodifiableMap(skills);
        this.itemRequirements = itemRequirements == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(itemRequirements));
        this.itemRequirementExpression = itemRequirementExpression;
        this.questPointsRequired = Math.max(0, questPointsRequired);
        this.accessChecks = immutable(accessChecks);
        this.startLocation = startLocation;
        this.unlocks = immutable(unlocks);
        EnumMap<Skill, Integer> rewards = new EnumMap<>(Skill.class);
        if (rewardXp != null) rewards.putAll(rewardXp);
        this.rewardXp = Collections.unmodifiableMap(rewards);
        this.fieldUncertainties = immutable(fieldUncertainties);
    }

    public String getName() { return name; }
    public boolean isFreeToPlay() { return freeToPlay; }
    public List<String> getPrerequisites() { return prerequisites; }
    public Map<Skill, Integer> getSkillRequirements() { return skillRequirements; }
    public List<QuestItemRequirement> getItemRequirements() { return itemRequirements; }
    public ItemRequirementExpression getItemRequirementExpression()
    {
        return itemRequirementExpression;
    }
    public int getQuestPointsRequired() { return questPointsRequired; }
    public List<String> getAccessChecks() { return accessChecks; }
    public String getStartLocation() { return startLocation; }
    public List<String> getUnlocks() { return unlocks; }
    public Map<Skill, Integer> getRewardXp() { return rewardXp; }
    public List<String> getFieldUncertainties() { return fieldUncertainties; }
    public boolean hasFieldUncertainty() { return !fieldUncertainties.isEmpty(); }

    private static List<String> immutable(List<String> values)
    {
        return values == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static final class QuestItemRequirement
    {
        private final String name;
        private final int quantity;

        public QuestItemRequirement(String name, int quantity)
        {
            this.name = name;
            this.quantity = Math.max(1, quantity);
        }

        public String getName() { return name; }
        public int getQuantity() { return quantity; }
    }
}

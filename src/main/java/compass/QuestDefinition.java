package compass;
import static java.lang.Math.*;
import lombok.*;
import static java.util.Collections.*;

import java.util.*;
import net.runelite.api.Skill;

/** Verified quest requirements and progression effects used by the local planner. */
@Getter
public final class QuestDefinition
{
    final String name;
    final boolean freeToPlay;
    final List<String> prerequisites;
    final Map<Skill, Integer> skillRequirements;
    final List<QuestItemRequirement> itemRequirements;
    final ItemRule itemRequirementExpression;
    final int questPointsRequired;
    final List<String> accessChecks;
    final String startLocation;
    final List<String> unlocks;
    final Map<Skill, Integer> rewardXp;
    final List<String> fieldUncertainties;
   public QuestDefinition(String name, boolean freeToPlay,
            List<String> prerequisites, Map<Skill, Integer> skillRequirements,
            List<QuestItemRequirement> itemRequirements,
            ItemRule itemRequirementExpression,
            int questPointsRequired, List<String> accessChecks,
            String startLocation, List<String> unlocks,
            Map<Skill, Integer> rewardXp, List<String> fieldUncertainties)
    {
        this.name = name;
        this.freeToPlay = freeToPlay;
        this.prerequisites = immutable(prerequisites);
        EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
        if (skillRequirements != null) skills.putAll(skillRequirements);
        this.skillRequirements = unmodifiableMap(skills);
        this.itemRequirements = itemRequirements == null
                ? emptyList()
                : unmodifiableList(new ArrayList<>(itemRequirements));
        this.itemRequirementExpression = itemRequirementExpression;
        this.questPointsRequired = max(0, questPointsRequired);
        this.accessChecks = immutable(accessChecks);
        this.startLocation = startLocation;
        this.unlocks = immutable(unlocks);
        EnumMap<Skill, Integer> rewards = new EnumMap<>(Skill.class);
        if (rewardXp != null) rewards.putAll(rewardXp);
        this.rewardXp = unmodifiableMap(rewards);
        this.fieldUncertainties = immutable(fieldUncertainties);
    }
    public boolean hasFieldUncertainty() { return !fieldUncertainties.isEmpty(); }

    private static List<String> immutable(List<String> values)
    {
        return values == null ? emptyList()
                : unmodifiableList(new ArrayList<>(values));
    }

    @Getter
    public static final class QuestItemRequirement
    {
        final String name;
        final int quantity;

        public QuestItemRequirement(String name, int quantity)
        {
            this.name = name;
            this.quantity = max(1, quantity);
        }

    }
}

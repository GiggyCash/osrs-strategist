package compass;
import static java.lang.Math.*;
import lombok.*;
import static java.util.Collections.*;

import java.util.*;
import net.runelite.api.Skill;

/** Verified quest requirements and progression effects used by the local planner. */
@Getter
@AllArgsConstructor
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
    public boolean hasFieldUncertainty() { return !fieldUncertainties.isEmpty(); }

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

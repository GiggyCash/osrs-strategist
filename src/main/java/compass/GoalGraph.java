package compass;

import java.util.*;
import javax.inject.Singleton;

/** Verified quest roots used by the shared goal-path traversal. */
@Singleton
public final class GoalGraph
{
    private static final Map<GoalType, List<String>> QUEST_ROOTS;
    static
    {
        Map<GoalType, List<String>> roots = new EnumMap<>(GoalType.class);
        roots.put(GoalType.BARROWS_GLOVES,
                Collections.singletonList(Text.get(1198)));
        roots.put(GoalType.PRIFDDINAS,
                Collections.singletonList(Text.get(1721)));
        roots.put(GoalType.BOWFA,
                Collections.singletonList(Text.get(1721)));
        QUEST_ROOTS = Collections.unmodifiableMap(roots);
    }

    public List<String> questRootsFor(GoalType goal)
    {
        return QUEST_ROOTS.getOrDefault(goal, Collections.emptyList());
    }

    public boolean hasPlanningPath(GoalType goal)
    {
        return goal != null && goal != GoalType.AUTOMATIC
                && goal != GoalType.CUSTOM;
    }
}

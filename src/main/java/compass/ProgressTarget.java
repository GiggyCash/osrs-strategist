package compass;

import lombok.Getter;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** The skill checkpoint currently being executed by the active plan. */
@Getter
public final class ProgressTarget
{
    private final String activityId;
    private final String methodId;
    private final Skill skill;
    private final int targetLevel;
    private final int targetXp;

    public ProgressTarget(
            String activityId,
            String methodId,
            Skill skill,
            int targetLevel)
    {
        if (skill == null || targetLevel < 2 || targetLevel > 126)
        {
            throw new IllegalArgumentException(Text.get(1157));
        }
        this.activityId = activityId;
        this.methodId = methodId;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.targetXp = Experience.getXpForLevel(targetLevel);
    }

}

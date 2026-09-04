package compass;
import lombok.*;

import java.util.*;


import net.runelite.api.Skill;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class TrainingMethod
{
    final String id;
    final Skill skill;
    final int minLevel;
    final int maxLevel;
    final String name;
    final String instructions;
    @Getter(AccessLevel.NONE)
    final double efficientScore;
    @Getter(AccessLevel.NONE)
    final double balancedScore;
    @Getter(AccessLevel.NONE)
    final double relaxedScore;
    final AttentionLevel attentionLevel;
    final int minimumSessionMinutes;
    final int setupMinutes;
    final List<String> requirements;
    final Confidence confidence;
    final boolean membersOnly;
    final boolean wilderness;
    final boolean progressionProtected;
    @Getter(AccessLevel.NONE)
    final boolean delegatesMethodChoice;

    public TrainingMethod(
            String id,
            Skill skill,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            double efficientScore,
            double balancedScore,
            double relaxedScore,
            AttentionLevel attentionLevel,
            int minimumSessionMinutes,
            int setupMinutes,
            List<String> requirements,
            Confidence confidence)
    {
        this(id, skill, minLevel, maxLevel, name, instructions,
                efficientScore, balancedScore, relaxedScore, attentionLevel,
                minimumSessionMinutes, setupMinutes, requirements, confidence,
                false, false, false);
    }

    public TrainingMethod(
            String id,
            Skill skill,
            int minLevel,
            int maxLevel,
            String name,
            String instructions,
            double efficientScore,
            double balancedScore,
            double relaxedScore,
            AttentionLevel attentionLevel,
            int minimumSessionMinutes,
            int setupMinutes,
            List<String> requirements,
            Confidence confidence,
            boolean membersOnly,
            boolean wilderness,
            boolean progressionProtected)
    {
        this(id, skill, minLevel, maxLevel, name, instructions,
                efficientScore, balancedScore, relaxedScore, attentionLevel,
                minimumSessionMinutes, setupMinutes,
                Collections.unmodifiableList(new ArrayList<>(requirements)),
                confidence, membersOnly, wilderness, progressionProtected,
                false);
    }

    public boolean delegatesMethodChoice() { return delegatesMethodChoice; }

    public boolean supportsLevel(int level)
    {
        return level >= minLevel && level <= maxLevel;
    }

    public double scoreFor(
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        double score;

        switch (strategyMode)
        {
            case EFFICIENT:
                score = efficientScore;
                break;
            case RELAXED:
                score = relaxedScore;
                break;
            case BALANCED:
            default:
                score = balancedScore;
                break;
        }

        switch (sessionIntent)
        {
            case QUICK_20_MIN:
                if (minimumSessionMinutes <= 20) score += 4.0;
                else score -= 5.0;
                if (setupMinutes <= 5) score += 3.0;
                break;
            case ONE_HOUR:
                if (minimumSessionMinutes <= 60) score += 2.0;
                break;
            case LONG_SESSION:
                score += efficientScore * 0.10;
                break;
            case AFK:
                switch (attentionLevel)
                {
                    case AFK: score += 8.0; break;
                    case LOW: score += 5.0; break;
                    case ACTIVE: score -= 8.0; break;
                    case MODERATE: score -= 2.0; break;
                    default: break;
                }
                break;
            case PICK_FOR_ME:
            default:
                break;
        }

        if (confidence == Confidence.CHECK_NEEDED)
        {
            score -= 1.5;
        }
        else if (confidence == Confidence.BLOCKED)
        {
            score -= 1000.0;
        }

        return score;
    }
}

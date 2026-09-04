package compass;
import lombok.*;

import java.util.*;


import net.runelite.api.Skill;

public final class TrainingMethod
{
    @Getter
    final String id;
    @Getter
    final Skill skill;
    @Getter
    final int minLevel;
    @Getter
    final int maxLevel;
    @Getter
    final String name;
    @Getter
    final String instructions;
    final double efficientScore;
    final double balancedScore;
    final double relaxedScore;
    @Getter
    final AttentionLevel attentionLevel;
    @Getter
    final int minimumSessionMinutes;
    @Getter
    final int setupMinutes;
    @Getter
    final List<String> requirements;
    @Getter
    final Confidence confidence;
    @Getter
    final boolean membersOnly;
    @Getter
    final boolean wilderness;
    @Getter
    final boolean progressionProtected;
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
            boolean membersOnly)
    {
        this(id, skill, minLevel, maxLevel, name, instructions,
                efficientScore, balancedScore, relaxedScore, attentionLevel,
                minimumSessionMinutes, setupMinutes, requirements, confidence,
                membersOnly, false, false);
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
                minimumSessionMinutes, setupMinutes, requirements, confidence,
                membersOnly, wilderness, progressionProtected, false);
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
            boolean progressionProtected,
            boolean delegatesMethodChoice)
    {
        this.id = id;
        this.skill = skill;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.name = name;
        this.instructions = instructions;
        this.efficientScore = efficientScore;
        this.balancedScore = balancedScore;
        this.relaxedScore = relaxedScore;
        this.attentionLevel = attentionLevel;
        this.minimumSessionMinutes = minimumSessionMinutes;
        this.setupMinutes = setupMinutes;
        this.requirements = Collections.unmodifiableList(
                new ArrayList<>(requirements)
        );
        this.confidence = confidence;
        this.membersOnly = membersOnly;
        this.wilderness = wilderness;
        this.progressionProtected = progressionProtected;
        this.delegatesMethodChoice = delegatesMethodChoice;
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

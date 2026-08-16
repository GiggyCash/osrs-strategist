package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;

public final class TrainingMethod
{
    private final String id;
    private final Skill skill;
    private final int minLevel;
    private final int maxLevel;
    private final String name;
    private final String instructions;
    private final double efficientScore;
    private final double balancedScore;
    private final double relaxedScore;
    private final AttentionLevel attentionLevel;
    private final int minimumSessionMinutes;
    private final int setupMinutes;
    private final List<String> requirements;
    private final RecommendationConfidence confidence;
    private final boolean membersOnly;
    private final boolean wilderness;
    private final boolean progressionProtected;

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
            RecommendationConfidence confidence)
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
            RecommendationConfidence confidence,
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
            RecommendationConfidence confidence,
            boolean membersOnly,
            boolean wilderness,
            boolean progressionProtected)
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
    }

    public String getId() { return id; }
    public Skill getSkill() { return skill; }
    public int getMinLevel() { return minLevel; }
    public int getMaxLevel() { return maxLevel; }
    public String getName() { return name; }
    public String getInstructions() { return instructions; }
    public AttentionLevel getAttentionLevel() { return attentionLevel; }
    public int getMinimumSessionMinutes() { return minimumSessionMinutes; }
    public int getSetupMinutes() { return setupMinutes; }
    public List<String> getRequirements() { return requirements; }
    public RecommendationConfidence getConfidence() { return confidence; }
    public boolean isMembersOnly() { return membersOnly; }
    public boolean isWilderness() { return wilderness; }
    public boolean isProgressionProtected() { return progressionProtected; }

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
                    case ACTIVE: score -= 4.0; break;
                    case MODERATE:
                    default: break;
                }
                break;
            case PICK_FOR_ME:
            default:
                break;
        }

        if (confidence == RecommendationConfidence.CHECK_NEEDED)
        {
            score -= 1.5;
        }
        else if (confidence == RecommendationConfidence.BLOCKED)
        {
            score -= 1000.0;
        }

        return score;
    }
}

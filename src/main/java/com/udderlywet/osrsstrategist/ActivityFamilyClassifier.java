package com.udderlywet.osrsstrategist;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * Maps stable recommendation IDs to broad activity families.
 *
 * <p>The classifier accepts both live {@link Recommendation} objects and the
 * older ID-only history records already stored in RuneLite profile config. That
 * means future family-level learning can be added without invalidating existing
 * player preference/history data.</p>
 */
public final class ActivityFamilyClassifier
{
    public ActivityFamily classify(Recommendation recommendation)
    {
        if (recommendation == null) return ActivityFamily.OTHER;
        TrainingPlan plan = recommendation.getTrainingPlan();
        if (plan != null && plan.getMethod() != null
                && plan.getMethod().getSkill() != null)
        {
            return familyForSkill(plan.getMethod().getSkill());
        }
        return classify(recommendation.getId());
    }

    public ActivityFamily classify(String activityId)
    {
        if (activityId == null) return ActivityFamily.OTHER;
        String id = activityId.toLowerCase(Locale.ROOT);

        if (id.startsWith("skill:"))
        {
            String skillName = id.substring("skill:".length());
            for (Skill skill : Skill.values())
            {
                if (skill.getName().toLowerCase(Locale.ROOT)
                        .replace(" ", "_")
                        .equals(skillName.replace(" ", "_")))
                {
                    return familyForSkill(skill);
                }
            }
        }

        if (startsWithAny(id, "quest:", "quest-")) return ActivityFamily.QUEST;
        if (startsWithAny(id, "pvm:", "boss:", "raid:")) return ActivityFamily.PVM;
        if (startsWithAny(id, "clue:", "opportunity:clue")) return ActivityFamily.CLUE;
        if (startsWithAny(id, "diary:", "achievement-diary:")) return ActivityFamily.DIARY;
        if (startsWithAny(id, "minigame:", "activity:minigame:")) return ActivityFamily.MINIGAME;
        if (startsWithAny(id, "gear:", "upgrade:", "equipment:")) return ActivityFamily.GEAR;
        if (startsWithAny(id, "money:", "moneymaking:", "resource:money")) return ActivityFamily.MONEY;
        if (startsWithAny(id, "collection:", "clog:", "objective:")) return ActivityFamily.COLLECTION;
        if (startsWithAny(id, "farming:", "opportunity:herb", "opportunity:tree",
                "opportunity:farming")) return ActivityFamily.FARMING;
        if (startsWithAny(id, "sailing:", "port-task:")) return ActivityFamily.SAILING;
        return ActivityFamily.OTHER;
    }

    public ActivityFamily familyForSkill(Skill skill)
    {
        if (skill == null) return ActivityFamily.OTHER;
        switch (skill)
        {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case RANGED:
            case MAGIC:
            case HITPOINTS:
            case PRAYER:
            case SLAYER:
                return ActivityFamily.COMBAT;

            case MINING:
            case FISHING:
            case WOODCUTTING:
            case HUNTER:
                return ActivityFamily.GATHERING;

            case SMITHING:
            case COOKING:
            case CRAFTING:
            case FLETCHING:
            case HERBLORE:
            case CONSTRUCTION:
            case FIREMAKING:
                return ActivityFamily.PRODUCTION;

            case FARMING:
                return ActivityFamily.FARMING;
            case SAILING:
                return ActivityFamily.SAILING;

            case AGILITY:
            case THIEVING:
            case RUNECRAFT:
            default:
                return ActivityFamily.UTILITY;
        }
    }

    private static boolean startsWithAny(String value, String... prefixes)
    {
        if (value == null || prefixes == null) return false;
        for (String prefix : prefixes)
        {
            if (prefix != null && value.startsWith(prefix)) return true;
        }
        return false;
    }
}

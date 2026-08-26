package com.udderlywet.osrsstrategist;

/**
 * Goals that are complete enough to expose in RuneLite configuration.
 *
 * <p>Internal planning goals remain in {@link GoalType}; keeping this list
 * separate prevents experimental enum values from silently becoming public
 * controls.</p>
 */
public enum PlayerGoal
{
    AUTOMATIC(GoalType.AUTOMATIC, "Automatic"),
    BARROWS_GLOVES(GoalType.BARROWS_GLOVES, "Barrows gloves"),
    FIRE_CAPE(GoalType.FIRE_CAPE, "Fire cape"),
    QUEST_CAPE(GoalType.QUEST_CAPE, "Quest cape"),
    PRIFDDINAS(GoalType.PRIFDDINAS, "Prifddinas"),
    BOWFA(GoalType.BOWFA, "Bowfa"),
    INFERNAL_CAPE(GoalType.INFERNAL_CAPE, "Infernal cape"),
    MAX(GoalType.MAX, "Max cape");

    private final GoalType planningGoal;
    private final String displayName;

    PlayerGoal(GoalType planningGoal, String displayName)
    {
        this.planningGoal = planningGoal;
        this.displayName = displayName;
    }

    public GoalType toPlanningGoal()
    {
        return planningGoal;
    }

    public static boolean isPlayerFacing(GoalType goal)
    {
        if (goal == null) return false;
        for (PlayerGoal candidate : values())
            if (candidate.planningGoal == goal) return true;
        return false;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}

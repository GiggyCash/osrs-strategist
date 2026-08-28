package com.udderlywet.osrsstrategist;

/** Deterministic Slayer point/streak rules shared by master and task decisions. */
public final class SlayerPointEconomy
{
    public static final int SKIP_COST = 30;

    private SlayerPointEconomy() {}

    public static boolean isBonusCompletion(int completedAfterTask)
    {
        return completedAfterTask > 0 && (completedAfterTask % 1_000 == 0
                || completedAfterTask % 250 == 0
                || completedAfterTask % 100 == 0
                || completedAfterTask % 50 == 0
                || completedAfterTask % 10 == 0);
    }

    public static int pointMultiplier(int completedAfterTask)
    {
        if (completedAfterTask < 5) return 0;
        if (completedAfterTask > 0 && completedAfterTask % 1_000 == 0) return 50;
        if (completedAfterTask > 0 && completedAfterTask % 250 == 0) return 35;
        if (completedAfterTask > 0 && completedAfterTask % 100 == 0) return 25;
        if (completedAfterTask > 0 && completedAfterTask % 50 == 0) return 15;
        if (completedAfterTask > 0 && completedAfterTask % 10 == 0) return 5;
        return 1;
    }

    public static int blockCapacity(int questPoints, boolean lumbridgeElite)
    {
        int ordinary = Math.min(6, Math.max(0, questPoints) / 50);
        return ordinary + (lumbridgeElite ? 1 : 0);
    }

    /** Avoids spending the account's final cancellation on an ordinary dislike. */
    public static boolean hasSustainableSkipBalance(int points)
    {
        return points >= SKIP_COST * 2;
    }
}

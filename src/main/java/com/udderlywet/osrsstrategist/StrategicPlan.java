package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Ordered plan retained across recommendation refreshes for one account/goal. */
public final class StrategicPlan
{
    @Getter
    private final GoalType goal;
    private final long accountHash;
    private final String playerName;
    private final AccountMode accountMode;
    private final MembershipStatus membership;
    @Getter
    private final List<StrategicPlanStep> steps;
    @Getter
    private final int currentIndex;
    @Getter
    private final long createdAtMillis;

    public StrategicPlan(
            GoalType goal,
            AccountSnapshot account,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        this(goal,
                account == null ? 0L : account.getAccountHash(),
                account == null ? "" : account.getPlayerName(),
                account == null ? AccountMode.UNKNOWN
                        : AccountMode.fromTypeCode(account.getAccountTypeCode()),
                account == null ? MembershipStatus.UNKNOWN
                        : account.getMembershipStatus(),
                steps, currentIndex, createdAtMillis);
    }

    private StrategicPlan(
            GoalType goal,
            long accountHash,
            String playerName,
            AccountMode accountMode,
            MembershipStatus membership,
            List<StrategicPlanStep> steps,
            int currentIndex,
            long createdAtMillis)
    {
        if (goal == null || goal == GoalType.AUTOMATIC
                || steps == null || steps.isEmpty())
            throw new IllegalArgumentException(
                    Text.get(791));
        this.goal = goal;
        this.accountHash = accountHash;
        this.playerName = playerName == null ? "" : playerName;
        this.accountMode = accountMode == null
                ? AccountMode.UNKNOWN : accountMode;
        this.membership = membership == null
                ? MembershipStatus.UNKNOWN : membership;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.currentIndex = Math.max(0,
                Math.min(currentIndex, this.steps.size() - 1));
        this.createdAtMillis = Math.max(0L, createdAtMillis);
    }

    public StrategicPlan advanceCompleted(GameData data)
    {
        int next = currentIndex;
        while (next < steps.size() - 1 && steps.get(next).isComplete(data))
            next++;
        if (next == currentIndex) return this;
        return copyAt(next, createdAtMillis);
    }

    StrategicPlan copyAt(int index, long createdAt)
    {
        return new StrategicPlan(goal, accountHash, playerName, accountMode,
                membership, steps, index, createdAt);
    }

    public boolean matchesContext(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null
                || goal != context.getActiveGoal()) return false;
        AccountSnapshot account = context.data().account();
        if (accountHash != 0L && account.getAccountHash() != 0L)
            return accountHash == account.getAccountHash()
                    && accountMode == context.accountMode()
                    && membership == account.getMembershipStatus();
        return playerName != null && playerName.equals(account.getPlayerName())
                && accountMode == context.accountMode()
                && membership == account.getMembershipStatus();
    }

    public StrategicPlanStep getCurrentStep() { return steps.get(currentIndex); }
    public StrategicPlanStep getNextStep()
    {
        return currentIndex + 1 < steps.size()
                ? steps.get(currentIndex + 1) : null;
    }
}

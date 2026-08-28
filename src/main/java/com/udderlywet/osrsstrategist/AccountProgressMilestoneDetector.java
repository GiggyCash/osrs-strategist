package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Detects durable non-XP progress from successive live account snapshots. */
public final class AccountProgressMilestoneDetector
{
    private StrategyDataBundle previous;

    public List<ProgressMilestone> observe(
            StrategyDataBundle current, GoalType goal, long nowMillis)
    {
        if (current == null || current.getAccount() == null)
        {
            previous = null;
            return Collections.emptyList();
        }
        if (previous == null || differentAccount(previous, current))
        {
            previous = current;
            return Collections.emptyList();
        }
        List<ProgressMilestone> result = new ArrayList<>();
        quests(previous.getQuests(), current.getQuests(), goal, nowMillis,
                result);
        transport(previous.getTransport(), current.getTransport(), goal,
                nowMillis, result);
        storage(previous.getStorage(), current.getStorage(), goal, nowMillis,
                result);
        poh(previous.getPoh(), current.getPoh(), goal, nowMillis, result);
        previous = current;
        return result;
    }

    public void clear()
    {
        previous = null;
    }

    private static void quests(QuestSnapshot before, QuestSnapshot after,
            GoalType goal, long now, List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        for (Map.Entry<String, QuestStatus> entry
                : after.getQuests().entrySet())
            if (entry.getValue() == QuestStatus.COMPLETE
                    && before.statusOf(entry.getKey()) != QuestStatus.COMPLETE)
                result.add(milestone("quest:" + slug(entry.getKey()),
                        ProgressMilestoneType.QUEST,
                        entry.getKey() + " complete", goal, now));
    }

    private static void transport(TransportSnapshot before,
            TransportSnapshot after, GoalType goal, long now,
            List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        for (String route : after.getVerifiedRoutes())
            if (!before.hasVerifiedRoute(route))
                result.add(milestone("transport:" + route,
                        ProgressMilestoneType.TRANSPORT,
                        "Transport unlocked: " + display(route), goal, now));
    }

    private static void storage(StorageSnapshot before, StorageSnapshot after,
            GoalType goal, long now, List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        for (StorageCapability capability : StorageCapability.values())
            if (after.stateOf(capability) == CapabilityState.VERIFIED
                    && before.stateOf(capability)
                            != CapabilityState.VERIFIED)
                result.add(milestone("storage:"
                                + capability.name().toLowerCase(),
                        ProgressMilestoneType.INFRASTRUCTURE,
                        "Storage unlocked: " + display(capability.name()),
                        goal, now));
    }

    private static void poh(PohSnapshot before, PohSnapshot after,
            GoalType goal, long now, List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        if (after.getHouseAccess() == CapabilityState.VERIFIED
                && before.getHouseAccess() != CapabilityState.VERIFIED)
            result.add(milestone("infrastructure:poh-access",
                    ProgressMilestoneType.INFRASTRUCTURE,
                    "Player-owned house access verified", goal, now));
        for (Map.Entry<String, CapabilityState> entry
                : after.getFurniture().entrySet())
            if (entry.getValue() == CapabilityState.VERIFIED
                    && before.furnitureState(entry.getKey())
                            != CapabilityState.VERIFIED)
                result.add(milestone("infrastructure:poh:"
                                + entry.getKey(),
                        ProgressMilestoneType.INFRASTRUCTURE,
                        "POH upgrade: " + display(entry.getKey()), goal, now));
    }

    private static ProgressMilestone milestone(String id,
            ProgressMilestoneType type, String title, GoalType goal, long now)
    {
        return new ProgressMilestone(id + ":" + now, type, title,
                "Observed from live account state.",
                goal == null ? null : goal.name(), now);
    }

    private static boolean differentAccount(
            StrategyDataBundle first, StrategyDataBundle second)
    {
        AccountSnapshot left = first.getAccount();
        AccountSnapshot right = second.getAccount();
        if (left.getAccountHash() != 0L && right.getAccountHash() != 0L)
            return left.getAccountHash() != right.getAccountHash();
        return !left.getPlayerName().equals(right.getPlayerName());
    }

    private static String display(String value)
    {
        String clean = value == null ? "Unknown" : value.replace('_', ' ')
                .replace('-', ' ').toLowerCase();
        return clean.isEmpty() ? "Unknown"
                : Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

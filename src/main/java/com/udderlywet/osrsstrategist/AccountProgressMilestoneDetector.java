package com.udderlywet.osrsstrategist;

import java.util.*;

/** Detects durable non-XP progress from successive live account snapshots. */
public final class AccountProgressMilestoneDetector
{
    private GameData previous;

    public List<ProgressMilestone> observe(
            GameData current, GoalType goal, long nowMillis)
    {
        if (current == null || current.account() == null)
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
        quests(previous.quests(), current.quests(), goal, nowMillis,
                result);
        transport(previous.transport(), current.transport(), goal,
                nowMillis, result);
        storage(previous.storage(), current.storage(), goal, nowMillis,
                result);
        poh(previous.poh(), current.poh(), goal, nowMillis, result);
        diaries(previous.diaries(), current.diaries(), goal, nowMillis,
                result);
        slayer(previous.slayer(), current.slayer(), goal, nowMillis,
                result);
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
                : after.quests().entrySet())
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
                        Text.get(1448) + display(route), goal, now));
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
                                + capability.name().toLowerCase(Locale.ROOT),
                        ProgressMilestoneType.INFRASTRUCTURE,
                        Text.get(1449) + display(capability.name()),
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
                    Text.get(1450), goal, now));
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

    private static void diaries(DiarySnapshot before, DiarySnapshot after,
            GoalType goal, long now, List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        for (String region : after.getRegions())
            for (DiaryTier tier : DiaryTier.values())
                if (after.isTierComplete(region, tier)
                        && !before.isTierComplete(region, tier))
                    result.add(milestone("diary:" + slug(region) + ":"
                                    + tier.name().toLowerCase(Locale.ROOT),
                            ProgressMilestoneType.DIARY,
                            region + " " + display(tier.name())
                                    + " diary complete", goal, now));
    }

    private static void slayer(SlayerSnapshot before, SlayerSnapshot after,
            GoalType goal, long now, List<ProgressMilestone> result)
    {
        if (before == null || after == null) return;
        for (SlayerReward reward : SlayerReward.values())
            if (after.getRewards().isUnlocked(reward)
                    && !before.getRewards().isUnlocked(reward))
                result.add(milestone("slayer-unlock:" + reward.getId(),
                        ProgressMilestoneType.SLAYER_UNLOCK,
                        "Slayer unlock: " + reward.getDisplayName(), goal,
                        now));
    }

    private static ProgressMilestone milestone(String id,
            ProgressMilestoneType type, String title, GoalType goal, long now)
    {
        return new ProgressMilestone(id, type, title,
                Text.get(1451),
                goal == null ? null : goal.name(), now);
    }

    private static boolean differentAccount(
            GameData first, GameData second)
    {
        AccountSnapshot left = first.account();
        AccountSnapshot right = second.account();
        if (left.getAccountHash() != 0L && right.getAccountHash() != 0L)
            return left.getAccountHash() != right.getAccountHash();
        return !left.getPlayerName().equals(right.getPlayerName());
    }

    private static String display(String value)
    {
        String clean = value == null ? "Unknown" : value.replace('_', ' ')
                .replace('-', ' ').toLowerCase(Locale.ROOT);
        return clean.isEmpty() ? "Unknown"
                : Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Remembers distinct exact inventory layouts only when multiple live activity
 * families are blocked by their own sourced footprint. Repeated evaluations of
 * one unchanged full inventory therefore cannot manufacture infrastructure
 * value.
 */
@Singleton
public final class UimRecurringPressureService
{
    private static final int MAX_LAYOUTS_PER_ACCOUNT = 8;
    private final Map<String, LinkedHashSet<Integer>> layouts = new HashMap<>();
    private final ActivityStrategyKnowledgeCatalog activityCatalog =
            new ActivityStrategyKnowledgeCatalog();

    public synchronized UimRecurringPressureAssessment observe(
            StrategyContext context)
    {
        List<String> blocked = blockedFamilies(context);
        if (blocked.size() < 2)
            return new UimRecurringPressureAssessment(0, blocked);

        StrategyDataBundle data = context.getData();
        String account = accountKey(data.getAccount());
        LinkedHashSet<Integer> observed = layouts.computeIfAbsent(account,
                ignored -> new LinkedHashSet<>());
        observed.add(fingerprint(data.getInventory()));
        while (observed.size() > MAX_LAYOUTS_PER_ACCOUNT)
            observed.remove(observed.iterator().next());
        return new UimRecurringPressureAssessment(observed.size(), blocked);
    }

    private List<String> blockedFamilies(StrategyContext context)
    {
        List<String> result = new ArrayList<>();
        if (context == null
                || context.getAccountMode() != AccountMode.ULTIMATE_IRONMAN
                || context.getData() == null) return result;
        InventorySnapshot inventory = context.getData().getInventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return result;
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));

        QuestSnapshot quests = context.getData().getQuests();
        if (quests != null && quests.getQuests().values().stream().anyMatch(
                status -> status == QuestStatus.NOT_STARTED
                        || status == QuestStatus.IN_PROGRESS)
                && blocked("quest:observed", free)) result.add("questing");

        ClueSnapshot clue = context.getData().getClue();
        if (clue != null && clue.isCluePresent()
                && blocked("clue:observed", free)) result.add("clues");

        PvmSnapshot pvm = context.getData().getPvm();
        if (pvm != null && !pvm.getReadinessByActivity().isEmpty()
                && blocked("pvm:observed", free)) result.add("pvm");

        MinigameSnapshot minigames = context.getData().getMinigames();
        if (minigames != null)
            for (String id : minigames.getUnlocked())
                if (blocked("minigame:" + id, free))
                {
                    result.add("minigames");
                    break;
                }
        return result;
    }

    private boolean blocked(String candidateId, int free)
    {
        ActivityStrategyProfile profile = activityCatalog.profileFor(
                candidateId, AccountMode.ULTIMATE_IRONMAN);
        return profile != null && profile.getInventoryFootprint() != null
                && profile.getInventoryFootprint()
                        .getMinimumPracticalFreeSlots() > free;
    }

    private static String accountKey(AccountSnapshot account)
    {
        if (account == null) return "unknown-uim";
        if (account.hasStableAccountIdentity())
            return Long.toUnsignedString(account.getAccountHash());
        return account.getPlayerName() + ":" + account.getAccountTypeCode();
    }

    private static int fingerprint(InventorySnapshot inventory)
    {
        int value = 1;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            value = 31 * value + (item == null ? 0 : item.getItemId());
            value = 31 * value + (item == null ? -1 : item.getSlotIndex());
            value = 31 * value + (item == null ? 0 : item.getQuantity());
        }
        return value;
    }
}

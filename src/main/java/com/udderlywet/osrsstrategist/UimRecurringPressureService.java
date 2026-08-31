package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Skill;

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
    private final MethodStrategyKnowledgeCatalog methodCatalog =
            new MethodStrategyKnowledgeCatalog();
    private final List<CuratedTrainingMethod> skillingMethods =
            skillingMethods();

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

        if (blockedSkilling(context.getData().getAccount(), free))
            result.add("skilling");

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

    private boolean blockedSkilling(AccountSnapshot account, int free)
    {
        if (account == null) return false;
        for (CuratedTrainingMethod candidate : skillingMethods)
        {
            TrainingMethod method = candidate.getMethod();
            TrainingMethodMetadata metadata = candidate.getMetadata();
            if (method == null || metadata == null
                    || !metadata.isUimFriendly()
                    || !method.supportsLevel(account.getSkillLevel(
                            method.getSkill()))
                    || method.getConfidence()
                            != RecommendationConfidence.VERIFIED
                    || !method.getRequirements().isEmpty()
                    || !AccountBuildPolicy.allowsMethod(account, method)
                    || !ContentAccessRules.isMethodAvailable(method,
                            account.getMembershipStatus())) continue;
            MethodStrategyProfile profile = methodCatalog.profileFor(method,
                    metadata, AccountMode.ULTIMATE_IRONMAN);
            if (profile != null && profile.getInventoryFootprint() != null
                    && profile.getInventoryFootprint()
                            .getMinimumPracticalFreeSlots() > free)
                return true;
        }
        return false;
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

    private static List<CuratedTrainingMethod> skillingMethods()
    {
        List<CuratedTrainingMethod> result = new ArrayList<>();
        ExpandedTrainingMethodCatalog expanded =
                new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog f2p = new F2pBaselineMethodCatalog();
        for (Skill skill : Skill.values())
        {
            result.addAll(expanded.methodsFor(skill));
            result.addAll(f2p.methodsFor(skill));
        }
        return java.util.Collections.unmodifiableList(result);
    }
}

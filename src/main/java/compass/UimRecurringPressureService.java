package compass;

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
        var blocked = blockedFamilies(context);
        if (blocked.size() < 2)
            return new UimRecurringPressureAssessment(0, blocked);

        var data = context.data();
        var account = accountKey(data.account());
        LinkedHashSet<Integer> observed = layouts.computeIfAbsent(account,
                ignored -> new LinkedHashSet<>());
        observed.add(fingerprint(data.inventory()));
        while (observed.size() > MAX_LAYOUTS_PER_ACCOUNT)
            observed.remove(observed.iterator().next());
        return new UimRecurringPressureAssessment(observed.size(), blocked);
    }

    private List<String> blockedFamilies(StrategyContext context)
    {
        List<String> result = new ArrayList<>();
        if (context == null
                || context.accountMode() != AccountMode.ULTIMATE_IRONMAN
                || context.data() == null) return result;
        var inventory = context.data().inventory();
        if (inventory == null || !inventory.hasCompleteSlotObservation())
            return result;
        int free = Math.max(0, 28
                - UimSetupCostService.occupiedInventorySlots(inventory));

        if (blockedSkilling(context.data().account(), free))
            result.add("skilling");

        var quests = context.data().quests();
        if (quests != null && quests.quests().values().stream().anyMatch(
                status -> status == QuestStatus.NOT_STARTED
                        || status == QuestStatus.IN_PROGRESS)
                && blocked("quest:observed", free)) result.add("questing");

        var clue = context.data().clue();
        if (clue != null && clue.isCluePresent()
                && blocked("clue:observed", free)) result.add("clues");

        var pvm = context.data().pvm();
        if (pvm != null && !pvm.getReadinessByActivity().isEmpty()
                && blocked("pvm:observed", free)) result.add("pvm");

        var minigames = context.data().minigames();
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
            var method = candidate.method();
            var metadata = candidate.getMetadata();
            if (method == null || metadata == null
                    || !metadata.isUimFriendly()
                    || !method.supportsLevel(account.level(
                            method.getSkill()))
                    || method.getConfidence()
                            != Confidence.VERIFIED
                    || !method.getRequirements().isEmpty()
                    || !AccountBuildPolicy.allowsMethod(account, method)
                    || !ContentAccessRules.isMethodAvailable(method,
                            account.membership())) continue;
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
        return account.getPlayerName() + ":" + account.modeCode();
    }

    private static int fingerprint(ItemsState inventory)
    {
        var value = 1;
        for (ItemState item : inventory.getItems())
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
        var f2p = new F2pBaselineMethodCatalog();
        for (Skill skill : Skill.values())
        {
            result.addAll(expanded.methodsFor(skill));
            result.addAll(f2p.methodsFor(skill));
        }
        return java.util.Collections.unmodifiableList(result);
    }
}

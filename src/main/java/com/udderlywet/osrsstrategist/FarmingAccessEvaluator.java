package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves Farming access from the strongest evidence available.
 *
 * <p>Direct observation wins. Otherwise, open-world patches are inferred for a
 * member account and quest-gated patches are inferred only when the required
 * quest is confirmed complete. This is the pattern other content systems can
 * follow later: live state first, remembered proof second, safe inference third.</p>
 */
@Singleton
public class FarmingAccessEvaluator
{
    private final FarmingAccessCatalog catalog;

    @Inject
    public FarmingAccessEvaluator(FarmingAccessCatalog catalog)
    {
        this.catalog = catalog;
    }

    public FarmingSnapshot evaluate(
            AccountSnapshot account,
            QuestSnapshot quests,
            AccessMemorySnapshot memory,
            FarmingSnapshot existing)
    {
        Set<String> reachable = new HashSet<>();
        Map<String, CapabilityState> tools = new HashMap<>();
        Map<String, Long> readyAt = new HashMap<>();

        if (existing != null)
        {
            reachable.addAll(existing.getReachablePatchIds());
            tools.putAll(existing.getLeprechaunTools());
            readyAt.putAll(existing.getPatchReadyAtMillis());
        }

        if (account == null
                || account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return new FarmingSnapshot(reachable, tools, readyAt);
        }

        AccessMemorySnapshot safeMemory = memory == null
                ? AccessMemorySnapshot.empty()
                : memory;

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (safeMemory.hasObserved(definition.observationKey()))
            {
                reachable.add(definition.getId());
                continue;
            }

            String requiredQuest = definition.getRequiredQuest();
            if (requiredQuest == null)
            {
                reachable.add(definition.getId());
                continue;
            }

            if (quests != null
                    && quests.statusOf(requiredQuest) == QuestStatus.COMPLETE)
            {
                reachable.add(definition.getId());
            }
        }

        return new FarmingSnapshot(reachable, tools, readyAt);
    }

    public String firstReachablePatchName(FarmingSnapshot farming)
    {
        if (farming == null)
        {
            return null;
        }

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (farming.isPatchReachable(definition.getId()))
            {
                return definition.getDisplayName();
            }
        }
        return null;
    }

    public String firstReachableHerbPatchName(FarmingSnapshot farming)
    {
        if (farming == null)
        {
            return null;
        }

        for (FarmingAccessDefinition definition : catalog.all())
        {
            if (definition.isHerbPatch()
                    && farming.isPatchReachable(definition.getId()))
            {
                return definition.getDisplayName();
            }
        }
        return null;
    }
}

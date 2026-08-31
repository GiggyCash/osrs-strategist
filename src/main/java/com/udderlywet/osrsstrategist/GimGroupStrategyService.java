package com.udderlywet.osrsstrategist;

import java.util.Set;
import javax.inject.Singleton;

/**
 * GIM strategy grounded only in enabled, fresh Group Storage item evidence.
 * It never infers teammate levels, roles, POH rooms, or other capabilities.
 */
@Singleton
public final class GimGroupStrategyService
{
    public GroupResourceAssessment assess(
            StrategyContext context, GroupResourceNeed need)
    {
        if (need == null)
            throw new IllegalArgumentException("Group resource need is required");
        AccountMode mode = context == null
                ? AccountMode.UNKNOWN : context.getAccountMode();
        if (!mode.isGroupIronman())
            return result(GroupResourceState.NOT_A_GROUP_ACCOUNT,
                    RecommendationConfidence.VERIFIED, 0, need, 0.0,
                    PlayerText.get("GGSS1"));
        if (!context.isUseGroupStorage())
            return result(GroupResourceState.GROUP_STORAGE_DISABLED,
                    RecommendationConfidence.VERIFIED, 0, need, 0.0,
                    PlayerText.get("GGSS2"));
        StrategyDataBundle data = context.getData();
        GroupStorageSnapshot storage = data == null
                ? null : data.getGroupStorage();
        if (storage == null || !storage.isObserved())
            return result(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                    RecommendationConfidence.CHECK_NEEDED, 0, need, 0.0,
                    PlayerText.get("GGSS3"));

        int quantity = quantity(storage, need.getAcceptableItemIds());
        if (quantity <= 0)
            return result(GroupResourceState.SHARED_STOCK_NONE,
                    RecommendationConfidence.VERIFIED, 0, need, 0.0,
                    PlayerText.get("GGSS4"));
        double fraction = Math.min(1.0, quantity
                / (double) need.getQuantity());
        if (quantity < need.getQuantity())
            return result(GroupResourceState.SHARED_STOCK_PARTIAL,
                    RecommendationConfidence.VERIFIED, quantity, need,
                    fraction * 0.45,
                    PlayerText.get("GGSS5"));
        double avoidance = need.isReusable() ? 1.0 : 0.75;
        return result(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                RecommendationConfidence.VERIFIED, quantity, need, avoidance,
                PlayerText.get("GGSS6"));
    }

    public SharedInfrastructureAssessment assessTeammateInfrastructure(
            StrategyContext context)
    {
        if (context == null || !context.getAccountMode().isGroupIronman())
            return new SharedInfrastructureAssessment(CapabilityState.BLOCKED,
                    RecommendationConfidence.VERIFIED,
                    PlayerText.get("GGSS7"));
        return new SharedInfrastructureAssessment(CapabilityState.UNKNOWN,
                RecommendationConfidence.CHECK_NEEDED,
                PlayerText.get("GGSS8"));
    }

    private static GroupResourceAssessment result(GroupResourceState state,
            RecommendationConfidence confidence, int quantity,
            GroupResourceNeed need, double avoidance, String reason)
    {
        return new GroupResourceAssessment(state, confidence, quantity,
                need.getQuantity(), avoidance, reason);
    }

    private static int quantity(GroupStorageSnapshot storage, Set<Integer> ids)
    {
        int total = 0;
        for (ItemStackSnapshot item : storage.getItems())
        {
            if (item == null || !ids.contains(item.getItemId())) continue;
            int amount = Math.max(0, item.getQuantity());
            if (total >= Integer.MAX_VALUE - amount) return Integer.MAX_VALUE;
            total += amount;
        }
        return total;
    }
}

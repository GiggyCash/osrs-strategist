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
                    "This account has no Group Storage capability.");
        if (!context.isUseGroupStorage())
            return result(GroupResourceState.GROUP_STORAGE_DISABLED,
                    RecommendationConfidence.VERIFIED, 0, need, 0.0,
                    "Use Group Storage is disabled, so shared stock is not counted.");
        StrategyDataBundle data = context.getData();
        GroupStorageSnapshot storage = data == null
                ? null : data.getGroupStorage();
        if (storage == null || !storage.isObserved())
            return result(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                    RecommendationConfidence.CHECK_NEEDED, 0, need, 0.0,
                    "No fresh Group Storage observation proves shared stock.");

        int quantity = quantity(storage, need.getAcceptableItemIds());
        if (quantity <= 0)
            return result(GroupResourceState.SHARED_STOCK_NONE,
                    RecommendationConfidence.VERIFIED, 0, need, 0.0,
                    "The latest Group Storage observation contains none of the required item.");
        double fraction = Math.min(1.0, quantity
                / (double) need.getQuantity());
        if (quantity < need.getQuantity())
            return result(GroupResourceState.SHARED_STOCK_PARTIAL,
                    RecommendationConfidence.VERIFIED, quantity, need,
                    fraction * 0.45,
                    "Fresh shared stock covers part of the observed requirement.");
        double avoidance = need.isReusable() ? 1.0 : 0.75;
        return result(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                RecommendationConfidence.VERIFIED, quantity, need, avoidance,
                "Fresh shared stock satisfies this requirement, so acquiring another copy now would duplicate group work.");
    }

    public SharedInfrastructureAssessment assessTeammateInfrastructure(
            StrategyContext context)
    {
        if (context == null || !context.getAccountMode().isGroupIronman())
            return new SharedInfrastructureAssessment(CapabilityState.BLOCKED,
                    RecommendationConfidence.VERIFIED,
                    "This account has no group infrastructure capability.");
        return new SharedInfrastructureAssessment(CapabilityState.UNKNOWN,
                RecommendationConfidence.CHECK_NEEDED,
                "RuneLite does not expose reliable teammate POH, unlock, or specialization state; Compass does not infer it from Group Storage.");
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

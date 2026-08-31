package compass;
import static compass.Text.get;

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
            throw new IllegalArgumentException(get(1429));
        AccountMode mode = context == null
                ? AccountMode.UNKNOWN : context.accountMode();
        if (!mode.isGroupIronman())
            return result(GroupResourceState.NOT_A_GROUP_ACCOUNT,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(287));
        if (!context.usesGroupStorage())
            return result(GroupResourceState.GROUP_STORAGE_DISABLED,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(288));
        var data = context.data();
        ItemsState storage = data == null
                ? null : data.groupStorage();
        if (storage == null || !storage.isObserved())
            return result(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                    Confidence.CHECK_NEEDED, 0, need, 0.0,
                    get(289));

        var quantity = quantity(storage, need.getAcceptableItemIds());
        if (quantity <= 0)
            return result(GroupResourceState.SHARED_STOCK_NONE,
                    Confidence.VERIFIED, 0, need, 0.0,
                    get(290));
        double fraction = Math.min(1.0, quantity
                / (double) need.getQuantity());
        if (quantity < need.getQuantity())
            return result(GroupResourceState.SHARED_STOCK_PARTIAL,
                    Confidence.VERIFIED, quantity, need,
                    fraction * 0.45,
                    get(291));
        var avoidance = need.isReusable() ? 1.0 : 0.75;
        return result(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                Confidence.VERIFIED, quantity, need, avoidance,
                get(292));
    }

    public SharedInfrastructureAssessment assessTeammateInfrastructure(
            StrategyContext context)
    {
        if (context == null || !context.accountMode().isGroupIronman())
            return new SharedInfrastructureAssessment(CapabilityState.BLOCKED,
                    Confidence.VERIFIED,
                    get(293));
        return new SharedInfrastructureAssessment(CapabilityState.UNKNOWN,
                Confidence.CHECK_NEEDED,
                get(294));
    }

    private static GroupResourceAssessment result(GroupResourceState state,
            Confidence confidence, int quantity,
            GroupResourceNeed need, double avoidance, String reason)
    {
        return new GroupResourceAssessment(state, confidence, quantity,
                need.getQuantity(), avoidance, reason);
    }

    private static int quantity(ItemsState storage, Set<Integer> ids)
    {
        var total = 0;
        for (ItemState item : storage.getItems())
        {
            if (item == null || !ids.contains(item.getItemId())) continue;
            var amount = Math.max(0, item.getQuantity());
            if (total >= Integer.MAX_VALUE - amount) return Integer.MAX_VALUE;
            total += amount;
        }
        return total;
    }
}

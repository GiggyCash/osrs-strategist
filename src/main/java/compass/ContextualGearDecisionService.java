package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Singleton;

/** Produces seven evidence-separated gear answers for one encounter context. */
@Singleton
public final class ContextualGearDecisionService
{
    private final GearAcquisitionCatalog acquisition =
            new GearAcquisitionCatalog();

    public ContextualGearAssessment assess(GearProgressionEntry entry,
            StrategyContext context)
    {
        Map<GearDecisionKind, ContextualGearDecision> result =
                new EnumMap<>(GearDecisionKind.class);
        ItemIndex items = new ItemIndex(
                context == null ? null : context.data(),
                context != null && context.usesGroupStorage());
        var ownershipObserved = items.usableOwnershipObserved();
        List<String> owned = new ArrayList<>();
        List<String> unresolvedRoutes = new ArrayList<>();
        for (String target : entry.getRecommendedItems())
        {
            if (!isExactOwnershipTarget(target)) continue;
            if (items.has(target)) owned.add(target);
            else if (acquisition.forItem(target) != null)
                unresolvedRoutes.add(target);
        }
        String ownedValue = !ownershipObserved
                ? get(142)
                : owned.isEmpty()
                ? get(143)
                : owned.get(0);
        put(result, GearDecisionKind.BEST_OWNED, ownedValue,
                ownershipObserved && !owned.isEmpty()
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_USABLE,
                owned.isEmpty()
                        ? get(144)
                        : owned.get(0) + get(145),
                Confidence.CHECK_NEEDED);

        String routed = unresolvedRoutes.isEmpty() ? null
                : unresolvedRoutes.get(0);
        AccountMode mode = context == null ? AccountMode.UNKNOWN
                : context.accountMode();
        String available = routed == null
                ? get(146)
                : mode.usesGrandExchange()
                ? get(147) + routed
                : get(148) + routed;
        put(result, GearDecisionKind.BEST_AVAILABLE_NOW, available,
                Confidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_VALUE_UPGRADE,
                get(149),
                Confidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_PRACTICAL_UPGRADE,
                routed == null ? entry.getWeaponGuidance()
                        : routed + get(150),
                Confidence.CHECK_NEEDED);
        put(result, GearDecisionKind.LONG_TERM_TARGET,
                entry.getWeaponGuidance(), Confidence.CHECK_NEEDED);
        put(result, GearDecisionKind.TARGET_SPECIFIC_BEST,
                entry.getNote(), Confidence.CHECK_NEEDED);
        return new ContextualGearAssessment(result);
    }

    /** Compound slot prose must never be treated as proof that one exact item is missing. */
    static boolean isExactOwnershipTarget(String target)
    {
        if (target == null || target.trim().isEmpty()) return false;
        var value = target.toLowerCase(Locale.ROOT);
        return !value.contains(" or ") && !value.contains("/")
                && !value.contains("depending") && !value.contains("target-")
                && !value.contains(" mix") && !value.contains(" pieces")
                && !value.contains(" switch") && !value.contains(" as ")
                && !value.contains(" progression") && !value.contains("applicable");
    }

    private static void put(
            Map<GearDecisionKind, ContextualGearDecision> decisions,
            GearDecisionKind kind, String value,
            Confidence confidence)
    {
        decisions.put(kind, new ContextualGearDecision(kind, value, confidence));
    }
}

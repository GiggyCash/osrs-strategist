package com.udderlywet.osrsstrategist;

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
        ObservedItemIndex items = new ObservedItemIndex(
                context == null ? null : context.getData(),
                context != null && context.isUseGroupStorage());
        boolean ownershipObserved = items.usableOwnershipObserved();
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
                ? PlayerText.get("CGDS1")
                : owned.isEmpty()
                ? PlayerText.get("CGDS2")
                : owned.get(0);
        put(result, GearDecisionKind.BEST_OWNED, ownedValue,
                ownershipObserved && !owned.isEmpty()
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_USABLE,
                owned.isEmpty()
                        ? PlayerText.get("CGDS3")
                        : owned.get(0) + PlayerText.get("CGDS4"),
                RecommendationConfidence.CHECK_NEEDED);

        String routed = unresolvedRoutes.isEmpty() ? null
                : unresolvedRoutes.get(0);
        AccountMode mode = context == null ? AccountMode.UNKNOWN
                : context.getAccountMode();
        String available = routed == null
                ? PlayerText.get("CGDS5")
                : mode.usesGrandExchange()
                ? PlayerText.get("CGDS6") + routed
                : PlayerText.get("CGDS7") + routed;
        put(result, GearDecisionKind.BEST_AVAILABLE_NOW, available,
                RecommendationConfidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_VALUE_UPGRADE,
                PlayerText.get("CGDS8"),
                RecommendationConfidence.CHECK_NEEDED);
        put(result, GearDecisionKind.BEST_PRACTICAL_UPGRADE,
                routed == null ? entry.getWeaponGuidance()
                        : routed + PlayerText.get("CGDS9"),
                RecommendationConfidence.CHECK_NEEDED);
        put(result, GearDecisionKind.LONG_TERM_TARGET,
                entry.getWeaponGuidance(), RecommendationConfidence.CHECK_NEEDED);
        put(result, GearDecisionKind.TARGET_SPECIFIC_BEST,
                entry.getNote(), RecommendationConfidence.CHECK_NEEDED);
        return new ContextualGearAssessment(result);
    }

    /** Compound slot prose must never be treated as proof that one exact item is missing. */
    static boolean isExactOwnershipTarget(String target)
    {
        if (target == null || target.trim().isEmpty()) return false;
        String value = target.toLowerCase(Locale.ROOT);
        return !value.contains(" or ") && !value.contains("/")
                && !value.contains("depending") && !value.contains("target-")
                && !value.contains(" mix") && !value.contains(" pieces")
                && !value.contains(" switch") && !value.contains(" as ")
                && !value.contains(" progression") && !value.contains("applicable");
    }

    private static void put(
            Map<GearDecisionKind, ContextualGearDecision> decisions,
            GearDecisionKind kind, String value,
            RecommendationConfidence confidence)
    {
        decisions.put(kind, new ContextualGearDecision(kind, value, confidence));
    }
}

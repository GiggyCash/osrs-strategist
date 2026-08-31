package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Measures setup reuse from observed items, region and spellbook evidence. */
@Singleton
public final class SetupReuseService
{
    public SetupReuseAssessment assess(ActivitySetupProfile profile,
            StrategyDataBundle data, CurrentSetupEvidence live)
    {
        if (profile == null)
            return new SetupReuseAssessment(
                    RecommendationConfidence.CHECK_NEEDED, 0, 0, 0, 0.0,
                    java.util.Collections.emptyList());
        CurrentSetupEvidence evidence = live == null
                ? CurrentSetupEvidence.unknown() : live;
        int required = 0;
        int matched = 0;
        boolean unknown = false;
        List<String> reasons = new ArrayList<>();

        Set<Integer> equipped = itemIds(data == null
                || data.getEquipment() == null ? null
                : data.getEquipment().getEquippedItems());
        for (int itemId : profile.getEquippedItemIds())
        {
            required++;
            if (equipped == null) unknown = true;
            else if (equipped.contains(itemId))
            {
                matched++;
                reasons.add("equipped:" + itemId);
            }
        }

        Set<Integer> inventory = itemIds(data == null
                || data.getInventory() == null ? null
                : data.getInventory().getItems());
        for (int itemId : profile.getInventoryItemIds())
        {
            required++;
            if (inventory == null) unknown = true;
            else if (inventory.contains(itemId))
            {
                matched++;
                reasons.add("inventory:" + itemId);
            }
        }

        if (profile.getRegionId() != null)
        {
            required++;
            if (!evidence.hasRegion()) unknown = true;
            else if (profile.getRegionId().equals(evidence.getRegionId()))
            {
                matched++;
                reasons.add("region:" + profile.getRegionId());
            }
        }
        if (profile.getSpellbookId() != null)
        {
            required++;
            if (!evidence.hasSpellbook()) unknown = true;
            else if (profile.getSpellbookId().equals(evidence.getSpellbookId()))
            {
                matched++;
                reasons.add("spellbook:" + profile.getSpellbookId());
            }
        }

        double fraction = required == 0 ? 0.0 : matched / (double) required;
        int minutes = (int) Math.floor(profile.getSetupMinutes() * fraction);
        return new SetupReuseAssessment(unknown
                ? RecommendationConfidence.CHECK_NEEDED
                : RecommendationConfidence.VERIFIED,
                matched, required, minutes, fraction, reasons);
    }

    private static Set<Integer> itemIds(List<ItemStackSnapshot> items)
    {
        if (items == null) return null;
        Set<Integer> result = new HashSet<>();
        for (ItemStackSnapshot item : items)
            if (item != null && item.getItemId() > 0
                    && item.getQuantity() > 0) result.add(item.getItemId());
        return result;
    }
}

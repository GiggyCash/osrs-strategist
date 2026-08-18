package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Account-mode-aware sourcing planner for a required item. */
@Singleton
public class ResourceAcquisitionPlanner
{
    private final ResourceSourceCatalog sourceCatalog;

    @Inject
    public ResourceAcquisitionPlanner(ResourceSourceCatalog sourceCatalog)
    {
        this.sourceCatalog = sourceCatalog;
    }

    /** Compatibility constructor for existing focused tests. */
    public ResourceAcquisitionPlanner()
    {
        this(new ResourceSourceCatalog());
    }

    public ResourceAcquisitionPlan plan(
            StrategyContext context,
            ResourceNeed need)
    {
        if (context == null || need == null || context.getData() == null)
        {
            return checkNeeded(need, "Account state is not available.");
        }

        StrategyDataBundle data = context.getData();
        AccountMode mode = context.getAccountMode();
        int inventoryQuantity = quantityIn(data.getInventory(), need.getItemId());

        if (inventoryQuantity >= need.getQuantity())
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.INVENTORY, inventoryQuantity,
                    RecommendationConfidence.VERIFIED,
                    "Required quantity is confirmed in inventory."
            );
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            StoredResource stored = findVerifiedStoredResource(
                    data.getStorage(), need.getItemId(), need.getQuantity());
            if (stored != null)
            {
                boolean needsAccessCheck = requiresAdditionalAccessCheck(
                        stored.capability);
                return new ResourceAcquisitionPlan(
                        need,
                        AcquisitionSource.VERIFIED_STORAGE,
                        stored.quantity,
                        needsAccessCheck
                                ? RecommendationConfidence.CHECK_NEEDED
                                : RecommendationConfidence.VERIFIED,
                        needsAccessCheck
                                ? "Required quantity is observed in "
                                        + pretty(stored.capability)
                                        + ", but retrieval needs an explicit UIM access/risk/precondition check."
                                : "Required quantity is confirmed in observed "
                                        + pretty(stored.capability) + "."
                );
            }
        }
        else
        {
            int bankQuantity = quantityIn(data.getBank(), need.getItemId());
            if (bankQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need, AcquisitionSource.BANK, bankQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in the latest observed bank snapshot."
                );
            }
        }

        if (AccountModePolicy.mayUseGroupStorage(
                mode, context.isUseGroupStorage()))
        {
            GroupStorageSnapshot groupStorage = data.getGroupStorage();
            int groupQuantity = quantityIn(groupStorage, need.getItemId());
            if (groupStorage != null
                    && groupStorage.isObserved()
                    && groupQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need, AcquisitionSource.GROUP_STORAGE, groupQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed in observed Group Storage."
                );
            }
        }

        String sourceNote = sourceSuggestions(
                need, mode, context.isAllowWildernessMethods());

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.GRAND_EXCHANGE, inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    "GE is an option, but price, available GP, and opportunity cost must be verified before recommending a purchase."
                            + sourceNote
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.SELF_SOURCE, inventoryQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    (mode == AccountMode.ULTIMATE_IRONMAN
                            ? "No sufficient directly usable UIM inventory/storage source is known."
                            : "No sufficient owned source is confirmed yet.")
                            + sourceNote
            );
        }

        return checkNeeded(need,
                "A verified acquisition route is not available yet." + sourceNote);
    }

    /** Builds an ordered chain without pretending prose source hints are verified unlocks. */
    public ResourceAcquisitionChain planChain(StrategyContext context,
            ResourceNeed need)
    {
        ResourceAcquisitionPlan ownership = plan(context, need);
        List<ResourceAcquisitionStep> steps = new ArrayList<>();
        int shortfall = ownership == null || need == null ? 0
                : Math.max(0, need.getQuantity() - ownership.getConfirmedQuantity());
        if (ownership == null || need == null)
            return new ResourceAcquisitionChain(need, shortfall, steps);

        if (ownership.hasEnoughConfirmed())
        {
            steps.add(new ResourceAcquisitionStep(ownership.getSource(),
                    ownership.getNote(), RecommendationConfidence.VERIFIED));
            return new ResourceAcquisitionChain(need, 0, steps);
        }

        steps.add(new ResourceAcquisitionStep(ownership.getSource(),
                ownership.getNote(), ownership.getConfidence()));
        if (sourceCatalog != null && context != null)
        {
            List<String> routes = sourceCatalog.suggestions(need.getItemName(),
                    context.getAccountMode(), context.isAllowWildernessMethods());
            for (String route : routes)
                steps.add(new ResourceAcquisitionStep(
                        context.getAccountMode().usesGrandExchange()
                                ? AcquisitionSource.GRAND_EXCHANGE
                                : AcquisitionSource.SELF_SOURCE,
                        route, RecommendationConfidence.CHECK_NEEDED));
        }
        return new ResourceAcquisitionChain(need, shortfall, steps);
    }

    private String sourceSuggestions(
            ResourceNeed need,
            AccountMode mode,
            boolean allowWilderness)
    {
        if (sourceCatalog == null || need == null) return "";
        List<String> suggestions = sourceCatalog.suggestions(
                need.getItemName(), mode, allowWilderness);
        if (suggestions.isEmpty())
        {
            return " No verified item-specific gathering, shop, crafting, minigame, or drop source is currently available for this resource.";
        }

        StringBuilder note = new StringBuilder(" Candidate route");
        if (suggestions.size() > 1) note.append("s");
        note.append(": ");
        for (int i = 0; i < suggestions.size(); i++)
        {
            if (i > 0) note.append(" | ");
            note.append(suggestions.get(i));
        }
        return note.toString();
    }

    private static StoredResource findVerifiedStoredResource(
            StorageSnapshot storage,
            int itemId,
            int needed)
    {
        if (storage == null) return null;
        StoredResource restrictedFallback = null;
        for (Map.Entry<StorageCapability, java.util.List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            int quantity = 0;
            for (ItemStackSnapshot item : entry.getValue())
            {
                if (item.getItemId() == itemId) quantity += item.getQuantity();
            }
            if (quantity < needed) continue;

            StoredResource candidate = new StoredResource(capability, quantity);
            if (!requiresAdditionalAccessCheck(capability)) return candidate;
            if (restrictedFallback == null) restrictedFallback = candidate;
        }
        return restrictedFallback;
    }

    private static boolean requiresAdditionalAccessCheck(
            StorageCapability capability)
    {
        return capability == StorageCapability.LOOTING_BAG
                || capability == StorageCapability.DEATH_STORAGE
                || capability == StorageCapability.DEATHPILE;
    }

    private static ResourceAcquisitionPlan checkNeeded(
            ResourceNeed need,
            String note)
    {
        return new ResourceAcquisitionPlan(
                need, AcquisitionSource.CHECK_NEEDED, 0,
                RecommendationConfidence.CHECK_NEEDED, note
        );
    }

    private static int quantityIn(InventorySnapshot inventory, int itemId)
    {
        return inventory == null ? 0 : quantityInItems(inventory.getItems(), itemId);
    }

    private static int quantityIn(BankSnapshot bank, int itemId)
    {
        return bank == null ? 0 : quantityInItems(bank.getItems(), itemId);
    }

    private static int quantityIn(GroupStorageSnapshot storage, int itemId)
    {
        if (storage == null || !storage.isObserved()) return 0;
        return quantityInItems(storage.getItems(), itemId);
    }

    private static int quantityInItems(Iterable<ItemStackSnapshot> items, int itemId)
    {
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item.getItemId() == itemId) total += item.getQuantity();
        }
        return total;
    }

    private static String pretty(StorageCapability capability)
    {
        return capability.name().toLowerCase().replace('_', ' ');
    }

    private static final class StoredResource
    {
        private final StorageCapability capability;
        private final int quantity;

        private StoredResource(StorageCapability capability, int quantity)
        {
            this.capability = capability;
            this.quantity = quantity;
        }
    }
}

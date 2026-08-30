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
    private final ResourceDependencyCatalog dependencyCatalog;

    @Inject
    public ResourceAcquisitionPlanner(ResourceSourceCatalog sourceCatalog,
            ResourceDependencyCatalog dependencyCatalog)
    {
        this.sourceCatalog = sourceCatalog;
        this.dependencyCatalog = dependencyCatalog;
    }

    public ResourceAcquisitionPlanner(ResourceSourceCatalog sourceCatalog)
    {
        this(sourceCatalog, new ResourceDependencyCatalog());
    }

    /** Compatibility constructor for existing focused tests. */
    public ResourceAcquisitionPlanner()
    {
        this(new ResourceSourceCatalog(), new ResourceDependencyCatalog());
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
        int confirmedQuantity = inventoryQuantity;

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
            int remaining = Math.max(0, need.getQuantity() - inventoryQuantity);
            StoredResource stored = findVerifiedStoredResource(
                    data.getStorage(), need.getItemId(), remaining);
            if (stored != null)
            {
                boolean needsAccessCheck = stored.requiresAccessCheck();
                confirmedQuantity = safeAdd(inventoryQuantity, stored.quantity);
                return new ResourceAcquisitionPlan(
                        need,
                        AcquisitionSource.VERIFIED_STORAGE,
                        confirmedQuantity,
                        needsAccessCheck
                                ? RecommendationConfidence.CHECK_NEEDED
                                : RecommendationConfidence.VERIFIED,
                        needsAccessCheck
                                ? "Required quantity is observed across inventory and "
                                        + pretty(stored.capabilities)
                                        + ", but retrieval needs an explicit UIM access/risk/precondition check."
                                : "Required quantity is confirmed across inventory and observed "
                                        + pretty(stored.capabilities) + "."
                );
            }
        }
        else
        {
            int bankQuantity = quantityIn(data.getBank(), need.getItemId());
            int ordinaryQuantity = safeAdd(inventoryQuantity, bankQuantity);
            confirmedQuantity = ordinaryQuantity;
            if (ordinaryQuantity >= need.getQuantity())
            {
                return new ResourceAcquisitionPlan(
                        need, AcquisitionSource.BANK, ordinaryQuantity,
                        RecommendationConfidence.VERIFIED,
                        "Required quantity is confirmed across observed inventory and bank state."
                );
            }

            if (AccountModePolicy.mayUseGroupStorage(
                    mode, context.isUseGroupStorage()))
            {
                GroupStorageSnapshot groupStorage = data.getGroupStorage();
                int groupQuantity = quantityIn(groupStorage, need.getItemId());
                if (groupStorage != null && groupStorage.isObserved())
                {
                    confirmedQuantity = safeAdd(ordinaryQuantity, groupQuantity);
                    if (confirmedQuantity >= need.getQuantity())
                    {
                        return new ResourceAcquisitionPlan(
                                need, AcquisitionSource.GROUP_STORAGE,
                                confirmedQuantity,
                                RecommendationConfidence.VERIFIED,
                                "Required quantity is confirmed across observed inventory, bank, and Group Storage state."
                        );
                    }
                }
            }
        }

        String sourceNote = sourceSuggestions(
                need, mode, context.isAllowWildernessMethods());

        // Do not turn an unobserved container into a proven shortfall. An
        // inventory read is required for every mode; ordinary accounts also
        // require the bank, and opted-in GIM requires fresh Group Storage.
        if (data.getInventory() == null)
            return checkNeeded(need,
                    "Open the inventory tab so carried resources can be observed before choosing an acquisition route.");
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.getBank() == null)
            return checkNeeded(need,
                    "Open the bank once so stored resources can be observed before choosing an acquisition route.");
        if (AccountModePolicy.mayUseGroupStorage(mode,
                context.isUseGroupStorage())
                && (data.getGroupStorage() == null
                || !data.getGroupStorage().isObserved()))
            return checkNeeded(need,
                    "Group Storage is enabled but unobserved; inspect it before treating the resource as missing.");

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.GRAND_EXCHANGE, confirmedQuantity,
                    RecommendationConfidence.CHECK_NEEDED,
                    "GE is an option, but price, available GP, and opportunity cost must be verified before recommending a purchase."
                            + sourceNote
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new ResourceAcquisitionPlan(
                    need, AcquisitionSource.SELF_SOURCE, confirmedQuantity,
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

    /**
     * Plans a shortfall already proven by another evidence-aware evaluator.
     * The root quantity is the missing quantity, so owned state must not be
     * subtracted a second time here.
     */
    public ResourceAcquisitionPlan planKnownShortfall(
            StrategyContext context,
            ResourceNeed shortfall)
    {
        if (context == null || shortfall == null || context.getData() == null)
        {
            return checkNeeded(shortfall, "Account state is not available.");
        }

        AccountMode mode = context.getAccountMode();
        String sourceNote = sourceSuggestions(
                shortfall, mode, context.isAllowWildernessMethods());
        String prefix = "Confirmed shortfall: " + shortfall.getQuantity()
                + " × " + shortfall.getItemName() + ". ";

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new ResourceAcquisitionPlan(
                    shortfall, AcquisitionSource.GRAND_EXCHANGE, 0,
                    RecommendationConfidence.CHECK_NEEDED,
                    prefix + "GE is an option, but price, available GP, and opportunity cost must be verified before recommending a purchase."
                            + sourceNote);
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new ResourceAcquisitionPlan(
                    shortfall, AcquisitionSource.SELF_SOURCE, 0,
                    RecommendationConfidence.CHECK_NEEDED,
                    prefix + (mode == AccountMode.ULTIMATE_IRONMAN
                            ? "Acquire the missing quantity with a UIM-safe route."
                            : "Self-source the missing quantity.")
                            + sourceNote);
        }

        return checkNeeded(shortfall,
                prefix + "Verify account mode before choosing an acquisition route."
                        + sourceNote);
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
                    ownership.getNote(), ownership.getConfidence()));
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

    /** Resolves acquisition prerequisites recursively with bounded, cycle-safe traversal. */
    public ResourceDependencyResolution resolveDependencies(
            StrategyContext context, ResourceNeed need)
    {
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolve(context, need);
    }

    /**
     * Resolves a proven shortfall by canonical dependency output name. Unknown
     * names deliberately remain with the caller's conservative source guidance.
     */
    public ResourceDependencyResolution resolveKnownShortfall(
            StrategyContext context, String itemName, int quantity)
    {
        if (dependencyCatalog == null) return null;
        ResourceDependencyDefinition definition = dependencyCatalog.forItemName(itemName);
        if (definition == null) return null;
        String canonical = definition.getItemName() == null
                ? itemName : definition.getItemName();
        ResourceNeed need = new ResourceNeed(definition.getItemId(), canonical, quantity);
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolveKnownShortfall(context, need);
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

        StringBuilder note = new StringBuilder(" Useful route");
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
        List<StorageCapability> safeCapabilities = new ArrayList<>();
        List<StorageCapability> restrictedCapabilities = new ArrayList<>();
        int safeQuantity = 0;
        int restrictedQuantity = 0;
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
            if (quantity <= 0) continue;
            if (requiresAdditionalAccessCheck(capability))
            {
                restrictedCapabilities.add(capability);
                restrictedQuantity = safeAdd(restrictedQuantity, quantity);
            }
            else
            {
                safeCapabilities.add(capability);
                safeQuantity = safeAdd(safeQuantity, quantity);
            }
        }
        if (safeQuantity >= needed)
            return new StoredResource(safeCapabilities, safeQuantity);
        if (safeAdd(safeQuantity, restrictedQuantity) >= needed)
        {
            safeCapabilities.addAll(restrictedCapabilities);
            return new StoredResource(safeCapabilities,
                    safeAdd(safeQuantity, restrictedQuantity));
        }
        return null;
    }

    private static boolean requiresAdditionalAccessCheck(
            StorageCapability capability)
    {
        return UimStorageMechanics.isRestrictedRetrieval(capability);
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
            if (item.getItemId() == itemId) total = safeAdd(total, item.getQuantity());
        }
        return total;
    }

    private static int safeAdd(int left, int right)
    {
        int safeRight = Math.max(0, right);
        if (left > Integer.MAX_VALUE - safeRight) return Integer.MAX_VALUE;
        return left + safeRight;
    }

    private static String pretty(StorageCapability capability)
    {
        return capability.name().toLowerCase().replace('_', ' ');
    }

    private static String pretty(List<StorageCapability> capabilities)
    {
        if (capabilities == null || capabilities.isEmpty())
            return "verified storage";
        List<String> names = new ArrayList<>();
        for (StorageCapability capability : capabilities)
            names.add(pretty(capability));
        if (names.size() == 1) return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1))
                + " and " + names.get(names.size() - 1);
    }

    private static final class StoredResource
    {
        private final List<StorageCapability> capabilities;
        private final int quantity;

        private StoredResource(List<StorageCapability> capabilities,
                int quantity)
        {
            this.capabilities = new ArrayList<>(capabilities);
            this.quantity = quantity;
        }

        private boolean requiresAccessCheck()
        {
            for (StorageCapability capability : capabilities)
                if (requiresAdditionalAccessCheck(capability)) return true;
            return false;
        }
    }
}

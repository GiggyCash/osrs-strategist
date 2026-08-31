package compass;
import static compass.Text.get;

import java.util.*;
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

    public AcquisitionPlan plan(
            StrategyContext context,
            ResourceNeed need)
    {
        if (context == null || need == null || context.data() == null)
        {
            return checkNeeded(need, get(1430));
        }

        var data = context.data();
        var mode = context.accountMode();
        var inventoryQuantity = quantityIn(data.inventory(), need.getItemId());
        var confirmedQuantity = inventoryQuantity;

        if (inventoryQuantity >= need.getQuantity())
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.INVENTORY, inventoryQuantity,
                    Confidence.VERIFIED,
                    get(566)
            );
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            var remaining = Math.max(0, need.getQuantity() - inventoryQuantity);
            StoredResource stored = findVerifiedStoredResource(
                    data.storage(), need.getItemId(), remaining);
            if (stored != null)
            {
                var needsAccessCheck = stored.requiresAccessCheck();
                confirmedQuantity = safeAdd(inventoryQuantity, stored.quantity);
                return new AcquisitionPlan(
                        need,
                        AcquisitionSource.VERIFIED_STORAGE,
                        confirmedQuantity,
                        needsAccessCheck
                                ? Confidence.CHECK_NEEDED
                                : Confidence.VERIFIED,
                        needsAccessCheck
                                ? get(575)
                                        + pretty(stored.capabilities)
                                        + get(576)
                                : get(577)
                                        + pretty(stored.capabilities) + "."
                );
            }
        }
        else
        {
            var bankQuantity = quantityIn(data.bank(), need.getItemId());
            var ordinaryQuantity = safeAdd(inventoryQuantity, bankQuantity);
            confirmedQuantity = ordinaryQuantity;
            if (ordinaryQuantity >= need.getQuantity())
            {
                return new AcquisitionPlan(
                        need, AcquisitionSource.BANK, ordinaryQuantity,
                        Confidence.VERIFIED,
                        get(578)
                );
            }

            if (AccountModePolicy.mayUseGroupStorage(
                    mode, context.usesGroupStorage()))
            {
                var groupStorage = data.groupStorage();
                int groupQuantity = groupStorage != null
                        && groupStorage.isObserved()
                        ? quantityIn(groupStorage, need.getItemId()) : 0;
                if (groupStorage != null && groupStorage.isObserved())
                {
                    confirmedQuantity = safeAdd(ordinaryQuantity, groupQuantity);
                    if (confirmedQuantity >= need.getQuantity())
                    {
                        return new AcquisitionPlan(
                                need, AcquisitionSource.GROUP_STORAGE,
                                confirmedQuantity,
                                Confidence.VERIFIED,
                                get(579)
                        );
                    }
                }
            }
        }

        var sourceNote = sourceSuggestions(need, context);

        // Do not turn an unobserved container into a proven shortfall. An
        // inventory read is required for every mode; ordinary accounts also
        // require the bank, and opted-in GIM requires fresh Group Storage.
        if (data.inventory() == null)
            return checkNeeded(need,
                    get(580));
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
            return checkNeeded(need,
                    get(581));
        if (AccountModePolicy.mayUseGroupStorage(mode,
                context.usesGroupStorage())
                && (data.groupStorage() == null
                || !data.groupStorage().isObserved()))
            return checkNeeded(need,
                    get(582));

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.GRAND_EXCHANGE, confirmedQuantity,
                    Confidence.CHECK_NEEDED,
                    get(567)
                            + sourceNote
            );
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new AcquisitionPlan(
                    need, AcquisitionSource.SELF_SOURCE, confirmedQuantity,
                    Confidence.CHECK_NEEDED,
                    (mode == AccountMode.ULTIMATE_IRONMAN
                            ? get(568)
                            : get(569))
                            + sourceNote
            );
        }

        return checkNeeded(need,
                get(570) + sourceNote);
    }

    /**
     * Plans a shortfall already proven by another evidence-aware evaluator.
     * The root quantity is the missing quantity, so owned state must not be
     * subtracted a second time here.
     */
    public AcquisitionPlan planKnownShortfall(
            StrategyContext context,
            ResourceNeed shortfall)
    {
        if (context == null || shortfall == null || context.data() == null)
        {
            return checkNeeded(shortfall, get(1430));
        }

        var mode = context.accountMode();
        var sourceNote = sourceSuggestions(shortfall, context);
        String prefix = get(1431) + shortfall.getQuantity()
                + " × " + shortfall.getItemName() + ". ";

        if (AccountModePolicy.mayUseGrandExchange(mode))
        {
            return new AcquisitionPlan(
                    shortfall, AcquisitionSource.GRAND_EXCHANGE, 0,
                    Confidence.CHECK_NEEDED,
                    prefix + get(571)
                            + sourceNote);
        }

        if (AccountModePolicy.requiresSelfSourcing(mode))
        {
            return new AcquisitionPlan(
                    shortfall, AcquisitionSource.SELF_SOURCE, 0,
                    Confidence.CHECK_NEEDED,
                    prefix + (mode == AccountMode.ULTIMATE_IRONMAN
                            ? get(572)
                            : get(1432))
                            + sourceNote);
        }

        return checkNeeded(shortfall,
                prefix + get(573)
                        + sourceNote);
    }

    /** Builds an ordered chain without pretending prose source hints are verified unlocks. */
    public ResourceAcquisitionChain planChain(StrategyContext context,
            ResourceNeed need)
    {
        var ownership = plan(context, need);
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
                    context.accountMode(), membership(context),
                    context.allowsWilderness());
            for (String route : routes)
                steps.add(new ResourceAcquisitionStep(
                        context.accountMode().usesGrandExchange()
                                ? AcquisitionSource.GRAND_EXCHANGE
                                : AcquisitionSource.SELF_SOURCE,
                        route, Confidence.CHECK_NEEDED));
        }
        return new ResourceAcquisitionChain(need, shortfall, steps);
    }

    /** Resolves acquisition prerequisites recursively with bounded, cycle-safe traversal. */
    public DependencyResolution resolveDependencies(
            StrategyContext context, ResourceNeed need)
    {
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolve(context, need);
    }

    /**
     * Resolves a proven shortfall by canonical dependency output name. Unknown
     * names deliberately remain with the caller's conservative source guidance.
     */
    public DependencyResolution resolveKnownShortfall(
            StrategyContext context, String itemName, int quantity)
    {
        if (dependencyCatalog == null) return null;
        var definition = dependencyCatalog.forItemName(itemName);
        if (definition == null) return null;
        String canonical = definition.getItemName() == null
                ? itemName : definition.getItemName();
        var need = new ResourceNeed(definition.getItemId(), canonical, quantity);
        return new ResourceDependencyResolver(this, dependencyCatalog)
                .resolveKnownShortfall(context, need);
    }

    private String sourceSuggestions(ResourceNeed need, StrategyContext context)
    {
        if (sourceCatalog == null || need == null) return "";
        List<String> suggestions = sourceCatalog.suggestions(
                need.getItemName(), context == null ? AccountMode.UNKNOWN
                        : context.accountMode(), membership(context),
                context != null && context.allowsWilderness());
        if (suggestions.isEmpty())
        {
            return get(574);
        }

        var note = new StringBuilder(" Useful route");
        if (suggestions.size() > 1) note.append("s");
        note.append(": ");
        for (int i = 0; i < suggestions.size(); i++)
        {
            if (i > 0) note.append(" | ");
            note.append(suggestions.get(i));
        }
        return note.toString();
    }

    private static MembershipStatus membership(StrategyContext context)
    {
        return context == null || context.data() == null
                || context.data().account() == null
                ? MembershipStatus.UNKNOWN
                : context.data().account().membership();
    }

    private static StoredResource findVerifiedStoredResource(
            StorageSnapshot storage,
            int itemId,
            int needed)
    {
        if (storage == null) return null;
        List<StorageCapability> safeCapabilities = new ArrayList<>();
        List<StorageCapability> restrictedCapabilities = new ArrayList<>();
        var safeQuantity = 0;
        var restrictedQuantity = 0;
        for (Map.Entry<StorageCapability, java.util.List<ItemState>> entry
                : storage.getObservedContents().entrySet())
        {
            var capability = entry.getKey();
            if (!storage.verified(capability)) continue;
            var quantity = 0;
            for (ItemState item : entry.getValue())
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

    private static AcquisitionPlan checkNeeded(
            ResourceNeed need,
            String note)
    {
        return new AcquisitionPlan(
                need, AcquisitionSource.CHECK_NEEDED, 0,
                Confidence.CHECK_NEEDED, note
        );
    }

    private static int quantityIn(ItemsState items, int itemId)
    {
        return items == null ? 0 : quantityInItems(items.getItems(), itemId);
    }

    private static int quantityInItems(Iterable<ItemState> items, int itemId)
    {
        var total = 0;
        for (ItemState item : items)
        {
            if (item.getItemId() == itemId) total = safeAdd(total, item.getQuantity());
        }
        return total;
    }

    private static int safeAdd(int left, int right)
    {
        var safeRight = Math.max(0, right);
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
            return get(1955);
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

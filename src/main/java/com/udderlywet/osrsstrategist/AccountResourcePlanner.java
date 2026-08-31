package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Shared account-aware material planner for deterministic recommendations.
 *
 * <p>This is the single place that decides whether an observed item can satisfy
 * a milestone plan. Main, Iron, GIM, and UIM guidance therefore use the same
 * shortfall math instead of each skill reimplementing bank semantics.</p>
 */
@Singleton
public class AccountResourcePlanner
{
    private final PurchaseCostAdvisor purchaseCostAdvisor;
    private final MainEconomyPlanner mainEconomyPlanner;
    private final ResourceSourceCatalog resourceSourceCatalog;

    @Inject
    public AccountResourcePlanner(PurchaseCostAdvisor purchaseCostAdvisor,
            MainEconomyPlanner mainEconomyPlanner,
            ResourceSourceCatalog resourceSourceCatalog)
    {
        this.purchaseCostAdvisor = purchaseCostAdvisor;
        this.mainEconomyPlanner = mainEconomyPlanner;
        this.resourceSourceCatalog = resourceSourceCatalog;
    }

    public AccountResourcePlanner(PurchaseCostAdvisor purchaseCostAdvisor)
    {
        this(purchaseCostAdvisor, new MainEconomyPlanner(),
                new ResourceSourceCatalog());
    }

    /** Test/compatibility constructor that deliberately omits live prices. */
    public AccountResourcePlanner()
    {
        this(null, new MainEconomyPlanner(), new ResourceSourceCatalog());
    }

    public SupplyPlan plan(
            GameData data,
            List<MethodInput> rawNeeds,
            boolean useGroupStorage)
    {
        AccountSnapshot account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        ItemIndex observed = new ItemIndex(data, useGroupStorage);
        boolean primaryObserved = observed.usableOwnershipObserved();
        boolean groupIncluded = useGroupStorage && mode.isGroupIronman();
        boolean groupObserved = observed.groupStorageObserved();

        List<MethodInput> needs = merge(rawNeeds);
        List<ResourcePlanEntry> entries = new ArrayList<>();
        for (MethodInput need : needs)
        {
            String reusable = reusableSourceFor(observed, need.getName());
            int owned = observed.quantity(need.getName());
            int restricted = observed.restrictedQuantity(need.getName());
            int missing = reusable == null
                    ? Math.max(0, need.getQuantity() - owned)
                    : 0;
            entries.add(new ResourcePlanEntry(
                    need.getName(),
                    need.getItemId(),
                    need.getQuantity(),
                    owned,
                    missing,
                    restricted,
                    reusable));
        }

        String guidance = buildGuidance(
                data,
                mode,
                primaryObserved,
                groupIncluded,
                groupObserved,
                entries);
        return new SupplyPlan(
                mode,
                primaryObserved,
                groupIncluded,
                groupObserved,
                entries,
                guidance);
    }

    private String buildGuidance(
            GameData data,
            AccountMode mode,
            boolean primaryObserved,
            boolean groupIncluded,
            boolean groupObserved,
            List<ResourcePlanEntry> entries)
    {
        if (entries.isEmpty())
        {
            return get(69);
        }

        List<String> required = new ArrayList<>();
        List<String> verified = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> reusable = new ArrayList<>();
        List<String> restricted = new ArrayList<>();
        List<MethodInput> missingInputs = new ArrayList<>();

        for (ResourcePlanEntry entry : entries)
        {
            required.add(format(entry.getRequired()) + " " + entry.getName());
            if (entry.isSatisfiedByReusableSource())
            {
                reusable.add(entry.getName() + " supplied by "
                        + entry.getReusableSource());
            }
            else
            {
                verified.add(format(entry.getUsableOwned()) + " "
                        + entry.getName());
            }

            if (entry.getMissing() > 0)
            {
                missing.add(format(entry.getMissing()) + " " + entry.getName());
                missingInputs.add(entry.missingInput());
            }
            if (entry.getRestrictedOwned() > 0)
            {
                restricted.add(format(entry.getRestrictedOwned()) + " "
                        + entry.getName());
            }
        }

        StringBuilder text = new StringBuilder();
        text.append("Need ").append(join(required)).append(". ");
        if (!reusable.isEmpty())
        {
            text.append("Reusable setup: ").append(join(reusable)).append(". ");
        }

        // An unobserved ownership surface is unknown, never empty. UIM uses
        // inventory/equipment evidence instead of the conventional bank.
        if (!primaryObserved)
        {
            if (mode == AccountMode.ULTIMATE_IRONMAN)
                text.append(get(80));
            else
                text.append(get(1376))
                        .append(get(81));
            if (groupIncluded && !groupObserved)
            {
                text.append(get(82));
            }
            return text.toString();
        }

        if (!verified.isEmpty())
        {
            text.append("Verified usable: ")
                    .append(join(verified)).append(". ");
        }

        if (missing.isEmpty())
        {
            text.append(get(83));
            if (groupIncluded && groupObserved)
            {
                text.append(get(84));
            }
            appendRestrictedUimNote(text, restricted);
            return text.toString();
        }

        String shortfall = join(missing);
        if (mode.usesGrandExchange())
        {
            appendMainOpportunityGuidance(text, data, shortfall,
                    missingInputs);
        }
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            List<String> routes = sourceRoutes(missingInputs, mode,
                    data == null || data.account() == null
                            ? MembershipStatus.UNKNOWN
                            : data.account().getMembershipStatus(), true);
            text.append(get(1377)).append(shortfall)
                    .append(get(85));
            if (!routes.isEmpty())
                text.append(" Route: ").append(routes.get(0));
            text.append(get(86));
        }
        else if (mode.isGroupIronman())
        {
            text.append("Self-source ").append(shortfall)
                    .append(get(1378));
            if (groupIncluded && groupObserved)
            {
                text.append(get(87));
            }
            else if (groupIncluded)
            {
                text.append(get(70));
            }
        }
        else if (mode.isIronLike())
        {
            List<String> routes = sourceRoutes(missingInputs, mode,
                    data == null || data.account() == null
                            ? MembershipStatus.UNKNOWN
                            : data.account().getMembershipStatus(), false);
            text.append("Self-source ").append(shortfall)
                    .append(get(1378));
            if (!routes.isEmpty())
                text.append(" Route: ").append(routes.get(0));
        }
        else
        {
            text.append("Source ").append(shortfall)
                    .append(get(71));
        }

        appendRestrictedUimNote(text, restricted);
        return text.toString();
    }

    private void appendMainOpportunityGuidance(StringBuilder text,
            GameData data, String shortfall,
            List<MethodInput> missingInputs)
    {
        PurchaseCostEstimate estimate = purchaseCostAdvisor == null
                ? PurchaseCostEstimate.unknown()
                : purchaseCostAdvisor.estimate(missingInputs);
        List<String> routes = mainRoutes(missingInputs,
                data == null || data.account() == null
                        ? MembershipStatus.UNKNOWN
                        : data.account().getMembershipStatus());
        MainPurchaseDecision decision = mainEconomyPlanner == null
                ? null : mainEconomyPlanner.evaluateUnmeasuredPurchase(
                        data == null ? null : data.economy(), estimate,
                        !routes.isEmpty());

        if (decision != null && decision.getChoice() == MainPurchaseChoice.BUY)
        {
            text.append("Buy ").append(shortfall)
                    .append(get(1379))
                    .append(get(1380))
                    .append(format(decision.getTotalCost()))
                    .append(get(72))
                    .append(format(decision.getObservedCoins()))
                    .append(get(1381));
            return;
        }

        if (decision != null
                && decision.getChoice() == MainPurchaseChoice.SELF_SOURCE)
        {
            text.append("Self-source ").append(shortfall).append(". ")
                    .append(get(1382))
                    .append(format(decision.getTotalCost()))
                    .append(" of ")
                    .append(format(decision.getObservedCoins()))
                    .append(get(73))
                    .append(routes.get(0));
            return;
        }

        if (decision != null && decision.getChoice()
                == MainPurchaseChoice.EARN_GP_OR_REVIEW_RESOURCES)
        {
            text.append(get(74))
                    .append(format(decision.getTotalCost()))
                    .append(" coins, but only ")
                    .append(format(decision.getObservedCoins()))
                    .append(get(1383));
            if (!routes.isEmpty())
                text.append(get(75))
                        .append(routes.get(0));
            return;
        }

        text.append(get(76))
                .append(get(77));
        if (!routes.isEmpty())
            text.append(get(1384)).append(routes.get(0));
    }

    private List<String> mainRoutes(List<MethodInput> missingInputs,
            MembershipStatus membership)
    {
        if (resourceSourceCatalog == null || missingInputs == null)
            return java.util.Collections.emptyList();
        List<String> routes = new ArrayList<>();
        for (MethodInput input : missingInputs)
        {
            for (String route : resourceSourceCatalog.suggestions(
                    input.getName(), AccountMode.MAIN, membership, false))
            {
                if (!routes.contains(route)) routes.add(route);
            }
        }
        return routes;
    }

    private List<String> sourceRoutes(List<MethodInput> missingInputs,
            AccountMode mode, MembershipStatus membership,
            boolean justInTime)
    {
        if (resourceSourceCatalog == null || missingInputs == null)
            return java.util.Collections.emptyList();
        List<String> routes = new ArrayList<>();
        for (MethodInput input : missingInputs)
            for (String route : resourceSourceCatalog.suggestions(
                    input.getName(), mode, membership, justInTime))
                if (!routes.contains(route)) routes.add(route);
        return routes;
    }

    private static void appendRestrictedUimNote(
            StringBuilder text,
            List<String> restricted)
    {
        if (restricted == null || restricted.isEmpty()) return;
        text.append(get(78))
                .append(join(restricted))
                .append(get(79));
    }

    /**
     * Only currently equipped reusable rune sources waive rune consumption.
     * Owning a staff in a bank is not enough because build requirements and the
     * recommendation's intended equipment setup may make it unusable.
     */
    private static String reusableSourceFor(
            ItemIndex observed,
            String itemName)
    {
        if (observed == null || itemName == null) return null;
        String rune = normalize(itemName);
        if ("fire rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of fire", "Fire battlestaff", "Mystic fire staff",
                    "Lava battlestaff", "Mystic lava staff",
                    "Steam battlestaff", get(1385),
                    "Smoke battlestaff", get(1386),
                    "Tome of fire");
        }
        if ("water rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of water", "Water battlestaff", get(1387),
                    "Mud battlestaff", "Mystic mud staff",
                    "Steam battlestaff", get(1385),
                    "Mist battlestaff", "Mystic mist staff");
        }
        if ("earth rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of earth", "Earth battlestaff", get(1388),
                    "Lava battlestaff", "Mystic lava staff",
                    "Mud battlestaff", "Mystic mud staff",
                    "Dust battlestaff", "Mystic dust staff");
        }
        if ("air rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of air", "Air battlestaff", "Mystic air staff",
                    "Smoke battlestaff", get(1386),
                    "Mist battlestaff", "Mystic mist staff",
                    "Dust battlestaff", "Mystic dust staff");
        }
        return null;
    }

    private static String firstEquipped(
            ItemIndex observed,
            String... candidates)
    {
        for (String candidate : candidates)
        {
            if (observed.equipped(candidate)) return candidate;
        }
        return null;
    }

    /** Merge duplicate recipe rows before comparing them with storage. */
    private static List<MethodInput> merge(
            List<MethodInput> rawNeeds)
    {
        if (rawNeeds == null || rawNeeds.isEmpty()) return new ArrayList<>();
        Map<String, MutableNeed> merged = new LinkedHashMap<>();
        for (MethodInput input : rawNeeds)
        {
            if (input == null || input.getName() == null
                    || input.getName().trim().isEmpty()
                    || input.getQuantity() <= 0)
            {
                continue;
            }
            String key = normalize(input.getName());
            MutableNeed existing = merged.get(key);
            if (existing == null)
            {
                merged.put(key, new MutableNeed(
                        input.getName().trim(),
                        input.getItemId(),
                        input.getQuantity()));
            }
            else
            {
                existing.quantity = safeAdd(
                        existing.quantity, input.getQuantity());
            }
        }

        List<MethodInput> result = new ArrayList<>();
        for (MutableNeed need : merged.values())
        {
            result.add(new MethodInput(
                    need.name, need.itemId, need.quantity));
        }
        return result;
    }

    private static int safeAdd(int a, int b)
    {
        if (a >= Integer.MAX_VALUE - b) return Integer.MAX_VALUE;
        return a + b;
    }

    private static String join(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0) text.append(i == parts.size() - 1 ? ", and " : ", ");
            text.append(parts.get(i));
        }
        return text.toString();
    }

    private static String format(long value)
    {
        return String.format(Locale.ROOT, "%,d", Math.max(0, value));
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class MutableNeed
    {
        private final String name;
        private final int itemId;
        private int quantity;

        private MutableNeed(String name, int itemId, int quantity)
        {
            this.name = name;
            this.itemId = itemId;
            this.quantity = Math.max(0, quantity);
        }
    }
}

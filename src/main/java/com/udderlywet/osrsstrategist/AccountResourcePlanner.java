package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Inject
    public AccountResourcePlanner(PurchaseCostAdvisor purchaseCostAdvisor)
    {
        this.purchaseCostAdvisor = purchaseCostAdvisor;
    }

    /** Test/compatibility constructor that deliberately omits live prices. */
    public AccountResourcePlanner()
    {
        this(null);
    }

    public AccountResourcePlan plan(
            StrategyDataBundle data,
            List<ResolvedMethodInput> rawNeeds,
            boolean useGroupStorage)
    {
        AccountSnapshot account = data == null ? null : data.getAccount();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.getAccountTypeCode());
        ObservedItemIndex observed = new ObservedItemIndex(data, useGroupStorage);
        boolean primaryObserved = mode == AccountMode.ULTIMATE_IRONMAN
                || observed.bankObserved();
        boolean groupIncluded = useGroupStorage && mode.isGroupIronman();
        boolean groupObserved = observed.groupStorageObserved();

        List<ResolvedMethodInput> needs = merge(rawNeeds);
        List<ResourcePlanEntry> entries = new ArrayList<>();
        for (ResolvedMethodInput need : needs)
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
        return new AccountResourcePlan(
                mode,
                primaryObserved,
                groupIncluded,
                groupObserved,
                entries,
                guidance);
    }

    private String buildGuidance(
            StrategyDataBundle data,
            AccountMode mode,
            boolean primaryObserved,
            boolean groupIncluded,
            boolean groupObserved,
            List<ResourcePlanEntry> entries)
    {
        if (entries.isEmpty())
        {
            return "No consumed material is required for the modeled action.";
        }

        List<String> required = new ArrayList<>();
        List<String> verified = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> reusable = new ArrayList<>();
        List<String> restricted = new ArrayList<>();
        List<ResolvedMethodInput> missingInputs = new ArrayList<>();

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

        // An unopened bank is unknown, never empty. Do not publish a fake Main
        // or Iron shortfall until the ordinary bank has been observed once.
        if (!primaryObserved && mode != AccountMode.ULTIMATE_IRONMAN)
        {
            text.append("Open your bank once so Strategist can verify stored ")
                    .append("materials before calculating the real shortfall.");
            if (groupIncluded && !groupObserved)
            {
                text.append(" Group Storage is also enabled but has not been observed yet.");
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
            text.append("You already have the modeled inputs for this milestone.");
            if (groupIncluded && groupObserved)
            {
                text.append(" Observed Group Storage was included in that total.");
            }
            appendRestrictedUimNote(text, restricted);
            return text.toString();
        }

        String shortfall = join(missing);
        if (mode.usesGrandExchange())
        {
            text.append("Buy ").append(shortfall)
                    .append(" at the Grand Exchange.");
            String costAdvice = purchaseCostAdvisor == null
                    ? null
                    : purchaseCostAdvisor.advice(
                            data == null ? null : data.getEconomy(),
                            missingInputs);
            if (costAdvice != null)
            {
                text.append(" ").append(costAdvice);
            }
            else
            {
                text.append(" Exact quantities are known; live price or cash evidence is not complete enough for an exact GP total.");
            }
        }
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            text.append("Acquire ").append(shortfall)
                    .append(" just in time. Strategist only counted inventory, equipment, and verified directly usable storage; a normal bank is never counted for UIM.");
        }
        else if (mode.isGroupIronman())
        {
            text.append("Self-source ").append(shortfall).append(".");
            if (groupIncluded && groupObserved)
            {
                text.append(" Observed Group Storage was already included before calculating this shortfall.");
            }
            else if (groupIncluded)
            {
                text.append(" Group Storage is enabled but unobserved, so this shortfall intentionally excludes it until Strategist sees it.");
            }
        }
        else if (mode.isIronLike())
        {
            text.append("Self-source ").append(shortfall).append(".");
        }
        else
        {
            text.append("Source ").append(shortfall)
                    .append(" after account mode is verified. Strategist will not assume Grand Exchange access for an unknown account mode.");
        }

        appendRestrictedUimNote(text, restricted);
        return text.toString();
    }

    private static void appendRestrictedUimNote(
            StringBuilder text,
            List<String> restricted)
    {
        if (restricted == null || restricted.isEmpty()) return;
        text.append(" Observed in retrieval-only UIM storage: ")
                .append(join(restricted))
                .append(". Those items are not counted until a retrieval plan is deliberately chosen.");
    }

    /**
     * Only currently equipped reusable rune sources waive rune consumption.
     * Owning a staff in a bank is not enough because build requirements and the
     * recommendation's intended equipment setup may make it unusable.
     */
    private static String reusableSourceFor(
            ObservedItemIndex observed,
            String itemName)
    {
        if (observed == null || itemName == null) return null;
        String rune = normalize(itemName);
        if ("fire rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of fire", "Fire battlestaff", "Mystic fire staff",
                    "Lava battlestaff", "Mystic lava staff",
                    "Steam battlestaff", "Mystic steam staff",
                    "Smoke battlestaff", "Mystic smoke staff",
                    "Tome of fire");
        }
        if ("water rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of water", "Water battlestaff", "Mystic water staff",
                    "Mud battlestaff", "Mystic mud staff",
                    "Steam battlestaff", "Mystic steam staff",
                    "Mist battlestaff", "Mystic mist staff");
        }
        if ("earth rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of earth", "Earth battlestaff", "Mystic earth staff",
                    "Lava battlestaff", "Mystic lava staff",
                    "Mud battlestaff", "Mystic mud staff",
                    "Dust battlestaff", "Mystic dust staff");
        }
        if ("air rune".equals(rune))
        {
            return firstEquipped(observed,
                    "Staff of air", "Air battlestaff", "Mystic air staff",
                    "Smoke battlestaff", "Mystic smoke staff",
                    "Mist battlestaff", "Mystic mist staff",
                    "Dust battlestaff", "Mystic dust staff");
        }
        return null;
    }

    private static String firstEquipped(
            ObservedItemIndex observed,
            String... candidates)
    {
        for (String candidate : candidates)
        {
            if (observed.equipped(candidate)) return candidate;
        }
        return null;
    }

    /** Merge duplicate recipe rows before comparing them with storage. */
    private static List<ResolvedMethodInput> merge(
            List<ResolvedMethodInput> rawNeeds)
    {
        if (rawNeeds == null || rawNeeds.isEmpty()) return new ArrayList<>();
        Map<String, MutableNeed> merged = new LinkedHashMap<>();
        for (ResolvedMethodInput input : rawNeeds)
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

        List<ResolvedMethodInput> result = new ArrayList<>();
        for (MutableNeed need : merged.values())
        {
            result.add(new ResolvedMethodInput(
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

    private static String format(int value)
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

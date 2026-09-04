package compass;
import lombok.*;
import static net.runelite.api.Skill.*;
import static compass.Text.get;

import java.util.*;
import javax.inject.*;
import net.runelite.api.*;

/**
 * Conservative first-pass readiness for every boss identity RuneLite exposes.
 * Exact encounter modules can override these broad floors with better evidence.
 */
@Singleton
@RequiredArgsConstructor(access = AccessLevel.PUBLIC, onConstructor_ = @Inject)
public class PvmReadinessAnalyzer
{
    private final PvmActivityCatalog catalog;
    private final PvmEvidenceProfileCatalog evidenceProfiles;
    private final PvmPreparationProfileCatalog preparationProfiles;
    @Inject
    public PvmSnapshot analyze(
            AccountSnapshot account,
            QuestSnapshot quests,
            ItemsState equipment,
            ItemsState inventory,
            ItemsState bank,
            PvmSnapshot observed)
    {
        return analyze(account, quests, equipment, inventory, null, bank, observed);
    }

    public PvmSnapshot analyze(
            AccountSnapshot account,
            QuestSnapshot quests,
            ItemsState equipment,
            ItemsState inventory,
            StorageSnapshot storage,
            ItemsState bank,
            PvmSnapshot observed)
    {
        if (account == null) return observed;
        var mode = AccountMode.fromTypeCode(account.modeCode());
        Map<String, PvmReadiness> result = new HashMap<>();
        for (PvmActivity activity : catalog.all())
        {
            var prior = priorFor(observed, activity.id);
            if (prior != null
                    && prior.confidence == Confidence.BLOCKED)
            {
                result.put(activity.id, prior);
                continue;
            }
            List<String> missing = new ArrayList<>();
            PvmEvidenceProfile exact = evidenceProfiles == null ? null
                    : evidenceProfiles.forActivity(activity.id);
            PvmPreparationProfile preparation = preparationProfiles == null ? null
                    : preparationProfiles.forActivity(activity.id);
            if (preparation == null)
            {
                result.put(activity.id, new PvmReadiness(activity.id, false,
                        Confidence.CHECK_NEEDED,
                        Collections.singletonList(get(1565))));
                continue;
            }
            // Exact profiles are recomputed from current carried state so a
            // preparation -> ready transition cannot retain stale failures.
            if (exact == null && prior != null)
                missing.addAll(prior.missingRequirements);

            requireLevel(account, ATTACK, preparation.getAttack(), missing);
            requireLevel(account, STRENGTH, preparation.getStrength(), missing);
            requireLevel(account, DEFENCE, preparation.getDefence(), missing);
            requireLevel(account, RANGED, preparation.getRanged(), missing);
            requireLevel(account, MAGIC, preparation.getMagic(), missing);
            requireLevel(account, PRAYER, preparation.getPrayer(), missing);
            requireLevel(account, SLAYER, preparation.getSlayer(), missing);

            if (preparation.requiredQuest != null
                    && !questUsable(quests, preparation.requiredQuest,
                    preparation.isQuestMayBeInProgress()))
            {
                missing.add("Quest/access: " + preparation.requiredQuest);
            }

            String requiredStyle = exact == null ? preparation.getPreferredStyle()
                    : exact.getWeaponStyle();
            if (!hasCombatWeapon(equipment, requiredStyle))
                addMissing(missing, "Equip a usable " + preparation.getPreferredStyle() + get(1566));
            else if (exact == null)
                addMissing(missing, get(443));

            int requiredFood = exact == null ? (preparation.isRequiresSupplies() ? 5 : 0)
                    : exact.getMinimumFood();
            if (carriedFoodQuantity(inventory) < requiredFood)
                addMissing(missing, get(445));
            int requiredRestore = exact == null ? (preparation.getPrayer() >= 43 ? 1 : 0)
                    : exact.getMinimumRestoration();
            if (carriedRestorationQuantity(inventory) < requiredRestore)
                addMissing(missing, get(446));
            if (usesRanged(preparation.getPreferredStyle()))
                addMissing(missing, carriedAmmoQuantity(inventory, equipment) > 0
                        ? get(447)
                        : get(448));
            if (usesMagic(preparation.getPreferredStyle()))
                addMissing(missing, runeEvidence(inventory, storage)
                        ? get(449)
                        : get(450));

            if (exact == null)
                addMissing(missing, get(451));
            else
                for (String accessItem : exact.getAccessItems())
                    if (carriedQuantity(inventory, accessItem.toLowerCase(Locale.ROOT)) < 1)
                        addMissing(missing, "Carry " + accessItem + get(1567));

            if (exact == null && preparation != null)
                for (String check : preparation.getChecks()) addMissing(missing, check);

            if (mode == AccountMode.ULTIMATE_IRONMAN && preparation.isRequiresSupplies())
                addMissing(missing, get(452));

            var fullyVerified = exact != null && missing.isEmpty();
            result.put(activity.id, new PvmReadiness(
                    activity.id,
                    fullyVerified,
                    fullyVerified ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    missing
            ));
        }
        return new PvmSnapshot(result);
    }


    private static PvmReadiness priorFor(PvmSnapshot observed, String activityId)
    {
        if (observed == null || activityId == null) return null;
        var prior = observed.readinessFor(activityId);
        if (prior != null) return prior;
        String shortId = activityId.startsWith("pvm:")
                ? activityId.substring("pvm:".length()) : activityId;
        return observed.readinessFor(shortId);
    }

    private static void requireLevel(AccountSnapshot account, Skill skill,
            int required, List<String> missing)
    {
        if (required > 1 && account.level(skill) < required)
            missing.add(skill.getName() + " " + required);
    }

    private static boolean questUsable(QuestSnapshot quests, String quest,
            boolean inProgressAllowed)
    {
        if (quests == null || quest == null) return false;
        for (Map.Entry<String, QuestStatus> entry : quests.quests().entrySet())
        {
            if (!entry.getKey().equalsIgnoreCase(quest)) continue;
            if (entry.getValue() == QuestStatus.COMPLETE) return true;
            return inProgressAllowed && entry.getValue() == QuestStatus.IN_PROGRESS;
        }
        return false;
    }

    private static boolean hasCombatWeapon(ItemsState equipment, String style)
    {
        List<ItemState> items = new ArrayList<>();
        if (equipment != null) items.addAll(equipment.getEquippedItems());
        boolean melee = false, ranged = false, magic = false;
        for (ItemState item : items)
        {
            // Persisted snapshots without slot provenance cannot prove a readied weapon.
            if (item.slotIndex != EquipmentInventorySlot.WEAPON.getSlotIdx()) continue;
            var name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
            melee |= containsAny(name, "scimitar", "sword", "whip", "mace", "axe",
                    "halberd", "spear", "hasta", "fang", "scythe", "maul", "bludgeon", "lance");
            ranged |= containsAny(name, "bow", "crossbow", "blowpipe", "ballista", "atlatl");
            magic |= containsAny(name, "staff", "wand", "trident", "sceptre", "scepter", "shadow");
        }
        if ("melee".equals(style)) return melee;
        if ("ranged".equals(style)) return ranged;
        if ("magic".equals(style)) return magic;
        if ("ranged or magic".equals(style)) return ranged || magic;
        if ("hybrid".equals(style)) return (melee && ranged) || (ranged && magic) || (melee && magic);
        return melee || ranged || magic;
    }

    private static int carriedFoodQuantity(ItemsState inventory)
    {
        return carriedQuantity(inventory, "shark", "karambwan", "anglerfish",
                "manta ray", get(1568), "lobster", "swordfish", "pizza");
    }

    private static int carriedRestorationQuantity(ItemsState inventory)
    {
        return carriedQuantity(inventory, "prayer potion", "super restore");
    }

    private static int carriedAmmoQuantity(ItemsState inventory,
            ItemsState equipment)
    {
        int quantity = carriedQuantity(inventory, "arrow", "bolt", "dart",
                "javelin", "chinchompa");
        if (equipment != null)
            for (ItemState item : equipment.getEquippedItems())
                if (item.slotIndex == EquipmentInventorySlot.AMMO.getSlotIdx()
                        && containsAny(item.getName() == null ? ""
                        : item.getName().toLowerCase(Locale.ROOT),
                        "arrow", "bolt", "dart", "javelin"))
                    quantity += item.quantity;
        return quantity;
    }

    private static int carriedRuneQuantity(ItemsState inventory)
    {
        return carriedQuantity(inventory, " rune");
    }

    private static boolean runeEvidence(ItemsState inventory,
            StorageSnapshot storage)
    {
        if (carriedRuneQuantity(inventory) > 0) return true;
        return storage != null
                && storage.verified(StorageKind.RUNE_POUCH)
                && storage.hasObservedContents(StorageKind.RUNE_POUCH)
                && !storage.contentsOf(StorageKind.RUNE_POUCH).isEmpty();
    }

    private static void addMissing(List<String> missing, String requirement)
    {
        if (requirement != null && !requirement.trim().isEmpty()
                && !missing.contains(requirement)) missing.add(requirement);
    }

    private static int carriedQuantity(ItemsState inventory, String... terms)
    {
        if (inventory == null) return 0;
        var total = 0;
        for (ItemState item : inventory.getItems())
        {
            var name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
            if (containsAny(name, terms)) total += Math.max(0, item.quantity);
        }
        return total;
    }

    private static boolean usesRanged(String style)
    {
        return style != null && (style.contains("ranged") || "hybrid".equals(style));
    }

    private static boolean usesMagic(String style)
    {
        return style != null && (style.contains("magic") || "hybrid".equals(style));
    }

    private static boolean containsAny(String value, String... terms)
    {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

}

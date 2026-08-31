package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;

/**
 * Conservative first-pass readiness for every boss identity RuneLite exposes.
 * Exact encounter modules can override these broad floors with better evidence.
 */
@Singleton
public class PvmReadinessAnalyzer
{
    private final PvmActivityCatalog catalog;
    private final PvmEvidenceProfileCatalog evidenceProfiles;
    private final PvmPreparationProfileCatalog preparationProfiles;

    public PvmReadinessAnalyzer(PvmActivityCatalog catalog)
    {
        this(catalog, new PvmEvidenceProfileCatalog(),
                new PvmPreparationProfileCatalog());
    }

    @Inject
    public PvmReadinessAnalyzer(PvmActivityCatalog catalog,
            PvmEvidenceProfileCatalog evidenceProfiles)
    {
        this(catalog, evidenceProfiles, new PvmPreparationProfileCatalog());
    }

    public PvmReadinessAnalyzer(PvmActivityCatalog catalog,
            PvmEvidenceProfileCatalog evidenceProfiles,
            PvmPreparationProfileCatalog preparationProfiles)
    {
        this.catalog = catalog;
        this.evidenceProfiles = evidenceProfiles;
        this.preparationProfiles = preparationProfiles;
    }

    public PvmSnapshot analyze(
            AccountSnapshot account,
            QuestSnapshot quests,
            EquipmentSnapshot equipment,
            InventorySnapshot inventory,
            BankSnapshot bank,
            PvmSnapshot observed)
    {
        return analyze(account, quests, equipment, inventory, null, bank, observed);
    }

    public PvmSnapshot analyze(
            AccountSnapshot account,
            QuestSnapshot quests,
            EquipmentSnapshot equipment,
            InventorySnapshot inventory,
            StorageSnapshot storage,
            BankSnapshot bank,
            PvmSnapshot observed)
    {
        if (account == null) return observed;
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        Map<String, PvmReadiness> result = new HashMap<>();
        for (PvmActivityDefinition activity : catalog.all())
        {
            PvmReadiness prior = priorFor(observed, activity.getId());
            if (prior != null
                    && prior.getConfidence() == RecommendationConfidence.BLOCKED)
            {
                result.put(activity.getId(), prior);
                continue;
            }
            List<String> missing = new ArrayList<>();
            PvmEvidenceProfile exact = evidenceProfiles == null ? null
                    : evidenceProfiles.forActivity(activity.getId());
            PvmPreparationProfile preparation = preparationProfiles == null ? null
                    : preparationProfiles.forActivity(activity.getId());
            if (preparation == null)
            {
                result.put(activity.getId(), new PvmReadiness(activity.getId(), false,
                        RecommendationConfidence.CHECK_NEEDED,
                        Collections.singletonList("Verify current encounter requirements")));
                continue;
            }
            // Exact profiles are recomputed from current carried state so a
            // preparation -> ready transition cannot retain stale failures.
            if (exact == null && prior != null)
                missing.addAll(prior.getMissingRequirements());

            requireLevel(account, Skill.ATTACK, preparation.getAttack(), missing);
            requireLevel(account, Skill.STRENGTH, preparation.getStrength(), missing);
            requireLevel(account, Skill.DEFENCE, preparation.getDefence(), missing);
            requireLevel(account, Skill.RANGED, preparation.getRanged(), missing);
            requireLevel(account, Skill.MAGIC, preparation.getMagic(), missing);
            requireLevel(account, Skill.PRAYER, preparation.getPrayer(), missing);
            requireLevel(account, Skill.SLAYER, preparation.getSlayer(), missing);

            if (preparation.getRequiredQuest() != null
                    && !questUsable(quests, preparation.getRequiredQuest(),
                    preparation.isQuestMayBeInProgress()))
            {
                missing.add("Quest/access: " + preparation.getRequiredQuest());
            }

            String requiredStyle = exact == null ? preparation.getPreferredStyle()
                    : exact.getWeaponStyle();
            if (!hasCombatWeapon(equipment, requiredStyle))
                addMissing(missing, "Equip a usable " + preparation.getPreferredStyle() + " combat weapon/loadout");
            else if (exact == null)
                addMissing(missing, Text.get(443));

            int requiredFood = exact == null ? (preparation.isRequiresSupplies() ? 5 : 0)
                    : exact.getMinimumFood();
            if (carriedFoodQuantity(inventory) < requiredFood)
                addMissing(missing, Text.get(445));
            int requiredRestore = exact == null ? (preparation.getPrayer() >= 43 ? 1 : 0)
                    : exact.getMinimumRestoration();
            if (carriedRestorationQuantity(inventory) < requiredRestore)
                addMissing(missing, Text.get(446));
            if (usesRanged(preparation.getPreferredStyle()))
                addMissing(missing, carriedAmmoQuantity(inventory, equipment) > 0
                        ? Text.get(447)
                        : Text.get(448));
            if (usesMagic(preparation.getPreferredStyle()))
                addMissing(missing, runeEvidence(inventory, storage)
                        ? Text.get(449)
                        : Text.get(450));

            if (exact == null)
                addMissing(missing, Text.get(451));
            else
                for (String accessItem : exact.getAccessItems())
                    if (carriedQuantity(inventory, accessItem.toLowerCase(Locale.ROOT)) < 1)
                        addMissing(missing, "Carry " + accessItem + " for encounter access");

            if (exact == null && preparation != null)
                for (String check : preparation.getChecks()) addMissing(missing, check);

            if (mode == AccountMode.ULTIMATE_IRONMAN && preparation.isRequiresSupplies())
                addMissing(missing, Text.get(452));

            boolean fullyVerified = exact != null && missing.isEmpty();
            result.put(activity.getId(), new PvmReadiness(
                    activity.getId(),
                    fullyVerified,
                    fullyVerified ? RecommendationConfidence.VERIFIED
                            : RecommendationConfidence.CHECK_NEEDED,
                    missing
            ));
        }
        return new PvmSnapshot(result);
    }


    private static PvmReadiness priorFor(PvmSnapshot observed, String activityId)
    {
        if (observed == null || activityId == null) return null;
        PvmReadiness prior = observed.readinessFor(activityId);
        if (prior != null) return prior;
        String shortId = activityId.startsWith("pvm:")
                ? activityId.substring("pvm:".length()) : activityId;
        return observed.readinessFor(shortId);
    }

    private static void requireLevel(AccountSnapshot account, Skill skill,
            int required, List<String> missing)
    {
        if (required > 1 && account.getSkillLevel(skill) < required)
            missing.add(skill.getName() + " " + required);
    }

    private static boolean questUsable(QuestSnapshot quests, String quest,
            boolean inProgressAllowed)
    {
        if (quests == null || quest == null) return false;
        for (Map.Entry<String, QuestStatus> entry : quests.getQuests().entrySet())
        {
            if (!entry.getKey().equalsIgnoreCase(quest)) continue;
            if (entry.getValue() == QuestStatus.COMPLETE) return true;
            return inProgressAllowed && entry.getValue() == QuestStatus.IN_PROGRESS;
        }
        return false;
    }

    private static boolean hasCombatWeapon(EquipmentSnapshot equipment, String style)
    {
        List<ItemStackSnapshot> items = new ArrayList<>();
        if (equipment != null) items.addAll(equipment.getEquippedItems());
        boolean melee = false, ranged = false, magic = false;
        for (ItemStackSnapshot item : items)
        {
            // Persisted snapshots without slot provenance cannot prove a readied weapon.
            if (item.getSlotIndex() != EquipmentInventorySlot.WEAPON.getSlotIdx()) continue;
            String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
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

    private static int carriedFoodQuantity(InventorySnapshot inventory)
    {
        return carriedQuantity(inventory, "shark", "karambwan", "anglerfish",
                "manta ray", "moonlight antelope", "lobster", "swordfish", "pizza");
    }

    private static int carriedRestorationQuantity(InventorySnapshot inventory)
    {
        return carriedQuantity(inventory, "prayer potion", "super restore");
    }

    private static int carriedAmmoQuantity(InventorySnapshot inventory,
            EquipmentSnapshot equipment)
    {
        int quantity = carriedQuantity(inventory, "arrow", "bolt", "dart",
                "javelin", "chinchompa");
        if (equipment != null)
            for (ItemStackSnapshot item : equipment.getEquippedItems())
                if (item.getSlotIndex() == EquipmentInventorySlot.AMMO.getSlotIdx()
                        && containsAny(item.getName() == null ? ""
                        : item.getName().toLowerCase(Locale.ROOT),
                        "arrow", "bolt", "dart", "javelin"))
                    quantity += item.getQuantity();
        return quantity;
    }

    private static int carriedRuneQuantity(InventorySnapshot inventory)
    {
        return carriedQuantity(inventory, " rune");
    }

    private static boolean runeEvidence(InventorySnapshot inventory,
            StorageSnapshot storage)
    {
        if (carriedRuneQuantity(inventory) > 0) return true;
        return storage != null
                && storage.verified(StorageCapability.RUNE_POUCH)
                && storage.hasObservedContents(StorageCapability.RUNE_POUCH)
                && !storage.contentsOf(StorageCapability.RUNE_POUCH).isEmpty();
    }

    private static void addMissing(List<String> missing, String requirement)
    {
        if (requirement != null && !requirement.trim().isEmpty()
                && !missing.contains(requirement)) missing.add(requirement);
    }

    private static int carriedQuantity(InventorySnapshot inventory, String... terms)
    {
        if (inventory == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            String name = item.getName() == null ? "" : item.getName().toLowerCase(Locale.ROOT);
            if (containsAny(name, terms)) total += Math.max(0, item.getQuantity());
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

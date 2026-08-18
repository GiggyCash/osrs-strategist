package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Conservative first-pass readiness for every boss identity RuneLite exposes.
 * Exact encounter modules can override these broad floors with better evidence.
 */
@Singleton
public class PvmReadinessAnalyzer
{
    private final PvmActivityCatalog catalog;

    @Inject
    public PvmReadinessAnalyzer(PvmActivityCatalog catalog)
    {
        this.catalog = catalog;
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
            ReadinessFloor floor = floorFor(activity);
            List<String> missing = new ArrayList<>();
            if (prior != null) missing.addAll(prior.getMissingRequirements());

            requireLevel(account, Skill.ATTACK, floor.attack, missing);
            requireLevel(account, Skill.STRENGTH, floor.strength, missing);
            requireLevel(account, Skill.DEFENCE, floor.defence, missing);
            requireLevel(account, Skill.RANGED, floor.ranged, missing);
            requireLevel(account, Skill.MAGIC, floor.magic, missing);
            requireLevel(account, Skill.PRAYER, floor.prayer, missing);
            requireLevel(account, Skill.SLAYER, floor.slayer, missing);

            if (floor.requiredQuest != null
                    && !questUsable(quests, floor.requiredQuest,
                    floor.questMayBeInProgress))
            {
                missing.add("Quest/access: " + floor.requiredQuest);
            }

            if (!hasCombatWeapon(equipment, floor.preferredStyle))
                addMissing(missing, "Equip a usable " + floor.preferredStyle + " combat weapon/loadout");
            else
                addMissing(missing, "Verify the observed weapon is in the weapon slot and is suitable for this encounter");

            if (floor.requiresSupplies && carriedFoodQuantity(inventory) < 5)
                addMissing(missing, "Carry more than a token food item and verify an encounter-appropriate healing supply");
            if (floor.prayer >= 43 && carriedRestorationQuantity(inventory) < 1)
                addMissing(missing, "Carry a recognised prayer-restoration item and verify its usable doses");
            if (usesRanged(floor.preferredStyle))
                addMissing(missing, carriedAmmoQuantity(inventory) > 0
                        ? "Verify the carried ammunition or charges are compatible with the equipped ranged weapon"
                        : "Carry and verify ammunition or charges compatible with the equipped ranged weapon");
            if (usesMagic(floor.preferredStyle))
                addMissing(missing, runeEvidence(inventory, storage)
                        ? "Verify the observed inventory/rune-pouch runes satisfy the selected spell and current spellbook"
                        : "Carry the required rune combination and verify the selected spell and current spellbook");

            addMissing(missing, "Verify encounter-specific protection items, prayers, charges, and access requirements");

            if (mode == AccountMode.ULTIMATE_IRONMAN && floor.requiresSupplies)
                addMissing(missing, "Verify UIM inventory layout, retrieval route, and safe death/storage state for this encounter");

            result.put(activity.getId(), new PvmReadiness(
                    activity.getId(),
                    false,
                    RecommendationConfidence.CHECK_NEEDED,
                    missing
            ));
        }
        return new PvmSnapshot(result);
    }

    private static ReadinessFloor floorFor(PvmActivityDefinition activity)
    {
        String name = activity.getId().substring("pvm:".length())
                .toUpperCase(Locale.ROOT);

        if ("BRUTUS".equals(name))
            return floor(20, 20, 10, 1, 1, 1, 1, "melee", "The Ides of Milk", false, true);
        if ("OBOR".equals(name) || "BRYOPHYTA".equals(name))
            return floor(40, 40, 40, 1, 1, 31, 1, "melee", null, false, true);
        if ("BARROWS_CHESTS".equals(name))
            return floor(1, 1, 40, 1, 50, 43, 1, "magic", "Priest in Peril", false, true);
        if ("SCURRIUS".equals(name))
            return floor(40, 40, 40, 1, 1, 31, 1, "melee", null, false, true);
        if ("GIANT_MOLE".equals(name))
            return floor(40, 40, 40, 1, 1, 43, 1, "melee", null, false, true);
        if ("SARACHNIS".equals(name))
            return floor(60, 60, 60, 1, 1, 43, 1, "melee", null, false, true);
        if ("HESPORI".equals(name))
            return floor(50, 50, 50, 1, 1, 43, 1, "melee", null, false, true);
        if ("ZULRAH".equals(name))
            return floor(1, 1, 60, 75, 75, 43, 1, "ranged or magic", "Regicide", true, true);
        if ("VORKATH".equals(name))
            return floor(1, 1, 70, 75, 1, 43, 1, "ranged", "Dragon Slayer II", false, true);
        if (name.contains("GAUNTLET"))
            return floor(75, 75, 70, 75, 75, 43, 1, "hybrid", "Song of the Elves", false, false);
        if (name.startsWith("TOMBS_OF_AMASCUT"))
            return floor(75, 75, 70, 75, 75, 70, 1, "hybrid", "Beneath Cursed Sands", false, true);
        if (name.startsWith("CHAMBERS_OF_XERIC"))
            return floor(75, 75, 70, 75, 75, 70, 1, "hybrid", null, false, true);
        if (name.startsWith("THEATRE_OF_BLOOD"))
            return floor(85, 85, 75, 85, 85, 70, 1, "hybrid", "A Taste of Hope", false, true);
        if ("ALCHEMICAL_HYDRA".equals(name))
            return floor(1, 1, 70, 85, 1, 70, 95, "ranged", null, false, true);
        if ("CERBERUS".equals(name))
            return floor(75, 75, 70, 1, 1, 70, 91, "melee", null, false, true);
        if ("ARAXXOR".equals(name))
            return floor(80, 80, 70, 1, 1, 70, 92, "melee", null, false, true);
        if ("KRAKEN".equals(name))
            return floor(1, 1, 60, 1, 75, 43, 87, "magic", null, false, true);
        if ("TZTOK_JAD".equals(name))
            return floor(1, 1, 60, 70, 1, 43, 1, "ranged", null, false, true);
        if ("TZKAL_ZUK".equals(name))
            return floor(1, 1, 85, 92, 75, 77, 1, "ranged", null, false, true);
        if ("SOL_HEREDIT".equals(name))
            return floor(85, 85, 85, 85, 85, 77, 1, "hybrid", null, false, true);
        if ("NEX".equals(name))
            return floor(1, 1, 80, 90, 1, 70, 1, "ranged", null, false, true);
        if (name.contains("GENERAL_GRAARDOR") || name.contains("KREEARRA")
                || name.contains("COMMANDER_ZILYANA") || name.contains("KRIL_TSUTSAROTH"))
            return floor(70, 70, 70, 75, 70, 70, 1, "hybrid", null, false, true);
        if (name.contains("DUKE_SUCELLUS") || name.contains("LEVIATHAN")
                || name.contains("VARDORVIS") || name.contains("WHISPERER"))
            return floor(80, 80, 75, 80, 80, 70, 1, "hybrid",
                    "Desert Treasure II - The Fallen Empire", false, true);
        if (activity.isRaid() || activity.getRiskLevel() == RiskLevel.HIGH)
            return floor(80, 80, 75, 80, 80, 70, 1, "hybrid", null, false, true);

        return floor(60, 60, 60, 60, 60, 43, 1, "combat", null, false, true);
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

    private static int carriedAmmoQuantity(InventorySnapshot inventory)
    {
        return carriedQuantity(inventory, "arrow", "bolt", "dart", "javelin", "chinchompa");
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

    private static ReadinessFloor floor(int attack, int strength, int defence,
            int ranged, int magic, int prayer, int slayer, String style,
            String quest, boolean questMayBeInProgress, boolean supplies)
    {
        return new ReadinessFloor(attack, strength, defence, ranged, magic,
                prayer, slayer, style, quest, questMayBeInProgress, supplies);
    }

    private static final class ReadinessFloor
    {
        private final int attack, strength, defence, ranged, magic, prayer, slayer;
        private final String preferredStyle;
        private final String requiredQuest;
        private final boolean questMayBeInProgress;
        private final boolean requiresSupplies;

        private ReadinessFloor(int attack, int strength, int defence, int ranged,
                int magic, int prayer, int slayer, String preferredStyle,
                String requiredQuest, boolean questMayBeInProgress,
                boolean requiresSupplies)
        {
            this.attack = attack;
            this.strength = strength;
            this.defence = defence;
            this.ranged = ranged;
            this.magic = magic;
            this.prayer = prayer;
            this.slayer = slayer;
            this.preferredStyle = preferredStyle;
            this.requiredQuest = requiredQuest;
            this.questMayBeInProgress = questMayBeInProgress;
            this.requiresSupplies = requiresSupplies;
        }
    }
}

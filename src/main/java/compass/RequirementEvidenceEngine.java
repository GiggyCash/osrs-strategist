package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/** Resolves method requirements from live access, account and item evidence. */
@Singleton
public class RequirementEvidenceEngine
{
    private final FarmingAccessEvaluator farmingAccess;
    private final AgilityAccessEvaluator agilityAccess;
    private final FarmingSupplyCatalog farmingSupplies;
    private final RunecraftSupplyCatalog runecraftSupplies;

    @Inject
    public RequirementEvidenceEngine(FarmingAccessEvaluator farmingAccess,
            AgilityAccessEvaluator agilityAccess,
            FarmingSupplyCatalog farmingSupplies,
            RunecraftSupplyCatalog runecraftSupplies)
    {
        this.farmingAccess = farmingAccess;
        this.agilityAccess = agilityAccess;
        this.farmingSupplies = farmingSupplies;
        this.runecraftSupplies = runecraftSupplies;
    }

    public RequirementEvidenceEngine(FarmingAccessEvaluator farming,
            AgilityAccessEvaluator agility)
    {
        this(farming, agility, new FarmingSupplyCatalog(),
                new RunecraftSupplyCatalog());
    }

    public RequirementEvidenceEngine(FarmingAccessEvaluator farming)
    {
        this(farming, null);
    }

    public List<RequirementCheck> evaluate(GameData data, TrainingMethod method)
    {
        return evaluate(data, method, false);
    }

    public List<RequirementCheck> evaluate(GameData data, TrainingMethod method,
            boolean group)
    {
        if (method == null) return new ArrayList<>();
        var id = method.getId();
        var skill = method.getSkill();
        var items = new ItemIndex(data, group);
        if (skill == Skill.FARMING) return farming(data, method, items);
        if (skill == Skill.AGILITY && agilityAccess != null)
            return agility(data, id);
        if (skill == Skill.SAILING) return sailing(data, id, items);
        if (skill == Skill.RUNECRAFT && runecraftSupplies.supports(id))
            return runecraft(id, items);
        if (skill == Skill.MAGIC && (id.equals("magic_f2p_combat")
                || id.equals("magic_f2p_fire_bolt")
                || id.equals("magic_f2p_fire_blast")
                || id.equals("magic_f2p_fire_strike_splash")
                || id.equals("magic_f2p_curse"))) return magic(data, id, items);
        if (skill == Skill.COOKING && id.equals("cooking_f2p_fish"))
            return cookedFish(data, items);
        if (skill == Skill.COOKING && (id.equals("cooking_hosidius")
                || id.equals("cooking_wines"))) return cooking(data, id, items);
        if (skill == Skill.FISHING)
        {
            var result = fishing(data, id, items);
            if (result != null) return result;
        }
        if (skill == Skill.HUNTER)
        {
            var result = hunter(data, id, items);
            if (result != null) return result;
        }
        if (id.equals("runecraft_gotr"))
        {
            List<RequirementCheck> result = quest(data, "Temple of the Eye",
                    "quest:temple_of_the_eye");
            result.add(tool(items, ItemRequirementClass.PICKAXE,
                    "resource:gotr_pickaxe", "Usable pickaxe"));
            result.add(item(items, "resource:gotr_chisel", "Chisel", 1,
                    ItemID.CHISEL));
            return result;
        }
        if (id.equals("runecraft_zmi")) return list(item(items,
                "resource:zmi_pure_essence", "Pure essence", 1,
                ItemID.BLANKRUNE_HIGH));
        if (id.equals("construction_crude_chairs"))
            return construction(data, items, false);
        if (id.equals("construction_oak_larders"))
            return construction(data, items, true);
        if (skill == Skill.MINING || skill == Skill.WOODCUTTING)
        {
            ItemRequirementClass type = skill == Skill.MINING
                    ? ItemRequirementClass.PICKAXE : ItemRequirementClass.AXE;
            List<RequirementCheck> result = list(tool(items, type,
                    skill == Skill.MINING ? "resource:usable-pickaxe"
                            : "resource:usable-axe",
                    skill == Skill.MINING ? "Usable pickaxe" : "Usable axe"));
            addGeneric(result, method);
            return result;
        }
        List<RequirementCheck> result = new ArrayList<>();
        addGeneric(result, method);
        return result;
    }

    private static List<RequirementCheck> magic(GameData data, String id,
            ItemIndex items)
    {
        var combat = data == null ? null : data.combatEvidence();
        var observed = combat != null;
        List<RequirementCheck> result = list(new RequirementCheck(
                "spellbook:standard", get(1534), observed
                        ? combat.getSpellbookSelector() == 0
                        ? RequirementState.VERIFIED : RequirementState.BLOCKED
                        : RequirementState.CHECK_NEEDED,
                !observed ? get(613) : combat.getSpellbookSelector() == 0
                        ? get(1535) : get(624)));
        if (id.equals("magic_f2p_curse"))
        {
            result.add(item(items, "resource:curse_body", "Body rune", 1,
                    ItemID.BODYRUNE));
            result.add(item(items, "resource:curse_earth", "Earth runes", 3,
                    ItemID.EARTHRUNE));
            result.add(item(items, "resource:curse_water", "Water runes", 2,
                    ItemID.WATERRUNE));
            result.add(splashing(data));
            return result;
        }
        var splash = id.equals("magic_f2p_fire_strike_splash");
        int air = id.equals("magic_f2p_fire_blast") ? 4
                : id.equals("magic_f2p_fire_bolt") ? 3 : splash ? 2 : 1;
        result.add(item(items, "resource:combat_magic_air", get(1536),
                air, ItemID.AIRRUNE));
        if (splash)
        {
            result.add(item(items, "resource:splash_fire", "Fire runes", 3,
                    ItemID.FIRERUNE));
            result.add(item(items, "resource:splash_mind", "Mind rune", 1,
                    ItemID.MINDRUNE));
            result.add(splashing(data));
        }
        else if (id.equals("magic_f2p_fire_bolt")
                || id.equals("magic_f2p_fire_blast"))
        {
            var blast = id.endsWith("blast");
            result.add(item(items, "resource:combat_magic_fire", get(1537),
                    blast ? 5 : 4, ItemID.FIRERUNE));
            result.add(item(items, "resource:combat_magic_catalytic",
                    get(1538), 1, blast ? ItemID.DEATHRUNE : ItemID.CHAOSRUNE));
        }
        else result.add(item(items, "resource:wind_strike_mind", "Mind rune",
                1, ItemID.MINDRUNE));
        return result;
    }

    private static RequirementCheck splashing(GameData data)
    {
        var equipment = data == null ? null : data.equipment();
        boolean helm = false, body = false, legs = false, shield = false,
                boots = false, staff = false;
        if (equipment != null) for (ItemState item : equipment.getEquippedItems())
        {
            if (item == null || item.getName() == null || item.getQuantity() <= 0)
                continue;
            var name = item.getName().toLowerCase(Locale.ROOT);
            helm |= metal(name, "full helm");
            body |= metal(name, "platebody");
            legs |= metal(name, "platelegs") || metal(name, "plateskirt");
            shield |= metal(name, "kiteshield");
            boots |= name.equals("fancy boots") || name.equals("fighting boots")
                    || name.equals("decorative boots");
            staff |= name.equals(get(1542));
        }
        var ready = helm && body && legs && shield && boots && staff;
        return new RequirementCheck("equipment:f2p_splashing", get(661),
                ready ? RequirementState.VERIFIED : RequirementState.CHECK_NEEDED,
                get(ready ? 614 : 615));
    }

    private static boolean metal(String name, String piece)
    {
        if (!name.endsWith(piece)) return false;
        for (String metal : new String[]{"bronze ", "iron ", "steel ", "black ",
                "mithril ", "adamant ", "rune ", "gilded "})
            if (name.startsWith(metal)) return true;
        return false;
    }

    private static List<RequirementCheck> sailing(GameData data, String id,
            ItemIndex items)
    {
        List<RequirementCheck> result = quest(data, "Pandemonium",
                "quest:pandemonium");
        if (id.equals("sailing_courier"))
        {
            var sailing = data == null ? null : data.sailing();
            boolean route = sailing != null
                    && sailing.hasPort(SailingSnapshot.PORT_SARIM)
                    && sailing.hasPort(SailingSnapshot.PORT_PANDEMONIUM)
                    && sailing.hasActivity(SailingSnapshot.ACTIVITY_COURIER);
            result.add(state("sailing:courier-route", get(1539), route,
                    get(635), get(646)));
            result.add(item(items, "resource:captains-log", "Captain's log", 1,
                    ItemID.SAILING_LOG_INITIAL, ItemID.SAILING_LOG));
        }
        else if (id.equals("sailing_charting")) result.add(new RequirementCheck(
                "sailing:uncompleted-chart", get(1540),
                RequirementState.CHECK_NEEDED, get(657)));
        else if (id.startsWith("sailing_barracuda_"))
        {
            if (id.contains("jubbly")) result.addAll(quest(data, get(1541),
                    "quest:zogre-flesh-eaters"));
            if (id.contains("gwenith")) result.addAll(quest(data, "Regicide",
                    "quest:regicide"));
            result.add(new RequirementCheck("preparation:sailing-trial-boat",
                    "Trial-ready boat", RequirementState.CHECK_NEEDED,
                    get(658)));
        }
        else result.add(new RequirementCheck("sailing:live-route", get(659),
                    RequirementState.CHECK_NEEDED, get(660)));
        return result;
    }

    private static List<RequirementCheck> cookedFish(GameData data,
            ItemIndex items)
    {
        int level = data == null || data.account() == null ? 1
                : data.account().level(Skill.COOKING);
        int[] ids = {ItemID.RAW_SHRIMP, ItemID.RAW_SARDINE, ItemID.RAW_HERRING,
                ItemID.RAW_TROUT, ItemID.RAW_PIKE, ItemID.RAW_SALMON,
                ItemID.RAW_TUNA, ItemID.RAW_LOBSTER, ItemID.RAW_SWORDFISH};
        int[] levels = {1, 1, 5, 15, 20, 25, 30, 40, 45};
        var count = 0;
        for (int required : levels) if (level >= required) count++;
        return list(item(items, "resource:raw_fish", get(1543), 1,
                Arrays.copyOf(ids, count)));
    }

    private static List<RequirementCheck> cooking(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals("cooking_wines")) return list(
                item(items, "resource:wine_grapes", "Grapes", 1, ItemID.GRAPES),
                item(items, "resource:wine_water", "Jug of water", 1,
                        ItemID.JUG_WATER));
        var result = cookedFish(data, items);
        var diaries = data == null ? null : data.diaries();
        boolean ready = diaries != null
                && diaries.isTierComplete("Kourend & Kebos", DiaryTier.EASY);
        result.add(state("access:hosidius_kitchen", get(1544), ready,
                get(616), get(617)));
        return result;
    }

    private static List<RequirementCheck> fishing(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals("fishing_lumbridge_shrimps")
                || id.equals("fishing_tempoross")) return new ArrayList<>();
        if (id.equals("fishing_f2p_fly")) return list(
                item(items, "resource:fly_rod", "Fly fishing rod", 1,
                        ItemID.FLY_FISHING_ROD),
                item(items, "resource:fly_feathers", "Feathers", 1,
                        ItemID.FEATHER));
        if (!id.equals("fishing_karambwan")) return null;
        var quests = data == null ? null : data.quests();
        var trio = complete(quests, get(1545));
        var transport = data == null ? null : data.transport();
        boolean observedRoute = transport != null
                && transport.hasVerifiedRoute("fairy-rings");
        var fairy = observedRoute || complete(quests, get(1547));
        var diaries = data == null ? null : data.diaries();
        boolean staffless = diaries != null && diaries.isTierComplete(
                get(1152), DiaryTier.ELITE);
        boolean staff = observedRoute || staffless || items.quantity(
                ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF) > 0;
        return list(state("quest:tai_bwo_wannai_trio", get(1546), trio,
                        get(618), get(619)),
                state("transport:fairy-rings", get(1548), fairy,
                        get(620), get(621)),
                state("resource:fairy_ring_staff", get(622), staff,
                        get(623), get(625)),
                item(items, "resource:karambwan_vessel", "Karambwan vessel", 1,
                        ItemID.TBWT_KARAMBWAN_VESSEL,
                        ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI),
                item(items, "resource:karambwanji", get(1549), 1,
                        ItemID.TBWT_RAW_KARAMBWANJI,
                        ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI));
    }

    private static List<RequirementCheck> hunter(GameData data, String id,
            ItemIndex items)
    {
        if (id.equals("hunter_falconry")) return list(item(items,
                "resource:falcon_rental", get(1550), 500, ItemID.COINS));
        if (id.equals("hunter_bird_traps")) return list(item(items,
                "resource:bird_snare", "Bird snare", 1, ItemID.HUNTING_SNARE));
        if (!id.equals("hunter_herbiboar")) return null;
        int herblore = data == null || data.account() == null ? 1
                : data.account().level(Skill.HERBLORE);
        var voyage = complete(data == null ? null : data.quests(), "Bone Voyage");
        return list(new RequirementCheck("skill:herbiboar_herblore",
                        "31 Herblore", herblore >= 31 ? RequirementState.VERIFIED
                        : RequirementState.BLOCKED, get(1551) + herblore + "."),
                state("quest:bone_voyage", get(1552), voyage,
                        get(626), get(627)));
    }

    private static List<RequirementCheck> construction(GameData data,
            ItemIndex items, boolean oak)
    {
        var poh = data == null ? null : data.poh();
        CapabilityState house = poh == null ? CapabilityState.UNKNOWN
                : poh.getHouseAccess();
        CapabilityState room = poh == null ? CapabilityState.UNKNOWN
                : poh.furnitureState(oak ? "room:kitchen" : "room:parlour");
        List<RequirementCheck> result = list(
                capability("construction:poh", get(1554), house,
                        get(oak ? 631 : 629)),
                capability(oak ? "construction:kitchen" : "construction:parlour",
                        oak ? "POH Kitchen" : "POH Parlour", room,
                        get(oak ? 632 : 630)),
                item(items, oak ? "resource:construction_oak_planks"
                        : "resource:construction_planks",
                        oak ? "Oak planks" : "Planks", oak ? 8 : 2,
                        oak ? ItemID.PLANK_OAK : ItemID.WOODPLANK));
        if (!oak) result.add(item(items, "resource:construction_nails", "Nails",
                2, ItemID.NAILS_BRONZE, ItemID.NAILS_IRON, ItemID.NAILS,
                ItemID.NAILS_BLACK, ItemID.NAILS_MITHRIL, ItemID.NAILS_ADAMANT,
                ItemID.NAILS_RUNE));
        result.add(item(items, "resource:construction_hammer", "Hammer", 1,
                ItemID.HAMMER));
        result.add(item(items, "resource:construction_saw", "Saw", 1,
                ItemID.POH_SAW, ItemID.EYEGLO_CRYSTAL_SAW, ItemID.WEARABLE_SAW));
        return result;
    }

    private List<RequirementCheck> runecraft(String id, ItemIndex items)
    {
        List<RequirementCheck> result = list(items.check(
                runecraftSupplies.runeEssence()));
        var entry = runecraftSupplies.altarEntryFor(id);
        if (entry != null) result.add(items.check(entry));
        return result;
    }

    private List<RequirementCheck> agility(GameData data, String id)
    {
        if (id.equals("agility_wilderness")) return list(
                agilityAccess.wildernessCourseCheck(data), new RequirementCheck(
                        "agility:wilderness_risk", get(1556),
                        RequirementState.VERIFIED, get(636)));
        return list(agilityAccess.courseCheck(data,
                agilityAccess.bestStandardCourse(data)));
    }

    private List<RequirementCheck> farming(GameData data, TrainingMethod method,
            ItemIndex items)
    {
        var id = method.getId();
        var account = data == null ? null : data.account();
        var farming = data == null ? null : data.farming();
        var level = account == null ? 1 : account.level(Skill.FARMING);
        if (id.equals("farming_early"))
        {
            var patch = farmingAccess.firstReachablePatchName(farming);
            return list(new RequirementCheck("farming:reachable_patch",
                    get(1557), patch == null ? RequirementState.CHECK_NEEDED
                    : RequirementState.VERIFIED, patch == null ? get(637)
                    : patch + get(638)), new RequirementCheck(
                    "farming:supplies", get(1558),
                    RequirementState.CHECK_NEEDED, get(639)));
        }
        if (id.equals("farming_falador_potatoes")
                || id.equals("farming_falador_watermelons"))
        {
            var watermelon = id.endsWith("watermelons");
            var reachable = farming != null && farming.isPatchReachable("falador");
            RequirementCheck seeds = items.check(watermelon
                    ? farmingSupplies.watermelonSeeds()
                    : farmingSupplies.potatoSeeds());
            List<RequirementCheck> result = list(state("farming:falador_patch",
                    get(1559), reachable, get(640), get(641)), seeds);
            if (watermelon && seeds.getState() != RequirementState.VERIFIED
                    && (account == null || account.level(Skill.THIEVING) < 38))
                result.add(new RequirementCheck("farming:watermelon_seed_source",
                        get(642), RequirementState.BLOCKED, get(643)));
            result.add(farmingTool(items, farming, farmingSupplies.rake(),
                    "rake", get(644)));
            result.add(farmingTool(items, farming, farmingSupplies.dibber(),
                    "dibber", get(645)));
            result.add(farmingTool(items, farming, farmingSupplies.spade(),
                    "spade", get(647)));
            return result;
        }
        if (id.equals("farming_tithe"))
        {
            int cans = items.quantity(ItemID.WATERING_CAN_1, ItemID.WATERING_CAN_2,
                    ItemID.WATERING_CAN_3, ItemID.WATERING_CAN_4,
                    ItemID.WATERING_CAN_5, ItemID.WATERING_CAN_6,
                    ItemID.WATERING_CAN_7, ItemID.WATERING_CAN_8);
            var water = cans >= 8 || items.quantity(ItemID.ZEAH_WATERINGCAN) > 0;
            return list(items.check(farmingSupplies.spade()),
                    items.check(farmingSupplies.dibber()),
                    state("resource:tithe_watering", get(648), water,
                            get(649), get(650)));
        }
        if (id.equals("farming_herbs") || id.equals("farming_herbs_expanded"))
        {
            var patch = farmingAccess.firstReachableHerbPatchName(farming);
            return list(new RequirementCheck("farming:level_9", "9 Farming",
                            level >= 9 ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED, get(1560) + level + "."),
                    new RequirementCheck("farming:herb_patch", get(1561),
                            patch == null ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED, patch == null
                            ? get(651) : patch + get(652)),
                    items.check(farmingSupplies.herbSeedsForLevel(level)),
                    farmingTool(items, farming, farmingSupplies.rake(), "rake",
                            get(653)),
                    farmingTool(items, farming, farmingSupplies.dibber(), "dibber",
                            get(654)),
                    farmingTool(items, farming, farmingSupplies.spade(), "spade",
                            get(655)));
        }
        List<RequirementCheck> result = new ArrayList<>();
        addGeneric(result, method);
        return result;
    }

    private static RequirementCheck farmingTool(ItemIndex items,
            FarmingSnapshot farming, ResourceRequirement need, String tool,
            String evidence)
    {
        if (farming != null
                && farming.leprechaunToolState(tool) == CapabilityState.VERIFIED)
            return new RequirementCheck(need.getId(), need.getLabel(),
                    RequirementState.VERIFIED, evidence == null
                    ? get(1569) : evidence);
        return items.check(need);
    }

    private static RequirementCheck item(ItemIndex items, String id,
            String label, int quantity, int... itemIds)
    {
        return items.check(new ResourceRequirement(id, label, quantity, itemIds));
    }

    private static RequirementCheck tool(ItemIndex items,
            ItemRequirementClass type, String id, String label)
    {
        var ready = items.quantityMatching(type, Collections.emptyList()) > 0;
        return new RequirementCheck(id, label, ready ? RequirementState.VERIFIED
                : RequirementState.CHECK_NEEDED, ready ? label + get(633)
                : "No " + label.toLowerCase() + get(634));
    }

    private static RequirementCheck state(String id, String label,
            boolean ready, String yes, String no)
    {
        return new RequirementCheck(id, label, ready ? RequirementState.VERIFIED
                : RequirementState.CHECK_NEEDED, ready ? yes : no);
    }

    private static RequirementCheck capability(String id, String label,
            CapabilityState value, String unknown)
    {
        var ready = value == CapabilityState.VERIFIED;
        return state(id, label, ready, label + get(1555), unknown);
    }

    private static List<RequirementCheck> quest(GameData data, String name,
            String id)
    {
        var ready = complete(data == null ? null : data.quests(), name);
        return list(state(id, name + " completed", ready,
                name + get(628), name + get(1553)));
    }

    private static boolean complete(QuestSnapshot quests, String name)
    {
        return quests != null && quests.statusOf(name) == QuestStatus.COMPLETE;
    }

    @SafeVarargs
    private static <T> List<T> list(T... values)
    {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static void addGeneric(List<RequirementCheck> result,
            TrainingMethod method)
    {
        for (String value : method.getRequirements()) result.add(
                new RequirementCheck("generic:" + value, value,
                        RequirementState.CHECK_NEEDED, get(656)));
    }
}

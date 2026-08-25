package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Structured high-value transport systems audited against current quest evidence. */
@Singleton
public final class TransportCatalog
{
    public static final String PROVENANCE =
            "RuneLite 1.12.35 quest/teleport identities and maintained current-live audit 2026-08-25";
    private final Map<String, TransportDefinition> routes = new LinkedHashMap<>();

    public TransportCatalog()
    {
        add("fairy-rings", "Fairy ring network", TransportCategory.FAIRY_RING,
                true, "Fairytale II - Cure a Queen", true, null, 0,
                "Carry a dramen or lunar staff unless the staff-free diary unlock is observed",
                null, false, "quest routes", "Slayer locations", "clues", "skilling routes");
        add("spirit-trees", "Spirit tree network", TransportCategory.SPIRIT_TREE,
                true, "Tree Gnome Village", false, null, 0,
                "Verify each destination node needed by the route", null, false,
                "gnome quest chain", "Grand Exchange", "Tree Gnome Stronghold", "Battlefield of Khazard");
        add("gnome-gliders", "Gnome glider network", TransportCategory.GNOME,
                true, "The Grand Tree", false, null, 0,
                "Verify the destination is unlocked and reachable", null, false,
                "Karamja", "White Wolf Mountain", "Al Kharid", "Feldip Hills");
        add("gnome-stronghold-transport", "Gnome stronghold local transport",
                TransportCategory.GNOME, true, "The Grand Tree", false, null, 0,
                "Verify the local tree/glider route", null, false,
                "Grand Tree", "Agility routes", "gnome quest starts");
        add("minecart-networks", "Minecart networks", TransportCategory.MINECART,
                true, null, false, null, 0,
                "Verify the specific minecart network, fare, and destination unlock",
                null, false, "Keldagrim", "Kebos and Kourend", "mines", "quest routes");
        add("scheduled-boats", "Scheduled ferries and boats", TransportCategory.BOAT,
                false, null, false, null, 0,
                "Verify the departure, fare, quest access, and destination",
                null, false, "Karamja", "Entrana", "fishing platforms", "islands");
        add("charter-ships", "Charter ships", TransportCategory.CHARTER,
                true, null, false, null, 0,
                "Verify both charter ports are accessible and carry the current fare",
                null, false, "port network", "quest routing", "shop access", "clues");
        add("ectophial", "Ectophial", TransportCategory.ITEM_TELEPORT,
                true, "Ghosts Ahoy", false, null, 0,
                "Carry the Ectophial and verify it is usable", null, false,
                "Port Phasmatys", "Morytania farming", "Slayer routes");
        add("standard-spellbook-teleports", "Standard spellbook teleports",
                TransportCategory.STANDARD_SPELL, false, null, false, Skill.MAGIC,
                1, "Verify the exact spell level, runes, quest and destination access",
                null, false, "cities", "POH", "quest routes", "clues");
        add("ancient-magicks-teleports", "Ancient Magicks teleports",
                TransportCategory.ANCIENT_SPELL, true, "Desert Treasure I", false,
                Skill.MAGIC, 54, "Use the Ancient spellbook and verify the exact spell/runes",
                null, true, "Wilderness routes", "ancient locations", "clues");
        add("lunar-teleports", "Lunar spellbook teleports",
                TransportCategory.LUNAR_SPELL, true, "Lunar Diplomacy", false,
                Skill.MAGIC, 65, "Use the Lunar spellbook and verify the exact spell/runes",
                null, false, "Lunar Isle", "group utility", "farming and quest routes");
        add("arceuus-teleports", "Arceuus spellbook teleports",
                TransportCategory.ARCEUUS_SPELL, true, null, false, Skill.MAGIC,
                60, "Verify Arceuus spellbook access, exact spell level and runes",
                null, false, "Kourend", "reanimation", "Barrows", "quest routes");
        add("games-necklace", "Games necklace", TransportCategory.JEWELLERY,
                true, null, false, null, 0, "Carry a charged games necklace",
                null, false, "Burthorpe", "Wintertodt", "Tears of Guthix", "Corporeal Beast cave");
        add("ring-of-dueling", "Ring of dueling", TransportCategory.JEWELLERY,
                true, null, false, null, 0, "Carry a charged ring of dueling",
                null, false, "Emir's Arena", "Castle Wars", "Ferox Enclave");
        add("amulet-of-glory", "Amulet of glory", TransportCategory.JEWELLERY,
                true, null, false, null, 0, "Carry a charged amulet of glory",
                null, false, "Edgeville", "Karamja", "Draynor", "Al Kharid");
        add("skills-necklace", "Skills necklace", TransportCategory.JEWELLERY,
                true, null, false, null, 0, "Carry a charged skills necklace",
                null, false, "skill guilds", "Farming Guild", "Woodcutting Guild");
        add("necklace-of-passage", "Necklace of passage", TransportCategory.JEWELLERY,
                true, null, false, null, 0, "Carry a charged necklace of passage",
                null, false, "Wizards' Tower", "The Outpost", "Eagles' Eyrie");
        add("slayer-ring", "Slayer ring", TransportCategory.SLAYER,
                true, null, false, Skill.CRAFTING, 75,
                "Verify Slayer reward unlock or an owned charged ring",
                null, false, "Slayer Tower", "Fremennik Slayer Dungeon", "Tarn's Lair", "Dark beasts");
        add("diary-equipment-teleports", "Achievement Diary equipment teleports",
                TransportCategory.DIARY, true, null, false, null, 0,
                "Verify the exact diary tier and owned reward item", null, false,
                "regional task routes", "farming", "Slayer", "clues");
        add("minigame-teleports", "Minigame grouping teleports",
                TransportCategory.MINIGAME, true, null, false, null, 0,
                "Verify the activity unlock, cooldown, and destination restrictions",
                null, false, "minigames", "quest routes", "regional travel");
        add("quest-item-teleports", "Quest item teleports", TransportCategory.QUEST,
                true, null, false, null, 0,
                "Verify the specific quest completion, item, charges, and destination",
                null, false, "quest hubs", "repeatable activities", "regional travel");
        add("poh-portal-nexus", "POH portal chamber or nexus", TransportCategory.POH,
                true, null, false, Skill.CONSTRUCTION, 50,
                "Verify the exact destination is installed", "portal-nexus", false,
                "spell destinations", "house-centred routes", "clues", "boss returns");
        add("poh-mounted-teleports", "POH mounted and jewellery teleports", TransportCategory.POH,
                true, null, false, Skill.CONSTRUCTION, 47,
                "Verify the exact mounted item or jewellery box tier", "mounted-teleports", false,
                "jewellery destinations", "quest routes", "skilling loops");
        add("poh-spirit-tree", "POH spirit tree", TransportCategory.POH,
                true, null, false, Skill.CONSTRUCTION, 75,
                "Verify the spirit tree is built; never infer it from Construction level",
                "spirit-tree", false, "spirit tree network", "house hub");
        add("poh-fairy-ring", "POH fairy ring", TransportCategory.POH,
                true, null, false, Skill.CONSTRUCTION, 80,
                "Verify the fairy ring is built; never infer it from Construction level",
                "fairy-ring", false, "fairy ring network", "house hub");
        add("sailing-ports-boats", "Sailing ports and player-owned boats", TransportCategory.SAILING,
                true, null, false, Skill.SAILING, 1,
                "Observe the current boat class, port discoveries, crew/cargo setup, and route before relying on Sailing transport",
                null, false, "current ports", "island access", "Sailing activities", "quest routes");
        liveAgilityShortcuts();
    }

    public List<TransportDefinition> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(routes.values()));
    }
    public TransportDefinition get(String id) { return routes.get(id); }

    private void liveAgilityShortcuts()
    {
        shortcut("draynor-manor-west-gap", "Draynor Manor west fence gap", 49,
                "Draynor routes", "manor clue and quest routing");
        shortcut("river-lum-champions-stones", "River Lum stepping stones south of the Champions' Guild", 52,
                "Champions' Guild routes", "east-west River Lum travel");
        shortcut("giants-plateau-gap", "Giant's Plateau south-west shortcut", 54,
                "desert access", "Giant's Plateau activities");
        shortcut("edgeville-dungeon-pipe", "Southern Edgeville Dungeon pipe", 60,
                "Edgeville Dungeon travel", "routes without a Brass key");
        shortcut("arceuus-library-middle-drop", "Arceuus Library middle-floor drop", 52,
                "Arceuus Library navigation", "library activity routes");
        shortcut("arceuus-library-top-drop", "Arceuus Library top-floor drop", 62,
                "Arceuus Library navigation", "library activity routes");
        shortcut("sophanem-river-elid-stones", "River Elid stepping stones north of Sophanem", 79,
                "Sophanem routes", "desert clue travel");
        shortcut("mos-le-harmless-island-stones", "Mos le'Harmless island stepping stones", 82,
                "Mos le'Harmless routes", "Treasure Trail travel");
        shortcut("pollnivneach-west-plateau", "Pollnivneach west plateau shortcut", 83,
                "Ali the Hag routes", "desert plateau travel");
        barehanded("river-lum-broken-raft", "River Lum broken raft barehanded shortcut", 48,
                "River Lum travel", "former grapple route");
        barehanded("falador-rough-wall", "Falador rough wall barehanded shortcut", 52,
                "Falador travel", "former grapple route");
        barehanded("catherby-taverley-rock-climb", "Catherby-Taverley two-way rock climb", 68,
                "Catherby travel", "Taverley travel");
        barehanded("water-obelisk-catherby-crossing", "Water Obelisk-Catherby barehanded crossing", 72,
                "Water Obelisk access", "Catherby travel");
        barehanded("yanille-rough-wall", "Yanille rough wall barehanded shortcut", 69,
                "Yanille travel", "former grapple route");
        barehanded("karamja-volcano-strong-trees", "Karamja Volcano strong-tree shortcut", 78,
                "Karamja Volcano travel", "former grapple route");
    }

    private void shortcut(String id, String name, int level, String... uses)
    {
        add(id, name, TransportCategory.AGILITY_SHORTCUT, true, null, false,
                Skill.AGILITY, level,
                "Verify live route access before relying on the shortcut",
                null, false, uses);
    }

    private void barehanded(String id, String name, int level, String... uses)
    {
        add(id, name, TransportCategory.AGILITY_SHORTCUT, true, null, false,
                Skill.AGILITY, level,
                "No grapple is needed for travel, but an Achievement Diary task may still require using one",
                null, false, uses);
    }

    private void add(String id, String name, TransportCategory category,
            boolean membersOnly, String quest, boolean questStartSuffices,
            Skill skill, int level, String check, String pohFurniture,
            boolean wilderness, String... uses)
    {
        TransportDefinition value = new TransportDefinition(id, name, category,
                membersOnly, quest, questStartSuffices, skill, level, check,
                pohFurniture, wilderness, Arrays.asList(uses));
        if (routes.put(id, value) != null)
            throw new IllegalStateException("Duplicate transport id " + id);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Curated verified quest corpus. Unknown quests remain fail-closed and partial. */
@Singleton
public class QuestKnowledgeCatalog
{
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();

    public QuestKnowledgeCatalog()
    {
        seedFreeToPlay();
        seedEarlyMembers();
        seedUnlockChains();
        seedMidgameUnlocks();
    }

    private void seedFreeToPlay()
    {
        add(new QuestDefinition("Rune Mysteries", true,
                Collections.emptyList(), skills(), Collections.emptyList(), 0,
                Collections.emptyList(),
                "Talk to Duke Horacio on the first floor of Lumbridge Castle.",
                Arrays.asList("Rune essence mine access",
                        "Runecraft lamp and book-of-knowledge access"), skills()));

        add(new QuestDefinition("Druidic Ritual", false,
                Collections.emptyList(), skills(), Arrays.asList(
                item("Raw bear meat", 1), item("Raw rat meat", 1),
                item("Raw beef", 1), item("Raw chicken", 1)), 0,
                Collections.emptyList(),
                "Talk to Kaqemeex at the stone circle north of Taverley.",
                Collections.singletonList("Herblore progression"),
                skills(Skill.HERBLORE, 250)));

        add(new QuestDefinition("Bone Voyage", false,
                Collections.singletonList("The Dig Site"), skills(), Arrays.asList(
                item("Vodka", 2), item("Marrentill potion (unf)", 1)), 0,
                Collections.singletonList("Verify at least 100 Museum Kudos"),
                "Talk to Curator Haig Halen in Varrock Museum.",
                Collections.singletonList("Fossil Island access"), skills()));

        add(new QuestDefinition("Dragon Slayer I", true,
                Collections.emptyList(), skills(), Collections.emptyList(), 32,
                Collections.singletonList(
                        "Verify the combat setup and dragonfire-protection route"),
                "Talk to the Guildmaster in the Champions' Guild.",
                Arrays.asList("Crandor access", "Corsair Cove Resource Area access",
                        "Rune platebody and green d'hide body equipment progression"),
                skills(Skill.STRENGTH, 18_650, Skill.DEFENCE, 18_650)));

        add(q("The Restless Ghost", true, none(), skills(), none(), 0, none(),
                "Talk to Father Aereck in Lumbridge church.",
                a("Ghostspeak amulet", "Prayer XP reward"),
                skills(Skill.PRAYER, 1_125)));
        add(q("Ernest the Chicken", true, none(), skills(), none(), 0, none(),
                "Talk to Veronica outside Draynor Manor.",
                a("Draynor Manor quest progression"), skills()));
        add(q("Goblin Diplomacy", true, none(), skills(),
                a(item("Blue dye", 1), item("Orange dye", 1)), 0, none(),
                "Talk to General Bentnoze or General Wartface in Goblin Village.",
                a("Goblin quest-chain progression"), skills(Skill.CRAFTING, 200)));
        add(q("Demon Slayer", true, none(), skills(),
                a(item("Bucket of water", 1)), 0,
                a("Recover Silverlight and verify a legal Delrith combat setup"),
                "Talk to Gideon Bede in the Saradominist church in Varrock.",
                a("Silverlight"), skills()));
    }

    private void seedEarlyMembers()
    {
        add(q("Waterfall Quest", false, none(), skills(),
                a(item("Air rune", 6), item("Earth rune", 6),
                        item("Water rune", 6), item("Rope", 1)), 0,
                a("Be able to pass the unarmed Glarial's Tomb section and evade level 84-86 enemies"),
                "Talk to Almera in her house north-east of Baxtorian Falls.",
                a("Early Attack and Strength progression"),
                skills(Skill.ATTACK, 13_750, Skill.STRENGTH, 13_750)));
        add(q("Tree Gnome Village", false, none(), skills(),
                a(item("Logs", 6)), 0,
                a("Verify a legal setup to defeat the safespottable Khazard warlord"),
                "Talk to King Bolren in the centre of the Tree Gnome Village maze.",
                a("Tree Gnome Village spirit tree", "Monkey Madness I prerequisite"),
                skills(Skill.ATTACK, 11_450)));
        add(q("The Grand Tree", false, none(), skills(Skill.AGILITY, 25), none(), 0,
                a("Verify a legal setup to defeat the safespottable Black demon"),
                "Talk to King Narnode Shareen in the Grand Tree.",
                a("Gnome glider network", "Grand Tree spirit tree", "Monkey Madness I prerequisite"),
                skills(Skill.ATTACK, 18_400, Skill.MAGIC, 2_150, Skill.AGILITY, 7_900)));
        add(q("Fight Arena", false, none(), skills(), a(item("Coins", 5)), 0,
                a("Verify a legal safespot or combat setup for the arena enemies"),
                "Talk to Lady Servil west of the Ardougne Monastery.",
                a("Early Attack and Thieving progression"),
                skills(Skill.ATTACK, 12_175, Skill.THIEVING, 2_175)));
        add(q("Lost City", false, none(),
                skills(Skill.CRAFTING, 31, Skill.WOODCUTTING, 36),
                a(item("Knife", 1)), 0,
                a("Verify an Entrana-legal setup to defeat the level 101 Tree spirit"),
                "Talk to the warrior in the north-west corner of Lumbridge Swamp.",
                a("Zanaris", "Dragon longsword and dragon dagger access", "Fairy quest chain"),
                skills()));
        add(q("Priest in Peril", false, none(), skills(),
                a(item("Rune essence", 50), item("Bucket", 1)), 0,
                a("Verify a legal setup for the two level 30 enemies"),
                "Talk to King Roald in Varrock Palace.",
                a("Morytania access"), skills(Skill.PRAYER, 1_406)));
        add(q("The Dig Site", false, none(),
                skills(Skill.AGILITY, 10, Skill.HERBLORE, 10, Skill.THIEVING, 25),
                a(item("Pestle and mortar", 1), item("Vial", 1),
                        item("Tinderbox", 1), item("Rope", 2)), 0, none(),
                "Talk to an examiner in the Exam Centre south of the Digsite.",
                a("Digsite pendant progression", "Bone Voyage prerequisite", "Desert Treasure I prerequisite"),
                skills(Skill.MINING, 15_300, Skill.HERBLORE, 2_000)));
        add(q("Ghosts Ahoy", false, a("Priest in Peril", "The Restless Ghost"),
                skills(Skill.AGILITY, 25, Skill.COOKING, 20), none(), 0,
                a("Obtain the required ecto-tokens; the exact total depends on the chosen travel route"),
                "Talk to Velorina in Port Phasmatys.",
                a("Ectophial", "Free entry to Port Phasmatys"), skills(Skill.PRAYER, 2_400)));
        add(q("The Tourist Trap", false, none(),
                skills(Skill.FLETCHING, 10, Skill.SMITHING, 20),
                a(item("Desert shirt", 1), item("Desert robe", 1),
                        item("Desert boots", 1), item("Hammer", 1),
                        item("Bronze bar", 1), item("Feather", 10)), 0,
                a("Verify desert protection and a legal setup for the level 47 Mercenary Captain"),
                "Talk to Irena by the Shantay Pass south of Al Kharid.",
                a("Desert Mining Camp access", "Smithing or Agility XP choice"), skills()));
        add(q("Death Plateau", false, none(), skills(),
                a(item("Bread", 10), item("Trout", 10), item("Iron bar", 1),
                        item("Asgarnian ale", 1)), 0,
                a("Bring at least 60 coins; the gambling step can require more"),
                "Talk to Denulth in his tent in Burthorpe.",
                a("Climbing boots access", "Troll Stronghold prerequisite"),
                skills(Skill.ATTACK, 3_000)));
        add(q("Troll Stronghold", false, a("Death Plateau"),
                skills(Skill.AGILITY, 15), none(), 0,
                a("Verify a legal setup for Dad and the safespottable Troll General"),
                "Talk to Denulth in his tent in Burthorpe.",
                a("Trollheim access", "Desert Treasure I prerequisite"), skills(Skill.AGILITY, 9_000)));
    }

    private void seedUnlockChains()
    {
        add(q("Animal Magnetism", false,
                a("The Restless Ghost", "Ernest the Chicken", "Priest in Peril"),
                skills(Skill.SLAYER, 18, Skill.CRAFTING, 19,
                        Skill.RANGED, 30, Skill.WOODCUTTING, 35),
                a(item("Mithril axe", 1), item("Iron bar", 5),
                        item("Ghostspeak amulet", 1), item("Ecto-token", 20),
                        item("Hammer", 1), item("Hard leather", 1),
                        item("Holy symbol", 1), item("Polished buttons", 1)), 0, none(),
                "Talk to Ava in Draynor Manor.",
                a("Ava's devices", "Dragon Slayer II prerequisite"),
                skills(Skill.CRAFTING, 1_000, Skill.FLETCHING, 1_000,
                        Skill.SLAYER, 1_000, Skill.WOODCUTTING, 2_500)));
        add(q("Monkey Madness I", false, a("The Grand Tree", "Tree Gnome Village"),
                skills(), a(item("Gold bar", 1), item("Ball of wool", 1)), 0,
                a("Verify poison protection, Ape Atoll access, and a legal Jungle Demon setup"),
                "Talk to King Narnode Shareen in the Grand Tree.",
                a("Dragon scimitar access", "Ape Atoll travel progression"), skills()));
        add(q("Desert Treasure I", false,
                a("The Dig Site", "Temple of Ikov", "The Tourist Trap",
                        "Troll Stronghold", "Death Plateau", "Priest in Peril", "Waterfall Quest"),
                skills(Skill.THIEVING, 53, Skill.MAGIC, 50,
                        Skill.FIREMAKING, 50, Skill.SLAYER, 10), none(), 0,
                a("Verify Morytania Graveyard access and every diamond-boss combat/setup requirement"),
                "Talk to the Asgarnia Smith at the Bedabin Camp.",
                a("Ancient Magicks", "Ancient staff access"), skills(Skill.MAGIC, 20_000)));
        add(q("Fairy Tale I - Growing Pains", false, a("Lost City", "Nature Spirit"),
                skills(), none(), 0,
                a("Obtain the three randomly selected items requested by Malignius Mortifer"),
                "Talk to Martin the Master Gardener in Draynor Village.",
                a("Magic secateurs", "Fairy Tale II prerequisite"),
                skills(Skill.FARMING, 3_500, Skill.ATTACK, 2_000, Skill.MAGIC, 1_000)));
        add(q("Fairy Tale II - Cure a Queen", false, a("Fairy Tale I - Growing Pains"),
                skills(Skill.THIEVING, 40, Skill.FARMING, 49, Skill.HERBLORE, 57),
                none(), 0, a("Begin the quest far enough to unlock fairy-ring use"),
                "Talk to Martin the Master Gardener in Draynor Village.",
                a("Fairy ring transportation"), skills()));
    }

    private void seedMidgameUnlocks()
    {
        add(q("Nature Spirit", false, a("Priest in Peril", "The Restless Ghost"),
                skills(), a(item("Silver sickle", 1), item("Ghostspeak amulet", 1)),
                0, a("Verify a legal setup for three level 30 Ghasts"),
                "Talk to Drezel beneath Paterdomus Temple by the River Salve.",
                a("Mort Myre Swamp access", "Druid pouch", "Fairy Tale I prerequisite",
                        "Myreque quest-chain access"),
                skills(Skill.CRAFTING, 3_000, Skill.DEFENCE, 2_000,
                        Skill.HITPOINTS, 2_000)));
        add(q("Plague City", false, none(), skills(),
                a(item("Dwellberries", 1), item("Rope", 1),
                        item("Spade", 1), item("Bucket of water", 4)), 0, none(),
                "Talk to Edmond in the north-westernmost house of East Ardougne.",
                a("West Ardougne access", "Ardougne teleport spell progression",
                        "Biohazard prerequisite"), skills(Skill.MINING, 2_425)));
        add(q("Biohazard", false, a("Plague City"), skills(),
                a(item("Bird feed", 1), item("Priest gown", 1),
                        item("Priest gown (bottom)", 1)), 0,
                a("Verify a legal setup for the level 13 Mourner"),
                "Talk to Elena in her house in East Ardougne.",
                a("Underground Pass prerequisite", "Plague quest-chain progression"),
                skills(Skill.THIEVING, 1_250)));
        add(q("Underground Pass", false, a("Biohazard", "Plague City"),
                skills(Skill.RANGED, 25),
                a(item("Rope", 1), item("Bow", 1), item("Arrow", 1),
                        item("Spade", 1)), 0,
                a("Verify a legal sustained setup for the Underground Pass encounters and traps"),
                "Talk to King Lathas upstairs in Ardougne Castle.",
                a("Iban's staff", "Regicide prerequisite"),
                skills(Skill.ATTACK, 3_000, Skill.AGILITY, 3_000)));
        add(q("Regicide", false, a("Underground Pass", "Biohazard", "Plague City"),
                skills(Skill.CRAFTING, 10, Skill.AGILITY, 56),
                a(item("Coal", 5), item("Bow", 1), item("Rope", 2),
                        item("Spade", 1), item("Limestone", 1),
                        item("Tinderbox", 1), item("Pestle and mortar", 1)), 0,
                a("Verify a legal safespot or combat setup for the Tyras guard"),
                "Talk to King Lathas upstairs in Ardougne Castle.",
                a("Tirannwn charter access", "Zulrah access progression",
                        "Roving Elves prerequisite"), skills(Skill.AGILITY, 13_750)));
        add(q("Jungle Potion", false, a("Druidic Ritual"),
                skills(Skill.HERBLORE, 3), none(), 0, none(),
                "Talk to Trufitus in Tai Bwo Wannai.",
                a("Shilo Village prerequisite", "Tai Bwo Wannai quest progression"),
                skills(Skill.HERBLORE, 775)));
        add(q("Shilo Village", false, a("Jungle Potion", "Druidic Ritual"),
                skills(Skill.CRAFTING, 20, Skill.AGILITY, 32),
                a(item("Spade", 1), item("Rope", 1), item("Bronze wire", 1),
                        item("Chisel", 1), item("Bones", 3)), 0,
                a("Verify a consumable light source and legal setup for Nazastarool's three forms"),
                "Talk to Mosol Rei outside Shilo Village in southern Karamja.",
                a("Shilo Village access", "Duradel access progression",
                        "Lunar Diplomacy prerequisite"), skills(Skill.CRAFTING, 3_875)));
        add(q("The Fremennik Trials", false, none(), skills(),
                a(item("Coins", 5_252), item("Tinderbox", 1)), 0,
                a("Obtain an accepted raw fish and a lyre or its verified crafting route",
                        "Verify legal combat for Draugen and the unarmed Koschei trial"),
                "Talk to Brundt the Chieftain in the Rellekka longhall.",
                a("Rellekka facilities", "Enchanted lyre progression",
                        "Fremennik Isles and Lunar Diplomacy prerequisites"), skills()));
        add(q("The Fremennik Isles", false, a("The Fremennik Trials"),
                skills(Skill.CONSTRUCTION, 20, Skill.AGILITY, 40),
                a(item("Raw tuna", 1), item("Rope", 8), item("Knife", 1)), 0,
                a("Resolve the Mining-level-dependent ore requirement",
                        "Verify legal melee protection for the Ice Troll King and ten ice trolls"),
                "Talk to Mord Gunnars at the northernmost dock of Rellekka.",
                a("Helm of neitiznot", "Neitiznot and Jatizso travel progression"), skills()));
        add(q("Horror from the Deep", false, none(),
                skills(Skill.AGILITY, 35),
                a(item("Fire rune", 1), item("Air rune", 1),
                        item("Water rune", 1), item("Earth rune", 1),
                        item("Molten glass", 1), item("Tinderbox", 1),
                        item("Hammer", 1), item("Steel nails", 60),
                        item("Plank", 2), item("Swamp tar", 1)), 0,
                a("Complete Alfred Grimhand's Barcrawl",
                        "Verify all required damage types for the Dagannoth Mother"),
                "Talk to Larrissa outside the Lighthouse north of Barbarian Outpost.",
                a("God books", "Lighthouse dagannoth access"), skills()));
        add(q("Eadgar's Ruse", false,
                a("Druidic Ritual", "Troll Stronghold", "Death Plateau"),
                skills(Skill.HERBLORE, 31),
                a(item("Climbing boots", 1), item("Vodka", 1),
                        item("Pineapple chunks", 1), item("Logs", 1),
                        item("Grain", 10), item("Raw chicken", 5),
                        item("Pestle and mortar", 1), item("Ranarr potion (unf)", 1)),
                0, none(),
                "Talk to Sanfew upstairs in the herbalist shop in Taverley.",
                a("Trollheim teleport spell", "My Arm's Big Adventure prerequisite"),
                skills(Skill.HERBLORE, 11_000)));
        add(q("My Arm's Big Adventure", false,
                a("Eadgar's Ruse", "The Feud", "Jungle Potion",
                        "Druidic Ritual", "Troll Stronghold", "Death Plateau"),
                skills(Skill.FARMING, 29, Skill.WOODCUTTING, 10),
                a(item("Bucket", 1), item("Supercompost", 7),
                        item("Spade", 1), item("Rake", 1),
                        item("Seed dibber", 1), item("Climbing boots", 1)), 0,
                a("Verify at least 60% Tai Bwo Wannai Cleanup favour",
                        "Verify legal combat for the Baby Roc"),
                "Talk to Burntmeat in the Troll Stronghold kitchen.",
                a("Troll Stronghold herb patch", "Making Friends with My Arm prerequisite"),
                skills(Skill.HERBLORE, 10_000, Skill.FARMING, 5_000)));
        add(q("Lunar Diplomacy", false,
                a("The Fremennik Trials", "Lost City", "Rune Mysteries",
                        "Shilo Village", "Jungle Potion", "Druidic Ritual"),
                skills(Skill.HERBLORE, 5, Skill.CRAFTING, 61,
                        Skill.DEFENCE, 40, Skill.FIREMAKING, 49,
                        Skill.MAGIC, 65, Skill.MINING, 60,
                        Skill.WOODCUTTING, 55), none(), 0,
                a("Verify access to all four elemental runic altars and a legal Suqah setup"),
                "Talk to Lokar Searunner at the westernmost dock of Rellekka.",
                a("Lunar spellbook", "Lunar Isle access"), skills()));
        add(q("In Search of the Myreque", false,
                a("Nature Spirit", "Priest in Peril", "The Restless Ghost"),
                skills(Skill.AGILITY, 25),
                a(item("Steel longsword", 1), item("Steel sword", 2),
                        item("Steel mace", 1), item("Steel warhammer", 1),
                        item("Steel dagger", 1), item("Steel nails", 225),
                        item("Hammer", 1), item("Plank", 6)), 0,
                a("Charge and carry a Druid pouch",
                        "Verify a legal setup for the Skeleton Hellhound"),
                "Talk to Vanstrom Klause in the Hair of the Dog tavern in Canifis.",
                a("Myreque Hideout access", "In Aid of the Myreque prerequisite"), skills()));
        add(q("In Aid of the Myreque", false,
                a("In Search of the Myreque", "Nature Spirit",
                        "Priest in Peril", "The Restless Ghost"),
                skills(Skill.AGILITY, 25, Skill.CRAFTING, 25,
                        Skill.MINING, 15, Skill.MAGIC, 7),
                a(item("Spade", 1), item("Hammer", 1), item("Plank", 11),
                        item("Swamp paste", 1), item("Bronze axe", 10),
                        item("Tinderbox", 4), item("Steel bar", 2),
                        item("Coal", 1), item("Soft clay", 1),
                        item("Rope", 1), item("Silver bar", 1)), 0,
                a("Verify a legal vampyre-damaging weapon and resolve the requested food branch"),
                "Talk to Veliaf Hurtz in the Myreque Hideout below the Canifis pub.",
                a("Burgh de Rott facilities", "Darkness of Hallowvale prerequisite"), skills()));
    }

    private static QuestDefinition q(String name, boolean f2p,
            java.util.List<String> prerequisites, Map<Skill, Integer> requirements,
            java.util.List<QuestDefinition.QuestItemRequirement> items, int qp,
            java.util.List<String> checks, String start, java.util.List<String> unlocks,
            Map<Skill, Integer> rewards)
    {
        return new QuestDefinition(name, f2p, prerequisites, requirements, items,
                qp, checks, start, unlocks, rewards);
    }

    @SafeVarargs
    private static <T> java.util.List<T> a(T... values) { return Arrays.asList(values); }
    private static <T> java.util.List<T> none() { return Collections.emptyList(); }

    public QuestDefinition definitionFor(String name)
    {
        return definitions.get(normalize(name));
    }

    public Map<String, QuestDefinition> all()
    {
        return Collections.unmodifiableMap(definitions);
    }

    private void add(QuestDefinition definition)
    {
        definitions.put(normalize(definition.getName()), definition);
    }

    private static QuestDefinition.QuestItemRequirement item(String name, int quantity)
    {
        return new QuestDefinition.QuestItemRequirement(name, quantity);
    }

    private static Map<Skill, Integer> skills(Object... values)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (int i = 0; i + 1 < values.length; i += 2)
            result.put((Skill) values[i], (Integer) values[i + 1]);
        return result;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}

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
        seedProgressionDepth();
        seedSupplementalMiniquests();
        seedImportedRequirements();
    }

    private void seedSupplementalMiniquests()
    {
        add(q("Daddy's Home", false, none(), skills(),
                a(item("Plank", 10), item("Bolt of cloth", 5)), 0,
                a("Bring at least 16 nails of one usable metal type; a hammer and saw can be obtained during the miniquest"),
                "Speak to Marlo in the Estate Agent's house in north-east Varrock.",
                a("Mahogany Homes access", "Construction training introduction"),
                skills()));
        add(q("The Enchanted Key", false, a("Making History"), skills(),
                a(item("Enchanted key", 1), item("Spade", 1)), 0, none(),
                "Reclaim the enchanted key from the silver merchant in East Ardougne if needed.",
                a("Enchanted-key treasure locations", "Mjolnir and resource rewards"),
                skills()));
        add(q("The General's Shadow", false,
                a("Curse of the Empty Lord", "Fight Arena"), skills(),
                a(item("Coins", 40), item("Ghostspeak amulet", 1),
                        item("Ghostly hood", 1), item("Ghostly robe", 1),
                        item("Ghostly robe bottom", 1), item("Ghostly gloves", 1),
                        item("Ghostly boots", 1), item("Ghostly cloak", 1),
                        item("Ring of visibility", 1)), 0,
                a("Start Desert Treasure I to obtain and use the ring of visibility",
                        "Verify a legal setup for the prayer-disabled, safespottable Ghost of Bouncer fight"),
                "Find General Khazard's ghost south-east of Rellekka.",
                a("Shadow sword", "Secrets of the North prerequisite"),
                skills(Skill.SLAYER, 2_000)));
        add(q("Family Pest", false, a("Family Crest"), skills(),
                a(item("Coins", 500_000)), 0, none(),
                "Speak to Dimintheis in south-east Varrock.",
                a("Own all three Family Crest gauntlet variants simultaneously"), skills()));
        add(q("Lair of Tarn Razorlor", false, a("Haunted Mine"),
                skills(Skill.SLAYER, 40), none(), 0,
                a("Verify a legal setup for Tarn's two forms and the lair's rapid Prayer drain"),
                "Enter Tarn's Lair from the Haunted Mine area.",
                a("Salve amulet enchantment", "Tarn's diary access"), skills()));
        add(q("Skippy and the Mogres", false, none(), skills(Skill.COOKING, 20),
                a(item("Bucket of water", 1), item("Nettle tea", 1),
                        item("Hangover cure", 1)), 0, none(),
                "Speak to Skippy south-east of Rimmington.",
                a("Mogre Slayer access at 32 Slayer"), skills()));
        add(q("In Search of Knowledge", false, none(), skills(), none(), 0,
                a("Bring five pieces of suitable food, a slash weapon or knife, and a legal combat setup for Forthos Dungeon monsters"),
                "Speak to Brother Aimeri in the south-east of Forthos Dungeon.",
                a("Lamp of knowledge", "Temple page trading"), skills()));
        add(q("Land of the Goblins", false,
                a("Another Slice of H.A.M.", "Fishing Contest"),
                skills(Skill.AGILITY, 38, Skill.FISHING, 40,
                        Skill.THIEVING, 45, Skill.HERBLORE, 48),
                a(item("Toadflax potion (unf)", 1), item("Goblin mail", 1),
                        item("Yellow dye", 1), item("Blue dye", 1),
                        item("Orange dye", 1), item("Purple dye", 1),
                        item("Fishing rod", 1), item("Raw slimy eel", 1),
                        item("Coins", 5)), 0,
                a("Bring a usable light source; Ultimate Iron accounts must plan the required unequipped disguise setup",
                        "Verify a legal setup for the five goblin fights"),
                "Talk to Grubfoot in the Dorgesh-Kaan mine.",
                a("Yu'biusk access", "Goblin quest-chain progression"), skills()));
        add(q("Vale Totems", false, a("Children of the Sun"),
                skills(Skill.FLETCHING, 20), none(), 0,
                a("Bring oak-or-better logs, a knife, and four matching unstrung bows or shields"),
                "Talk to Ranulph in north-western Auburnvale.",
                a("Vale Totems minigame", "Vale offerings and research points"),
                skills()));
    }

    private void seedImportedRequirements()
    {
        for (AuthoritativeQuestRequirementCatalog.Record record
                : new AuthoritativeQuestRequirementCatalog().all().values())
        {
            if (definitionFor(record.getName()) != null) continue;
            java.util.List<String> checks = new java.util.ArrayList<>(
                    record.getOtherChecks());
            checks.add("Open the quest journal to check required items, access, and combat preparation");
            add(new QuestDefinition(record.getName(),
                    QuestMembershipPolicy.isFreeToPlayQuest(record.getName()),
                    record.getPrerequisites(), record.getSkills(), none(), null,
                    record.getQuestPoints(), checks, record.getStartLocation(),
                    none(), skills(), a("items", "access/combat", "start location",
                            "rewards/unlocks")));
        }
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
                Collections.emptyList(), skills(), a(
                        item("Unfired bowl", 1), item("Wizard's mind bomb", 1),
                        item("Lobster pot", 1), item("Silk", 1), item("Hammer", 1),
                        item("Steel nails", 90), item("Plank", 3), item("Coins", 2_000)), 32,
                Collections.singletonList(
                        "Obtain the anti-dragon shield during the quest and verify a legal setup to defeat Elvarg"),
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
        add(q("Fairytale I - Growing Pains", false, a("Lost City", "Nature Spirit"),
                skills(), none(), 0,
                a("Obtain the three randomly selected items requested by Malignius Mortifer"),
                "Talk to Martin the Master Gardener in Draynor Village.",
                a("Magic secateurs", "Fairytale II prerequisite"),
                skills(Skill.FARMING, 3_500, Skill.ATTACK, 2_000, Skill.MAGIC, 1_000)));
        add(q("Fairytale II - Cure a Queen", false, a("Fairytale I - Growing Pains"),
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
                a("Mort Myre Swamp access", "Druid pouch", "Fairytale I prerequisite",
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

    private void seedProgressionDepth()
    {
        add(q("Cook's Assistant", true, none(), skills(),
                a(item("Bucket of milk", 1), item("Pot of flour", 1),
                        item("Egg", 1)), 0, none(),
                "Talk to the Cook in Lumbridge Castle kitchen.",
                a("Cook-o-matic 100 access"), skills(Skill.COOKING, 300)));
        add(q("Vampyre Slayer", true, none(), skills(),
                a(item("Beer", 1)), 0,
                a("Obtain the stake and verify a legal setup for Count Draynor"),
                "Talk to Morgan in northern Draynor Village.",
                a("Count Draynor Nightmare Zone access"),
                skills(Skill.ATTACK, 4_825)));
        add(q("Witch's House", false, none(), skills(),
                a(item("Leather gloves", 1)), 0,
                a("Verify food and a legal setup for four consecutive monster forms"),
                "Talk to the boy by the gate west of Falador in Taverley.",
                a("Witch's experiment Nightmare Zone access"),
                skills(Skill.HITPOINTS, 6_325)));
        add(q("Gertrude's Cat", false, none(), skills(),
                a(item("Coins", 100), item("Seasoned sardine", 1)), 0, none(),
                "Talk to Gertrude in her house west of Varrock.",
                a("Kitten access", "Icthlarin's Little Helper prerequisite"),
                skills(Skill.COOKING, 1_525)));
        add(q("The Feud", false, none(), skills(Skill.THIEVING, 30),
                a(item("Kharidian headpiece", 1),
                        item("Fake beard", 1), item("Beer", 3)), 0,
                a("Obtain gloves accepted for handling the quest cactus",
                        "Verify a legal setup for the Bandit champion and Tough Guy"),
                "Talk to Ali Morrisane in northern Al Kharid.",
                a("Blackjack Thieving progression", "My Arm's Big Adventure prerequisite"),
                skills(Skill.THIEVING, 15_000)));
        add(q("Temple of Ikov", false, none(), skills(Skill.THIEVING, 42),
                a(item("Limpwurt root", 20)), 0,
                a("Reduce carried weight below 0 kg",
                        "Verify a legal ranged route for the Fire Warrior of Lesarkus"),
                "Talk to Lucien at the Flying Horse Inn in East Ardougne.",
                a("Desert Treasure I prerequisite", "Pendant of Lucien"), skills()));
        add(q("The Golem", false, none(),
                skills(Skill.CRAFTING, 20, Skill.THIEVING, 25),
                a(item("Pestle and mortar", 1), item("Papyrus", 1),
                        item("Soft clay", 4), item("Phoenix feather", 1)), 0,
                a("Verify a safe desert-entry and heat-management route"),
                "Talk to the clay golem in the ruins of Uzer.",
                a("Shadow of the Storm prerequisite"), skills()));
        add(q("Shadow of the Storm", false, a("The Golem", "Demon Slayer"),
                skills(Skill.CRAFTING, 30),
                a(item("Silver bar", 1), item("Black dye", 1)), 0,
                a("Wear three accepted black clothing pieces",
                        "Choose the combat XP reward only after checking protected build stats",
                        "Verify a legal setup for Agrith-Naar"),
                "Talk to Father Reen south of Al Kharid bank.",
                a("Darklight", "Recipe for Disaster Evil Dave prerequisite"), skills()));
        add(q("Icthlarin's Little Helper", false, a("Gertrude's Cat"), skills(),
                a(item("Tinderbox", 1), item("Willow logs", 1),
                        item("Bag of salt", 1), item("Bucket of sap", 1),
                        item("Waterskin(4)", 1), item("Linen", 1)), 0,
                a("Verify desert heat, poison protection and a legal setup for the level 91 guardian"),
                "Talk to the wanderer west of the Agility Pyramid.",
                a("Sophanem access", "Contact! prerequisite"), skills()));
        add(q("Merlin's Crystal", false, none(), skills(),
                a(item("Tinderbox", 1), item("Bucket of wax", 1),
                        item("Bat bones", 1)), 0,
                a("Verify a legal setup for Sir Mordred"),
                "Talk to King Arthur in Camelot.",
                a("Excalibur", "Holy Grail prerequisite", "Heroes' Quest prerequisite"), skills()));
        add(q("Holy Grail", false, a("Merlin's Crystal"), skills(Skill.ATTACK, 20),
                none(), 0,
                a("Verify a legal safespot or flinch setup for the Black Knight Titan",
                        "This quest awards Defence and Prayer XP; check protected build stats first"),
                "Talk to King Arthur in Camelot.",
                a("King's Ransom prerequisite", "Fisher Realm access"),
                skills(Skill.DEFENCE, 15_300, Skill.PRAYER, 11_000)));
        add(q("Black Knights' Fortress", true, none(), skills(),
                a(item("Iron chainbody", 1), item("Bronze med helm", 1)), 12,
                a("Verify a safe route past level 33 Black Knights"),
                "Talk to Sir Amik Varze in the western tower of Falador Castle.",
                a("King's Ransom prerequisite"), skills()));
        add(q("Murder Mystery", false, none(), skills(), none(), 0, none(),
                "Talk to a guard around Sinclair Mansion north of Seers' Village.",
                a("King's Ransom prerequisite"), skills(Skill.CRAFTING, 1_406)));
        add(q("One Small Favour", false, a("Rune Mysteries", "Shilo Village"),
                skills(Skill.AGILITY, 36, Skill.CRAFTING, 25,
                        Skill.HERBLORE, 18, Skill.SMITHING, 30),
                a(item("Bronze bar", 1), item("Iron bar", 1),
                        item("Guam leaf", 2), item("Marrentill", 1),
                        item("Harralander", 1), item("Hammer", 1),
                        item("Pigeon cage", 5), item("Pot", 1),
                        item("Soft clay", 1)), 0,
                a("Resolve the cut-gem and Guthix-rest processing supplies before starting"),
                "Talk to Yanni Salika in Shilo Village.",
                a("King's Ransom prerequisite", "Gnome glider route to Feldip Hills"), skills()));
        add(q("King's Ransom", false,
                a("Black Knights' Fortress", "Holy Grail", "Murder Mystery", "One Small Favour"),
                skills(Skill.MAGIC, 45, Skill.DEFENCE, 65),
                a(item("Air rune", 1), item("Law rune", 1),
                        item("Black full helm", 1), item("Black platebody", 1),
                        item("Black platelegs", 1), item("Bronze med helm", 1),
                        item("Iron chainbody", 1)), 0,
                a("This quest awards Defence XP; check protected build stats first",
                        "Knight Waves and its prayer unlocks require a separate legal combat setup"),
                "Talk to the Gossip outside Sinclair Mansion.",
                a("Knight Waves Training Grounds", "Piety", "Chivalry"),
                skills(Skill.DEFENCE, 33_000)));
        add(q("Shield of Arrav", true, none(), skills(), none(), 0,
                a("Arrange a trusted partner or use the official OSRS SOA chat-channel"),
                "Talk to Reldo in Varrock Castle library.",
                a("Heroes' Quest prerequisite"), skills()));
        add(q("Heroes' Quest", false,
                a("Shield of Arrav", "Lost City", "Merlin's Crystal", "Dragon Slayer I"),
                skills(Skill.COOKING, 53, Skill.FISHING, 53,
                        Skill.HERBLORE, 25, Skill.MINING, 50), none(), 55,
                a("Arrange a partner from the opposite Shield of Arrav gang",
                        "Verify dragonfire protection and a legal Ice Queen setup"),
                "Talk to Achietties outside the Heroes' Guild.",
                a("Heroes' Guild", "Throne of Miscellania prerequisite"), skills()));
        add(q("Throne of Miscellania", false,
                a("Heroes' Quest", "The Fremennik Trials"), skills(),
                a(item("Gold ring", 1)), 0,
                a("Choose and verify one legal favour-gaining route"),
                "Talk to King Vargas in Miscellania Castle.",
                a("Managing Miscellania", "Royal Trouble prerequisite"), skills()));
        add(q("Royal Trouble", false, a("Throne of Miscellania"),
                skills(Skill.AGILITY, 40, Skill.SLAYER, 40),
                a(item("Rope", 2), item("Plank", 1)), 0,
                a("Verify poison protection and a legal combat setup"),
                "Talk to Advisor Ghrim in Miscellania Castle.",
                a("Expanded Managing Miscellania resource allocation"), skills()));
        add(q("Haunted Mine", false, a("Priest in Peril"),
                skills(Skill.CRAFTING, 35), none(), 0,
                a("Verify Abandoned Mine access and a legal setup for Treus Dayth"),
                "Talk to the Zealot by the Abandoned Mine in south-west Morytania.",
                a("Salve amulet", "Lair of Tarn Razorlor access"),
                skills(Skill.STRENGTH, 22_000)));
        add(q("Darkness of Hallowvale", false, a("In Aid of the Myreque"),
                skills(Skill.CONSTRUCTION, 5, Skill.MINING, 20,
                        Skill.THIEVING, 22, Skill.AGILITY, 26,
                        Skill.CRAFTING, 32, Skill.MAGIC, 33, Skill.STRENGTH, 40),
                a(item("Plank", 2), item("Hammer", 1), item("Air rune", 1),
                        item("Law rune", 1)), 0,
                a("Verify a safe Meiyerditch route and protection from Vyrewatch attacks"),
                "Talk to Veliaf Hurtz beneath the Burgh de Rott inn.",
                a("Meiyerditch access", "A Taste of Hope prerequisite"), skills()));
        add(q("A Taste of Hope", false, a("Darkness of Hallowvale"),
                skills(Skill.CRAFTING, 48, Skill.AGILITY, 45,
                        Skill.ATTACK, 40, Skill.HERBLORE, 40, Skill.SLAYER, 38),
                a(item("Knife", 1), item("Emerald", 1), item("Chisel", 1)), 0,
                a("Verify standard spellbook enchantment access or carry an enchant tablet",
                        "Verify a legal setup and restoration for the Abomination"),
                "Talk to Garth by the Theatre of Blood entrance in Ver Sinhaza.",
                a("Drakan's medallion", "Sins of the Father prerequisite"), skills()));
        add(q("The Lost Tribe", false, a("Goblin Diplomacy", "Rune Mysteries"),
                skills(Skill.AGILITY, 13, Skill.THIEVING, 13, Skill.MINING, 17),
                a(item("Light source", 1)), 0,
                a("Use a safe light source for Lumbridge Swamp Caves"),
                "Talk to Sigmund in Lumbridge Castle.",
                a("Dorgeshuun mine access", "Death to the Dorgeshuun prerequisite"), skills()));
        add(q("Death to the Dorgeshuun", false, a("The Lost Tribe"),
                skills(Skill.AGILITY, 23, Skill.THIEVING, 23),
                a(item("H.A.M. hood", 2), item("H.A.M. robe", 2),
                        item("H.A.M. shirt", 2), item("H.A.M. logo", 2),
                        item("H.A.M. cloak", 2), item("H.A.M. gloves", 2),
                        item("H.A.M. boots", 2), item("Sapphire lantern", 1),
                        item("Chisel", 1)), 0,
                a("Verify a legal melee or Magic setup for Sigmund"),
                "Talk to Mistag in the Dorgesh-Kaan mine.",
                a("Dorgeshuun crossbow", "Dorgesh-Kaan quest progression"), skills()));
        add(q("Big Chompy Bird Hunting", false, none(),
                skills(Skill.FLETCHING, 5, Skill.COOKING, 30, Skill.RANGED, 30),
                a(item("Feather", 100), item("Knife", 1), item("Chisel", 1),
                        item("Wolf bones", 4), item("Cabbage", 1),
                        item("Tomato", 1), item("Onion", 1),
                        item("Potato", 1), item("Equa leaves", 1),
                        item("Doogle leaves", 1)), 0,
                a("Verify a legal ranged setup using the quest's ogre bow and arrows"),
                "Talk to Rantz on the eastern coast of Feldip Hills.",
                a("Ogre bow progression", "Zogre Flesh Eaters prerequisite"), skills()));
        add(q("Zogre Flesh Eaters", false,
                a("Big Chompy Bird Hunting", "Jungle Potion"),
                skills(Skill.SMITHING, 4, Skill.HERBLORE, 8, Skill.RANGED, 30),
                a(item("Knife", 1)), 0,
                a("Verify disease protection and a legal brutal-arrow or Crumble Undead setup for Slash Bash"),
                "Talk to Grish at Jiggig south of Castle Wars.",
                a("Relicym's balm", "Ogre coffin access", "Rum Deal prerequisite"), skills()));
        add(q("Rum Deal", false, a("Zogre Flesh Eaters", "Priest in Peril"),
                skills(Skill.CRAFTING, 42, Skill.FARMING, 40,
                        Skill.PRAYER, 47, Skill.SLAYER, 42), none(), 0,
                a("Verify ranged damage or disease protection for the Evil Spirit",
                        "Resolve the Fishing requirement or verified monster-drop bypass"),
                "Talk to Pirate Pete on the dock north of Port Phasmatys.",
                a("Braindeath Island", "Cabin Fever prerequisite"), skills()));
        add(q("Pirate's Treasure", true, none(), skills(),
                a(item("White apron", 1), item("Coins", 60),
                        item("Banana", 10), item("Spade", 1)), 0,
                a("Verify a safe route past the level 4 gardener"),
                "Talk to Redbeard Frank at the northern Port Sarim dock.",
                a("Cabin Fever prerequisite"), skills()));
        add(q("Cabin Fever", false, a("Pirate's Treasure", "Rum Deal"),
                skills(Skill.AGILITY, 42, Skill.CRAFTING, 45,
                        Skill.SMITHING, 50, Skill.RANGED, 40),
                a(item("Repair plank", 6), item("Tacks", 30),
                        item("Swamp paste", 3), item("Hammer", 1)), 0,
                a("Verify Port Phasmatys transport and a legal setup for attacking pirates"),
                "Talk to Bill Teach in the Green Ghost inn at Port Phasmatys.",
                a("Mos Le'Harmless", "Cave horror access", "Barrelchest anchor progression"), skills()));
        add(q("Watchtower", false, none(),
                skills(Skill.MAGIC, 14, Skill.THIEVING, 15,
                        Skill.AGILITY, 25, Skill.HERBLORE, 14, Skill.MINING, 40),
                a(item("Coins", 20), item("Gold bar", 1),
                        item("Tinderbox", 1), item("Death rune", 1),
                        item("Pickaxe", 1), item("Dragon bones", 1),
                        item("Rope", 2), item("Guam leaf", 1),
                        item("Vial of water", 1), item("Light source", 1),
                        item("Pestle and mortar", 1), item("Bat bones", 1),
                        item("Jangerberries", 1)), 0,
                a("Verify a legal setup for Gorad and dragonfire protection for passing blue dragons",
                        "This quest awards Magic XP; check protected build stats first"),
                "Talk to the Watchtower Wizard north of Yanille after climbing the tower's north trellis.",
                a("Watchtower Teleport", "Gu'Tanoth and Ogre Enclave access",
                        "Monkey Madness II prerequisite"),
                skills(Skill.MAGIC, 15_250)));
        add(q("Dwarf Cannon", false, none(), skills(),
                a(item("Hammer", 1)), 0,
                a("Verify a safe route through the dwarf base and the nearby combat areas"),
                "Talk to Captain Lawgof south of the Coal Trucks and north-west of the Fishing Guild.",
                a("Dwarf multicannon access", "Cannonball production progression"),
                skills(Skill.CRAFTING, 750)));
        add(q("Tears of Guthix", false, none(), skills(),
                a(item("Sapphire lantern", 1), item("Chisel", 1),
                        item("Pickaxe", 1), item("Rope", 1)), 43,
                a("Use a lit sapphire lantern and a safe light-source route through Lumbridge Swamp Caves"),
                "Talk to Juna deep inside Lumbridge Swamp Caves.",
                a("Tears of Guthix weekly lowest-skill XP activity",
                        "While Guthix Sleeps prerequisite"),
                skills(Skill.CRAFTING, 1_000)));
        add(q("Below Ice Mountain", true, none(), skills(),
                a(item("Cooked meat", 1), item("Bread", 1),
                        item("Knife", 1), item("Beer", 1)), 16,
                a("Verify a legal setup for the Ancient Guardian or the optional Mining route"),
                "Talk to Willow on the path south of Ice Mountain.",
                a("Ruins of Camdozaal", "Barronite and Camdozaal skilling progression",
                        "Defender of Varrock prerequisite"), skills()));
        add(q("Temple of the Eye", false,
                a("Rune Mysteries", "Enter the Abyss"),
                skills(Skill.RUNECRAFT, 10),
                a(item("Bucket of water", 1), item("Chisel", 1),
                        item("Pickaxe", 1)), 0, none(),
                "Talk to Wizard Persten north of Al Kharid near the Lumbridge gate.",
                a("Guardians of the Rift", "Temple of the Eye access",
                        "Medium rune pouch progression"),
                skills(Skill.RUNECRAFT, 5_000)));
        add(q("Sleeping Giants", false, none(), skills(Skill.SMITHING, 15),
                a(item("Oak logs", 3), item("Wool", 1),
                        item("Nails", 10), item("Hammer", 1),
                        item("Chisel", 1), item("Bucket of water", 1)), 0,
                none(), "Talk to Kovac at the Giants' Plateau east of Al Kharid.",
                a("Giants' Foundry", "Smiths' Uniform and mould progression"), skills()));
        add(q("A Porcine of Interest", false, none(), skills(),
                a(item("Rope", 1), item("Knife", 1)), 0,
                a("Verify a legal setup for the Sourhog and the instanced Pig Thing"),
                "Read the notice board behind Fortunato's Wine Shop in Draynor Village.",
                a("Sourhog Slayer assignment progression", "Spria Slayer-master progression"),
                skills()));
        add(q("Enter the Abyss", false, a("Rune Mysteries"), skills(),
                none(), 0,
                a("Entering the Abyss passes through low-level Wilderness and requires explicit Wilderness risk approval",
                        "Verify a legal non-combat rift-entry obstacle for the current build"),
                "Talk to the Mage of Zamorak north of Edgeville by the River Lum.",
                a("Abyss Runecraft access", "Small pouch", "Temple of the Eye prerequisite"),
                skills(Skill.RUNECRAFT, 1_000)));
        add(q("Alfred Grimhand's Barcrawl", false, none(), skills(),
                a(item("Coins", 208)), 0,
                a("Plan safe travel to all required pubs and carry food because the drinks reduce Hitpoints and skills"),
                "Talk to a barbarian guard at the Barbarian Outpost Agility Course entrance.",
                a("Barbarian Outpost Agility Course", "Horror from the Deep prerequisite",
                        "Scorpion Catcher prerequisite", "Automatic empty-vial smashing"), skills()));
        add(q("Mage Arena I", false, none(), skills(Skill.MAGIC, 60),
                none(), 0,
                a("Explicitly allow deep-Wilderness travel and verify runes or a powered staff, a web-cutting item, food and a legal Magic-only Kolodion setup"),
                "Talk to Kolodion at the Mage Arena bank in level 53 Wilderness.",
                a("God cape", "God staff", "God spell progression",
                        "Mage Arena II prerequisite", "Wilderness Hard Diary progression"), skills()));
        add(q("Mage Arena II", false, a("Mage Arena I"),
                skills(Skill.MAGIC, 75), none(), 0,
                a("Verify all three god spells are unlocked outside the arena",
                        "Explicitly allow deep-Wilderness travel and verify all three god staves, spell runes, food, restoration and a level-30 escape"),
                "Talk to Kolodion in the Mage Arena bank after meeting the spell requirement.",
                a("Imbued god cape progression"), skills()));
        add(q("The Frozen Door", false, a("Desert Treasure I"),
                skills(Skill.AGILITY, 70, Skill.HITPOINTS, 70,
                        Skill.RANGED, 70, Skill.STRENGTH, 70),
                a(item("Mith grapple", 1), item("Rope", 2)), 0,
                a("Verify God Wars Dungeon protection, transport and equipment for defeating each of the four faction generals",
                        "Carry a crossbow for the grapple and a hammer or dragon warhammer for Bandos access"),
                "Read the important letter delivered by the messenger.",
                a("Frozen key", "Ancient Prison", "Nex access"), skills()));
        add(q("Barbarian Training", false, a("Tai Bwo Wannai Trio"),
                skills(Skill.FISHING, 55, Skill.FIREMAKING, 35,
                        Skill.STRENGTH, 35, Skill.AGILITY, 15, Skill.FARMING, 15),
                none(), 0,
                a("Verify each skill-specific training step and a safe Ancient Cavern entry setup before the final descent"),
                "Talk to Otto Godblessed in his grotto above Baxtorian Falls.",
                a("Ancient Cavern", "Barbarian Fishing", "Barbarian Farming",
                        "Barbarian potion mixes", "Hasta and spear Smithing progression"), skills()));
        add(q("Dragon Slayer II", false,
                a("Legends' Quest", "Dream Mentor", "A Tail of Two Cats",
                        "Animal Magnetism", "Ghosts Ahoy", "Bone Voyage",
                        "Client of Kourend", "Barbarian Training"),
                skills(Skill.MAGIC, 75, Skill.SMITHING, 70, Skill.MINING, 68,
                        Skill.CRAFTING, 62, Skill.AGILITY, 60, Skill.THIEVING, 60,
                        Skill.CONSTRUCTION, 50, Skill.HITPOINTS, 50),
                a(item("Goutweed", 1),
                        item("Glassblowing pipe", 1), item("Spade", 1),
                        item("Astral rune", 1), item("Tinderbox", 1),
                        item("Pestle and mortar", 1), item("Insulated boots", 1)), 200,
                a("Carry any usable pickaxe, and verify Catspeak/Ghostspeak amulet or diary-equipment alternatives",
                        "Verify every Ancient Cavern, dragon and boss combat setup, including Vorkath and Galvek"),
                "Talk to Alec Kincade outside the Myths' Guild.",
                a("Myths' Guild", "Repeatable Vorkath", "Adamant and rune dragons",
                        "Ava's assembler progression", "Wrath rune progression"), skills()));
        add(q("Monkey Madness II", false,
                a("Enlightened Journey", "The Eyes of Glouphrie",
                        "Recipe for Disaster", "Troll Stronghold", "Watchtower"),
                skills(Skill.SLAYER, 69, Skill.CRAFTING, 70, Skill.HUNTER, 60,
                        Skill.FIREMAKING, 60, Skill.AGILITY, 55, Skill.THIEVING, 55),
                a(item("Lemon", 1), item("Grapes", 1), item("Logs", 1)), 0,
                a("Verify the Grand Tree balloon route and legal combat setups for Kruk, the generals and Glough"),
                "Talk to King Narnode Shareen in the Grand Tree.",
                a("Demonic gorillas", "Royal seed pod", "Heavy and light ballista progression",
                        "Ape Atoll post-quest access without a greegree"), skills()));
        add(q("Sins of the Father", false, a("Vampyre Slayer", "A Taste of Hope"),
                skills(Skill.WOODCUTTING, 62, Skill.FLETCHING, 60, Skill.CRAFTING, 56,
                        Skill.AGILITY, 52, Skill.ATTACK, 50, Skill.SLAYER, 50, Skill.MAGIC, 49),
                a(item("Ivandis flail", 1), item("Ruby", 1),
                        item("Knife", 1), item("Chisel", 1)), 0,
                a("Carry any usable axe, verify the Vyrewatch disguise and a legal Vanstrom Klause combat setup"),
                "Talk to Veliaf Hurtz outside the church in Slepe.",
                a("Darkmeyer", "Blisterwood flail", "Daeyalt essence mine",
                        "Vyrewatch Sentinel progression"), skills()));
        add(q("Beneath Cursed Sands", false, a("Contact!"),
                skills(Skill.AGILITY, 62, Skill.CRAFTING, 55, Skill.FIREMAKING, 55),
                a(item("Coal", 1)), 0,
                a("Prepare desert-heat protection, antipoison and legal melee/ranged setups",
                        "Verify a setup for the Head Menaphite Guard fight without protection prayers"),
                "Talk to Jamila at her stall in Sophanem.",
                a("Tombs of Amascut", "Keris partisan", "Necropolis progression"), skills()));
        add(q("Song of the Elves", false,
                a("Mourning's End Part II", "Making History", "Druidic Ritual"),
                skills(Skill.AGILITY, 70, Skill.CONSTRUCTION, 70, Skill.FARMING, 70,
                        Skill.HERBLORE, 70, Skill.HUNTER, 70, Skill.MINING, 70,
                        Skill.SMITHING, 70, Skill.WOODCUTTING, 70),
                a(item("Steel full helm", 1), item("Steel platebody", 1),
                        item("Steel platelegs", 1), item("Red dye", 1),
                        item("Purple dye", 1), item("Silk", 1), item("Runite bar", 1),
                        item("Limestone brick", 8), item("Rope", 1), item("Nature rune", 1),
                        item("Adamant chainbody", 1), item("Cadantine seed", 1)), 0,
                a("Verify full mourner gear and all remaining tool/ingredient alternatives",
                        "Verify legal combat setups for Arianwyn, Essyllt and the Fragment of Seren"),
                "Talk to Edmond at his home in East Ardougne.",
                a("Prifddinas", "The Gauntlet", "Zalcano", "Crystal equipment and skilling progression"), skills()));
        add(q("Desert Treasure II - The Fallen Empire", false,
                a("Desert Treasure I", "Secrets of the North", "Enakhra's Lament",
                        "Temple of the Eye", "The Garden of Death", "Below Ice Mountain",
                        "His Faithful Servants"),
                skills(Skill.FIREMAKING, 75, Skill.MAGIC, 75, Skill.THIEVING, 70,
                        Skill.HERBLORE, 62, Skill.RUNECRAFT, 60, Skill.CONSTRUCTION, 60),
                a(item("Ring of visibility", 1), item("Death rune", 8),
                        item("Chaos rune", 16), item("Fire rune", 2),
                        item("Air rune", 3), item("Water rune", 4),
                        item("Soul rune", 2), item("Blood rune", 2)), 0,
                a("Switch to Ancient Magicks and verify Blood, Ice, Smoke and Shadow Burst access",
                        "Verify encounter-specific legal setups for every quest boss and a safe transport route"),
                "Investigate the Ancient Vault north-east of Nardah.",
                a("Repeatable Vardorvis, Leviathan, Duke Sucellus and Whisperer",
                        "Ancient sceptre upgrade progression", "Soulreaper axe component progression"), skills()));
        add(q("Mourning's End Part I", false,
                a("Roving Elves", "Big Chompy Bird Hunting", "Sheep Herder"),
                skills(Skill.RANGED, 60, Skill.THIEVING, 50),
                a(item("Bear fur", 1), item("Silk", 2), item("Red dye", 1),
                        item("Yellow dye", 1), item("Green dye", 1), item("Blue dye", 1),
                        item("Bucket of water", 1), item("Feather", 1),
                        item("Magic logs", 1), item("Leather", 1), item("Ogre bellows", 1)), 0,
                a("Verify the quest-specific rotten apple, food and naphtha ingredients",
                        "Prepare for the forced-stat Mourner fight with combat stats temporarily set to 20"),
                "Talk to Eluned inside the elven woods of Isafdar.",
                a("Mourner disguise", "Mourner Headquarters", "Mourning's End Part II prerequisite"), skills()));
        add(q("Mourning's End Part II", false, a("Mourning's End Part I"), skills(),
                a(item("Rope", 1), item("Chisel", 1)), 0,
                a("Wear full mourner clothes and verify a death/catalytic talisman, tiara, Runecraft cape or the 50-item alternative",
                        "Prepare food and restoration for aggressive shadows in the Temple of Light"),
                "Talk to Arianwyn in Lletya.",
                a("Death altar", "Dark beast access", "Song of the Elves prerequisite"), skills()));
        add(q("Making History", false, a("Priest in Peril", "The Restless Ghost"), skills(),
                a(item("Spade", 1), item("Sapphire amulet", 1)), 0,
                a("Verify a Ghostspeak item and a legal Port Phasmatys entry route"),
                "Talk to Jorral at the outpost north-west of West Ardougne.",
                a("Enchanted key progression", "Song of the Elves prerequisite"), skills()));
        add(q("Contact!", false, a("Prince Ali Rescue", "Icthlarin's Little Helper"),
                skills(), none(), 0,
                a("Carry an extinguish-resistant light source or a light source with tinderbox",
                        "Verify a legal setup for the Giant Scarab and its summons"),
                "Talk to the High Priest of Icthlarin in Sophanem.",
                a("Sophanem bank", "Beneath Cursed Sands prerequisite"), skills()));
        add(q("Secrets of the North", false,
                a("Making Friends with My Arm", "The General's Shadow",
                        "Devious Minds", "Hazeel Cult"),
                skills(Skill.AGILITY, 69, Skill.THIEVING, 64, Skill.HUNTER, 56),
                a(item("Coins", 100), item("Lockpick", 1), item("Tinderbox", 1)), 0,
                a("Verify legal encounter setups for the quest bosses and the final Muspah fight"),
                "Talk to the guard outside Carnillean Mansion in south-west East Ardougne.",
                a("Repeatable Phantom Muspah", "Ancient icon", "Desert Treasure II prerequisite"), skills()));
        add(q("Enakhra's Lament", false, none(),
                skills(Skill.CRAFTING, 50, Skill.PRAYER, 43, Skill.MAGIC, 39),
                a(item("Chisel", 1), item("Tinderbox", 1), item("Candle", 1),
                        item("Logs", 1), item("Oak logs", 1), item("Willow logs", 1),
                        item("Maple logs", 1)), 0,
                a("Use the standard spellbook and verify food, quarry materials and runes for the required elemental and Crumble Undead spells; 45 Firemaking identifies the braziers but is not mandatory"),
                "Talk to Lazim north-east of the Desert Quarry.",
                a("Camulet", "Desert Treasure II prerequisite"), skills()));
        add(q("The Garden of Death", false, none(), skills(Skill.FARMING, 20),
                none(), 0, none(),
                "Search the tent east of the chasm at the southern end of the Kebos Lowlands.",
                a("Desert Treasure II prerequisite", "Kebos Lowlands lore progression"), skills()));
        add(q("His Faithful Servants", false, a("Priest in Peril"), skills(),
                a(item("Spade", 1)), 0,
                a("Verify legal equipment and supplies to defeat all six Barrows brothers"),
                "Talk to the Strange Old Man at Barrows, east of Mort'ton.",
                a("Ancient icon upgrade progression", "Desert Treasure II prerequisite"), skills()));
        add(q("Dream Mentor", false, a("Lunar Diplomacy", "Eadgar's Ruse"), skills(),
                a(item("Seal of passage", 1), item("Goutweed", 1),
                        item("Astral rune", 1), item("Tinderbox", 1),
                        item("Pestle and mortar", 1)), 0,
                a("Verify combat level 85 and three suitable food types totalling the required feeding setup",
                        "Prepare to defeat four bosses consecutively without prayer; this is unsafe death content for Hardcore accounts"),
                "Talk to the fallen man in the small cave in the Lunar Isle mine.",
                a("Seven additional Lunar spells", "Dragon Slayer II prerequisite"),
                skills(Skill.HITPOINTS, 15_000, Skill.MAGIC, 10_000)));
        add(q("A Tail of Two Cats", false, a("Icthlarin's Little Helper"), skills(),
                a(item("Catspeak amulet", 1), item("Death rune", 5),
                        item("Chocolate cake", 1), item("Tinderbox", 1),
                        item("Bucket of milk", 1), item("Shears", 1),
                        item("Potato seed", 4), item("Rake", 1)), 0,
                a("Bring a cat or kitten and verify the remaining farming tools and clothing alternatives"),
                "Talk to Unferth at his house in Burthorpe.",
                a("Catspeak amulet (e)", "Dragon Slayer II prerequisite"), skills()));
        add(q("Client of Kourend", false, a("X Marks the Spot"), skills(),
                none(), 0, a("Carry any accepted feather"),
                "Talk to Veos at Port Piscarilius.",
                a("Kharedst's memoirs", "Kourend quest-chain progression"), skills()));
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
        String id = normalize(definition.getName());
        if (definitions.put(id, definition) != null)
            throw new IllegalStateException("Duplicate quest identity: "
                    + definition.getName());
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

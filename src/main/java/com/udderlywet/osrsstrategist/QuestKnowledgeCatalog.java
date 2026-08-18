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

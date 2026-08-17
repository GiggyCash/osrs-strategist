package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Deterministic execution units for curated training methods.
 *
 * <p>This is intentionally strict. A method only appears here when RuneLite has
 * a stable action XP value that can be matched without inventing a rate. Team
 * minigames, bosses, variable Slayer tasks, rumours, contracts, and other
 * variable-XP loops stay out until Strategist has a dedicated model.</p>
 */
@Singleton
public class MethodExecutionProfileCatalog
{
    private final Map<String, MethodExecutionProfile> profiles = new HashMap<>();

    public MethodExecutionProfileCatalog()
    {
        agility();
        gathering();
        production();
        utility();
    }

    public MethodExecutionProfile forMethod(String methodId)
    {
        return methodId == null ? null : profiles.get(methodId);
    }

    public Map<String, MethodExecutionProfile> all()
    {
        return Collections.unmodifiableMap(profiles);
    }

    private void agility()
    {
        add(p("agility_rooftops", "lap", "laps", none(),
                "Base lap XP from RuneLite; diary/course bonuses can reduce the final lap count.",
                "rooftop"));
        add(p("agility_canifis_marks", "lap", "laps", none(),
                "Canifis lap XP; Marks of grace are tracked as a separate progression objective.",
                "canifis_rooftop"));
        add(p("agility_seers", "lap", "laps", none(),
                "Seers lap XP before diary bonus adjustments.",
                "seers_village_rooftop"));
        add(p("agility_sep", "floor completion", "floor completions", none(),
                "Uses the deepest unlocked Hallowed Sepulchre floor represented by RuneLite at the current level.",
                "hallowed_sepulchre"));
        add(p("agility_prif", "lap", "laps", none(), null,
                "prifddinas_agility_course"));
        add(p("agility_wildy_expanded", "lap", "laps", none(), null,
                "wilderness_agility_course"));
    }

    private void gathering()
    {
        add(p("mining_f2p_iron", "iron ore mined", "iron ores mined", none(), null,
                "iron_ore"));
        add(p("mining_granite_3t", "granite success", "granite successes", none(),
                "Tick manipulation changes actions per hour, not XP awarded by each successful mined rock.",
                "granite"));
        add(p("mining_calcified", "calcified rock success", "calcified rock successes", none(), null,
                "calcified"));

        add(p("fishing_f2p_fly", "fish caught", "fish caught", none(),
                "Selects the highest unlocked trout/salmon action in the route.",
                "trout", "salmon"));
        add(p("fishing_barbarian", "fish caught", "fish caught", none(),
                "Selects the highest unlocked leaping fish. Passive Agility/Strength XP is extra.",
                "leaping"));
        add(p("fishing_3t_barb", "fish caught", "fish caught", none(),
                "Three-ticking changes speed, not the Fishing XP of each successful catch.",
                "leaping"));
        add(p("fishing_karambwan", "karambwan caught", "karambwans caught", none(), null,
                "karambwan"));
        add(p("fishing_minnows", "minnow catch", "minnow catches", none(), null,
                "minnow"));
        add(p("fishing_anglers", "anglerfish caught", "anglerfish caught", none(), null,
                "anglerfish"));
        add(p("fishing_dark_crabs", "dark crab caught", "dark crabs caught", none(), null,
                "dark_crab"));

        add(p("woodcutting_f2p_willows", "willow log cut", "willow logs cut", none(), null,
                "willow"));
        add(p("woodcutting_teaks", "teak log cut", "teak logs cut", none(), null,
                "teak"));
        add(p("woodcutting_tick_teaks", "teak log cut", "teak logs cut", none(),
                "Tick manipulation changes speed, not base XP per successful teak log.",
                "teak"));
        add(p("woodcutting_sulliusceps", "sulliuscep cut", "sulliusceps cut", none(), null,
                "sulliuscep"));
        add(p("woodcutting_redwoods", "redwood log cut", "redwood logs cut", none(), null,
                "redwood"));

        add(p("hunter_birdhouses", "birdhouse emptied", "birdhouses emptied", none(),
                "Selects the highest birdhouse tier unlocked by Hunter level. Each run normally contains four houses, so the UI can later group this count into runs.",
                "bird_house"));
        add(p("hunter_salamanders", "salamander caught", "salamanders caught", none(),
                "Black salamanders are deliberately excluded because they are a Wilderness method.",
                "swamp_lizard", "orange_salamander", "red_salamander"));
        add(p("hunter_chins", "chinchompa caught", "chinchompas caught", none(), null,
                ":chinchompa"));
        add(p("hunter_red_chins", "red chinchompa caught", "red chinchompas caught", none(), null,
                "carnivorous_chinchompa"));
    }

    private void production()
    {
        add(p("cooking_wines", "jug of wine made", "jugs of wine made",
                fixed("Grapes", 1.0),
                "Also needs one jug of water per action. Wine fermentation is delayed but XP is still deterministic per successful batch.",
                "jug_of_wine"));
        add(p("cooking_karambwan_1t", "karambwan cooked", "karambwans cooked",
                rawAction(),
                "Raw quantity should include any burn allowance appropriate to current Cooking level/location; a dedicated high-level burn model is still preferred.",
                "cooked_karambwan"));

        add(p("smithing_f2p_platebodies", "platebody smithed", "platebodies smithed",
                barForSmithing(),
                "Each platebody uses five bars. Strategist converts the action count into the matching bar total.",
                "platebody"));
        add(p("smithing_cannonballs", "steel bar processed", "steel bars processed",
                fixed("Steel bar", 1.0),
                "RuneLite's cannonball action XP is per steel bar processed.",
                "cannonball"));
        add(p("smithing_dart_tips", "bar smithed", "bars smithed",
                barForSmithing(),
                "The action XP is per bar used to smith dart tips.",
                "dart_tip"));

        add(p("crafting_gems", "gem cut", "gems cut", uncutGem(), null,
                "sapphire", "emerald", "ruby", "diamond", "dragonstone", "onyx", "zenyte"));

        add(p("fletching_arrow_shafts", "arrow shaft made", "arrow shafts made",
                fixed("Logs", 1.0 / 15.0),
                "One normal log makes 15 arrow shafts; the action calculator records XP per shaft.",
                "arrow_shaft"));
        add(p("fletching_bows", "unstrung bow fletched", "unstrung bows fletched",
                logForBow(),
                "Uses unstrung shortbow/longbow actions so the supply count is logs, not bowstrings.",
                "shortbow_u", "longbow_u"));
        add(p("fletching_broad_arrows", "broad arrow made", "broad arrows made",
                fixed("Headless arrow", 1.0),
                "Also needs one broad arrowhead per arrow.",
                "broad_arrows"));

        add(p("firemaking_f2p_logs", "log burned", "logs burned", actionItem(), null,
                "logs"));

        add(p("runecraft_f2p_body", "essence crafted", "essence crafted",
                fixed("Rune essence", 1.0),
                "Runecraft XP is based on essence crafted, not the number of runes produced by level multipliers.",
                "body_rune"));
        add(p("runecraft_lava", "essence crafted", "essence crafted",
                fixed("Pure essence", 1.0), null,
                "lava_rune"));
        add(p("runecraft_blood", "essence crafted", "essence crafted",
                fixed("Pure essence", 1.0), null,
                "blood_rune"));
        add(p("runecraft_soul", "essence crafted", "essence crafted",
                fixed("Pure essence", 1.0), null,
                "soul_rune"));

        add(p("herblore_prayer_potions", "potion made", "potions made",
                fixed("Ranarr weed", 1.0),
                "Also needs one snape grass and one vial of water per potion.",
                "prayer_potion"));
        add(p("herblore_restores", "super restore made", "super restores made",
                fixed("Snapdragon", 1.0),
                "Also needs one red spiders' eggs and one vial of water per potion.",
                "super_restore"));
        add(p("herblore_brews", "Saradomin brew made", "Saradomin brews made",
                fixed("Toadflax", 1.0),
                "Also needs one crushed nest and one vial of water per potion.",
                "saradomin_brew"));

        add(p("construction_oak_larders", "oak larder built", "oak larders built",
                fixed("Oak plank", 8.0), null,
                "oak_larder"));
        add(p("construction_oak_doors", "oak dungeon door built", "oak dungeon doors built",
                fixed("Oak plank", 10.0), null,
                "oak_dungeon_door"));
        add(p("construction_mahogany_tables", "mahogany table built", "mahogany tables built",
                fixed("Mahogany plank", 6.0), null,
                "mahogany_table"));
    }

    private void utility()
    {
        add(p("magic_high_alch", "High Alchemy cast", "High Alchemy casts",
                fixed("Nature rune", 1.0),
                "Each cast also needs five fire runes unless an equipped staff supplies them.",
                "high_level_alchemy", "high_alchemy"));
        add(p("magic_f2p_curse", "curse cast", "curse casts", none(),
                "Rune requirements depend on the exact curse spell selected; Strategist will not invent a rune quantity until that spell is resolved.",
                "curse"));

        add(new MethodExecutionProfile(
                "prayer_f2p_bones", "bone buried", "bones buried",
                1.0, MethodExecutionProfile.InputMode.ACTION_ITEM,
                null, 0.0,
                "Selects the highest sensible F2P bone action available to the calculator.",
                "bones"));
        add(new MethodExecutionProfile(
                "prayer_gilded_altar", "bone offered", "bones offered",
                3.5, MethodExecutionProfile.InputMode.ACTION_ITEM,
                null, 0.0,
                "Applies the standard lit gilded-altar 3.5x base Prayer XP multiplier to the selected bone action.",
                "dragon_bones"));

        add(p("thieving_fruit_stalls", "successful steal", "successful steals", none(), null,
                "fruit_stall"));
        add(p("thieving_ardy_knights", "successful pickpocket", "successful pickpockets", none(),
                "Failed pickpockets do not award XP, so this is the number of successful pickpockets needed.",
                "ardougne_knight", "knight_of_ardougne"));
    }

    private MethodExecutionProfile p(
            String id,
            String singular,
            String plural,
            MethodExecutionProfile input,
            String note,
            String... terms)
    {
        return new MethodExecutionProfile(
                id, singular, plural, 1.0,
                input.getInputMode(), input.getFixedInputName(),
                input.getFixedInputPerAction(), note, terms);
    }

    private static MethodExecutionProfile none()
    {
        return input(MethodExecutionProfile.InputMode.NONE, null, 0.0);
    }

    private static MethodExecutionProfile actionItem()
    {
        return input(MethodExecutionProfile.InputMode.ACTION_ITEM, null, 0.0);
    }

    private static MethodExecutionProfile rawAction()
    {
        return input(MethodExecutionProfile.InputMode.RAW_ACTION_ITEM, null, 0.0);
    }

    private static MethodExecutionProfile logForBow()
    {
        return input(MethodExecutionProfile.InputMode.LOG_FOR_BOW, null, 0.0);
    }

    private static MethodExecutionProfile barForSmithing()
    {
        return input(MethodExecutionProfile.InputMode.BAR_FOR_SMITHED_ITEM, null, 0.0);
    }

    private static MethodExecutionProfile uncutGem()
    {
        return input(MethodExecutionProfile.InputMode.UNCUT_GEM, null, 0.0);
    }

    private static MethodExecutionProfile fixed(String name, double perAction)
    {
        return input(MethodExecutionProfile.InputMode.FIXED, name, perAction);
    }

    private static MethodExecutionProfile input(
            MethodExecutionProfile.InputMode mode,
            String name,
            double perAction)
    {
        return new MethodExecutionProfile(
                "__input__", "action", "actions", 1.0,
                mode, name, perAction, null);
    }

    private void add(MethodExecutionProfile profile)
    {
        profiles.put(profile.getMethodId(), profile);
    }
}

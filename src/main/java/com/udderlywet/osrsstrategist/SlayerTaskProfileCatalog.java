package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;

/** Stable task mechanics used to enrich live Slayer assignments. */
@Singleton
public class SlayerTaskProfileCatalog
{
    private final List<SlayerTaskProfile> profiles = Arrays.asList(
            p("dust-devils", a("dust devil", "dust devils"),
                    a("Slayer helmet", "Slayer helmet (i)", "Facemask"),
                    "Catacombs of Kourend unless the live assignment specifies another area.",
                    "Magic bursting/barraging is strong in multicombat when the build, rune supply and budget support it; otherwise use a build-legal conventional style.",
                    "A facemask or Slayer helmet must be worn. Never route to the Wilderness Slayer Cave unless Wilderness methods are explicitly enabled.",
                    CapabilityState.UNKNOWN, CapabilityState.VERIFIED, true,
                    Collections.emptyList(),
                    "Do not skip automatically: compare rune/prayer cost and the assigned location against the account's current goals."),
            p("aberrant-spectres", a("aberrant spectre", "aberrant spectres"),
                    a("Slayer helmet", "Slayer helmet (i)", "Nose peg"),
                    "Slayer Tower or another non-Wilderness location allowed by the assignment.",
                    "Use a build-legal style and protect against their Magic attacks when appropriate.",
                    "A nose peg or Slayer helmet is mandatory protection against their stat-draining fumes."),
            p("gargoyles", a("gargoyle", "gargoyles"),
                    a("Rock hammer", "Rock thrownhammer", "Granite hammer"),
                    "Slayer Tower. Use the basement only when the live task/access state supports it.",
                    "Melee is a practical default; crush and Magic are useful weaknesses, but owned gear should decide the actual style.",
                    "A rock hammer or equivalent is required to finish ordinary gargoyles. Gargoyle Smasher can automate the finishing blow when unlocked."),
            p("kurasks", a("kurask", "kurasks"),
                    a("Leaf-bladed spear", "Leaf-bladed sword", "Leaf-bladed battleaxe", "Broad bolts", "Broad arrows", "Slayer's staff"),
                    "Fremennik Slayer Dungeon unless the assignment specifies another legal location.",
                    "Use a leaf-bladed weapon, broad ammunition, or Magic Dart. Ordinary weapons cannot damage Kurasks.",
                    "This task has a hard weapon restriction, so at least one legal damage option must be verified before the setup is ready."),
            p("turoths", a("turoth", "turoths"),
                    a("Leaf-bladed spear", "Leaf-bladed sword", "Leaf-bladed battleaxe", "Broad bolts", "Broad arrows", "Slayer's staff"),
                    "Fremennik Slayer Dungeon unless the live assignment specifies another legal location.",
                    "Use a leaf-bladed weapon, broad ammunition, or Magic Dart.",
                    "Turoths share the leafy damage restriction with Kurasks, so ordinary weapons should never be suggested."),
            p("banshees", a("banshee", "banshees", "twisted banshee", "twisted banshees"),
                    a("Slayer helmet", "Slayer helmet (i)", "Earmuffs"),
                    "Use the safest reachable location allowed by the task.",
                    "Use any build-legal combat style after the hearing protection requirement is satisfied.",
                    "Earmuffs or a Slayer helmet protect against the Banshee scream mechanic."),
            p("cockatrices", a("cockatrice", "cockatrices"),
                    a("Mirror shield"),
                    "Fremennik Slayer Dungeon unless the task specifies another legal area.",
                    "Use a one-handed build-legal weapon with the mirror shield.",
                    "The mirror-shield mechanic constrains the off-hand slot, so two-handed weapon recommendations should not be used by default."),
            p("basilisks", a("basilisk", "basilisks"),
                    a("Mirror shield"),
                    "Fremennik Slayer Dungeon for ordinary basilisks unless the assignment points to another variant.",
                    "Use a one-handed build-legal weapon with the required gaze protection for ordinary basilisks.",
                    "Basilisk Knights have separate quest access and setup and should not be treated as identical to ordinary basilisks."),
            p("wall-beasts", a("wall beast", "wall beasts"),
                    a("Spiny helmet", "Slayer helmet", "Slayer helmet (i)"),
                    "Lumbridge Swamp Caves.",
                    "Use any build-legal style after head protection is equipped.",
                    "A spiny helmet or Slayer helmet protects from the wall-beast grab while moving through their attack points."),
            p("rockslugs", a("rockslug", "rockslugs", "rock slug", "rock slugs"),
                    a("Bag of salt", "Brine sabre"),
                    "Fremennik Slayer Dungeon unless the live task location overrides it.",
                    "Use a build-legal style and keep the finishing item available.",
                    "Rockslugs need salt or an appropriate alternative to finish them at low Hitpoints."),
            p("desert-lizards", a("desert lizard", "desert lizards"),
                    a("Ice cooler"),
                    "Use the Kharidian Desert task area allowed by the assignment.",
                    "Use a build-legal style and keep Ice coolers in the inventory.",
                    "Desert lizards require an Ice cooler to finish them at low Hitpoints."),
            p("nechryaels", a("nechryael", "nechryaels", "greater nechryael", "greater nechryaels"),
                    Collections.emptyList(),
                    "Catacombs of Kourend is a strong non-Wilderness option when reachable and allowed by the assignment.",
                    "Multitarget Magic can be efficient when the build and rune supply support it; otherwise use a legal conventional style.",
                    "Variants and summons make fixed supply or kill-time estimates unreliable.",
                    CapabilityState.UNKNOWN, CapabilityState.VERIFIED, false,
                    Collections.emptyList(),
                    "Keep or extend only when the live location, rune supply, and combat goals make multitarget Magic worthwhile."),
            p("bloodvelds", a("bloodveld", "bloodvelds", "mutated bloodveld", "mutated bloodvelds"),
                    Collections.emptyList(),
                    "Prefer a safe reachable non-Wilderness location that matches the live assignment.",
                    "Choose the style from owned gear and the exact assigned location.",
                    "Locations differ enough that cannon or multicombat availability needs live location evidence."),
            p("cave-horrors", a("cave horror", "cave horrors"),
                    a("Witchwood icon", "Slayer helmet", "Slayer helmet (i)"),
                    "Mos Le'Harmless Caves after Cabin Fever.",
                    "Use a build-legal style while the required head/neck protection is equipped.",
                    "A witchwood icon or Slayer helmet protects against the cave horror scream; Mos Le'Harmless access must also be verified.",
                    CapabilityState.UNKNOWN, CapabilityState.UNKNOWN, false,
                    a("Black mask"), "For Iron accounts, the black mask is a major Slayer progression objective."),
            p("mogres", a("mogre", "mogres"),
                    a("Fishing explosive", "Super fishing explosive"),
                    "Mudskipper Point after the Mogre miniquest setup is complete.",
                    "Use a build-legal combat style after luring a mogre with the explosive.",
                    "The fishing explosive is used on an ominous fishing spot to make a mogre appear."),
            p("killerwatts", a("killerwatt", "killerwatts"),
                    a("Insulated boots"),
                    "Killerwatt plane through the Draynor Manor portal.",
                    "Use a build-legal style with insulated boots equipped.",
                    "Insulated boots are mandatory protection from the plane's electrical damage."),
            p("fever-spiders", a("fever spider", "fever spiders"),
                    a("Slayer gloves"),
                    "Braindeath Island brewery basement after Rum Deal access is verified.",
                    "Use a build-legal style with Slayer gloves equipped.",
                    "Slayer gloves are mandatory; verify Rum Deal access before travelling."),
            p("brine-rats", a("brine rat", "brine rats"),
                    Collections.emptyList(),
                    "Brine Rat Cavern after Olaf's Quest.",
                    "Use a build-legal sustainable style; the exact weapon should come from owned gear.",
                    "The cavern is quest-gated, so Olaf's Quest completion must be observed before routing here.",
                    CapabilityState.UNKNOWN, CapabilityState.UNKNOWN, false,
                    a("Brine sabre"), "Iron accounts may value the brine sabre drop for underwater combat progression."),
            p("skeletal-wyverns", a("skeletal wyvern", "skeletal wyverns"),
                    a("Elemental shield", "Mind shield", "Dragonfire shield", "Ancient wyvern shield"),
                    "Asgarnian Ice Dungeon.",
                    "Use a one-handed build-legal weapon with a verified wyvern-breath shield.",
                    "Ordinary antifire protection does not replace a valid wyvern shield; the off-hand requirement rules out two-handed defaults."),
            p("smoke-devils", a("smoke devil", "smoke devils", "thermonuclear smoke devil"),
                    a("Slayer helmet", "Slayer helmet (i)", "Facemask"),
                    "Smoke Devil Dungeon for the ordinary task unless a live boss choice is explicit.",
                    "Multitarget Magic is effective for ordinary smoke devils when runes, spellbook, prayer and location are verified.",
                    "A facemask or Slayer helmet is mandatory. Do not silently turn the assignment into a boss task.",
                    CapabilityState.UNKNOWN, CapabilityState.VERIFIED, false,
                    Collections.emptyList(), "Compare barrage cost with the player's Slayer and Magic goals before extending."),
            p("wyrms", a("wyrm", "wyrms"), Collections.emptyList(),
                    "Mount Karuulm; verify heat protection before entering the dungeon.",
                    "Use a build-legal ranged or melee setup selected from owned gear.",
                    "Karuulm dungeon heat protection/access must be verified; do not assume generic boots satisfy it."),
            p("drakes", a("drake", "drakes"), Collections.emptyList(),
                    "Mount Karuulm; verify heat protection before entering the dungeon.",
                    "Use a sustainable build-legal setup and verify the chosen anti-dragonfire coverage.",
                    "Drake breath and Karuulm heat are separate preparation concerns; neither should be inferred from a generic combat loadout."),
            p("hydras", a("hydra", "hydras", "alchemical hydra"), Collections.emptyList(),
                    "Mount Karuulm. Ordinary hydras and the Alchemical Hydra are different encounters.",
                    "Use the assignment's ordinary-monster setup unless the live state explicitly selects the boss encounter.",
                    "Verify Karuulm heat protection and do not promote a generic hydra assignment to boss readiness."),
            p("vampyres", a("vampyre", "vampyres", "vampire", "vampires"),
                    a("Silver weapon", "Ivandis flail", "Blisterwood flail"),
                    "Use a reachable non-Wilderness Morytania location matching the assigned vampyre tier.",
                    "The legal weapon depends on the exact vampyre tier and quest progression.",
                    "Vampyre tiers have different immunity and weapon rules, so verify the live variant before choosing a weapon."),
            p("waterfiends", a("waterfiend", "waterfiends"), Collections.emptyList(),
                    "Ancient Cavern only after its access route is verified; use another assigned legal location when applicable.",
                    "Crush is generally appropriate, but the actual weapon must be build-legal and owned.",
                    "Ancient Cavern access and aggressive surrounding monsters make this an access/setup check, not a generic low-risk location."),
            p("kalphites", a("kalphite", "kalphites"), Collections.emptyList(),
                    "Use the safest task-valid non-Wilderness lair supported by live access evidence.",
                    "Choose a sustainable build-legal style for the exact kalphite variant.",
                    "Cannon and multicombat suitability depend on the selected room; Kalphite Queen is a separate PvM encounter."),
            p("dagannoths", a("dagannoth", "dagannoths"), Collections.emptyList(),
                    "Choose a task-valid non-Wilderness location whose access is already proven.",
                    "Use a sustainable legal style; multitarget options depend on the exact room.",
                    "Lighthouse, Catacombs and Waterbirth variants have different access and combat context; Dagannoth Kings are separate PvM encounters."),
            p("black-demons", a("black demon", "black demons", "greater demon", "greater demons"),
                    Collections.emptyList(),
                    "Use a reachable task-valid non-Wilderness location unless risk is explicitly enabled.",
                    "Use a safespot or sustainable build-legal setup appropriate to the exact variant.",
                    "Demonic gorillas, tormented demons and Wilderness variants require their own quest, mechanics and risk evidence."));

    public SlayerTaskProfile profileFor(String taskName)
    {
        String task = n(taskName);
        if (task.isEmpty()) return null;
        for (SlayerTaskProfile profile : profiles)
            for (String alias : profile.getAliases())
            {
                String candidate = n(alias);
                if (task.equals(candidate) || task.contains(candidate)
                        || candidate.contains(task)) return profile;
            }
        return null;
    }

    public List<SlayerTaskProfile> all() { return profiles; }

    private static SlayerTaskProfile p(String id, List<String> aliases,
            List<String> required, String location, String style, String note)
    {
        return new SlayerTaskProfile(id, aliases, required, location, style, note);
    }

    private static SlayerTaskProfile p(String id, List<String> aliases,
            List<String> required, String location, String style, String note,
            CapabilityState cannon, CapabilityState multiTargetMagic,
            boolean wildernessVariant, List<String> ironObjectives,
            String decisionGuidance)
    {
        return new SlayerTaskProfile(id, aliases, required, location, style, note,
                cannon, multiTargetMagic, wildernessVariant, ironObjectives,
                decisionGuidance);
    }

    private static List<String> a(String... values) { return Arrays.asList(values); }
    private static String n(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

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
                    "A facemask or Slayer helmet must be worn. Never route to the Wilderness Slayer Cave unless Wilderness methods are explicitly enabled."),
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
                    "Variants and summons make fixed supply or kill-time estimates unreliable."),
            p("bloodvelds", a("bloodveld", "bloodvelds", "mutated bloodveld", "mutated bloodvelds"),
                    Collections.emptyList(),
                    "Prefer a safe reachable non-Wilderness location that matches the live assignment.",
                    "Choose the style from owned gear and the exact assigned location.",
                    "Locations differ enough that Strategist should not assume cannon or multicombat availability without location evidence."));

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

    private static List<String> a(String... values) { return Arrays.asList(values); }
    private static String n(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

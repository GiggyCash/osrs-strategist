package com.udderlywet.osrsstrategist;

import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Reviewable strategy metadata for tasks where current sources support a useful
 * decision. Missing metadata fails closed to PREP_FIRST, never to a guessed
 * skip/block verdict.
 *
 * <p>Sources: https://oldschool.runescape.wiki/w/Slayer_training,
 * https://oldschool.runescape.wiki/w/Duradel/Slayer_assignments, and the
 * matching Slayer_task pages (verified 2026-08-28).</p>
 */
@Singleton
public class SlayerTaskStrategicCatalog
{
    private static final Set<String> STRONG_XP = ids(
            "aquanites", "araxytes", "custodian-stalkers", "fire-giants",
            "gryphons", "lizardmen", "lesser-nagua", "venators");
    private static final Set<String> STRONG_RESOURCES = ids(
            "araxytes", "chaos-druids", "flesh-crawlers", "frost-dragons",
            "green-dragons", "lava-dragons", "lizardmen", "red-dragons",
            "revenants", "venators");
    private static final Set<String> HIGH_BURDEN = ids(
            "aquanites", "crocodiles", "custodian-stalkers", "frost-dragons",
            "jungle-horrors", "metal-dragons", "scabarites", "sea-snakes",
            "terror-dogs", "venators");
    private static final Set<String> HIGH_SETUP = ids(
            "aquanites", "araxytes", "aviansies", "custodian-stalkers",
            "frost-dragons", "gryphons", "jungle-horrors", "lava-dragons",
            "revenants", "scabarites", "sea-snakes", "terror-dogs",
            "venators");
    private static final Set<String> LOW_ATTENTION = ids(
            "bats", "bears", "black-knights", "cows", "crabs", "dogs",
            "dwarves", "flesh-crawlers", "ghosts", "ghouls", "goblins",
            "hill-giants", "hobgoblins", "ice-giants", "ice-warriors",
            "icefiends", "lesser-demons", "minotaurs", "moss-giants",
            "pirates", "rogues", "scorpions", "shades", "skeletons",
            "wolves", "zombies");
    private static final Set<String> INTRINSIC_WILDERNESS = ids(
            "lava-dragons", "mammoths", "revenants");
    private static final Map<String, String> DIRECT_BOSS_IDS = bossIds();

    private final SlayerTaskProfileCatalog taskProfiles;
    private final Map<String, SlayerTaskStrategicProfile> byProfileId;

    @Inject
    public SlayerTaskStrategicCatalog(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null
                ? new SlayerTaskProfileCatalog() : taskProfiles;
        Map<String, SlayerTaskStrategicProfile> values = new HashMap<>();
        add(values, taskEquipped("dust-devils", 5, 4, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 5, "nieve", 6, "chaeldar", 9, "konar", 6), null));
        add(values, task("nechryaels", 5, 4, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 9, "nieve", 7, "chaeldar", 12, "konar", 7), null));
        add(values, taskEquipped("smoke-devils", 5, 3, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 9, "nieve", 7, "konar", 7),
                alt("pvm:thermonuclear_smoke_devil",
                        "Thermonuclear smoke devil", "Smoke Devil Dungeon")));
        add(values, task("bloodvelds", 3, 4, 3, 2, AttentionLevel.LOW,
                weights("duradel", 8, "nieve", 9, "chaeldar", 8, "konar", 9), null));
        add(values, task("gargoyles", 2, 4, 4, 2, AttentionLevel.LOW,
                weights("duradel", 8, "nieve", 6, "chaeldar", 11, "konar", 6),
                alt("pvm:grotesque_guardians", "Grotesque Guardians",
                        "Slayer Tower rooftop")));
        add(values, task("hellhounds", 2, 1, 5, 2, AttentionLevel.LOW,
                weights("duradel", 10, "nieve", 8, "chaeldar", 9, "konar", 8),
                alt("pvm:cerberus", "Cerberus", "Taverley Dungeon")));
        add(values, task("abyssal-demons", 4, 4, 5, 4, AttentionLevel.ACTIVE,
                weights("duradel", 12, "nieve", 9, "chaeldar", 12, "konar", 9),
                alt("pvm:abyssal_sire",
                        "Abyssal Sire", "Abyssal Nexus")));
        add(values, task("greater-demons", 2, 2, 4, 2, AttentionLevel.LOW,
                weights("duradel", 9, "nieve", 7, "chaeldar", 9, "konar", 7),
                alt("pvm:kril_tsutsaroth", "K'ril Tsutsaroth",
                        "God Wars Dungeon")));
        add(values, task("black-demons", 2, 2, 5, 3, AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 9, "chaeldar", 10, "konar", 9), null));
        add(values, taskEquipped("kurasks", 2, 4, 3, 3, AttentionLevel.MODERATE,
                weights("duradel", 4, "chaeldar", 12, "nieve", 3, "konar", 3), null));
        add(values, taskEquipped("skeletal-wyverns", 2, 4, 5, 4, AttentionLevel.MODERATE,
                weights("duradel", 7, "nieve", 5, "chaeldar", 7, "konar", 5), null));
        add(values, task("drakes", 2, 3, 5, 4, AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 7, "konar", 10), null));
        add(values, task("kalphites", 4, 1, 2, 2, AttentionLevel.MODERATE,
                weights("duradel", 9, "nieve", 9, "chaeldar", 11, "konar", 9),
                alt("pvm:kalphite_queen", "Kalphite Queen",
                        "Kalphite Queen lair")));
        add(values, taskEquipped("aberrant-spectres", 2, 4, 4, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 7, "nieve", 6, "chaeldar", 8, "konar", 6), null));
        add(values, taskEquipped("turoths", 2, 3, 3, 3,
                AttentionLevel.MODERATE,
                weights("nieve", 3, "chaeldar", 10, "konar", 3), null));
        add(values, taskEquipped("banshees", 2, 2, 2, 2,
                AttentionLevel.LOW, weights(), null));
        add(values, taskEquipped("cockatrices", 2, 2, 2, 3,
                AttentionLevel.LOW, weights(), null));
        add(values, taskEquipped("basilisks", 2, 3, 4, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 7, "nieve", 6, "chaeldar", 7, "konar", 5), null));
        add(values, taskEquipped("wall-beasts", 1, 1, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("rockslugs", 1, 1, 2, 2, AttentionLevel.LOW,
                weights(), null));
        add(values, task("desert-lizards", 1, 1, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, taskEquipped("cave-horrors", 2, 3, 4, 4,
                AttentionLevel.MODERATE,
                weights("duradel", 4, "nieve", 5, "chaeldar", 10), null));
        add(values, task("mogres", 1, 1, 2, 3, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("killerwatts", 1, 1, 3, 4, AttentionLevel.ACTIVE,
                weights(), null));
        add(values, task("fever-spiders", 1, 2, 2, 3,
                AttentionLevel.MODERATE, weights("chaeldar", 7), null));
        add(values, task("brine-rats", 2, 2, 3, 3, AttentionLevel.LOW,
                weights("nieve", 3, "chaeldar", 7, "konar", 2), null));
        add(values, task("wyrms", 3, 3, 4, 3, AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 7, "konar", 10), null));
        add(values, task("hydras", 4, 5, 5, 4, AttentionLevel.ACTIVE,
                weights("konar", 10), alt("pvm:alchemical_hydra", "Alchemical Hydra",
                        "Mount Karuulm")));
        add(values, taskEquipped("vampyres", 2, 3, 4, 4,
                AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 6, "konar", 4), null));
        add(values, task("waterfiends", 1, 2, 5, 4,
                AttentionLevel.MODERATE,
                weights("duradel", 2, "konar", 2), null));
        add(values, task("dagannoths", 5, 3, 3, 3, AttentionLevel.ACTIVE,
                weights("duradel", 9, "nieve", 8, "chaeldar", 11, "konar", 8),
                alt("pvm:dagannoth_kings", "Dagannoth Kings",
                        "Waterbirth Island Dungeon")));
        add(values, task("crawling-hands", 1, 1, 2, 1,
                AttentionLevel.LOW, weights(), null));
        add(values, task("cave-crawlers", 1, 2, 2, 2,
                AttentionLevel.LOW, weights(), null));
        add(values, task("cave-slimes", 1, 1, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("pyrefiends", 2, 2, 3, 2,
                AttentionLevel.LOW, weights(), null));
        add(values, task("infernal-mages", 2, 2, 3, 2,
                AttentionLevel.LOW, weights(), null));
        add(values, task("jellies", 3, 2, 3, 3, AttentionLevel.MODERATE,
                weights("chaeldar", 10, "konar", 6), null));
        add(values, task("harpie-bug-swarms", 1, 1, 3, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("zygomites", 2, 3, 3, 3, AttentionLevel.MODERATE,
                weights("duradel", 2, "nieve", 2, "chaeldar", 7, "konar", 2), null));
        add(values, task("dark-beasts", 2, 3, 2, 3, AttentionLevel.AFK,
                weights("duradel", 11, "nieve", 5, "konar", 5), null));
        add(values, taskMagic("cave-kraken", 2, 3, 4, 3, AttentionLevel.LOW,
                weights("duradel", 9, "nieve", 6, "chaeldar", 12, "konar", 9),
                alt("pvm:kraken", "Kraken",
                        "Kraken Cove")));
        add(values, taskEquipped("warped-creatures", 2, 3, 4, 4,
                AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 6, "chaeldar", 6, "konar", 4), null));
        add(values, task("sulphur-lizards", 2, 2, 3, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("spiritual-creatures", 2, 3, 4, 5,
                AttentionLevel.ACTIVE,
                weights("duradel", 7, "nieve", 6, "chaeldar", 12), null));
        add(values, task("elves", 2, 2, 4, 4, AttentionLevel.MODERATE,
                weights("duradel", 4, "nieve", 4, "chaeldar", 8), null));
        add(values, task("ankous", 3, 2, 3, 2, AttentionLevel.LOW,
                weights("duradel", 5, "nieve", 5, "konar", 5), null));
        add(values, task("suqahs", 3, 1, 4, 4, AttentionLevel.MODERATE,
                weights("duradel", 8, "nieve", 8), null));
        add(values, task("trolls", 3, 2, 4, 3, AttentionLevel.MODERATE,
                weights("duradel", 6, "nieve", 6, "chaeldar", 11, "konar", 6), null));
        add(values, taskEquipped("blue-dragons", 2, 4, 4, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 4, "nieve", 4, "chaeldar", 8, "konar", 4),
                alt("pvm:vorkath", "Vorkath", "Ungael")));
        add(values, taskEquipped("black-dragons", 2, 3, 2, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 9, "nieve", 6, "konar", 6),
                alt("pvm:king_black_dragon", "King Black Dragon",
                        "King Black Dragon Lair")));
        add(values, taskEquipped("steel-dragons", 1, 3, 5, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, taskEquipped("mithril-dragons", 1, 3, 3, 5,
                AttentionLevel.ACTIVE, weights(), null));
        add(values, taskEquipped("fossil-island-wyverns", 1, 3, 5, 5,
                AttentionLevel.MODERATE,
                weights("duradel", 7, "nieve", 5, "chaeldar", 7, "konar", 5), null));
        add(values, task("molanisks", 1, 1, 2, 4,
                AttentionLevel.MODERATE, weights(), null));
        addReviewedLongTail(values);
        this.byProfileId = Collections.unmodifiableMap(values);
    }

    public SlayerTaskStrategicCatalog()
    {
        this(new SlayerTaskProfileCatalog());
    }

    public SlayerTaskStrategicProfile profileFor(String taskName)
    {
        SlayerTaskProfile mechanics = taskProfiles.profileFor(taskName);
        return mechanics == null ? null : byProfileId.get(mechanics.getId());
    }

    public int size()
    {
        return byProfileId.size();
    }

    public Collection<SlayerTaskStrategicProfile> all()
    {
        return byProfileId.values();
    }

    private static void add(Map<String, SlayerTaskStrategicProfile> values,
            SlayerTaskStrategicProfile profile)
    {
        values.put(profile.getTaskProfileId(), profile);
    }

    /**
     * Completes the mechanics census with conservative, reviewed ordinal
     * economics. Exact weights are included only where the current assignment
     * tables establish them; unknown weights remain unknown and can never
     * trigger a block recommendation.
     */
    private void addReviewedLongTail(
            Map<String, SlayerTaskStrategicProfile> values)
    {
        for (SlayerTaskProfile mechanics : taskProfiles.all())
        {
            String id = mechanics.getId();
            if (values.containsKey(id)) continue;
            String bossId = DIRECT_BOSS_IDS.get(id);
            if (bossId != null)
            {
                String name = mechanics.getAliases().isEmpty()
                        ? id : mechanics.getAliases().get(0);
                add(values, directBoss(id, bossId, name,
                        mechanics.getPreferredLocation(),
                        mechanics.isWildernessVariantKnown()));
                continue;
            }

            int xp = STRONG_XP.contains(id) ? 4 : 2;
            int resources = STRONG_RESOURCES.contains(id) ? 4 : 2;
            int burden = HIGH_BURDEN.contains(id) ? 4 : 2;
            int setup = HIGH_SETUP.contains(id) ? 4 : 2;
            AttentionLevel attention = LOW_ATTENTION.contains(id)
                    ? AttentionLevel.LOW : AttentionLevel.MODERATE;
            RiskLevel risk = INTRINSIC_WILDERNESS.contains(id)
                    ? RiskLevel.HIGH : RiskLevel.LOW;
            add(values, new SlayerTaskStrategicProfile(id, xp, resources,
                    burden, setup, attention, risk,
                    mechanics.getRequiredProtection().isEmpty()
                            ? SlayerRequiredItemUse.CARRIED_OR_EQUIPPED
                            : SlayerRequiredItemUse.EQUIPPED,
                    null, reviewedWeights(id), null, null, null, false));
        }
    }

    private static SlayerTaskStrategicProfile directBoss(String id,
            String activityId, String name, String location,
            boolean wilderness)
    {
        return new SlayerTaskStrategicProfile(id, 3, 3, 4, 5,
                AttentionLevel.ACTIVE,
                wilderness ? RiskLevel.HIGH : RiskLevel.MEDIUM,
                SlayerRequiredItemUse.CARRIED_OR_EQUIPPED, null, weights(),
                activityId, name, location, true);
    }

    private static Map<String, Integer> reviewedWeights(String id)
    {
        switch (id)
        {
            case "aquanites":
                return weights("duradel", 5, "nieve", 5);
            case "araxytes":
                return weights("duradel", 10, "nieve", 8);
            case "aviansies":
                return weights("duradel", 8);
            case "custodian-stalkers":
                return weights("chaeldar", 11, "nieve", 8);
            case "fire-giants":
                return weights("duradel", 7);
            case "frost-dragons":
                return weights("duradel", 5, "nieve", 5);
            case "gryphons":
                return weights("duradel", 7, "nieve", 7,
                        "chaeldar", 10, "vannaka", 10);
            case "lesser-nagua":
                return weights("chaeldar", 4);
            default:
                return weights();
        }
    }

    private static Set<String> ids(String... values)
    {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static Map<String, String> bossIds()
    {
        Map<String, String> values = new HashMap<>();
        putBoss(values, "abyssal-sire-task", "abyssal_sire");
        putBoss(values, "araxxor-task", "araxxor");
        putBoss(values, "barrows-task", "barrows_chests");
        putBoss(values, "callisto-task", "callisto");
        putBoss(values, "cerberus-task", "cerberus");
        putBoss(values, "chaos-elemental-task", "chaos_elemental");
        putBoss(values, "chaos-fanatic-task", "chaos_fanatic");
        putBoss(values, "crazy-archaeologist-task", "crazy_archaeologist");
        putBoss(values, "deranged-archaeologist-task", "deranged_archaeologist");
        putBoss(values, "duke-sucellus-task", "duke_sucellus");
        putBoss(values, "giant-mole-task", "giant_mole");
        putBoss(values, "graardor-task", "general_graardor");
        putBoss(values, "grotesque-guardians-task", "grotesque_guardians");
        putBoss(values, "jad-task", "tztok_jad");
        putBoss(values, "kreearra-task", "kreearra");
        putBoss(values, "kril-task", "kril_tsutsaroth");
        putBoss(values, "leviathan-task", "the_leviathan");
        putBoss(values, "maggot-king-task", "maggot_king");
        putBoss(values, "phantom-muspah-task", "phantom_muspah");
        putBoss(values, "sarachnis-task", "sarachnis");
        putBoss(values, "scorpia-task", "scorpia");
        putBoss(values, "shellbane-gryphon-task", "shellbane_gryphon");
        putBoss(values, "vardorvis-task", "vardorvis");
        putBoss(values, "venenatis-task", "venenatis");
        putBoss(values, "vetion-task", "vetion");
        putBoss(values, "vorkath-task", "vorkath");
        putBoss(values, "whisperer-task", "the_whisperer");
        putBoss(values, "zilyana-task", "commander_zilyana");
        putBoss(values, "zuk-task", "tzkal_zuk");
        putBoss(values, "zulrah-task", "zulrah");
        return Collections.unmodifiableMap(values);
    }

    private static void putBoss(Map<String, String> values, String taskId,
            String activityId)
    {
        values.put(taskId, "pvm:" + activityId);
    }

    private static SlayerTaskStrategicProfile task(String id, int xp,
            int resources, int length, int setup, AttentionLevel attention,
            Map<String, Integer> weights, Alternative alternative)
    {
        return new SlayerTaskStrategicProfile(id, xp, resources, length, setup,
                attention, RiskLevel.LOW,
                SlayerRequiredItemUse.CARRIED_OR_EQUIPPED, weights,
                alternative == null ? null : alternative.id,
                alternative == null ? null : alternative.name,
                alternative == null ? null : alternative.location);
    }

    private static SlayerTaskStrategicProfile taskEquipped(String id, int xp,
            int resources, int length, int setup, AttentionLevel attention,
            Map<String, Integer> weights, Alternative alternative)
    {
        return new SlayerTaskStrategicProfile(id, xp, resources, length, setup,
                attention, RiskLevel.LOW, SlayerRequiredItemUse.EQUIPPED,
                weights, alternative == null ? null : alternative.id,
                alternative == null ? null : alternative.name,
                alternative == null ? null : alternative.location);
    }

    private static SlayerTaskStrategicProfile taskMagic(String id, int xp,
            int resources, int length, int setup, AttentionLevel attention,
            Map<String, Integer> weights, Alternative alternative)
    {
        return new SlayerTaskStrategicProfile(id, xp, resources, length, setup,
                attention, RiskLevel.LOW,
                SlayerRequiredItemUse.CARRIED_OR_EQUIPPED, CombatStyle.MAGIC,
                weights, alternative == null ? null : alternative.id,
                alternative == null ? null : alternative.name,
                alternative == null ? null : alternative.location);
    }

    private static Alternative alt(String id, String name, String location)
    {
        return new Alternative(id, name, location);
    }

    private static Map<String, Integer> weights(Object... pairs)
    {
        Map<String, Integer> values = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
            values.put((String) pairs[i], (Integer) pairs[i + 1]);
        return values;
    }

    private static final class Alternative
    {
        private final String id;
        private final String name;
        private final String location;

        private Alternative(String id, String name, String location)
        {
            this.id = id;
            this.name = name;
            this.location = location;
        }
    }
}

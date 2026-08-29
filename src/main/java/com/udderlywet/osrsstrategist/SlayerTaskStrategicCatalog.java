package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    private final SlayerTaskProfileCatalog taskProfiles;
    private final Map<String, SlayerTaskStrategicProfile> byProfileId;

    @Inject
    public SlayerTaskStrategicCatalog(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null
                ? new SlayerTaskProfileCatalog() : taskProfiles;
        Map<String, SlayerTaskStrategicProfile> values = new HashMap<>();
        add(values, taskEquipped("dust-devils", 5, 4, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 5), null));
        add(values, task("nechryaels", 5, 4, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 9), null));
        add(values, taskEquipped("smoke-devils", 5, 3, 3, 4, AttentionLevel.ACTIVE,
                weights("duradel", 9), null));
        add(values, task("bloodvelds", 3, 4, 3, 2, AttentionLevel.LOW,
                weights("duradel", 8), null));
        add(values, task("gargoyles", 2, 4, 4, 2, AttentionLevel.LOW,
                weights("duradel", 8, "nieve", 6, "chaeldar", 11), null));
        add(values, task("hellhounds", 2, 1, 5, 2, AttentionLevel.LOW,
                weights("duradel", 10, "nieve", 8, "chaeldar", 9),
                alt("pvm:cerberus", "Cerberus", "Taverley Dungeon")));
        add(values, task("abyssal-demons", 4, 4, 5, 4, AttentionLevel.ACTIVE,
                weights("duradel", 12), null));
        add(values, task("greater-demons", 2, 2, 4, 2, AttentionLevel.LOW,
                weights("duradel", 9, "nieve", 7, "chaeldar", 9),
                alt("pvm:kril_tsutsaroth", "K'ril Tsutsaroth",
                        "God Wars Dungeon")));
        add(values, task("black-demons", 2, 2, 5, 3, AttentionLevel.MODERATE,
                weights("duradel", 8), null));
        add(values, taskEquipped("kurasks", 2, 4, 3, 3, AttentionLevel.MODERATE,
                weights("duradel", 4, "chaeldar", 12, "nieve", 3), null));
        add(values, taskEquipped("skeletal-wyverns", 2, 4, 5, 4, AttentionLevel.MODERATE,
                weights("duradel", 7), null));
        add(values, task("drakes", 2, 3, 5, 4, AttentionLevel.MODERATE,
                weights("duradel", 8), null));
        add(values, task("kalphites", 4, 1, 2, 2, AttentionLevel.MODERATE,
                weights("duradel", 9, "nieve", 9, "chaeldar", 11), null));
        add(values, taskEquipped("aberrant-spectres", 2, 4, 4, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 7, "nieve", 6, "chaeldar", 8), null));
        add(values, taskEquipped("turoths", 2, 3, 3, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, taskEquipped("banshees", 2, 2, 2, 2,
                AttentionLevel.LOW, weights(), null));
        add(values, taskEquipped("cockatrices", 2, 2, 2, 3,
                AttentionLevel.LOW, weights(), null));
        add(values, taskEquipped("basilisks", 2, 3, 4, 3,
                AttentionLevel.MODERATE,
                weights("duradel", 7, "chaeldar", 7), null));
        add(values, taskEquipped("wall-beasts", 1, 1, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("rockslugs", 1, 1, 2, 2, AttentionLevel.LOW,
                weights(), null));
        add(values, task("desert-lizards", 1, 1, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, taskEquipped("cave-horrors", 2, 3, 4, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("mogres", 1, 1, 2, 3, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("killerwatts", 1, 1, 3, 4, AttentionLevel.ACTIVE,
                weights(), null));
        add(values, task("fever-spiders", 1, 2, 2, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("brine-rats", 2, 2, 3, 3, AttentionLevel.LOW,
                weights("chaeldar", 7), null));
        add(values, task("wyrms", 3, 3, 4, 3, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("hydras", 4, 5, 5, 4, AttentionLevel.ACTIVE,
                weights(), alt("pvm:alchemical_hydra", "Alchemical Hydra",
                        "Mount Karuulm")));
        add(values, taskEquipped("vampyres", 2, 3, 4, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("waterfiends", 1, 2, 5, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("dagannoths", 5, 3, 3, 3, AttentionLevel.ACTIVE,
                weights(), alt("pvm:dagannoth_kings", "Dagannoth Kings",
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
                weights(), null));
        add(values, taskEquipped("harpie-bug-swarms", 1, 1, 3, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("zygomites", 2, 3, 3, 3, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("dark-beasts", 2, 3, 2, 3, AttentionLevel.AFK,
                weights(), null));
        add(values, task("cave-kraken", 2, 3, 4, 3, AttentionLevel.LOW,
                weights("duradel", 9), alt("pvm:kraken", "Kraken",
                        "Kraken Cove")));
        add(values, taskEquipped("warped-creatures", 2, 3, 4, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("sulphur-lizards", 2, 2, 3, 3,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("spiritual-creatures", 2, 3, 4, 5,
                AttentionLevel.ACTIVE, weights(), null));
        add(values, task("elves", 2, 2, 4, 4, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("ankous", 3, 2, 3, 2, AttentionLevel.LOW,
                weights("duradel", 5), null));
        add(values, task("suqahs", 3, 1, 4, 4, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("trolls", 3, 2, 4, 3, AttentionLevel.MODERATE,
                weights(), null));
        add(values, task("blue-dragons", 2, 4, 4, 3,
                AttentionLevel.MODERATE, weights("chaeldar", 8),
                alt("pvm:vorkath", "Vorkath", "Ungael")));
        add(values, task("black-dragons", 2, 3, 2, 3,
                AttentionLevel.MODERATE, weights(),
                alt("pvm:king_black_dragon", "King Black Dragon",
                        "King Black Dragon Lair")));
        add(values, task("steel-dragons", 1, 3, 5, 4,
                AttentionLevel.MODERATE, weights(), null));
        add(values, task("mithril-dragons", 1, 3, 3, 5,
                AttentionLevel.ACTIVE, weights(), null));
        add(values, task("fossil-island-wyverns", 1, 3, 5, 5,
                AttentionLevel.MODERATE, weights(), null));
        add(values, taskEquipped("molanisks", 1, 1, 2, 4,
                AttentionLevel.MODERATE, weights(), null));
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

    private static void add(Map<String, SlayerTaskStrategicProfile> values,
            SlayerTaskStrategicProfile profile)
    {
        values.put(profile.getTaskProfileId(), profile);
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

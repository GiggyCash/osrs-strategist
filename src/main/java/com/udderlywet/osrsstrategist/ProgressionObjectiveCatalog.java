package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * Useful outfits, untradeables, currencies, and Collection Log grinds worth
 * protecting from short checkpoint churn.
 *
 * <p>Multiple objectives may intentionally share one method. For example,
 * Motherlode Mine can advance Prospector, the coal bag, and the gem bag. The
 * service therefore evaluates every objective for a method rather than stopping
 * permanently at whichever record happened to be listed first.</p>
 */
@Singleton
public class ProgressionObjectiveCatalog
{
    private final List<ProgressionObjectiveDefinition> objectives = Arrays.asList(
            objective("objective:graceful", "Graceful outfit", "agility_rooftop", ProgressionObjectiveType.OUTFIT),
            objective("objective:graceful", "Graceful outfit", "agility_rooftops", ProgressionObjectiveType.OUTFIT),
            objective("objective:graceful", "Graceful outfit", "agility_canifis_marks", ProgressionObjectiveType.OUTFIT),

            objective("objective:prospector", "Prospector outfit", "mining_mlm", ProgressionObjectiveType.OUTFIT),
            objective("objective:prospector", "Prospector outfit", "mining_motherlode", ProgressionObjectiveType.OUTFIT),
            objective("objective:coal-bag", "Coal bag", "mining_mlm", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:coal-bag", "Coal bag", "mining_motherlode", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:gem-bag", "Gem bag", "mining_mlm", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:gem-bag", "Gem bag", "mining_motherlode", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:raiments", "Raiments of the Eye", "runecraft_gotr", ProgressionObjectiveType.OUTFIT),
            objective("objective:abyssal-needle", "Abyssal needle", "runecraft_gotr", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:lantern", "Abyssal lantern", "runecraft_gotr", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:smiths-uniform", "Smiths' Uniform", "smithing_foundry", ProgressionObjectiveType.OUTFIT),
            objective("objective:smiths-uniform", "Smiths' Uniform", "smithing_giants_foundry", ProgressionObjectiveType.OUTFIT),
            objective("objective:double-ammo-mould", "Double ammo mould", "smithing_giants_foundry", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:tempoross", "Tempoross Collection Log progression", "fishing_tempoross", ProgressionObjectiveType.COLLECTION_LOG),
            objective("objective:fish-barrel", "Fish barrel", "fishing_tempoross", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:tackle-box", "Tackle box", "fishing_tempoross", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:spirit-anglers", "Spirit angler outfit", "fishing_tempoross", ProgressionObjectiveType.OUTFIT),

            objective("objective:wintertodt", "Wintertodt Collection Log progression", "firemaking_wintertodt", ProgressionObjectiveType.COLLECTION_LOG),
            objective("objective:pyromancer", "Pyromancer outfit", "firemaking_wintertodt", ProgressionObjectiveType.OUTFIT),
            objective("objective:tome-fire", "Tome of fire", "firemaking_wintertodt", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:farmers-outfit", "Farmer's outfit", "farming_tithe", ProgressionObjectiveType.OUTFIT),
            objective("objective:seed-box", "Seed box", "farming_tithe", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:herb-sack", "Herb sack", "farming_tithe", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:carpenter", "Carpenter's outfit", "construction_mahogany_homes", ProgressionObjectiveType.OUTFIT),
            objective("objective:plank-sack", "Plank sack", "construction_mahogany_homes", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:rogue", "Rogue equipment", "thieving_rogues_den", ProgressionObjectiveType.OUTFIT),
            objective("objective:angler", "Angler outfit", "fishing_trawler", ProgressionObjectiveType.OUTFIT),

            objective("objective:lumberjack", "Lumberjack outfit", "temple_trekking", ProgressionObjectiveType.OUTFIT),
            objective("objective:lumberjack-forestry", "Lumberjack outfit", "woodcutting_forestry", ProgressionObjectiveType.OUTFIT),
            objective("objective:forestry-outfit", "Forestry outfit", "woodcutting_forestry", ProgressionObjectiveType.OUTFIT),
            objective("objective:forestry-kit", "Forestry kit progression", "woodcutting_forestry", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:guild-hunter-outfit", "Guild hunter outfit", "hunter_rumours", ProgressionObjectiveType.OUTFIT),
            objective("objective:hunter-rumours", "Hunter Rumours reward progression", "hunter_rumours", ProgressionObjectiveType.COLLECTION_LOG),

            objective("objective:void", "Void Knight equipment", "pest_control", ProgressionObjectiveType.OUTFIT),
            objective("objective:elite-void", "Elite Void Knight equipment", "pest_control", ProgressionObjectiveType.OUTFIT),
            objective("objective:fighter-torso", "Fighter torso", "barbarian_assault", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:dragon-defender", "Dragon defender", "warriors_guild", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:imbued-god-cape", "Imbued god cape", "mage_arena_2", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:rune-pouch", "Rune pouch", "slayer_rewards", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:herb-sack-slayer", "Herb sack", "slayer_rewards", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:slayer-helm", "Slayer helmet", "slayer_rewards", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:bonecrusher", "Bonecrusher", "morytania_diary", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:ash-sanctifier", "Ash sanctifier", "kourend_diary", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:explorers-ring", "Explorer's ring progression", "lumbridge_diary", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:ardougne-cloak", "Ardougne cloak progression", "ardougne_diary", ProgressionObjectiveType.UNTRADEABLE),

            objective("objective:crystal-bowfa", "Bow of faerdhinen and crystal armour", "corrupted_gauntlet", ProgressionObjectiveType.GEAR),
            objective("objective:barrows", "Useful Barrows equipment", "barrows", ProgressionObjectiveType.GEAR),
            objective("objective:moons", "Moons of Peril equipment", "perilous_moons", ProgressionObjectiveType.GEAR)
    );

    public List<ProgressionObjectiveDefinition> all()
    {
        return Collections.unmodifiableList(objectives);
    }

    /** Compatibility helper returning the first configured objective. */
    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        List<ProgressionObjectiveDefinition> matches = objectivesForMethod(methodId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public List<ProgressionObjectiveDefinition> objectivesForMethod(String methodId)
    {
        if (methodId == null) return Collections.emptyList();
        List<ProgressionObjectiveDefinition> matches = new ArrayList<>();
        for (ProgressionObjectiveDefinition objective : objectives)
        {
            if (methodId.equals(objective.getMethodId())) matches.add(objective);
        }
        return Collections.unmodifiableList(matches);
    }

    private static ProgressionObjectiveDefinition objective(
            String id, String title, String methodId, ProgressionObjectiveType type)
    {
        return new ProgressionObjectiveDefinition(id, title, methodId, type);
    }
}

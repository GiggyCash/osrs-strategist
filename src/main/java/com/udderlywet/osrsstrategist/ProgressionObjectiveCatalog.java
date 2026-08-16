package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Useful outfits, untradeables, currencies, and collection-log grinds worth protecting. */
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
            objective("objective:gem-bag", "Gem bag", "mining_mlm", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:raiments", "Raiments of the Eye", "runecraft_gotr", ProgressionObjectiveType.OUTFIT),
            objective("objective:abyssal-needle", "Abyssal needle", "runecraft_gotr", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:lantern", "Abyssal lantern", "runecraft_gotr", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:smiths-uniform", "Smiths' Uniform", "smithing_foundry", ProgressionObjectiveType.OUTFIT),
            objective("objective:smiths-uniform", "Smiths' Uniform", "smithing_giants_foundry", ProgressionObjectiveType.OUTFIT),
            objective("objective:double-ammo-mould", "Double ammo mould", "smithing_giants_foundry", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:tempoross", "Tempoross collection-log progression", "fishing_tempoross", ProgressionObjectiveType.COLLECTION_LOG),
            objective("objective:fish-barrel", "Fish barrel", "fishing_tempoross", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:tackle-box", "Tackle box", "fishing_tempoross", ProgressionObjectiveType.UNTRADEABLE),
            objective("objective:spirit-anglers", "Spirit angler outfit", "fishing_tempoross", ProgressionObjectiveType.OUTFIT),
            objective("objective:wintertodt", "Wintertodt collection-log progression", "firemaking_wintertodt", ProgressionObjectiveType.COLLECTION_LOG),
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

    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        if (methodId == null) return null;
        for (ProgressionObjectiveDefinition objective : objectives)
            if (methodId.equals(objective.getMethodId())) return objective;
        return null;
    }

    private static ProgressionObjectiveDefinition objective(
            String id, String title, String methodId, ProgressionObjectiveType type)
    {
        return new ProgressionObjectiveDefinition(id, title, methodId, type);
    }
}

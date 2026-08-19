package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;

/** High-value gear routes; routes describe acquisition, not universal BIS. */
@Singleton
public class GearAcquisitionCatalog
{
    public static final String PROVENANCE =
            "RuneLite 1.12.35 identities and OSRS Wiki item/activity pages; audited 2026-08-19";
    private final Map<String, GearAcquisitionRoute> routes = new LinkedHashMap<>();

    public GearAcquisitionCatalog()
    {
        melee();
        ranged();
        magic();
        slayer();
        skilling();
    }

    public List<GearAcquisitionRoute> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(routes.values()));
    }

    public GearAcquisitionRoute forItem(String itemName)
    {
        return routes.get(normalize(itemName));
    }

    private void melee()
    {
        route("Dragon defender", CombatStyle.MELEE_SLASH, false,
                minigame("Warriors' Guild", "Earn access, then progress through defenders to the dragon defender"),
                skill("Attack and Strength", "Verify the Warriors' Guild entry requirement"));
        route("Fighter torso", CombatStyle.MELEE_SLASH, false,
                minigame("Barbarian Assault", "Earn the required role points and queen kill"));
        route("Barrows gloves", CombatStyle.HYBRID, false,
                quest("Recipe for Disaster", "Complete the required subquests and Culinaromancer fight"));
        route("Fire cape", CombatStyle.MELEE_SLASH, false,
                minigame("Fight Caves", "Prepare for and defeat TzTok-Jad"));
        route("Infernal cape", CombatStyle.MELEE_SLASH, false,
                minigame("Inferno", "Only target after account readiness; player skill remains unknown"),
                verify("Fire cape", "Verify the Fight Caves prerequisite"));
        route("Helm of neitiznot", CombatStyle.MELEE_SLASH, false,
                quest("The Fremennik Isles", "Complete the quest, then obtain the helm from the verified quest source"));
        route("Neitiznot faceguard", CombatStyle.MELEE_SLASH, true,
                resource("Basilisk jaw", "Obtain the jaw from the verified Basilisk Knight route or buy it on a Main"),
                verify("Helm of neitiznot", "Own the base helm before combining"));
        route("Dragon boots", CombatStyle.MELEE_SLASH, true,
                boss("Spiritual mages", "Use a verified God Wars spiritual-mage Slayer route or buy on a Main"));
        route("Primordial boots", CombatStyle.MELEE_SLASH, true,
                boss("Cerberus", "Obtain the primordial crystal or buy it on a Main"),
                verify("Dragon boots", "Own dragon boots before combining"));
        route("Ferocious gloves", CombatStyle.MELEE_SLASH, true,
                boss("Alchemical Hydra", "Obtain hydra leather or buy it on a Main"),
                verify("Barrows gloves", "Use the verified conversion route"));
        route("Avernic defender", CombatStyle.MELEE_SLASH, true,
                boss("Theatre of Blood", "Obtain an avernic defender hilt or buy it on a Main"),
                verify("Dragon defender", "Own a dragon defender before combining"));
        route("Abyssal whip", CombatStyle.MELEE_SLASH, true,
                boss("Abyssal demons", "Obtain from abyssal demons with the Slayer requirement or buy on a Main"));
        route("Osmumten's fang", CombatStyle.MELEE_STAB, true,
                boss("Tombs of Amascut", "Obtain as a raid reward or buy on a Main"));
        route("Arclight", CombatStyle.MELEE_SLASH, false,
                quest("Shadow of the Storm", "Obtain Darklight through the verified quest chain"),
                resource("Ancient shards", "Charge the weapon using self-sourced ancient shards"));
        route("Zombie axe", CombatStyle.MELEE_CRUSH, true,
                quest("Defender of Varrock", "Reach the armoured-zombie access point"),
                boss("Armoured zombies", "Obtain the axe drop or buy it on a Main"));
    }

    private void ranged()
    {
        route("Rune crossbow", CombatStyle.RANGED, true,
                resource("Runite limbs", "Smith or obtain verified runite limbs"),
                resource("Yew stock", "Fletch a yew stock and assemble the crossbow; Mains may buy the finished item"));
        route("Magic shortbow (i)", CombatStyle.RANGED, true,
                resource("Magic shortbow", "Fletch or buy the base bow"),
                resource("Magic shortbow scroll", "Obtain the imbue scroll through its verified reward source"));
        route("Toxic blowpipe", CombatStyle.RANGED, true,
                boss("Zulrah", "Obtain a tanzanite fang or buy the completed weapon on a Main"),
                resource("Zulrah's scales", "Maintain a verified charge supply"));
        route("Bow of faerdhinen", CombatStyle.RANGED, false,
                quest("Song of the Elves", "Unlock Prifddinas and the Gauntlet"),
                boss("The Gauntlet", "Obtain an enhanced crystal weapon seed"),
                resource("Crystal shards", "Gather the shards needed for creation and charging"));
        route("Crystal armour", CombatStyle.RANGED, false,
                quest("Song of the Elves", "Unlock Prifddinas and the Gauntlet"),
                boss("The Gauntlet", "Obtain crystal armour seeds"),
                resource("Crystal shards", "Gather creation and charge resources"));
        route("Ava's accumulator", CombatStyle.RANGED, false,
                quest("Animal Magnetism", "Complete the quest and obtain the device"));
        route("Ava's assembler", CombatStyle.RANGED, false,
                quest("Dragon Slayer II", "Unlock Vorkath"),
                boss("Vorkath", "Obtain Vorkath's head"),
                verify("Ava's accumulator", "Upgrade the existing Ava device"));
        route("Necklace of anguish", CombatStyle.RANGED, true,
                skill("Crafting", "Verify the zenyte jewellery Crafting level or buy on a Main"),
                resource("Zenyte", "Self-source the gem and enchant it on Iron accounts"));
        route("Elite void ranged", CombatStyle.RANGED, false,
                minigame("Pest Control", "Earn Void equipment and the elite upgrade requirements"),
                verify("Western Provinces diary", "Complete the required diary tier before the elite upgrade"));
    }

    private void magic()
    {
        route("Iban's staff", CombatStyle.MAGIC, false,
                quest("Underground Pass", "Complete the quest and obtain the staff"));
        route("Ancient staff", CombatStyle.MAGIC, true,
                quest("Desert Treasure I", "Unlock Ancient Magicks and the verified staff source"));
        route("Warped sceptre", CombatStyle.MAGIC, true,
                quest("The Path of Glouphrie", "Unlock warped creatures"),
                boss("Warped creatures", "Obtain the sceptre or buy it on a Main"));
        route("Trident of the seas", CombatStyle.MAGIC, true,
                boss("Cave kraken", "Obtain while assigned cave kraken or buy it on a Main"),
                resource("Runes and coins", "Verify the charge supply before recommending use"));
        route("Master wand", CombatStyle.MAGIC, true,
                minigame("Mage Training Arena", "Earn the required points in each room"));
        route("Imbued god cape", CombatStyle.MAGIC, false,
                minigame("Mage Arena II", "Complete the Wilderness miniquest with explicit risk acceptance"));
        route("Occult necklace", CombatStyle.MAGIC, true,
                boss("Smoke devils", "Obtain with the Slayer requirement or buy on a Main"));
        route("Tormented bracelet", CombatStyle.MAGIC, true,
                skill("Crafting", "Verify the zenyte jewellery Crafting level or buy on a Main"),
                resource("Zenyte", "Self-source and enchant the bracelet on Iron accounts"));
        route("Elidinis' ward", CombatStyle.MAGIC, true,
                boss("Tombs of Amascut", "Obtain as a raid reward or buy on a Main"));
    }

    private void slayer()
    {
        route("Black mask", CombatStyle.MELEE_SLASH, true,
                quest("Cabin Fever", "Unlock Mos Le'Harmless cave horrors"),
                boss("Cave horrors", "Obtain the mask or buy it on a Main"));
        route("Slayer helmet", CombatStyle.HYBRID, false,
                skill("Crafting 55", "Meet the Crafting requirement"),
                resource("Slayer helmet components", "Unlock Malevolent Masquerade and assemble the verified components"));
        route("Slayer helmet (i)", CombatStyle.HYBRID, false,
                verify("Slayer helmet or black mask", "Own the base equipment"),
                minigame("Imbue source", "Use a currently supported Nightmare Zone, Soul Wars, or scroll route"));
    }

    private void skilling()
    {
        route("Graceful outfit", null, false,
                minigame("Rooftop Agility", "Collect marks of grace while training on eligible rooftop courses"));
        route("Prospector kit", null, false,
                minigame("Motherlode Mine", "Earn golden nuggets and buy the useful outfit pieces"));
        route("Angler's outfit", null, false,
                minigame("Fishing Trawler", "Complete reward rolls until the required pieces are owned"));
        route("Raiments of the Eye", null, false,
                quest("Temple of the Eye", "Unlock Guardians of the Rift"),
                minigame("Guardians of the Rift", "Earn abyssal pearls and buy the outfit pieces"));
        route("Smiths' uniform", null, false,
                minigame("Giants' Foundry", "Earn reputation and buy the uniform pieces"));
    }

    private void route(String item, CombatStyle style, boolean tradeable,
            GearAcquisitionStep... steps)
    {
        String id = normalize(item);
        GearAcquisitionRoute route = new GearAcquisitionRoute(id, item, style,
                tradeable, Arrays.asList(steps),
                "Skip this route when a better owned alternative exists or the time/setup cost is disproportionate to the active goal.",
                PROVENANCE);
        if (routes.put(id, route) != null)
            throw new IllegalStateException("Duplicate gear acquisition target: " + item);
    }

    private static GearAcquisitionStep quest(String target, String action) { return step(GearAcquisitionStep.Kind.QUEST, target, action); }
    private static GearAcquisitionStep skill(String target, String action) { return step(GearAcquisitionStep.Kind.SKILL, target, action); }
    private static GearAcquisitionStep boss(String target, String action) { return step(GearAcquisitionStep.Kind.BOSS, target, action); }
    private static GearAcquisitionStep minigame(String target, String action) { return step(GearAcquisitionStep.Kind.MINIGAME, target, action); }
    private static GearAcquisitionStep resource(String target, String action) { return step(GearAcquisitionStep.Kind.RESOURCE, target, action); }
    private static GearAcquisitionStep verify(String target, String action) { return step(GearAcquisitionStep.Kind.VERIFY, target, action); }
    private static GearAcquisitionStep step(GearAcquisitionStep.Kind kind, String target, String action) { return new GearAcquisitionStep(kind, target, action); }
    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", ""); }
}

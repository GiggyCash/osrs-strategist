package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * Practical gear ladders. BIS entries are contextual targets, never a claim
 * that one outfit or weapon is best against every monster in OSRS.
 */
@Singleton
public class GearProgressionCatalog
{
    private final List<GearProgressionEntry> entries = new ArrayList<>();

    public GearProgressionCatalog()
    {
        seedF2p();
        seedMelee();
        seedRanged();
        seedMagic();
        seedHybrid();
    }

    public List<GearProgressionEntry> all() { return Collections.unmodifiableList(entries); }

    public List<GearProgressionEntry> forStyle(CombatStyle style)
    {
        List<GearProgressionEntry> result = new ArrayList<>();
        for (GearProgressionEntry entry : entries)
            if (entry.getStyle() == style) result.add(entry);
        return Collections.unmodifiableList(result);
    }

    public List<GearProgressionEntry> forContext(String contextId)
    {
        List<GearProgressionEntry> result = new ArrayList<>();
        for (GearProgressionEntry entry : entries)
            if (entry.getContextId().equals(contextId)) result.add(entry);
        return Collections.unmodifiableList(result);
    }

    private void seedF2p()
    {
        add("f2p-melee", "general", CombatStyle.MELEE_SLASH, GearBudgetTier.F2P,
                items("Rune full helm", "Rune platebody or rune chainbody",
                        "Rune platelegs/plateskirt", "Amulet of strength or power", "Rune kiteshield"),
                "Rune scimitar",
                "F2P melee swaps platebody/chainbody and strength/power amulet depending on the target.",
                true, true, true, true);
        add("f2p-ranged", "general", CombatStyle.RANGED, GearBudgetTier.F2P,
                items("Green d'hide body", "Green d'hide chaps",
                        "Green d'hide vambraces", "Amulet of power", "Coif"),
                "Maple shortbow or another legal F2P ranged weapon appropriate to the target",
                "Ammunition economics and target defence matter more than a single static weapon order.",
                true, true, true, true);
        add("f2p-magic", "general", CombatStyle.MAGIC, GearBudgetTier.F2P,
                items("Wizard hat", "Wizard robe top", "Magic amulet"),
                "Elemental staff and strongest economical F2P spell for the target",
                "Magic accuracy, rune cost, and safespot access should drive the practical setup.",
                true, true, true, true);
    }

    private void seedMelee()
    {
        add("melee-budget", "general", CombatStyle.MELEE_SLASH, GearBudgetTier.BUDGET,
                items("Helm of neitiznot", "Fighter torso", "Barrows legs",
                        "Dragon defender", "Barrows gloves", "Dragon boots",
                        "Amulet of fury", "Fire cape"),
                "Dragon scimitar, abyssal whip, or another target-appropriate weapon",
                "Prioritize high-value untradeables before expensive marginal upgrades.",
                false, true, true, true);
        add("melee-mid", "general", CombatStyle.MELEE_SLASH, GearBudgetTier.MIDGAME,
                items("Neitiznot faceguard", "Bandos chestplate", "Bandos tassets",
                        "Dragon/Avernic defender", "Ferocious gloves", "Primordial boots",
                        "Amulet of torture or rancour", "Fire/Infernal cape"),
                "Abyssal tentacle, Osmumten's fang, or target-appropriate upgrade",
                "Fang is stab-focused; slash and crush targets can prefer a different weapon.",
                false, true, true, true);
        add("melee-high-stab", "boss-stab", CombatStyle.MELEE_STAB, GearBudgetTier.HIGH_END,
                items("Torva or target-appropriate offensive armour", "Amulet of rancour",
                        "Ferocious gloves", "Ultor/Bellator/Lightbearer as encounter demands",
                        "Infernal cape", "Avernic defender"),
                "Osmumten's fang or stronger encounter-specific stab weapon",
                "Defence, accuracy scaling, special attacks, and mechanics can change the weapon order.",
                false, false, false, true);
        add("melee-bis-slash", "boss-slash", CombatStyle.MELEE_SLASH, GearBudgetTier.BIS,
                items("Oathplate or Torva pieces depending the encounter", "Amulet of rancour",
                        "Infernal cape", "Ferocious gloves", "Avernic defender",
                        "Avernic treads/Primordial boots as applicable",
                        "Ultor/Bellator/Lightbearer by target"),
                "Scythe of vitur, Soulreaper axe, or another encounter-specific slash weapon",
                "There is no universal slash BIS: size, defence, slash resistance, specials, and mechanics can change the winner.",
                false, false, false, false);
        add("melee-bis-crush", "boss-crush", CombatStyle.MELEE_CRUSH, GearBudgetTier.BIS,
                items("Inquisitor/Oathplate/Torva mix as the target demands", "Amulet of rancour",
                        "Infernal cape", "Ferocious gloves", "Ultor/Bellator/Lightbearer by target"),
                "Best encounter-specific crush weapon",
                "Crush BIS is highly target-dependent; target-specific accuracy bonuses can beat generic strength armour.",
                false, false, false, false);
    }

    private void seedRanged()
    {
        add("ranged-budget", "general", CombatStyle.RANGED, GearBudgetTier.BUDGET,
                items("Blessed/black d'hide", "Ava's accumulator/assembler", "Barrows gloves",
                        "Amulet of fury or anguish", "God blessing"),
                "Rune crossbow, magic shortbow (i), or toxic blowpipe when unlocked",
                "Weapon and ammunition cost plus target defence matter more than one static DPS ranking.",
                false, true, true, true);
        add("ranged-mid-bowfa", "general", CombatStyle.RANGED, GearBudgetTier.MIDGAME,
                items("Crystal helm", "Crystal body", "Crystal legs", "Ava's assembler",
                        "Necklace of anguish", "Barrows gloves/Zaryte vambraces when available"),
                "Bow of faerdhinen",
                "Bowfa plus crystal is a major self-contained progression package, especially for Iron accounts.",
                false, true, true, true);
        add("ranged-high", "boss-ranged", CombatStyle.RANGED, GearBudgetTier.HIGH_END,
                items("Masori (fortified where appropriate)", "Necklace of anguish",
                        "Ava's assembler or Dizana's quiver", "Zaryte vambraces",
                        "Venator ring/Lightbearer by encounter"),
                "Twisted bow, Zaryte crossbow, Bowfa, or blowpipe depending on target and phase",
                "Magic level, defence, ruby-bolt scaling, and phase mechanics can change weapon order.",
                false, false, false, true);
        add("ranged-bis", "boss-ranged", CombatStyle.RANGED, GearBudgetTier.BIS,
                items("Masori (f) or encounter-specific crystal/void switch", "Necklace of anguish",
                        "Dizana's quiver", "Zaryte vambraces", "Pegasian boots/encounter alternative",
                        "Venator ring or Lightbearer when special attacks matter"),
                "Twisted bow / Zaryte crossbow / Bowfa / blowpipe according to the encounter",
                "Strategist must use the target model before calling one of these weapons BIS.",
                false, false, false, false);
    }

    private void seedMagic()
    {
        add("magic-budget", "general", CombatStyle.MAGIC, GearBudgetTier.BUDGET,
                items("Mystic or Bloodbark/Ahrim pieces", "God cape/imbued god cape when unlocked",
                        "Occult necklace when available", "Barrows gloves"),
                "Ancient staff, Iban's staff, or trident-family weapon as progression allows",
                "Spellbook, autocast, rune cost, and target magic defence determine the practical setup.",
                false, true, true, true);
        add("magic-mid", "general", CombatStyle.MAGIC, GearBudgetTier.MIDGAME,
                items("Ahrim/Blue moon/Virtus progression", "Occult necklace", "Tormented bracelet",
                        "Imbued god cape", "Elidinis' ward when using one-handed weapons"),
                "Trident of the swamp, Sanguinesti staff, or encounter-specific spell",
                "Ancient-spell tasks can favor Virtus even when another robe set is better for powered staves.",
                false, true, true, true);
        add("magic-bis", "boss-magic", CombatStyle.MAGIC, GearBudgetTier.BIS,
                items("Ancestral robes or encounter-specific Virtus switch", "Occult necklace",
                        "Tormented bracelet", "Imbued god cape", "Magus ring",
                        "Eternal boots/encounter alternative", "Elidinis' ward (f) for compatible one-handed setups"),
                "Tumeken's shadow, Sanguinesti/trident-family weapon, or spell-specific weapon by encounter",
                "Shadow scales strongly with magic-damage gear, while Ancient Magicks and one-handed setups can require different BIS gear.",
                false, false, false, false);
    }

    private void seedHybrid()
    {
        add("raid-budget", "raids", CombatStyle.HYBRID, GearBudgetTier.BUDGET,
                items("Fighter torso/Barrows melee pieces", "Blessed d'hide ranged switch",
                        "Ahrim/mystic magic switch", "Barrows gloves", "Fury"),
                "Entry-level melee, ranged, and magic weapons appropriate to the raid",
                "Build switches around encounter coverage before chasing tiny single-style upgrades.",
                false, true, true, false);
        add("raid-bis", "raids", CombatStyle.HYBRID, GearBudgetTier.BIS,
                items("Contextual Oathplate/Torva melee switch", "Masori (f) ranged switch",
                        "Ancestral/Virtus magic switch", "Rancour/anguish/occult switches",
                        "Infernal cape/Dizana's quiver/imbued god cape switches"),
                "Scythe/fang or encounter melee; Tbow/ZCB/Bowfa/blowpipe ranged; Shadow or encounter magic",
                "Raid BIS is room-, invocation-, inventory-, special-attack-, and team-role-dependent.",
                false, false, false, false);
    }

    private void add(String id, String contextId, CombatStyle style,
            GearBudgetTier tier, List<String> items, String weapon,
            String note, boolean f2p, boolean self, boolean uim, boolean hc)
    {
        entries.add(new GearProgressionEntry(id, contextId, style, tier,
                items, weapon, note, f2p, self, uim, hc));
    }

    private static List<String> items(String... values)
    {
        return Arrays.asList(values);
    }
}

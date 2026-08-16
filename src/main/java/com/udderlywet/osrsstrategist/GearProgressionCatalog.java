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

    public List<GearProgressionEntry> all()
    {
        return Collections.unmodifiableList(entries);
    }

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
                "Rune full helm", "Rune platebody or rune chainbody", "Rune platelegs/plateskirt",
                "Amulet of strength or power", "Rune kiteshield",
                "Rune scimitar",
                "F2P melee swaps platebody/chainbody and strength/power amulet depending defence and offensive needs.",
                true, true, true, true);
        add("f2p-ranged", "general", CombatStyle.RANGED, GearBudgetTier.F2P,
                "Green d'hide body", "Green d'hide chaps", "Green d'hide vambraces",
                "Amulet of power", "Coif",
                "Maple shortbow or best legal F2P ranged weapon",
                "Use ammunition and weapon choice appropriate to the target; do not spend scarce arrows just because they are technically stronger.",
                true, true, true, true);
        add("f2p-magic", "general", CombatStyle.MAGIC, GearBudgetTier.F2P,
                "Wizard hat", "Wizard robe top", "Magic amulet",
                "Elemental staff matching rune economics",
                "Strongest economical F2P spell for the target",
                "Magic accuracy, rune cost, and safespot availability matter more than chasing a cosmetic max-hit setup.",
                true, true, true, true);
    }

    private void seedMelee()
    {
        add("melee-budget", "general", CombatStyle.MELEE_SLASH, GearBudgetTier.BUDGET,
                "Helm of neitiznot", "Fighter torso", "Barrows legs", "Dragon defender",
                "Barrows gloves", "Dragon boots", "Amulet of fury", "Fire cape",
                "Dragon scimitar, abyssal whip, or another target-appropriate weapon",
                "Prioritize high-value untradeables such as defender, torso, gloves, and cape before expensive marginal upgrades.",
                false, true, true, true);
        add("melee-mid", "general", CombatStyle.MELEE_SLASH, GearBudgetTier.MIDGAME,
                "Neitiznot faceguard", "Bandos chestplate", "Bandos tassets",
                "Dragon/Avernic defender", "Ferocious gloves", "Primordial boots",
                "Amulet of torture or rancour", "Fire/Infernal cape",
                "Abyssal tentacle, Osmumten's fang, or target-appropriate upgrade",
                "Fang is a stab-focused progression piece; slash/crush targets can prefer a different weapon.",
                false, true, true, true);
        add("melee-high-stab", "boss-stab", CombatStyle.MELEE_STAB, GearBudgetTier.HIGH_END,
                "Torva or target-appropriate offensive armour", "Amulet of rancour",
                "Ferocious gloves", "Ultor/Bellator/Lightbearer as encounter demands",
                "Infernal cape", "Avernic defender",
                "Osmumten's fang or stronger encounter-specific stab weapon",
                "Defence, accuracy scaling, special attacks, and raid mechanics can change the weapon order.",
                false, false, false, true);
        add("melee-bis-slash", "boss-slash", CombatStyle.MELEE_SLASH, GearBudgetTier.BIS,
                "Oathplate or Torva pieces depending the encounter", "Amulet of rancour",
                "Infernal cape", "Ferocious gloves", "Avernic defender",
                "Avernic treads/Primordial boots as applicable", "Ultor/Bellator/Lightbearer by target",
                "Scythe of vitur, Soulreaper axe, or another encounter-specific slash weapon",
                "There is deliberately no universal slash BIS: size, defence, slash resistance, special attacks, and encounter mechanics can change the winner.",
                false, false, false, false);
        add("melee-bis-crush", "boss-crush", CombatStyle.MELEE_CRUSH, GearBudgetTier.BIS,
                "Inquisitor/Oathplate/Torva mix as the target demands", "Amulet of rancour",
                "Infernal cape", "Ferocious gloves", "Ultor/Bellator/Lightbearer by target",
                "Best encounter-specific crush weapon",
                "Crush BIS is highly target-dependent; Inquisitor-style bonuses can beat generic strength armour on the right target.",
                false, false, false, false);
    }

    private void seedRanged()
    {
        add("ranged-budget", "general", CombatStyle.RANGED, GearBudgetTier.BUDGET,
                "Blessed/black d'hide", "Ava's accumulator/assembler", "Barrows gloves",
                "Amulet of fury or anguish", "God blessing",
                "Rune crossbow, magic shortbow (i), or toxic blowpipe when unlocked",
                "Weapon/ammunition cost and target defence matter more than one static DPS ranking.",
                false, true, true, true);
        add("ranged-mid-bowfa", "general", CombatStyle.RANGED, GearBudgetTier.MIDGAME,
                "Crystal helm", "Crystal body", "Crystal legs", "Ava's assembler",
                "Necklace of anguish", "Barrows gloves/Zaryte vambraces when available",
                "Bow of faerdhinen",
                "Bowfa plus crystal is a major self-contained progression package and remains especially valuable to Iron accounts.",
                false, true, true, true);
        add("ranged-high", "boss-ranged", CombatStyle.RANGED, GearBudgetTier.HIGH_END,
                "Masori (fortified where appropriate)", "Necklace of anguish",
                "Ava's assembler or Dizana's quiver", "Zaryte vambraces",
                "Venator ring/Lightbearer by encounter",
                "Twisted bow, Zaryte crossbow, Bowfa, or blowpipe depending target and phase",
                "Magic level/accuracy, defence, range, ruby-bolt scaling, and phase-specific mechanics can all change weapon order.",
                false, false, false, true);
        add("ranged-bis", "boss-ranged", CombatStyle.RANGED, GearBudgetTier.BIS,
                "Masori (f) or encounter-specific crystal/void switch", "Necklace of anguish",
                "Dizana's quiver", "Zaryte vambraces", "Pegasian boots/encounter alternative",
                "Venator ring or Lightbearer when special attacks matter",
                "Twisted bow / Zaryte crossbow / Bowfa / blowpipe according to the encounter",
                "Strategist must use the target model before calling one of these weapons BIS.",
                false, false, false, false);
    }

    private void seedMagic()
    {
        add("magic-budget", "general", CombatStyle.MAGIC, GearBudgetTier.BUDGET,
                "Mystic or Bloodbark/Ahrim pieces", "God cape/imbued god cape when unlocked",
                "Occult necklace when available", "Barrows gloves",
                "Ancient staff/Iban's staff/trident-family weapon as progression allows",
                "Spellbook, autocast availability, rune cost, and target magic defence determine the practical setup.",
                false, true, true, true);
        add("magic-mid", "general", CombatStyle.MAGIC, GearBudgetTier.MIDGAME,
                "Ahrim/Blue moon/Virtus progression", "Occult necklace",
                "Tormented bracelet", "Imbued god cape", "Elidinis' ward when using one-handed weapons",
                "Trident of the swamp, Sanguinesti staff, or encounter-specific spell",
                "Ancient spell tasks can favor Virtus even when another robe set is better for powered staves.",
                false, true, true, true);
        add("magic-bis", "boss-magic", CombatStyle.MAGIC, GearBudgetTier.BIS,
                "Ancestral robes or encounter-specific Virtus switch", "Occult necklace",
                "Tormented bracelet", "Imbued god cape", "Magus ring",
                "Eternal boots/encounter alternative", "Elidinis' ward (f) for compatible one-handed setups",
                "Tumeken's shadow, Sanguinesti/trident-family weapon, or spell-specific weapon by encounter",
                "Tumeken's shadow scales strongly with magic-damage gear, but one-handed weapons and Ancient Magicks can require a different BIS loadout.",
                false, false, false, false);
    }

    private void seedHybrid()
    {
        add("raid-budget", "raids", CombatStyle.HYBRID, GearBudgetTier.BUDGET,
                "Fighter torso/Barrows melee pieces", "Blessed d'hide ranged switch",
                "Ahrim/mystic magic switch", "Barrows gloves", "Fury",
                "Entry-level melee, ranged, and magic weapons appropriate to the raid",
                "Build switches around room/encounter coverage before chasing tiny single-style upgrades.",
                false, true, true, false);
        add("raid-bis", "raids", CombatStyle.HYBRID, GearBudgetTier.BIS,
                "Contextual Oathplate/Torva melee switch", "Masori (f) ranged switch",
                "Ancestral/Virtus magic switch", "Amulet of rancour/anguish/occult switches",
                "Infernal cape/Dizana's quiver/imbued god cape switches",
                "Scythe/fang or encounter melee; Twisted bow/ZCB/Bowfa/blowpipe ranged; Tumeken's shadow or encounter magic",
                "Raid BIS is room- and invocation-dependent. Inventory slots, special-attack weapons, defence reduction, and team role are part of the loadout calculation.",
                false, false, false, false);
    }

    private void add(String id, String contextId, CombatStyle style,
            GearBudgetTier tier, String... values)
    {
        if (values.length < 7) throw new IllegalArgumentException("Gear entry metadata missing");
        int metadataStart = values.length - 6;
        List<String> items = Arrays.asList(Arrays.copyOfRange(values, 0, metadataStart));
        String weapon = values[metadataStart];
        String note = values[metadataStart + 1];
        boolean f2p = Boolean.parseBoolean(values[metadataStart + 2]);
        boolean self = Boolean.parseBoolean(values[metadataStart + 3]);
        boolean uim = Boolean.parseBoolean(values[metadataStart + 4]);
        boolean hc = Boolean.parseBoolean(values[metadataStart + 5]);
        entries.add(new GearProgressionEntry(id, contextId, style, tier,
                items, weapon, note, f2p, self, uim, hc));
    }

    private void add(String id, String contextId, CombatStyle style,
            GearBudgetTier tier, String i1, String i2, String i3,
            String i4, String i5, String weapon, String note,
            boolean f2p, boolean self, boolean uim, boolean hc)
    {
        entries.add(new GearProgressionEntry(id, contextId, style, tier,
                Arrays.asList(i1, i2, i3, i4, i5), weapon, note,
                f2p, self, uim, hc));
    }

    private void add(String id, String contextId, CombatStyle style,
            GearBudgetTier tier, String i1, String i2, String i3,
            String i4, String i5, String i6, String weapon, String note,
            boolean f2p, boolean self, boolean uim, boolean hc)
    {
        entries.add(new GearProgressionEntry(id, contextId, style, tier,
                Arrays.asList(i1, i2, i3, i4, i5, i6), weapon, note,
                f2p, self, uim, hc));
    }

    private void add(String id, String contextId, CombatStyle style,
            GearBudgetTier tier, String i1, String i2, String i3,
            String i4, String i5, String i6, String i7, String weapon,
            String note, boolean f2p, boolean self, boolean uim, boolean hc)
    {
        entries.add(new GearProgressionEntry(id, contextId, style, tier,
                Arrays.asList(i1, i2, i3, i4, i5, i6, i7), weapon, note,
                f2p, self, uim, hc));
    }
}

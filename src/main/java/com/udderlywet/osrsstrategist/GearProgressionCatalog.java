package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Practical equipment ladders. These are role ladders, not a claim that one
 * static set is BIS against every monster. Encounter-specific evaluators can
 * reorder alternatives according to defence, weakness and mechanics.
 */
@Singleton
public class GearProgressionCatalog
{
    private final Map<GearRole, List<GearProgressionStep>> byRole =
            new EnumMap<>(GearRole.class);

    public GearProgressionCatalog()
    {
        melee();
        ranged();
        magic();
        utility();
    }

    public List<GearProgressionStep> forRole(GearRole role)
    {
        return byRole.getOrDefault(role, Collections.emptyList());
    }

    private void melee()
    {
        add(GearRole.MELEE_GENERAL, 1, "Early melee",
                "Rune/dragon-tier weapon", "Amulet of strength/glory", "Rune armour");
        add(GearRole.MELEE_GENERAL, 2, "Core untradeable melee",
                "Dragon defender", "Fighter torso", "Barrows gloves", "Fire cape", "Dragon boots");
        add(GearRole.MELEE_GENERAL, 3, "Midgame strength melee",
                "Neitiznot faceguard", "Amulet of torture", "Bandos chestplate", "Bandos tassets");
        add(GearRole.MELEE_GENERAL, 4, "Late melee",
                "Avernic defender", "Ferocious gloves", "Primordial boots", "Ultor/Bellator/Lightbearer as encounter requires");
        addEnd(GearRole.MELEE_GENERAL, 5, "Endgame melee armour",
                "Torva armour", "Oathplate armour where its accuracy/defence profile wins", "Amulet of rancour", "Infernal cape", "Avernic treads");

        add(GearRole.MELEE_SLASH, 1, "Budget slash", "Dragon scimitar", "Abyssal whip");
        add(GearRole.MELEE_SLASH, 2, "Strong slash", "Blade of saeldor", "Soulreaper axe where appropriate");
        addEnd(GearRole.MELEE_SLASH, 3, "Endgame slash", "Scythe of vitur", "Oathplate/Torva chosen by target", "Bellator ring where accuracy matters");

        add(GearRole.MELEE_STAB, 1, "Budget stab", "Dragon sword", "Leaf-bladed sword where applicable");
        add(GearRole.MELEE_STAB, 2, "General stab", "Abyssal dagger", "Zamorakian hasta");
        add(GearRole.MELEE_STAB, 3, "High-value stab", "Osmumten's fang", "Dragon hunter lance against dragons");
        addEnd(GearRole.MELEE_STAB, 4, "Endgame stab", "Ghrazi rapier for suitable low-defence targets", "Osmumten's fang for high-defence targets");

        add(GearRole.MELEE_CRUSH, 1, "Budget crush", "Dragon mace", "Zombie axe where suitable");
        add(GearRole.MELEE_CRUSH, 2, "Strong crush", "Abyssal bludgeon", "Saradomin sword alternatives where relevant");
        addEnd(GearRole.MELEE_CRUSH, 3, "Endgame crush", "Inquisitor's mace", "Inquisitor armour for crush-specialized encounters", "Elder maul for defence reduction/spec use");
    }

    private void ranged()
    {
        add(GearRole.RANGED_GENERAL, 1, "Early ranged", "Maple/yew shortbow", "Rune crossbow", "Green/blue/black d'hide as unlocked", "Ava device when unlocked");
        add(GearRole.RANGED_GENERAL, 2, "Midgame ranged", "Magic shortbow (i)", "Dragon crossbow", "Karil/blessed d'hide", "Barrows gloves");
        add(GearRole.RANGED_GENERAL, 3, "Strong ranged", "Toxic blowpipe", "Armadyl crossbow", "Bow of faerdhinen + crystal armour");
        add(GearRole.RANGED_GENERAL, 4, "Late ranged", "Masori armour", "Zaryte crossbow", "Zaryte vambraces", "Necklace of anguish", "Ava's assembler");
        addEnd(GearRole.RANGED_GENERAL, 5, "Endgame ranged", "Twisted bow where target Magic makes it excel", "Masori (f)", "Dizana's quiver", "Avernic treads", "Venator/Lightbearer as encounter requires");

        add(GearRole.RANGED_BOW, 1, "Budget bows", "Magic shortbow (i)", "Crystal bow");
        add(GearRole.RANGED_BOW, 2, "Crystal progression", "Bow of faerdhinen", "Crystal helm/body/legs");
        addEnd(GearRole.RANGED_BOW, 3, "Endgame bow", "Twisted bow", "Masori (f)", "Dizana's quiver");

        add(GearRole.RANGED_CROSSBOW, 1, "Budget crossbow", "Rune crossbow", "Broad/diamond/ruby bolts as target requires");
        add(GearRole.RANGED_CROSSBOW, 2, "Dragon crossbow tier", "Dragon crossbow", "Dragon bolts (e)");
        add(GearRole.RANGED_CROSSBOW, 3, "Specialized crossbows", "Dragon hunter crossbow against dragons", "Armadyl crossbow");
        addEnd(GearRole.RANGED_CROSSBOW, 4, "Endgame crossbow", "Zaryte crossbow", "Twisted buckler where one-handed ranged is appropriate");
    }

    private void magic()
    {
        add(GearRole.MAGIC_GENERAL, 1, "Early magic", "Elemental staff", "Mystic robes", "Amulet of magic/glory");
        add(GearRole.MAGIC_GENERAL, 2, "Midgame magic", "Iban's staff", "Ancient staff", "Ahrim robes", "Occult necklace when unlocked");
        add(GearRole.MAGIC_GENERAL, 3, "Strong magic", "Trident of the seas/swamp", "Tormented bracelet", "Mage arena cape", "Elidinis' ward");
        add(GearRole.MAGIC_GENERAL, 4, "Late magic", "Sanguinesti staff", "Virtus/Ancestral pieces by use case", "Imbued heart where useful");
        addEnd(GearRole.MAGIC_GENERAL, 5, "Endgame magic", "Tumeken's shadow", "Ancestral/Virtus selected by spell/use case", "Magus ring", "Elidinis' ward (f) when not using a two-handed weapon");

        add(GearRole.MAGIC_BURST, 1, "Ancient burst setup", "Ancient Magicks", "Ancient staff/master wand", "Prayer-positive gear");
        add(GearRole.MAGIC_BURST, 2, "Improved barrage setup", "Kodai wand", "Occult necklace", "Tormented bracelet");
        addEnd(GearRole.MAGIC_BURST, 3, "Endgame barrage", "Kodai/nightmare-staff alternatives by target", "Virtus robes for Ancient Magicks", "Saturated heart where useful");
    }

    private void utility()
    {
        add(GearRole.SLAYER, 1, "Core Slayer", "Black mask", "Slayer helmet", "Task protection items");
        add(GearRole.SLAYER, 2, "Imbued Slayer", "Slayer helmet (i)", "Proselyte/prayer gear for prayer tasks", "Cannon where sustainable");
        addEnd(GearRole.SLAYER, 3, "Late Slayer", "Slayer helmet (i)", "Task-specific melee/ranged/magic BIS", "Specialized demonbane/dragonbane weapons where applicable");

        add(GearRole.PRAYER_TANK, 1, "Budget prayer", "Proselyte armour", "God book/blessing", "Prayer necklace alternatives");
        add(GearRole.PRAYER_TANK, 2, "Defensive sustain", "Barrows tank pieces", "Blessed spirit shield", "Ring of suffering (i)");
        addEnd(GearRole.PRAYER_TANK, 3, "Endgame defence", "Justiciar where damage reduction matters", "Oathplate where melee defence/slash accuracy matters", "Elysian spirit shield where shield slot is viable");

        add(GearRole.SPEC_WEAPON, 1, "Budget specials", "Dragon dagger", "Dragon mace");
        add(GearRole.SPEC_WEAPON, 2, "Defence reduction", "Dragon warhammer", "Bandos godsword");
        add(GearRole.SPEC_WEAPON, 3, "Damage specials", "Dragon claws", "Voidwaker", "Zaryte crossbow");
        addEnd(GearRole.SPEC_WEAPON, 4, "Endgame utility specials", "Elder maul", "Dragon claws/Voidwaker/Zaryte crossbow chosen by encounter", "Lightbearer when special-attack uptime dominates");
    }

    private void add(GearRole role, int tier, String name, String... items)
    {
        add(role, tier, name, false, items);
    }

    private void addEnd(GearRole role, int tier, String name, String... items)
    {
        add(role, tier, name, true, items);
    }

    private void add(GearRole role, int tier, String name, boolean endgame, String... items)
    {
        byRole.computeIfAbsent(role, ignored -> new ArrayList<>())
                .add(new GearProgressionStep(role, tier, name,
                        Arrays.asList(items),
                        "Choose by encounter weakness, account mode, owned gear and acquisition cost; do not blindly equip a global BIS set.",
                        endgame));
    }
}

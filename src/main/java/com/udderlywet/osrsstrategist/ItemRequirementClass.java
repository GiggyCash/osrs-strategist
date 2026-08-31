package com.udderlywet.osrsstrategist;

import java.util.Locale;

import lombok.Getter;

/**
 * A semantic item/preparation class from authoritative requirement evidence.
 *
 * <p>Simple name-stable tool families can be evaluated from observed item
 * names. Broader mechanical categories deliberately remain check-only: an
 * item name alone cannot prove that gear is a slash weapon, a safe light
 * source, or a suitable encounter loadout.</p>
 */
@Getter
public enum ItemRequirementClass
{
    AXE("any usable axe", true),
    PICKAXE("any usable pickaxe", true),
    BOW("any suitable bow", true),
    CROSSBOW("any suitable crossbow", true),
    CAT_OR_KITTEN("a cat or kitten", true),
    FEATHER("a usable feather", true),
    NAILS("nails of any usable metal", true),
    MACHETE("any usable machete", true),
    LIGHT_SOURCE("a suitable light source", false),
    SLASH_WEAPON("a suitable slash weapon", false),
    WEB_CUTTING_TOOL("a tool or weapon that can cut webs", false),
    MAGIC_COMBAT_LOADOUT(PlayerText.get("IRC1"), false),
    MAGIC_OR_RANGED_LOADOUT(PlayerText.get("IRC2"), false),
    TELEKINETIC_GRAB_RUNES(PlayerText.get("IRC3"), false),
    SPELL_RUNE_LOADOUT(PlayerText.get("IRC4"), false),
    POISON_CURE("any mechanically valid poison cure", false),
    WATER_CONTAINER("a mechanically valid water container", false),
    EMPTY_INVENTORY_SPACE("the required empty inventory space", false),
    COMBAT_EQUIPMENT("suitable combat equipment", false),
    HEALING_FOOD("suitable healing food", false),
    MULTI_STYLE_OR_POISON(PlayerText.get("IRC5"), false),
    FULL_HAM_ROBE_SET("a full seven-piece H.A.M. robe set", false);

    private final String label;
    private final boolean nameObservable;

    ItemRequirementClass(String label, boolean nameObservable)
    {
        this.label = label;
        this.nameObservable = nameObservable;
    }


    public boolean matches(String itemName)
    {
        if (!nameObservable || itemName == null) return false;
        String name = itemName.trim().toLowerCase(Locale.ROOT);
        switch (this)
        {
            case AXE:
                return (name.equals("axe") || name.contains(" axe"))
                        && !name.contains("pickaxe")
                        && !name.contains("battleaxe")
                        && !name.contains("greataxe")
                        && !name.contains(" axe head")
                        && !name.startsWith("broken ");
            case PICKAXE:
                return (name.equals("pickaxe") || name.contains(" pickaxe"))
                        && !name.contains("pickaxe head")
                        && !name.contains("pickaxe handle")
                        && !name.startsWith("broken ");
            case BOW:
                return (name.equals("bow") || name.endsWith(" bow"))
                        && !name.endsWith("crossbow");
            case CROSSBOW:
                return name.equals("crossbow") || name.endsWith(" crossbow");
            case CAT_OR_KITTEN:
                return name.equals("cat") || name.equals("kitten")
                        || name.endsWith(" cat") || name.endsWith(" kitten")
                        || name.endsWith(" hellcat") || name.endsWith(" hellkitten");
            case FEATHER:
                return name.equals("feather") || name.endsWith(" feather");
            case NAILS:
                return name.equals("nails") || name.endsWith(" nails");
            case MACHETE:
                return name.equals("machete") || name.endsWith(" machete");
            default:
                return false;
        }
    }
}

package compass;
import static compass.Text.get;

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
    PICKAXE(get(1139), true),
    BOW(get(1693), true),
    CROSSBOW(get(1140), true),
    CAT_OR_KITTEN("a cat or kitten", true),
    FEATHER(get(1694), true),
    NAILS(get(1141), true),
    MACHETE(get(1142), true),
    LIGHT_SOURCE(get(1143), false),
    SLASH_WEAPON(get(1144), false),
    WEB_CUTTING_TOOL(get(1145), false),
    MAGIC_COMBAT_LOADOUT(get(327), false),
    MAGIC_OR_RANGED_LOADOUT(get(328), false),
    TELEKINETIC_GRAB_RUNES(get(329), false),
    SPELL_RUNE_LOADOUT(get(330), false),
    POISON_CURE(get(1146), false),
    WATER_CONTAINER(get(1147), false),
    EMPTY_INVENTORY_SPACE(get(1148), false),
    COMBAT_EQUIPMENT(get(1149), false),
    HEALING_FOOD(get(1150), false),
    MULTI_STYLE_OR_POISON(get(331), false),
    FULL_HAM_ROBE_SET(get(1151), false);

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
        var name = itemName.trim().toLowerCase(Locale.ROOT);
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

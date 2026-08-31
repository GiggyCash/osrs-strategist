package compass;

/**
 * Optional account-build restrictions layered on top of the Jagex account mode.
 *
 * <p>These describe player-imposed stat restrictions. They are deliberately
 * separate from Main/Ironman/UIM/Hardcore/GIM because any of those account
 * modes can also be a pure or skiller.</p>
 */
public enum RestrictedBuildType
{
    STANDARD,
    SKILLER,
    PRAYER_SKILLER,
    F2P_SKILLER,
    ONE_DEFENCE_PURE,
    LOW_DEFENCE_PURE,
    INITIATE_PURE,
    RUNE_PURE,
    VOID_PURE,
    ZERKER,
    OBSIDIAN_MAULER,
    RANGE_TANK,
    MED_BUILD,
    DEFENCE_PURE,
    TEN_HITPOINTS,
    COMBAT_ONLY
}

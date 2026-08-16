package com.udderlywet.osrsstrategist;

/**
 * Optional restricted-build archetype inferred independently from account mode.
 *
 * <p>This never changes the underlying Main/Iron/GIM/UIM/HCIM mode. It only
 * tells planners that levelling certain combat skills may violate the player's
 * apparent build.</p>
 */
public enum AccountArchetype
{
    STANDARD,
    SKILLER,
    ONE_DEFENCE_PURE,
    DEFENCE_PURE,
    ZERKER,
    UNKNOWN
}

package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/**
 * Small, explicit decoder for the herb and standard-tree states used by the
 * active Farming checklist. Unknown holes stay UNKNOWN rather than being guessed.
 */
@Singleton
public class FarmingPatchStateDecoder
{
    public FarmingPatchCycleState decode(FarmingPatchKind kind, int value)
    {
        if (kind == FarmingPatchKind.HERB) return decodeHerb(value);
        if (kind == FarmingPatchKind.TREE) return decodeTree(value);
        return FarmingPatchCycleState.UNKNOWN;
    }

    private FarmingPatchCycleState decodeHerb(int value)
    {
        if (between(value, 0, 3) || value == 67
                || between(value, 176, 191)
                || between(value, 204, 219)
                || between(value, 221, 255))
        {
            return FarmingPatchCycleState.EMPTY;
        }
        if (between(value, 128, 169)
                || between(value, 173, 175)
                || between(value, 198, 200))
        {
            return FarmingPatchCycleState.DISEASED;
        }
        if (between(value, 170, 172)
                || between(value, 201, 203))
        {
            return FarmingPatchCycleState.DEAD;
        }
        if (inRanges(value,
                8,10, 15,17, 22,24, 29,31, 36,38, 43,45,
                50,52, 57,59, 64,66, 72,74, 79,81, 86,88,
                93,95, 100,102, 107,109, 196,197))
        {
            return FarmingPatchCycleState.READY;
        }
        if (inRanges(value,
                4,7, 11,14, 18,21, 25,28, 32,35, 39,42,
                46,49, 53,56, 60,63, 68,71, 75,78, 82,85,
                89,92, 96,99, 103,106, 192,195))
        {
            return FarmingPatchCycleState.GROWING;
        }
        return FarmingPatchCycleState.UNKNOWN;
    }

    private FarmingPatchCycleState decodeTree(int value)
    {
        if (between(value, 0, 7)
                || between(value, 63, 72)
                || between(value, 78, 79)
                || between(value, 87, 88)
                || between(value, 98, 99)
                || between(value, 111, 112)
                || between(value, 126, 136)
                || between(value, 142, 143)
                || between(value, 151, 152)
                || between(value, 162, 163)
                || between(value, 175, 176)
                || between(value, 190, 191)
                || between(value, 198, 255))
        {
            return FarmingPatchCycleState.EMPTY;
        }
        if (inRanges(value,
                73,75, 77,77, 80,84, 86,86, 89,95, 97,97,
                100,108, 110,110, 113,123, 125,125))
        {
            return FarmingPatchCycleState.DISEASED;
        }
        if (inRanges(value,
                137,139, 141,141, 144,148, 150,150, 153,159,
                161,161, 164,172, 174,174, 177,187, 189,189))
        {
            return FarmingPatchCycleState.DEAD;
        }
        if (inRanges(value,
                13,14, 22,23, 33,34, 46,47, 61,62, 192,197))
        {
            return FarmingPatchCycleState.READY;
        }
        if (inRanges(value,
                8,12, 15,21, 24,32, 35,45, 48,60))
        {
            return FarmingPatchCycleState.GROWING;
        }
        return FarmingPatchCycleState.UNKNOWN;
    }

    private static boolean inRanges(int value, int... endpoints)
    {
        for (int i = 0; i + 1 < endpoints.length; i += 2)
        {
            if (between(value, endpoints[i], endpoints[i + 1])) return true;
        }
        return false;
    }

    private static boolean between(int value, int min, int max)
    {
        return value >= min && value <= max;
    }
}

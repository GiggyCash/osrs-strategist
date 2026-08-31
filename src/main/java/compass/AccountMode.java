package compass;

public enum AccountMode
{
    MAIN,
    IRONMAN,
    ULTIMATE_IRONMAN,
    HARDCORE_IRONMAN,
    GROUP_IRONMAN,
    HARDCORE_GROUP_IRONMAN,
    UNRANKED_GROUP_IRONMAN,
    UNKNOWN;

    public static AccountMode fromTypeCode(int typeCode)
    {
        switch (typeCode)
        {
            case 0: return MAIN;
            case 1: return IRONMAN;
            case 2: return ULTIMATE_IRONMAN;
            case 3: return HARDCORE_IRONMAN;
            case 4: return GROUP_IRONMAN;
            case 5: return HARDCORE_GROUP_IRONMAN;
            case 6: return UNRANKED_GROUP_IRONMAN;
            default: return UNKNOWN;
        }
    }

    public boolean usesGrandExchange()
    {
        return this == MAIN;
    }

    public boolean isGroupIronman()
    {
        return this == GROUP_IRONMAN
                || this == HARDCORE_GROUP_IRONMAN
                || this == UNRANKED_GROUP_IRONMAN;
    }

    public boolean isUltimateIronman()
    {
        return this == ULTIMATE_IRONMAN;
    }

    public boolean isIronLike()
    {
        return this != MAIN && this != UNKNOWN;
    }
}

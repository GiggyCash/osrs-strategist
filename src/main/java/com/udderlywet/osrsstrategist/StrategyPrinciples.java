package com.udderlywet.osrsstrategist;

/** Central rules we do not want scattered through hundreds of conditionals. */
public final class StrategyPrinciples
{
    private StrategyPrinciples() {}

    public static final String NO_GUESSING = "Never recommend a path whose required capability is unknown.";
    public static final String GIM_STORAGE = "Use Group Storage only when enabled and the items are observed there.";
    public static final String UIM_STORAGE = "Only recommend POH, STASH, death storage, deathpile, Tool Leprechaun, or other storage after verifying the needed capability and item rules.";
    public static final String MAIN_GP = "For mains, compare available GP, safe-to-sell items, money-making time, GE purchase cost, and gather-yourself time before recommending a purchase.";
    public static final String PLAYER_AGENCY = "The plugin advises and prepares; it never automates gameplay.";
}

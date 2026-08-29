package com.udderlywet.osrsstrategist;

import net.runelite.api.gameval.VarbitID;

/** Reviewed, strategically meaningful Slayer rewards with live ownership varbits. */
public enum SlayerReward
{
    BIGGER_AND_BADDER("bigger-and-badder", "Bigger and Badder", 50,
            VarbitID.SLAYER_UNLOCK_SUPERIORMOBS),
    MALEVOLENT_MASQUERADE("malevolent-masquerade", "Malevolent Masquerade", 400,
            VarbitID.SLAYER_HELM_UNLOCKED),
    BROADER_FLETCHING("broader-fletching", "Broader Fletching", 300,
            VarbitID.SLAYER_AMMO_UNLOCKED),
    RING_BLING("ring-bling", "Ring Bling", 150,
            VarbitID.SLAYER_RING_UNLOCKED),
    TASK_STORAGE("task-storage", "Task Storage", 500,
            VarbitID.SLAYER_UNLOCK_STORAGE),
    LIKE_A_BOSS("like-a-boss", "Like a Boss", 200,
            VarbitID.SLAYER_UNLOCK_BOSSES),
    HOT_STUFF("hot-stuff", "Hot Stuff", 100,
            VarbitID.SLAYER_UNLOCK_TZHAAR),
    WATCH_THE_BIRDIE("watch-the-birdie", "Watch the Birdie", 80,
            VarbitID.SLAYER_UNLOCK_AVIANSIES),
    BASILOCKED("basilocked", "Basilocked", 80,
            VarbitID.SLAYER_UNLOCK_BASILISK),
    ACTUAL_VAMPYRE_SLAYER("actual-vampyre-slayer", "Actual Vampyre Slayer", 80,
            VarbitID.SLAYER_UNLOCK_VAMPYRES),
    REPTILE_GOT_RIPPED("reptile-got-ripped", "Reptile Got Ripped", 75,
            VarbitID.SLAYER_UNLOCK_LIZARDMEN),
    STOP_THE_WYVERN("stop-the-wyvern", "Stop the Wyvern", 500,
            VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK),
    EXTEND_ABYSSAL_DEMONS("extend-abyssal-demons", "Augment My Abbies", 100,
            VarbitID.SLAYER_LONGER_ABYSSALDEMONS),
    EXTEND_BLOODVELDS("extend-bloodvelds", "Bleed Me Dry", 75,
            VarbitID.SLAYER_LONGER_BLOODVELD),
    EXTEND_DUST_DEVILS("extend-dust-devils", "To Dust You Shall Return", 100,
            VarbitID.SLAYER_LONGER_DUSTDEVILS),
    EXTEND_GARGOYLES("extend-gargoyles", "Get Smashed", 100,
            VarbitID.SLAYER_LONGER_GARGOYLES),
    EXTEND_NECHRYAELS("extend-nechryaels", "Nechs Please", 100,
            VarbitID.SLAYER_LONGER_NECHRYAEL),
    EXTEND_KRAKEN("extend-kraken", "Krack On", 100,
            VarbitID.SLAYER_LONGER_CAVEKRAKEN);

    private final String id;
    private final String displayName;
    private final int pointCost;
    private final int varbitId;

    SlayerReward(String id, String displayName, int pointCost, int varbitId)
    {
        this.id = id;
        this.displayName = displayName;
        this.pointCost = pointCost;
        this.varbitId = varbitId;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getPointCost() { return pointCost; }
    public int getVarbitId() { return varbitId; }
}

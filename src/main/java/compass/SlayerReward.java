package compass;

import net.runelite.api.gameval.VarbitID;

/** Reviewed, strategically meaningful Slayer rewards with live ownership varbits. */
public enum SlayerReward
{
    BIGGER_AND_BADDER(Text.get(1926), Text.get(1927), 50,
            VarbitID.SLAYER_UNLOCK_SUPERIORMOBS),
    MALEVOLENT_MASQUERADE(Text.get(1928), Text.get(1180), 400,
            VarbitID.SLAYER_HELM_UNLOCKED),
    BROADER_FLETCHING(Text.get(1929), Text.get(1930), 300,
            VarbitID.SLAYER_AMMO_UNLOCKED),
    RING_BLING("ring-bling", "Ring Bling", 150,
            VarbitID.SLAYER_RING_UNLOCKED),
    TASK_STORAGE("task-storage", "Task Storage", 500,
            VarbitID.SLAYER_UNLOCK_STORAGE),
    LIKE_A_BOSS("like-a-boss", "Like a Boss", 200,
            VarbitID.SLAYER_UNLOCK_BOSSES),
    HOT_STUFF("hot-stuff", "Hot Stuff", 100,
            VarbitID.SLAYER_UNLOCK_TZHAAR),
    WATCH_THE_BIRDIE(Text.get(1931), Text.get(1932), 80,
            VarbitID.SLAYER_UNLOCK_AVIANSIES),
    BASILOCKED("basilocked", "Basilocked", 80,
            VarbitID.SLAYER_UNLOCK_BASILISK),
    ACTUAL_VAMPYRE_SLAYER(Text.get(1933), Text.get(1181), 80,
            VarbitID.SLAYER_UNLOCK_VAMPYRES),
    REPTILE_GOT_RIPPED(Text.get(1934), Text.get(1182), 75,
            VarbitID.SLAYER_UNLOCK_LIZARDMEN),
    STOP_THE_WYVERN("stop-the-wyvern", "Stop the Wyvern", 500,
            VarbitID.SLAYER_UNLOCK_FOSSILWYVERNBLOCK),
    EXTEND_ABYSSAL_DEMONS(Text.get(1935), Text.get(1936), 100,
            VarbitID.SLAYER_LONGER_ABYSSALDEMONS),
    EXTEND_BLOODVELDS(Text.get(1937), "Bleed Me Dry", 75,
            VarbitID.SLAYER_LONGER_BLOODVELD),
    EXTEND_DUST_DEVILS(Text.get(1938), Text.get(1183), 100,
            VarbitID.SLAYER_LONGER_DUSTDEVILS),
    EXTEND_GARGOYLES(Text.get(1939), "Get Smashed", 100,
            VarbitID.SLAYER_LONGER_GARGOYLES),
    EXTEND_NECHRYAELS(Text.get(1940), "Nechs Please", 100,
            VarbitID.SLAYER_LONGER_NECHRYAEL),
    EXTEND_KRAKEN("extend-kraken", "Krack On", 100,
            VarbitID.SLAYER_LONGER_CAVEKRAKEN);

    final String id;
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

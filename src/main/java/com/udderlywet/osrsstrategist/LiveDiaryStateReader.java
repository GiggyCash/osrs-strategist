package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;

/** Reads all 12 regions x 4 Achievement Diary tier states directly from RuneLite. */
@Singleton
public class LiveDiaryStateReader
{
    private final Client client;

    @Inject
    public LiveDiaryStateReader(Client client)
    {
        this.client = client;
    }

    public DiarySnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;

        Map<String, Integer> completed = new HashMap<>();
        Map<String, Integer> totals = new HashMap<>();
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();

        add(completed, totals, tiers, "Ardougne",
                VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
                VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE,
                VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT,
                VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT);
        add(completed, totals, tiers, "Desert",
                VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE,
                VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE,
                VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT,
                VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT);
        add(completed, totals, tiers, "Falador",
                VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
                VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE,
                VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT,
                VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT);
        add(completed, totals, tiers, "Fremennik",
                VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
                VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE,
                VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT,
                VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT);
        add(completed, totals, tiers, "Kandarin",
                VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
                VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE,
                VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT,
                VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT);
        add(completed, totals, tiers, "Karamja",
                VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
                VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE,
                VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT,
                VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT);
        add(completed, totals, tiers, "Kourend & Kebos",
                VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
                VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE,
                VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT,
                VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT);
        add(completed, totals, tiers, "Lumbridge & Draynor",
                VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
                VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE,
                VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT,
                VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT);
        add(completed, totals, tiers, "Morytania",
                VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
                VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE,
                VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT,
                VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT);
        add(completed, totals, tiers, "Varrock",
                VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
                VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE,
                VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT,
                VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT);
        add(completed, totals, tiers, "Western Provinces",
                VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE,
                VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE,
                VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT,
                VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT);
        add(completed, totals, tiers, "Wilderness",
                VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
                VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE,
                VarbitID.WILDERNESS_EASY_COUNT, VarbitID.WILDERNESS_MED_COUNT,
                VarbitID.WILDERNESS_HARD_COUNT, VarbitID.WILDERNESS_ELITE_COUNT);

        return new DiarySnapshot(completed, totals, tiers);
    }

    private void add(Map<String, Integer> completed,
            Map<String, Integer> totals,
            Map<String, Map<DiaryTier, Boolean>> tiers,
            String region,
            int easyTier, int mediumTier, int hardTier, int eliteTier,
            int easyCount, int mediumCount, int hardCount, int eliteCount)
    {
        EnumMap<DiaryTier, Boolean> tierMap = new EnumMap<>(DiaryTier.class);
        tierMap.put(DiaryTier.EASY, client.getVarbitValue(easyTier) >= 1);
        tierMap.put(DiaryTier.MEDIUM, client.getVarbitValue(mediumTier) >= 1);
        tierMap.put(DiaryTier.HARD, client.getVarbitValue(hardTier) >= 1);
        tierMap.put(DiaryTier.ELITE, client.getVarbitValue(eliteTier) >= 1);
        tiers.put(region, tierMap);

        int done = Math.max(0, client.getVarbitValue(easyCount))
                + Math.max(0, client.getVarbitValue(mediumCount))
                + Math.max(0, client.getVarbitValue(hardCount))
                + Math.max(0, client.getVarbitValue(eliteCount));
        completed.put(region, done);
        // Per-tier totals are maintained outside RuneLite's public count varbits.
        // Leave this unknown rather than freezing a copied third-party table.
        totals.put(region, 0);
    }
}

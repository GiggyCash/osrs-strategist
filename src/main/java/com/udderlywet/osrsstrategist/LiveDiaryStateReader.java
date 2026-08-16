package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
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
                Varbits.DIARY_ARDOUGNE_EASY, Varbits.DIARY_ARDOUGNE_MEDIUM,
                Varbits.DIARY_ARDOUGNE_HARD, Varbits.DIARY_ARDOUGNE_ELITE,
                VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT,
                VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT);
        add(completed, totals, tiers, "Desert",
                Varbits.DIARY_DESERT_EASY, Varbits.DIARY_DESERT_MEDIUM,
                Varbits.DIARY_DESERT_HARD, Varbits.DIARY_DESERT_ELITE,
                VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT,
                VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT);
        add(completed, totals, tiers, "Falador",
                Varbits.DIARY_FALADOR_EASY, Varbits.DIARY_FALADOR_MEDIUM,
                Varbits.DIARY_FALADOR_HARD, Varbits.DIARY_FALADOR_ELITE,
                VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT,
                VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT);
        add(completed, totals, tiers, "Fremennik",
                Varbits.DIARY_FREMENNIK_EASY, Varbits.DIARY_FREMENNIK_MEDIUM,
                Varbits.DIARY_FREMENNIK_HARD, Varbits.DIARY_FREMENNIK_ELITE,
                VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT,
                VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT);
        add(completed, totals, tiers, "Kandarin",
                Varbits.DIARY_KANDARIN_EASY, Varbits.DIARY_KANDARIN_MEDIUM,
                Varbits.DIARY_KANDARIN_HARD, Varbits.DIARY_KANDARIN_ELITE,
                VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT,
                VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT);
        add(completed, totals, tiers, "Karamja",
                Varbits.DIARY_KARAMJA_EASY, Varbits.DIARY_KARAMJA_MEDIUM,
                Varbits.DIARY_KARAMJA_HARD, Varbits.DIARY_KARAMJA_ELITE,
                VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT,
                VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT);
        add(completed, totals, tiers, "Kourend & Kebos",
                Varbits.DIARY_KOUREND_EASY, Varbits.DIARY_KOUREND_MEDIUM,
                Varbits.DIARY_KOUREND_HARD, Varbits.DIARY_KOUREND_ELITE,
                VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT,
                VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT);
        add(completed, totals, tiers, "Lumbridge & Draynor",
                Varbits.DIARY_LUMBRIDGE_EASY, Varbits.DIARY_LUMBRIDGE_MEDIUM,
                Varbits.DIARY_LUMBRIDGE_HARD, Varbits.DIARY_LUMBRIDGE_ELITE,
                VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT,
                VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT);
        add(completed, totals, tiers, "Morytania",
                Varbits.DIARY_MORYTANIA_EASY, Varbits.DIARY_MORYTANIA_MEDIUM,
                Varbits.DIARY_MORYTANIA_HARD, Varbits.DIARY_MORYTANIA_ELITE,
                VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT,
                VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT);
        add(completed, totals, tiers, "Varrock",
                Varbits.DIARY_VARROCK_EASY, Varbits.DIARY_VARROCK_MEDIUM,
                Varbits.DIARY_VARROCK_HARD, Varbits.DIARY_VARROCK_ELITE,
                VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT,
                VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT);
        add(completed, totals, tiers, "Western Provinces",
                Varbits.DIARY_WESTERN_EASY, Varbits.DIARY_WESTERN_MEDIUM,
                Varbits.DIARY_WESTERN_HARD, Varbits.DIARY_WESTERN_ELITE,
                VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT,
                VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT);
        add(completed, totals, tiers, "Wilderness",
                Varbits.DIARY_WILDERNESS_EASY, Varbits.DIARY_WILDERNESS_MEDIUM,
                Varbits.DIARY_WILDERNESS_HARD, Varbits.DIARY_WILDERNESS_ELITE,
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

package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/** Reads all 12 regions x 4 Achievement Diary tier states directly from RuneLite. */
@Singleton
public class LiveDiaryStateReader
{
    private final Client client;
    private final DiaryTaskCatalog taskCatalog = new DiaryTaskCatalog();
    private final Map<String, Boolean> observedTaskCompletion = new HashMap<>();

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

        return new DiarySnapshot(completed, totals, tiers,
                observedTaskCompletion);
    }

    /**
     * Captures exact completed/incomplete rows while an Achievement Diary page
     * is visible. Rows not present on the open page remain unknown.
     */
    public boolean observeOpenDiary()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        Widget title = client.getWidget(InterfaceID.Journalscroll.TITLE);
        Widget layer = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
        if (title == null || layer == null) return false;
        Widget[] children = layer.getStaticChildren();
        if (children == null || children.length == 0) return false;
        String region = regionFor(title.getText());
        if (region == null && children[0] != null)
            region = regionFor(children[0].getText());
        if (region == null) return false;

        Map<String, Boolean> before = new HashMap<>(observedTaskCompletion);
        List<String> rows = new ArrayList<>();
        for (Widget child : children)
            if (child != null && child.getText() != null)
                rows.add(child.getText());
        observedTaskCompletion.putAll(observedTasksFromRows(
                region, rows, taskCatalog));
        return !before.equals(observedTaskCompletion);
    }

    static Map<String, Boolean> observedTasksFromRows(String region,
            List<String> rows, DiaryTaskCatalog catalog)
    {
        Map<String, Boolean> result = new HashMap<>();
        if (region == null || rows == null || catalog == null) return result;
        List<DiaryTaskDefinition> regionTasks = catalog.all();
        String buffered = "";
        boolean bufferedComplete = false;
        for (String raw : rows)
        {
            if (raw == null) continue;
            String row = normalizeSpace(Text.removeTags(raw));
            if (row.isEmpty()) continue;
            boolean struck = raw.toLowerCase(Locale.ROOT).contains("<str>");

            Match direct = match(regionTasks, region, row);
            if (direct != null)
            {
                result.put(direct.task.getId(), struck);
                buffered = "";
                bufferedComplete = false;
                continue;
            }

            String combined = buffered.isEmpty() ? row : buffered + " " + row;
            Match wrapped = match(regionTasks, region, combined);
            if (wrapped != null)
            {
                result.put(wrapped.task.getId(),
                        bufferedComplete || struck);
                buffered = "";
                bufferedComplete = false;
                continue;
            }

            if (isTaskPrefix(regionTasks, region, combined))
            {
                buffered = combined;
                bufferedComplete = bufferedComplete || struck;
            }
            else if (isTaskPrefix(regionTasks, region, row))
            {
                buffered = row;
                bufferedComplete = struck;
            }
            else
            {
                buffered = "";
                bufferedComplete = false;
            }
        }
        return result;
    }

    public void clear()
    {
        observedTaskCompletion.clear();
    }

    private static Match match(List<DiaryTaskDefinition> tasks,
            String region, String row)
    {
        for (DiaryTaskDefinition task : tasks)
        {
            if (!task.getRegion().equals(region)) continue;
            String instruction = normalizeSpace(task.getTask());
            if (row.equals(instruction)
                    || row.startsWith(instruction + " ("))
                return new Match(task);
        }
        return null;
    }

    private static boolean isTaskPrefix(List<DiaryTaskDefinition> tasks,
            String region, String row)
    {
        for (DiaryTaskDefinition task : tasks)
            if (task.getRegion().equals(region)
                    && normalizeSpace(task.getTask()).startsWith(row))
                return true;
        return false;
    }

    private static String regionFor(String rawTitle)
    {
        String title = Text.removeTags(rawTitle == null ? "" : rawTitle)
                .toLowerCase(Locale.ROOT);
        if (title.contains("ardougne")) return "Ardougne";
        if (title.contains("desert")) return "Desert";
        if (title.contains("falador")) return "Falador";
        if (title.contains("fremennik")) return "Fremennik";
        if (title.contains("kandarin")) return "Kandarin";
        if (title.contains("karamja")) return "Karamja";
        if (title.contains("kourend")) return "Kourend & Kebos";
        if (title.contains("lumbridge")) return "Lumbridge & Draynor";
        if (title.contains("morytania")) return "Morytania";
        if (title.contains("varrock")) return "Varrock";
        if (title.contains("western")) return "Western Provinces";
        if (title.contains("wilderness")) return "Wilderness";
        return null;
    }

    private static String normalizeSpace(String value)
    {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static final class Match
    {
        private final DiaryTaskDefinition task;
        private Match(DiaryTaskDefinition task) { this.task = task; }
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

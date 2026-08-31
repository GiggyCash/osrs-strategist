package com.udderlywet.osrsstrategist;

import java.util.*;
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
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class LiveDiaryStateReader
{
    private static final String[] REGIONS = {
            "Ardougne", "Desert", "Falador", "Fremennik", "Kandarin",
            "Karamja", "Kourend & Kebos", "Lumbridge & Draynor",
            "Morytania", "Varrock", "Western Provinces", "Wilderness"
    };
    /** Tier-completion varbits followed by the four completed-task counts. */
    private static final int[][] VARBITS = {
            {VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT, VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT},
            {VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE, VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT, VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT},
            {VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE, VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT, VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT},
            {VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT, VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT},
            {VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT, VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT},
            {VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE, VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT, VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT},
            {VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE, VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT, VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT},
            {VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT, VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT},
            {VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE, VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT, VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT},
            {VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE, VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT, VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT},
            {VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE, VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT, VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT},
            {VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE, VarbitID.WILDERNESS_EASY_COUNT, VarbitID.WILDERNESS_MED_COUNT, VarbitID.WILDERNESS_HARD_COUNT, VarbitID.WILDERNESS_ELITE_COUNT}
    };
    private final Client client;
    private final DiaryTaskCatalog taskCatalog = new DiaryTaskCatalog();
    private final Map<String, Boolean> observedTaskCompletion = new HashMap<>();

    public DiarySnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;

        Map<String, Integer> completed = new HashMap<>();
        Map<String, Integer> totals = new HashMap<>();
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();

        for (int i = 0; i < REGIONS.length; i++)
            add(completed, totals, tiers, REGIONS[i], VARBITS[i]);

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
        var title = client.getWidget(InterfaceID.Journalscroll.TITLE);
        var layer = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
        if (title == null || layer == null) return false;
        var children = layer.getStaticChildren();
        if (children == null || children.length == 0) return false;
        var region = regionFor(title.getText());
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
        var regionTasks = catalog.all();
        var buffered = "";
        var bufferedComplete = false;
        for (String raw : rows)
        {
            if (raw == null) continue;
            var row = normalizeSpace(Text.removeTags(raw));
            if (row.isEmpty()) continue;
            var struck = raw.toLowerCase(Locale.ROOT).contains("<str>");

            var direct = match(regionTasks, region, row);
            if (direct != null)
            {
                result.put(direct.task.getId(), struck);
                buffered = "";
                bufferedComplete = false;
                continue;
            }

            var combined = buffered.isEmpty() ? row : buffered + " " + row;
            var wrapped = match(regionTasks, region, combined);
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
            var instruction = normalizeSpace(task.getTask());
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
        for (String region : REGIONS)
            if (title.contains(region.split(" ")[0].toLowerCase(Locale.ROOT)))
                return region;
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
            String region, int[] ids)
    {
        EnumMap<DiaryTier, Boolean> tierMap = new EnumMap<>(DiaryTier.class);
        var values = DiaryTier.values();
        for (int i = 0; i < values.length; i++)
            tierMap.put(values[i], client.getVarbitValue(ids[i]) >= 1);
        tiers.put(region, tierMap);

        var done = 0;
        for (int i = 4; i < ids.length; i++)
            done += Math.max(0, client.getVarbitValue(ids[i]));
        completed.put(region, done);
        // Per-tier totals are maintained outside RuneLite's public count varbits.
        // Leave this unknown rather than freezing a copied third-party table.
        totals.put(region, 0);
    }
}

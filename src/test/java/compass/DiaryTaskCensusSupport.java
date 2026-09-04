package compass;

import java.util.LinkedHashMap;
import java.util.Map;

final class DiaryTaskCensusSupport
{
    private DiaryTaskCensusSupport() { }

    static Map<String, Map<DiaryTier, Integer>> census(DiaryTaskCatalog catalog)
    {
        Map<String, Map<DiaryTier, Integer>> result = new LinkedHashMap<>();
        for (DiaryTaskDefinition task : catalog.all())
        {
            Map<DiaryTier, Integer> tiers = result.computeIfAbsent(
                    task.getRegion(), key -> new LinkedHashMap<>());
            tiers.put(task.getTier(), tiers.getOrDefault(task.getTier(), 0) + 1);
        }
        return result;
    }

    static boolean transportRelevant(DiaryTaskDefinition task)
    {
        String value = task.getTask().toLowerCase(java.util.Locale.ROOT);
        return value.contains("teleport") || value.contains("travel")
                || value.contains("fairy ring") || value.contains("glider")
                || value.contains("balloon") || value.contains("boat")
                || value.contains("minecart");
    }
}

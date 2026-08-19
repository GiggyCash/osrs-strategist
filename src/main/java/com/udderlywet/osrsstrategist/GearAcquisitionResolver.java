package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves gear and quest edges recursively without bypassing live evidence. */
public final class GearAcquisitionResolver
{
    private static final int MAX_DEPTH = 12;
    private final GearAcquisitionCatalog gear;
    private final QuestKnowledgeCatalog quests;
    private boolean cyclePrevented;
    private boolean depthLimited;

    public GearAcquisitionResolver(GearAcquisitionCatalog gear,
            QuestKnowledgeCatalog quests)
    {
        this.gear = gear;
        this.quests = quests;
    }

    public GearAcquisitionResolution resolve(String item,
            StrategyContext context)
    {
        List<GearAcquisitionStep> result = new ArrayList<>();
        cyclePrevented = false;
        depthLimited = false;
        resolveGear(item, context, 0, new HashSet<>(), result);
        return new GearAcquisitionResolution(item, result,
                cyclePrevented, depthLimited);
    }

    private void resolveGear(String item, StrategyContext context, int depth,
            Set<String> path, List<GearAcquisitionStep> result)
    {
        if (depth >= MAX_DEPTH) { depthLimited = true; return; }
        String key = "gear:" + normalize(item);
        if (!path.add(key)) { cyclePrevented = true; return; }
        GearAcquisitionRoute route = gear.forItem(item);
        if (route == null) { path.remove(key); return; }
        for (GearAcquisitionStep step : route.getSteps())
        {
            if (step.getKind() == GearAcquisitionStep.Kind.QUEST)
                resolveQuest(step.getTarget(), context, depth + 1, path, result);
            else if (step.getKind() == GearAcquisitionStep.Kind.VERIFY
                    && gear.forItem(step.getTarget()) != null)
                resolveGear(step.getTarget(), context, depth + 1, path, result);
            result.add(step);
        }
        path.remove(key);
    }

    private void resolveQuest(String name, StrategyContext context, int depth,
            Set<String> path, List<GearAcquisitionStep> result)
    {
        if (depth >= MAX_DEPTH) { depthLimited = true; return; }
        String key = "quest:" + normalize(name);
        if (!path.add(key)) { cyclePrevented = true; return; }
        QuestDefinition definition = quests.definitionFor(name);
        if (definition == null) { path.remove(key); return; }
        QuestSnapshot snapshot = context == null || context.getData() == null
                ? null : context.getData().getQuests();
        for (String prerequisite : definition.getPrerequisites())
        {
            QuestStatus status = snapshot == null ? QuestStatus.UNKNOWN
                    : snapshot.statusOf(prerequisite);
            if (status == QuestStatus.COMPLETE) continue;
            resolveQuest(prerequisite, context, depth + 1, path, result);
            result.add(new GearAcquisitionStep(GearAcquisitionStep.Kind.QUEST,
                    prerequisite, status == QuestStatus.UNKNOWN
                    ? "Verify and complete " + prerequisite + " before " + name
                    : "Complete " + prerequisite + " before " + name));
            break;
        }
        path.remove(key);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
    }
}

package compass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Deterministic development-time report of every unresolved quest field. */
public final class QuestUncertaintyReport
{
    private final List<QuestUncertaintyEntry> entries;

    public QuestUncertaintyReport()
    {
        List<QuestUncertaintyEntry> result = new ArrayList<>();
        for (QuestDefinition quest : new QuestKnowledgeCatalog().all().values())
            for (String field : quest.getFieldUncertainties())
                expand(result, quest.getName(), field);
        result.sort((left, right) ->
        {
            int byQuest = left.getQuestName().compareTo(right.getQuestName());
            return byQuest != 0 ? byQuest
                    : left.getCategory().compareTo(right.getCategory());
        });
        entries = Collections.unmodifiableList(result);
    }

    public List<QuestUncertaintyEntry> all() { return entries; }

    public long uncertainQuestCount()
    {
        return entries.stream().map(QuestUncertaintyEntry::getQuestName)
                .distinct().count();
    }

    public Map<QuestUncertaintyEntry.Category, Integer> countsByCategory()
    {
        Map<QuestUncertaintyEntry.Category, Integer> result =
                new EnumMap<>(QuestUncertaintyEntry.Category.class);
        for (QuestUncertaintyEntry entry : entries)
            result.merge(entry.getCategory(), 1, Integer::sum);
        return Collections.unmodifiableMap(result);
    }

    private static void expand(List<QuestUncertaintyEntry> target,
            String quest, String raw)
    {
        String field = raw == null ? "" : raw.toLowerCase();
        if (field.equals("items"))
        {
            add(target, quest, QuestUncertaintyEntry.Category.ITEMS, raw);
            add(target, quest, QuestUncertaintyEntry.Category.ITEM_ALTERNATIVES, raw);
            add(target, quest, QuestUncertaintyEntry.Category.QUANTITIES, raw);
        }
        else if (field.equals("access/combat"))
        {
            add(target, quest, QuestUncertaintyEntry.Category.ACCESS, raw);
            add(target, quest, QuestUncertaintyEntry.Category.TRANSPORTATION, raw);
            add(target, quest, QuestUncertaintyEntry.Category.COMBAT, raw);
        }
        else if (field.equals("rewards/unlocks"))
        {
            add(target, quest, QuestUncertaintyEntry.Category.REWARDS, raw);
            add(target, quest, QuestUncertaintyEntry.Category.XP_REWARDS, raw);
            add(target, quest, QuestUncertaintyEntry.Category.IRREVERSIBLE_XP, raw);
            add(target, quest, QuestUncertaintyEntry.Category.UNLOCKS, raw);
        }
        else if (field.equals("start location"))
            add(target, quest, QuestUncertaintyEntry.Category.START_LOCATION, raw);
        else if (field.equals("quest points"))
            add(target, quest, QuestUncertaintyEntry.Category.QUEST_POINTS, raw);
        else add(target, quest, QuestUncertaintyEntry.Category.OTHER, raw);
    }

    private static void add(List<QuestUncertaintyEntry> target, String quest,
            QuestUncertaintyEntry.Category category, String detail)
    {
        target.add(new QuestUncertaintyEntry(quest, category, detail));
    }
}

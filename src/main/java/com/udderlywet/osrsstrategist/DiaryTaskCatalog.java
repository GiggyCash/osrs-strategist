package com.udderlywet.osrsstrategist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/** Complete pinned RuneLite diary task/prerequisite catalogue. */
@Singleton
public final class DiaryTaskCatalog
{
    public static final int EXPECTED_TASKS = 378;
    public static final String PROVENANCE =
            Text.get(213);
    private static final Pattern SKILL = Pattern.compile(
            "new SkillRequirement\\(Skill\\.([A-Z_]+),\\s*(\\d+)\\)");
    private static final Pattern QUEST = Pattern.compile(
            "new QuestRequirement\\(Quest\\.([A-Z0-9_]+)(?:,\\s*(true|false))?\\)");
    private static final Pattern COMBAT = Pattern.compile(
            "new CombatLevelRequirement\\((\\d+)\\)");
    private static final Pattern QUEST_POINTS = Pattern.compile(
            "new QuestPointRequirement\\((\\d+)\\)");

    private final List<DiaryTaskDefinition> tasks;

    public DiaryTaskCatalog()
    {
        tasks = Collections.unmodifiableList(load());
        if (tasks.size() != EXPECTED_TASKS)
            throw new IllegalStateException("Expected " + EXPECTED_TASKS
                    + " diary tasks, found " + tasks.size());
    }

    public List<DiaryTaskDefinition> all() { return tasks; }

    public List<DiaryTaskDefinition> forTier(String region, DiaryTier tier)
    {
        List<DiaryTaskDefinition> result = new ArrayList<>();
        for (DiaryTaskDefinition task : tasks)
            if (task.getRegion().equalsIgnoreCase(region)
                    && task.getTier() == tier) result.add(task);
        return Collections.unmodifiableList(result);
    }

    public Map<String, Map<DiaryTier, Integer>> census()
    {
        Map<String, Map<DiaryTier, Integer>> result = new LinkedHashMap<>();
        for (DiaryTaskDefinition task : tasks)
        {
            Map<DiaryTier, Integer> tiers = result.computeIfAbsent(
                    task.getRegion(), key -> new LinkedHashMap<>());
            tiers.put(task.getTier(), tiers.getOrDefault(task.getTier(), 0) + 1);
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<DiaryTaskDefinition> load()
    {
        InputStream stream = DiaryTaskCatalog.class.getResourceAsStream(
                "/content/diary-tasks.tsv");
        if (stream == null)
            throw new IllegalStateException("Missing diary task evidence resource");
        List<DiaryTaskDefinition> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null)
            {
                number++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", 4);
                if (fields.length != 4)
                    throw new IllegalStateException("Invalid diary evidence line "
                            + number);
                DiaryTier tier = DiaryTier.valueOf(
                        fields[1].toUpperCase(Locale.ROOT));
                result.add(new DiaryTaskDefinition(fields[0], tier, fields[2],
                        requirements(fields[3])));
            }
        }
        catch (IOException | IllegalArgumentException ex)
        {
            throw new IllegalStateException("Unable to read diary task evidence", ex);
        }
        return result;
    }

    private static List<DiaryTaskRequirement> requirements(String raw)
    {
        List<DiaryTaskRequirement> result = new ArrayList<>();
        if (raw.contains("new OrRequirement"))
        {
            result.add(DiaryTaskRequirement.alternative(
                    Text.get(214) + raw));
            return result;
        }
        Matcher skill = SKILL.matcher(raw);
        while (skill.find())
            result.add(DiaryTaskRequirement.skill(
                    Skill.valueOf(skill.group(1)),
                    Integer.parseInt(skill.group(2))));
        Matcher quest = QUEST.matcher(raw);
        while (quest.find())
        {
            Quest identity = Quest.valueOf(quest.group(1));
            result.add(DiaryTaskRequirement.quest(identity.getName(),
                    Boolean.parseBoolean(quest.group(2))));
        }
        Matcher combat = COMBAT.matcher(raw);
        while (combat.find()) result.add(DiaryTaskRequirement.combat(
                Integer.parseInt(combat.group(1))));
        Matcher points = QUEST_POINTS.matcher(raw);
        while (points.find()) result.add(DiaryTaskRequirement.questPoints(
                Integer.parseInt(points.group(1))));
        return result;
    }
}

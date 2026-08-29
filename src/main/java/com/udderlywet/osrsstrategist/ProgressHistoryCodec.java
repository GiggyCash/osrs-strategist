package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

/** Versioned fail-closed persistence codec for bounded local progress data. */
final class ProgressHistoryCodec
{
    private static final int SCHEMA_VERSION = 2;
    private static final long MAX_ACCOUNT_XP = 5_000_000_000L;
    private static final int MAX_SESSION_LEVELS = 3_000;

    private ProgressHistoryCodec() { }

    static String encode(Gson gson, ProgressHistory history)
    {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray sessions = new JsonArray();
        for (ProgressSessionSummary value : history.getSessions())
        {
            JsonObject object = new JsonObject();
            object.addProperty("startedAtMillis", value.getStartedAtMillis());
            object.addProperty("endedAtMillis", value.getEndedAtMillis());
            object.addProperty("activeDurationMillis",
                    value.getActiveDurationMillis());
            object.addProperty("totalXpGained", value.getTotalXpGained());
            object.addProperty("levelsGained", value.getLevelsGained());
            object.add("xpBySkill", skills(value.getXpBySkill()));
            object.add("milestones", milestones(value.getMilestones()));
            sessions.add(object);
        }
        root.add("sessions", sessions);

        root.add("milestones", milestones(history.getMilestones()));

        JsonArray buckets = new JsonArray();
        for (ProgressTimeBucket value : history.getBuckets())
        {
            JsonObject object = new JsonObject();
            object.addProperty("startedAtMillis", value.getStartedAtMillis());
            object.add("xpBySkill", skills(value.getXpBySkill()));
            buckets.add(object);
        }
        root.add("buckets", buckets);
        return gson.toJson(root);
    }

    static ProgressHistory decode(Gson gson, String json)
    {
        ProgressHistory history = new ProgressHistory();
        JsonObject root = object(gson, json);
        int schema = root == null ? -1
                : integer(root.get("schemaVersion"), -1);
        if (schema != 1 && schema != SCHEMA_VERSION)
            return history;
        try
        {
            history.replaceAll(readSessions(root.get("sessions")),
                    readMilestones(root.get("milestones")),
                    readBuckets(root.get("buckets")));
        }
        catch (RuntimeException ignored)
        {
            // One corrupt document cannot leak partial or untrusted state.
            history.clear();
        }
        return history;
    }

    private static List<ProgressSessionSummary> readSessions(JsonElement value)
    {
        List<ProgressSessionSummary> result = new ArrayList<>();
        JsonArray array = array(value);
        if (array == null) return result;
        for (JsonElement element : array)
        {
            JsonObject object = asObject(element);
            if (object == null) continue;
            long started = naturalLong(object.get("startedAtMillis"), -1L);
            long ended = naturalLong(object.get("endedAtMillis"), -1L);
            long active = naturalLong(object.get("activeDurationMillis"), -1L);
            long xp = naturalLong(object.get("totalXpGained"), -1L);
            int levels = integer(object.get("levelsGained"), -1);
            if (started < 0L || ended < started || active < 0L
                    || active > ended - started || xp < 0L
                    || xp > MAX_ACCOUNT_XP || levels < 0
                    || levels > MAX_SESSION_LEVELS)
                continue;
            Map<Skill, Integer> skills = readSkills(object.get("xpBySkill"));
            long skillTotal = skills.values().stream()
                    .mapToLong(Integer::longValue).sum();
            if (skillTotal != xp) continue;
            result.add(new ProgressSessionSummary(started, ended, active, xp,
                    levels, skills,
                    readMilestones(object.get("milestones"))));
        }
        return result;
    }

    private static List<ProgressMilestone> readMilestones(JsonElement value)
    {
        List<ProgressMilestone> result = new ArrayList<>();
        JsonArray array = array(value);
        if (array == null) return result;
        for (JsonElement element : array)
        {
            JsonObject object = asObject(element);
            String id = string(object, "id");
            String type = string(object, "type");
            String title = string(object, "title");
            long at = object == null ? -1L
                    : naturalLong(object.get("occurredAtMillis"), -1L);
            if (id == null || type == null || title == null || at < 0L)
                continue;
            try
            {
                result.add(new ProgressMilestone(id,
                        ProgressMilestoneType.valueOf(type), title,
                        string(object, "detail"), string(object, "goalId"), at));
            }
            catch (IllegalArgumentException ignored)
            {
                // Removed/unknown milestone types do not become fake progress.
            }
        }
        return result;
    }

    private static JsonArray milestones(List<ProgressMilestone> values)
    {
        JsonArray result = new JsonArray();
        if (values == null) return result;
        for (ProgressMilestone value : values)
        {
            if (value == null) continue;
            JsonObject object = new JsonObject();
            object.addProperty("id", value.getId());
            object.addProperty("type", value.getType().name());
            object.addProperty("title", value.getTitle());
            addOptional(object, "detail", value.getDetail());
            addOptional(object, "goalId", value.getGoalId());
            object.addProperty("occurredAtMillis", value.getOccurredAtMillis());
            result.add(object);
        }
        return result;
    }

    private static List<ProgressTimeBucket> readBuckets(JsonElement value)
    {
        List<ProgressTimeBucket> result = new ArrayList<>();
        JsonArray array = array(value);
        if (array == null) return result;
        for (JsonElement element : array)
        {
            JsonObject object = asObject(element);
            long at = object == null ? -1L
                    : naturalLong(object.get("startedAtMillis"), -1L);
            Map<Skill, Integer> xp = object == null
                    ? new EnumMap<>(Skill.class)
                    : readSkills(object.get("xpBySkill"));
            if (at >= 0L && !xp.isEmpty())
                result.add(new ProgressTimeBucket(at, xp));
        }
        return result;
    }

    private static JsonObject skills(Map<Skill, Integer> values)
    {
        JsonObject result = new JsonObject();
        for (Map.Entry<Skill, Integer> entry : values.entrySet())
            if (!overall(entry.getKey()) && entry.getValue() != null
                    && entry.getValue() > 0)
                result.addProperty(entry.getKey().name(), entry.getValue());
        return result;
    }

    private static Map<Skill, Integer> readSkills(JsonElement value)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        JsonObject object = asObject(value);
        if (object == null) return result;
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            int xp = integer(entry.getValue(), -1);
            if (xp <= 0 || xp > ProgressAnalyticsService.MAX_SKILL_XP)
                continue;
            try
            {
                Skill skill = Skill.valueOf(entry.getKey());
                if (!overall(skill)) result.put(skill, xp);
            }
            catch (IllegalArgumentException ignored)
            {
                // New/removed skills are ignored by older clients.
            }
        }
        return result;
    }

    private static void addOptional(JsonObject object, String key, String value)
    {
        if (value != null && !value.trim().isEmpty()) object.addProperty(key, value);
    }

    private static JsonObject object(Gson gson, String json)
    {
        try
        {
            return gson == null || json == null ? null
                    : gson.fromJson(json, JsonObject.class);
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private static JsonObject asObject(JsonElement value)
    {
        return value != null && value.isJsonObject()
                ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonElement value)
    {
        return value != null && value.isJsonArray()
                ? value.getAsJsonArray() : null;
    }

    private static String string(JsonObject object, String key)
    {
        try
        {
            JsonElement value = object == null ? null : object.get(key);
            return value != null && value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isString()
                    ? value.getAsString() : null;
        }
        catch (RuntimeException ignored)
        {
            return null;
        }
    }

    private static int integer(JsonElement value, int fallback)
    {
        try
        {
            return value != null && value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isNumber()
                    ? value.getAsInt() : fallback;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static long naturalLong(JsonElement value, long fallback)
    {
        try
        {
            if (value == null || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber()) return fallback;
            long result = value.getAsLong();
            return result < 0L ? fallback : result;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static boolean overall(Skill skill)
    {
        return skill != null && "OVERALL".equals(skill.name());
    }
}

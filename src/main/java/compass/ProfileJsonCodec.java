package compass;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;

/** Explicit profile JSON decoding without plugin-side reflection or TypeToken. */
final class ProfileJsonCodec
{
    private ProfileJsonCodec() { }

    static Map<String, Double> doubles(Gson gson, String json)
    {
        var object = object(gson, json);
        if (object == null) return Collections.emptyMap();
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
            if (number(entry.getValue()))
            {
                var value = entry.getValue().getAsDouble();
                if (Double.isFinite(value)) result.put(entry.getKey(), value);
            }
        return result;
    }

    static Map<String, Long> longs(Gson gson, String json)
    {
        var object = object(gson, json);
        if (object == null) return Collections.emptyMap();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
            if (number(entry.getValue()))
                result.put(entry.getKey(), entry.getValue().getAsLong());
        return result;
    }

    static Set<Integer> integers(Gson gson, String json)
    {
        try
        {
            var array = gson.fromJson(json, JsonArray.class);
            if (array == null) return Collections.emptySet();
            Set<Integer> result = new LinkedHashSet<>();
            for (JsonElement value : array)
                if (number(value)) result.add(value.getAsInt());
            return result;
        }
        catch (RuntimeException ignored)
        {
            return Collections.emptySet();
        }
    }

    static Map<String, TimedScoreAdjustment> timedAdjustments(
            Gson gson, String json)
    {
        var object = object(gson, json);
        if (object == null) return Collections.emptyMap();
        Map<String, TimedScoreAdjustment> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            var value = asObject(entry.getValue());
            if (value == null || !number(value.get("scoreDelta"))
                    || !number(value.get("expiresAtMillis"))) continue;
            var score = value.get("scoreDelta").getAsDouble();
            if (!Double.isFinite(score)) continue;
            result.put(entry.getKey(), new TimedScoreAdjustment(score,
                    value.get("expiresAtMillis").getAsLong()));
        }
        return result;
    }

    static Map<String, ObservedFarmingPatchState> farmingStates(
            Gson gson, String json)
    {
        var object = object(gson, json);
        if (object == null) return Collections.emptyMap();
        Map<String, ObservedFarmingPatchState> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            var value = asObject(entry.getValue());
            if (value == null || value.get("state") == null
                    || !number(value.get("observedAtMillis"))) continue;
            try
            {
                FarmingPatchCycleState state = FarmingPatchCycleState.valueOf(
                        value.get("state").getAsString());
                if (state != FarmingPatchCycleState.UNKNOWN)
                    result.put(entry.getKey(), new ObservedFarmingPatchState(
                            state, value.get("observedAtMillis").getAsLong()));
            }
            catch (IllegalArgumentException ignored)
            {
                // A removed or corrupt enum value is unknown, never evidence.
            }
        }
        return result;
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

    private static boolean number(JsonElement value)
    {
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber();
    }
}

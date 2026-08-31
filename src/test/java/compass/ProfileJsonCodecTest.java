package compass;

import com.google.gson.Gson;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileJsonCodecTest
{
    private final Gson gson = new Gson();

    @Test
    public void preservesExistingProfileJsonShapesWithoutTypeToken()
    {
        assertEquals(1.5, ProfileJsonCodec.doubles(gson,
                "{\"skill:mining\":1.5}").get("skill:mining"), 0.0);
        assertEquals(Long.valueOf(1234), ProfileJsonCodec.longs(gson,
                "{\"quest:test\":1234}").get("quest:test"));
        assertTrue(ProfileJsonCodec.integers(gson, "[1,2,3]").contains(2));

        Map<String, TimedScoreAdjustment> timed =
                ProfileJsonCodec.timedAdjustments(gson,
                        "{\"semantic:skill:mining\":{"
                                + "\"scoreDelta\":-2.5,"
                                + "\"expiresAtMillis\":9000}}");
        assertEquals(-2.5, timed.get("semantic:skill:mining")
                .getScoreDelta(), 0.0);
        assertEquals(9000L, timed.get("semantic:skill:mining")
                .getExpiresAtMillis());

        Map<String, ObservedFarmingPatchState> farming =
                ProfileJsonCodec.farmingStates(gson,
                        "{\"falador-herb\":{"
                                + "\"state\":\"READY\","
                                + "\"observedAtMillis\":8000}}");
        assertEquals(FarmingPatchCycleState.READY,
                farming.get("falador-herb").getState());
    }

    @Test
    public void malformedOrUnknownProfileEvidenceFailsClosed()
    {
        assertTrue(ProfileJsonCodec.doubles(gson, "not-json").isEmpty());
        assertTrue(ProfileJsonCodec.longs(gson, "[]").isEmpty());
        assertTrue(ProfileJsonCodec.integers(gson, "{}").isEmpty());
        assertTrue(ProfileJsonCodec.timedAdjustments(gson,
                "{\"bad\":{\"scoreDelta\":\"x\"}}").isEmpty());
        Map<String, ObservedFarmingPatchState> farming =
                ProfileJsonCodec.farmingStates(gson,
                        "{\"unknown\":{"
                                + "\"state\":\"REMOVED_STATE\","
                                + "\"observedAtMillis\":1}}");
        assertFalse(farming.containsKey("unknown"));
    }
}

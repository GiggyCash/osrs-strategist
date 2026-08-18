package com.udderlywet.osrsstrategist;

import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Guards the official gameval replacements used by the live readers. */
public class RuneLiteVarbitMigrationTest
{
    @Test
    public void accountAndCombatAchievementMappingsPreserveLiveSemantics()
    {
        assertEquals(1777, VarbitID.IRONMAN);
        assertArrayEquals(new int[] {12863, 12864, 12865, 12866, 12867, 12868},
                new int[] {VarbitID.CA_TIER_STATUS_EASY,
                        VarbitID.CA_TIER_STATUS_MEDIUM,
                        VarbitID.CA_TIER_STATUS_HARD,
                        VarbitID.CA_TIER_STATUS_ELITE,
                        VarbitID.CA_TIER_STATUS_MASTER,
                        VarbitID.CA_TIER_STATUS_GRANDMASTER});
    }

    @Test
    public void diaryCompletionMappingsPreserveLiveSemantics()
    {
        assertArrayEquals(new int[] {4458, 4459, 4460, 4461},
                new int[] {VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE});
        assertArrayEquals(new int[] {3578, 3599, 3611, 4566},
                new int[] {VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE,
                        VarbitID.ATJUN_HARD_DONE,
                        VarbitID.KARAMJA_DIARY_ELITE_COMPLETE});
        assertArrayEquals(new int[] {4466, 4467, 4468, 4469},
                new int[] {VarbitID.WILDERNESS_DIARY_EASY_COMPLETE,
                        VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE,
                        VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,
                        VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE});
    }
}

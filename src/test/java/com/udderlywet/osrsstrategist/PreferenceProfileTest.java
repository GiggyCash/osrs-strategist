package com.udderlywet.osrsstrategist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreferenceProfileTest
{
    @Test
    public void laterSnoozesWithoutTeachingDislike()
    {
        PreferenceProfile profile = new PreferenceProfile();

        profile.apply(
                "skill:farming",
                FeedbackAction.LATER
        );

        assertTrue(
                profile.isOnCooldown("skill:farming")
        );
        assertEquals(
                0.0,
                profile.weightFor("skill:farming"),
                0.0001
        );
    }

    @Test
    public void dislikeAddsCooldownAndNegativePreference()
    {
        PreferenceProfile profile = new PreferenceProfile();

        profile.apply(
                "skill:agility",
                FeedbackAction.DISLIKE
        );

        assertTrue(
                profile.isOnCooldown("skill:agility")
        );
        assertTrue(
                profile.weightFor("skill:agility") < 0.0
        );
    }
}

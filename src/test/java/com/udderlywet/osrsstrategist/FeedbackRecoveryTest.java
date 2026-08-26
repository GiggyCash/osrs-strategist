package com.udderlywet.osrsstrategist;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FeedbackRecoveryTest
{
    @Test
    public void resetActionIsSecondaryExplainedAndConfirmed() throws Exception
    {
        Method method = OsrsStrategistConfig.class.getMethod(
                "resetLearnedFeedback");
        ConfigItem item = method.getAnnotation(ConfigItem.class);
        assertEquals("Reset learned feedback", item.name());
        assertEquals(OsrsStrategistConfig.advancedSection, item.section());
        assertTrue(item.description().contains("current character only"));
        assertTrue(item.description().contains("bank observations"));
        assertFalse(item.warning().trim().isEmpty());

        Field sectionField = OsrsStrategistConfig.class.getField(
                "advancedSection");
        ConfigSection section = sectionField.getAnnotation(ConfigSection.class);
        assertTrue(section.closedByDefault());
    }

    @Test
    public void resetClearsOnlyCurrentCharacterLearningAndForcesRefresh()
            throws Exception
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        RecordingPreferenceStore store = new RecordingPreferenceStore(
                "character-a");
        set(plugin, "accountPreferenceStore", store);
        set(plugin, "loadedPreferenceProfileKey", "character-a");
        set(plugin, "latestRecommendations", Collections.singletonList(
                recommendation()));

        PreferenceProfile preferences = (PreferenceProfile) get(
                plugin, "preferenceProfile");
        preferences.apply("skill:mining", FeedbackAction.DISLIKE);
        RecommendationHistory history = (RecommendationHistory) get(
                plugin, "recommendationHistory");
        history.add("skill:mining", "Train Mining",
                RecommendationHistoryAction.COMPLETED);

        assertTrue(plugin.resetLearnedFeedbackForActiveCharacter());

        assertEquals(1, store.clearCount);
        assertEquals("character-a", store.clearedProfile);
        assertTrue(preferences.snapshot().isEmpty());
        assertTrue(preferences.cooldownSnapshot().isEmpty());
        assertEquals(1, history.snapshot().size());
        assertTrue(((java.util.List<?>) get(plugin,
                "latestRecommendations")).isEmpty());
        assertTrue(plugin.consumeAccountRefreshPending());
    }

    @Test
    public void resetWithoutACharacterDoesNothing() throws Exception
    {
        OsrsStrategistPlugin plugin = new OsrsStrategistPlugin();
        RecordingPreferenceStore store = new RecordingPreferenceStore(null);
        set(plugin, "accountPreferenceStore", store);

        assertFalse(plugin.resetLearnedFeedbackForActiveCharacter());
        assertEquals(0, store.clearCount);
    }

    private static Recommendation recommendation()
    {
        return new Recommendation("skill:mining", "Mine copper to level 2",
                "Safe start.", 1.0, null,
                RecommendationConfidence.VERIFIED, 1, 2,
                new RecommendationGuidance("Mine copper and drop it when full.",
                        "Bronze pickaxe.",
                        "East Lumbridge Swamp mine.", null),
                CandidateSafetyEvidence.harmless(true));
    }

    private static Object get(Object target, String name) throws Exception
    {
        Field field = OsrsStrategistPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value)
            throws Exception
    {
        Field field = OsrsStrategistPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class RecordingPreferenceStore
            extends AccountPreferenceStore
    {
        private final String profile;
        private int clearCount;
        private String clearedProfile;

        private RecordingPreferenceStore(String profile)
        {
            super(null, new Gson());
            this.profile = profile;
        }

        @Override
        public String getActiveProfileKey()
        {
            return profile;
        }

        @Override
        public void loadInto(PreferenceProfile preferenceProfile)
        {
            // Tests seed the active in-memory profile directly.
        }

        @Override
        public void clear()
        {
            clearCount++;
            clearedProfile = profile;
        }
    }
}

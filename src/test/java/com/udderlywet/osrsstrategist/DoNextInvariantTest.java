package com.udderlywet.osrsstrategist;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DoNextInvariantTest
{
    @Test
    public void normalAndEveryNegativeFeedbackActionHaveImmediateDoNext()
    {
        Recommendation first = ready("skill:mining", 100);
        Recommendation second = ready("skill:fishing", 90);
        StrategyEngine engine = engine(first, second);
        StrategyDataBundle data = observedData();

        assertEquals(first.getId(), evaluate(engine, data,
                new PreferenceProfile()).getId());
        for (FeedbackAction action : Arrays.asList(FeedbackAction.LATER,
                FeedbackAction.NOT_TODAY, FeedbackAction.DISLIKE))
        {
            PreferenceProfile preferences = new PreferenceProfile();
            preferences.apply(first.getId(), action);
            assertEquals(second.getId(), evaluate(engine, data, preferences).getId());
        }
    }

    @Test
    public void multipleSuppressedCandidatesStillProduceFallback()
    {
        Recommendation first = ready("skill:mining", 100);
        Recommendation second = ready("skill:fishing", 90);
        PreferenceProfile preferences = new PreferenceProfile();
        preferences.apply(first.getId(), FeedbackAction.NOT_TODAY);
        preferences.apply(second.getId(), FeedbackAction.LATER);

        Recommendation result = evaluate(engine(first, second), observedData(),
                preferences);
        assertTrue(FallbackRecommendationFactory.isFallback(result));
        assertFalse(RecommendationPresentation.compactText(result).trim().isEmpty());
    }

    @Test
    public void usefulPreparationLeadsBeforeGenericFallback()
    {
        Recommendation preparation = new Recommendation(
                "prepare:bank", "Verify banked supplies", "Bank state is missing.",
                50, null, RecommendationConfidence.CHECK_NEEDED, 0, 0,
                new RecommendationGuidance(
                        "Open your bank and leave it open for one game tick.",
                        "No supplies required.", "Any bank.",
                        "This records an observed snapshot."),
                CandidateSafetyEvidence.harmless(true));

        assertEquals(preparation.getId(), evaluate(engine(preparation),
                observedData(), new PreferenceProfile()).getId());
    }

    @Test
    public void exhaustedAndUnavailablePoolsProduceHonestFallbacks()
    {
        Recommendation exhausted = evaluate(engine(), observedData(),
                new PreferenceProfile());
        assertEquals("fallback:goal", exhausted.getId());

        StrategyResult unavailable = engine().evaluate(null,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                new PreferenceProfile());
        assertEquals("fallback:login",
                unavailable.getRecommendations().get(0).getId());
    }

    @Test
    public void sidebarNeverRendersBlankWhenGivenEmptyOrNullQueue()
    {
        OsrsStrategistPanel panel = new OsrsStrategistPanel((id, action) -> { }, null);
        panel.updateRecommendations(Collections.emptyList());
        assertTrue(allText(panel).contains("Log in to continue"));
        panel.updateRecommendations(null);
        String text = allText(panel);
        assertTrue(text.contains("Log in to continue"));
        assertFalse(text.trim().isEmpty());
    }

    private static StrategyEngine engine(Recommendation... candidates)
    {
        RecommendationEngine recommendations = new RecommendationEngine(
                (TrainingMethodSelector) null)
        {
            @Override
            public List<Recommendation> recommendAll(StrategyDataBundle data,
                    StrategyMode mode, SessionIntent intent, boolean groupStorage,
                    boolean wilderness, PreferenceProfile preferences)
            {
                List<Recommendation> visible = new ArrayList<>();
                for (Recommendation candidate : candidates)
                    if (!preferences.isOnCooldown(candidate.getId())) visible.add(candidate);
                return visible;
            }
        };
        return new StrategyEngine(recommendations, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static Recommendation evaluate(StrategyEngine engine,
            StrategyDataBundle data, PreferenceProfile preferences)
    {
        List<Recommendation> recommendations = engine.evaluate(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                preferences).getRecommendations();
        assertFalse(recommendations.isEmpty());
        return recommendations.get(0);
    }

    private static Recommendation ready(String id, double score)
    {
        Skill skill = id.contains("mining") ? Skill.MINING : Skill.FISHING;
        return new Recommendation(id, "Train " + skill.getName(), "Safe training.",
                score, null, RecommendationConfidence.VERIFIED, 1, 2,
                new RecommendationGuidance("Use the verified safe method.",
                        "Use your observed setup.", "A safe area.",
                        "This is a legal training action."),
                CandidateSafetyEvidence.skill(true, skill));
    }

    private static StrategyDataBundle observedData()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 20); xp.put(skill, 0); }
        AccountSnapshot account = new AccountSnapshot("Fallback", 0, "Main",
                MembershipStatus.F2P, 0, 460, 0, levels, xp);
        return StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L)).build();
    }

    private static String allText(Component component)
    {
        StringBuilder text = new StringBuilder();
        if (component instanceof JTextArea)
            text.append(((JTextArea) component).getText()).append('\n');
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents())
                text.append(allText(child));
        return text.toString();
    }
}

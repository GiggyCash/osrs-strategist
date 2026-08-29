package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrategyCandidateFoundationTest
{
    @Test
    public void tierOnlyClueAsksForOneConcreteObservation()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account())
                .clue(new ClueSnapshot(
                        true, "Hard", System.currentTimeMillis(),
                        RecommendationConfidence.VERIFIED))
                .build();
        StrategyContext context = new StrategyContext(
                data, StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX,
                false, false, new PreferenceProfile());

        StrategyCandidate candidate = new ClueCandidateProvider()
                .candidates(context).get(0);
        Recommendation recommendation = candidate.toRecommendation();

        assertEquals("verify:clue-current-step", recommendation.getId());
        assertTrue(recommendation.getGuidance().getAction()
                .contains("Open the clue scroll once"));
        assertEquals(0, recommendation.getCurrentLevel());
        String compact = RecommendationPresentation.compactHtml(recommendation);
        assertTrue(compact.contains("ACTIVITY"));
        assertTrue(compact.contains("DO"));
    }

    @Test
    public void observedStepProducesOneCoherentPrepPlan()
    {
        ClueStepSnapshot step = new ClueStepSnapshot("emote step",
                "Cheer in Catherby bank wearing the listed equipment.",
                "Catherby bank", Arrays.asList("Maple longbow",
                        "Green d'hide chaps", "Iron med helm"),
                false, false, null, false, "outside catherby bank");
        StrategyCandidate candidate = new ClueCandidateProvider().candidates(
                context(new ClueSnapshot(true, "medium", 1L,
                        RecommendationConfidence.VERIFIED, step),
                        AccountMode.MAIN, false)).get(0);

        assertEquals("prepare:clue-current-step", candidate.getId());
        assertTrue(candidate.getTitle().contains("emote step"));
        assertEquals("Catherby bank", candidate.getGuidance().getLocation());
        assertTrue(candidate.getGuidance().getSupplies()
                .contains("Maple longbow"));
        assertTrue(candidate.getGuidance().getAction().contains("Cheer"));
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                candidate.getConfidence());
    }

    @Test
    public void wildernessStepIsHeldWhenRiskIsDisabled()
    {
        ClueStepSnapshot step = new ClueStepSnapshot("coordinate step",
                "Dig on RuneLite's marked coordinate tile.",
                "RuneLite's marked tile", Collections.emptyList(), true,
                false, null, true, null);
        StrategyCandidate candidate = new ClueCandidateProvider().candidates(
                context(new ClueSnapshot(true, "hard", 1L,
                        RecommendationConfidence.VERIFIED, step),
                        AccountMode.MAIN, false)).get(0);

        assertTrue(candidate.getTitle().startsWith("Hold hard clue"));
        assertTrue(candidate.getGuidance().getAction().startsWith("Bank"));
        assertFalse(candidate.getConfidence()
                == RecommendationConfidence.VERIFIED);
    }

    @Test
    public void setupFreeBeginnerStepCanLeadWithoutInventedRequirements()
    {
        ClueStepSnapshot step = new ClueStepSnapshot("emote step",
                "Bow to Brugsen Bursen at the Grand Exchange.",
                "Grand Exchange", Collections.emptyList(), false, false,
                null, false, null);
        StrategyCandidate candidate = new ClueCandidateProvider().candidates(
                context(new ClueSnapshot(true, "beginner", 1L,
                        RecommendationConfidence.VERIFIED, step),
                        AccountMode.MAIN, false)).get(0);

        assertEquals(RecommendationConfidence.VERIFIED,
                candidate.getConfidence());
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(candidate.toRecommendation()));
    }

    private static StrategyContext context(ClueSnapshot clue,
            AccountMode mode, boolean wilderness)
    {
        AccountSnapshot base = account();
        AccountSnapshot selected = new AccountSnapshot("Test", 99L,
                mode == AccountMode.ULTIMATE_IRONMAN ? 4 : 0, mode.name(),
                MembershipStatus.P2P, 1, 1, 0L,
                base.getSkillLevels(), base.getSkillExperience());
        return new StrategyContext(StrategyDataBundle.builder(selected)
                .clue(clue).build(), StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, wilderness, new PreferenceProfile());
    }

    private static AccountSnapshot account()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        return new AccountSnapshot(
                "Test", 0, "Main", MembershipStatus.P2P,
                1, 1, 0L, levels, xp);
    }
}

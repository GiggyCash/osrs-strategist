package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductConstitutionGuardTest
{
    @Test
    public void publicGoalListContainsOnlyShippedMilestones()
    {
        assertEquals(Arrays.asList(
                "Automatic", "Barrows gloves", "Fire cape", "Quest cape",
                "Prifddinas", "Bowfa", "Infernal cape", "Max cape"),
                Arrays.asList(Arrays.stream(PlayerGoal.values())
                        .map(PlayerGoal::toString).toArray(String[]::new)));
        for (PlayerGoal goal : PlayerGoal.values())
        {
            assertFalse(goal.toString().contains("Custom"));
            assertFalse(goal.toString().contains("Gear target"));
            assertFalse(goal.toString().contains("Raid ready"));
        }
    }

    @Test
    public void everyPublicGoalMapsToARealPlanningPath()
    {
        GoalGraph graph = new GoalGraph();
        for (PlayerGoal goal : PlayerGoal.values())
        {
            if (goal == PlayerGoal.AUTOMATIC) continue;
            assertTrue(goal.name(), graph.hasPlanningPath(goal.toPlanningGoal()));
        }
    }

    @Test
    public void legacyInternalGoalCannotLeakBackThroughCharacterProfile()
    {
        PlayerStrategyProfile legacy = new PlayerStrategyProfile(
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.GEAR_TARGET,
                true, false);
        assertEquals(GoalType.AUTOMATIC,
                legacy.sanitizedForPublicProduct().getActiveGoal());
        PlayerStrategyProfile shipped = new PlayerStrategyProfile(
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.FIRE_CAPE,
                true, false);
        assertEquals(GoalType.FIRE_CAPE,
                shipped.sanitizedForPublicProduct().getActiveGoal());
    }

    @Test
    public void placeholderLocationsAndActionsCannotReachDoNext()
    {
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        assertFalse(policy.canLeadQueue(nonSkill(
                "Choose the best available method.", "Any bank.")));
        assertFalse(policy.canLeadQueue(nonSkill(
                "Start the activity.", "Use a nearby furnace.")));
        assertFalse(policy.canLeadQueue(nonSkill(
                "Complete the named loop.",
                "Cut the best sensible tree at the best available location.")));
        assertTrue(policy.canLeadQueue(nonSkill(
                "Withdraw iron bars, smith them, bank, and repeat.",
                "Varrock West anvils, just south of the bank.")));
    }

    @Test
    public void antiSlopFamiliesFailAsSemanticClasses()
    {
        RecommendationActionabilityPolicy policy =
                new RecommendationActionabilityPolicy();
        String[][] vague = {
                {"Craft whichever rune is useful.", "Earth Altar."},
                {"Use a reachable tree and repeat.", "A training area."},
                {"Cook the fish.", "Use a nearby furnace."},
                {"Start the route.", "Use an F2P anvil."},
                {"Plant seeds.", "Use the active Farming checklist."},
                {"Take the highest Hunter Rumour tier you can access.",
                        "Hunter Guild."},
                {"Rob houses.",
                        "Use the unlocked Varlamore Thieving area."}
        };
        for (String[] example : vague)
            assertFalse(example[0] + " / " + example[1],
                    policy.canLeadQueue(nonSkill(example[0], example[1])));
    }

    @Test
    public void categoryOnlyTitlesCannotLeadEvenWithDecorativeDetail()
    {
        Recommendation vague = new Recommendation("upgrade:vague",
                "Get better gear", "Reason", 10, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance("Inspect the current loadout.",
                        "No supplies.", "Lumbridge Castle courtyard.", null),
                CandidateSafetyEvidence.harmless(true));
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(vague));

        for (Skill skill : Skill.values())
        {
            Recommendation categoryOnly = new Recommendation(
                    "skill:" + skill.name().toLowerCase(),
                    "Train " + skill.getName(), "Reason", 10, null,
                    RecommendationConfidence.VERIFIED, 1, 2,
                    new RecommendationGuidance(
                            "Complete the named training loop.",
                            "Required setup.",
                            "Lumbridge Castle courtyard.", null),
                    CandidateSafetyEvidence.skill(true, skill));
            assertFalse(skill.getName(),
                    new RecommendationActionabilityPolicy()
                            .canLeadQueue(categoryOnly));
        }
    }

    @Test
    public void contradictoryBasicRuneRouteCannotReachDoNext()
    {
        TrainingMethod method = new TrainingMethod(
                "runecraft_f2p_earth", Skill.RUNECRAFT, 9, 13,
                "Craft earth runes", "Earth Altar northeast of Varrock.",
                1, 1, 1, AttentionLevel.MODERATE, 10, 2,
                Collections.emptyList(), RecommendationConfidence.VERIFIED);
        Recommendation recommendation = new Recommendation(
                "skill:runecraft", "Train Runecraft to 10", "Progress.", 10,
                new TrainingPlan(method, "Concrete route",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 9, 10,
                new RecommendationGuidance(
                        "Craft water runes until level 10.", "Pure essence.",
                        "Earth Altar northeast of Varrock.", null));
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(recommendation));
    }

    @Test
    public void alternativesRepresentDifferentActivityDimensions()
    {
        StrategyEngine engine = new StrategyEngine(null, null, null, null,
                new RecommendationActionabilityPolicy());
        java.util.List<Recommendation> queue = engine.buildPlayerQueue(
                Arrays.asList(
                        nonSkill("Defeat boss A.", "Boss arena A."),
                        nonSkill("Defeat boss B.", "Boss arena B."),
                        skill()), null);
        assertEquals(2, queue.size());
        assertFalse(StrategyEngine.alternativeDimension(queue.get(0))
                .equals(StrategyEngine.alternativeDimension(queue.get(1))));
    }

    private static Recommendation nonSkill(String action, String location)
    {
        String id = action.contains("A") ? "pvm:a"
                : action.contains("B") ? "pvm:b" : "upgrade:test";
        return new Recommendation(id, "Concrete recommendation", "Reason.",
                id.endsWith("a") ? 30 : 20, null,
                RecommendationConfidence.VERIFIED, 0, 0,
                new RecommendationGuidance(action, "Required setup.",
                        location, null), CandidateSafetyEvidence.harmless(true));
    }

    private static Recommendation skill()
    {
        TrainingMethod method = new TrainingMethod(
                "mining_iron", Skill.MINING, 15, 99, "Mine iron",
                "Mine iron in the south-east Varrock mine.", 1, 1, 1,
                AttentionLevel.MODERATE, 20, 2, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new Recommendation("skill:mining", "Train Mining to 40",
                "Progress.", 10,
                new TrainingPlan(method, "Concrete route",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 39, 40,
                new RecommendationGuidance(
                        "Mine iron, drop it, and repeat until level 40.",
                        "A usable pickaxe.", "South-east Varrock mine.", null),
                CandidateSafetyEvidence.skill(true, Skill.MINING));
    }
}

package com.udderlywet.osrsstrategist;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StashUnitCatalogTest
{
    @Test
    public void representsEveryCurrentRuneLiteStashWithExactClueEvidence()
    {
        StashUnitCatalog catalog = new StashUnitCatalog();
        assertEquals(119, catalog.all().size());
        assertEquals(3, catalog.forTier(ClueTier.BEGINNER).size());
        assertEquals(31, catalog.forTier(ClueTier.EASY).size());
        assertEquals(25, catalog.forTier(ClueTier.MEDIUM).size());
        assertEquals(16, catalog.forTier(ClueTier.HARD).size());
        assertEquals(19, catalog.forTier(ClueTier.ELITE).size());
        assertEquals(25, catalog.forTier(ClueTier.MASTER).size());

        Set<String> ids = new HashSet<>();
        Set<Integer> objectIds = new HashSet<>();
        for (StashUnitDefinition unit : catalog.all())
        {
            assertTrue(ids.add(unit.getId()));
            assertTrue(objectIds.add(unit.getObjectId()));
            assertFalse(unit.getLocation().trim().isEmpty());
            assertFalse(unit.getClueText().trim().isEmpty());
            assertTrue(unit.getWorldPoints().length > 0);
            assertFalse(unit.getStoredEquipmentEvidence().trim().isEmpty());
            assertNotNull(unit.getTier());
        }
    }

    @Test
    public void tierBuildRulesAreCurrentAndExcludeDragonNails()
    {
        assertTier(StashTierDefinition.BEGINNER, 12, "plank", false);
        assertTier(StashTierDefinition.EASY, 27, "plank", false);
        assertTier(StashTierDefinition.MEDIUM, 42, "oak plank", false);
        assertTier(StashTierDefinition.HARD, 55, "teak plank", false);
        assertTier(StashTierDefinition.ELITE, 77, "mahogany plank", false);
        assertTier(StashTierDefinition.MASTER, 88, "mahogany plank", true);
    }

    @Test
    public void unobservedBuiltStateAlwaysLeadsWithAnExplicitCheck()
    {
        StashUnitDefinition unit = new StashUnitCatalog().all().get(0);
        StashBuildPlan plan = new StashDependencyPlanner().plan(
                unit, StashUnitState.UNKNOWN, null);
        assertEquals(2, plan.getSteps().size());
        assertEquals(GoalNodeKind.PREPARATION_ACTION,
                plan.nextAction().getKind());
        assertTrue(plan.nextAction().getAction().contains("Watson"));
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                plan.nextAction().getConfidence());
    }

    @Test
    public void knownUnbuiltUnitTraversesConstructionBeforeMaterials()
    {
        StashUnitDefinition unit = new StashUnitCatalog()
                .forTier(ClueTier.MASTER).get(0);
        StrategyContext context = context(MembershipStatus.P2P, 70, false);
        StashBuildPlan plan = new StashDependencyPlanner().plan(
                unit, StashUnitState.NOT_BUILT, context);
        assertEquals(GoalNodeKind.TRAINING_METHOD, plan.nextAction().getKind());
        assertTrue(plan.nextAction().getAction().contains("oak larders"));
        assertFalse(plan.nextAction().getAction().contains("Choose"));
        assertTrue(plan.getSteps().stream().anyMatch(step ->
                step.getKind() == GoalNodeKind.SKILL_LEVEL
                        && step.getAction().contains("88")));
    }

    @Test
    public void unknownMembershipFailsClosedBeforeBuildAdvice()
    {
        StashUnitDefinition unit = new StashUnitCatalog().all().get(0);
        StashBuildPlan plan = new StashDependencyPlanner().plan(unit,
                StashUnitState.NOT_BUILT,
                context(MembershipStatus.UNKNOWN, 99, false));
        assertEquals(GoalNodeKind.ACCESS, plan.nextAction().getKind());
        assertTrue(plan.nextAction().getAction().contains("membership"));
    }

    @Test
    public void wildernessUnitCannotRouteWithoutExplicitRiskPermission()
    {
        StashUnitDefinition wilderness = new StashUnitCatalog().all().stream()
                .filter(StashUnitDefinition::isWilderness)
                .findFirst().orElseThrow(AssertionError::new);
        // Unobserved inventory/bank produces a material check before the risk edge;
        // this still proves no unsafe Wilderness travel can become the action.
        StashBuildPlan plan = new StashDependencyPlanner().plan(wilderness,
                StashUnitState.NOT_BUILT,
                context(MembershipStatus.P2P, 99, false));
        assertFalse(plan.nextAction().getAction().startsWith("Verify the quest/access route"));
    }

    private static void assertTier(StashTierDefinition tier, int level,
            String plank, boolean leaf)
    {
        assertEquals(level, tier.getConstructionLevel());
        assertEquals(plank, tier.getPlank());
        assertEquals(leaf, tier.requiresGoldLeaf());
        String label = tier.materials().label();
        assertTrue(label.contains("2 × " + plank));
        assertTrue(label.contains("10 × nails"));
        assertTrue(label.contains("dragon nails"));
        assertEquals(leaf, label.contains("gold leaf"));
    }

    private static StrategyContext context(MembershipStatus membership,
            int construction, boolean wilderness)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.CONSTRUCTION ? construction : 1);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("player", 1L, 0,
                "NORMAL", membership, 0, construction, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, wilderness, new PreferenceProfile());
    }
}

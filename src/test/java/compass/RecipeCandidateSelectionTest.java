package compass;

import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeCandidateSelectionTest
{
    private final MethodIntelligenceCatalog catalog = new MethodIntelligenceCatalog();
    private final MethodReadinessService readiness = new MethodReadinessService(catalog,
            new MethodPreparationService(MethodIntelligenceTest.resources(0, 0)));
    private final TrainingMethodCatalog methods = new TrainingMethodCatalog();
    private final TrainingMethod bows = methods.curatedFor(Skill.FLETCHING).stream()
            .map(CuratedTrainingMethod::method).filter(m -> m.id.equals("fletching_bows"))
            .findFirst().orElseThrow(AssertionError::new);

    @Test
    public void selectorRetainsEachRecipeAndItsOwnExecutionEvidence()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.addAll(MethodIntelligenceTest.carried(1515, "Yew logs", 6));
        inventory.add(new ItemState(946, "Knife", 1));
        GameData data = MethodIntelligenceTest.data(1, -1, inventory, Collections.emptyList());
        TrainingMethodSelector selector = new TrainingMethodSelector(methods, null,
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService(), readiness);
        List<TrainingPlan> plans = selector.rankedCandidates(data, Skill.FLETCHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .filter(p -> p.method().id.equals(bows.id) && p.readiness.actionable())
                .collect(Collectors.toList());
        assertEquals(4, plans.size());
        assertEquals("yew_longbow_cutting", plans.get(0).readiness.recipe.id);
        Set<String> identities = new HashSet<>();
        for (TrainingPlan plan : plans)
        {
            MethodReadiness ready = plan.readiness;
            assertTrue(identities.add(ready.recipe.id));
            int input = ready.recipe.inputs.get(0).itemId;
            assertEquals(input == 1511 ? 4 : 6, ready.batch);
            assertEquals(ready.batch, ready.capacity.actions);
            assertTrue(ready.preparation.steps.stream().anyMatch(s -> s.input.itemId == input
                    && s.input.quantity == ready.batch && s.kind == MethodPreparation.Kind.CARRIED));
            assertSame(ready, plan.withCurrentStageTargetLevel(85).readiness);
            assertTrue(ready.guidance(85).getAction().contains(ready.recipe.action));
        }
        // The reviewed local-processing contract supersedes the legacy bank-loop assumption.
        GameData uim = MethodIntelligenceTest.data(2, -1, inventory, Collections.emptyList());
        List<TrainingPlan> uimBows = selector.rankedCandidates(uim, Skill.FLETCHING, 80,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false).stream()
                .filter(p -> p.method().id.equals(bows.id) && p.readiness.actionable())
                .collect(Collectors.toList());
        assertEquals(4, uimBows.size());
        assertEquals(BankingMode.LOCAL_PROCESSING,
                uimBows.get(0).getStrategyProfile().bankingBehavior);
    }

    @Test
    public void allEligibleRecipesAreEvaluatedWithoutManufacturingAccess()
    {
        GameData data = MethodIntelligenceTest.data(2, -1, Collections.emptyList(), Collections.emptyList());
        List<MethodReadiness> candidates = readiness.evaluateCandidates(data, bows, false);
        assertEquals(11, candidates.size());
        assertEquals(11, candidates.stream().map(r -> r.recipe.id).distinct().count());
        assertTrue(candidates.stream().allMatch(r -> r.recipe.level <= 80));
        assertFalse(candidates.stream().anyMatch(r -> r.recipe.id.equals("magic_longbow_cutting")));
        assertTrue(readiness.evaluateCandidates(null, bows, false).stream()
                .noneMatch(MethodReadiness::actionable));
        assertNull(readiness.evaluateCandidates(null, bows, false).get(0).recipe);
        assertNull(catalog.method("crafting_gems"));
    }

    @Test
    public void accessAndLevelFailuresRetainOneNonActionableDiagnostic()
    {
        for (Membership membership : new Membership[] {Membership.F2P, Membership.UNKNOWN, Membership.P2P})
        {
            Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
            levels.put(Skill.FLETCHING, membership == Membership.P2P ? 4 : 80);
            AccountSnapshot account = new AccountSnapshot("test", 1, 0, "MAIN", membership,
                    1, 1, 0, levels, Collections.emptyMap());
            List<MethodReadiness> candidates = readiness.evaluateCandidates(
                    GameData.builder(account).build(), bows, false);
            assertEquals(1, candidates.size());
            assertNull(candidates.get(0).recipe);
            assertFalse(candidates.get(0).actionable());
        }
    }

    @Test
    public void feasibleRecipesPrecedeDiagnosticsAndLegacyFacadeKeepsWinner()
    {
        List<ItemState> inventory = MethodIntelligenceTest.carried(1511, "Logs", 4);
        inventory.add(new ItemState(946, "Knife", 1));
        GameData data = MethodIntelligenceTest.data(2, -1, inventory, Collections.emptyList());
        List<MethodReadiness> candidates = readiness.evaluateCandidates(data, bows, false);
        assertTrue(candidates.get(0).actionable());
        assertEquals("longbow_cutting", candidates.get(0).recipe.id);
        assertEquals(candidates.get(0).recipe.id, readiness.evaluate(data, bows, false).recipe.id);
        boolean diagnosticSeen = false;
        for (MethodReadiness candidate : candidates)
        {
            if (!candidate.actionable()) diagnosticSeen = true;
            if (diagnosticSeen) assertFalse(candidate.actionable());
        }
    }
}

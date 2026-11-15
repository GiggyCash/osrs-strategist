package compass;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodPreparationPresentationTest
{
    @Test
    public void sidebarAdvancesFromSuppliesToMissingToolToProcessing()
    {
        Recommendation empty = recommendation(Collections.emptyList());
        String first = Presentation.compactText(empty);
        assertTrue(first.contains("DO\nCollect 4 Logs."));
        assertTrue(first.contains("1 Knife (prepare)"));
        assertTrue(first.contains("top floor"));
        List<ItemState> items = MethodIntelligenceTest.carried(1511, "Logs", 4);
        String tool = Presentation.compactText(recommendation(items));
        assertTrue(tool.contains("DO\nCollect 1 Knife."));
        assertTrue(tool.contains("4 Logs (ready)"));
        assertTrue(tool.contains("kitchen"));
        items.add(new ItemState(946, "Knife", 1));
        Recommendation ready = recommendation(items);
        assertNull(ready.plan().readiness.preparation.nextAction());
        assertFalse(Presentation.compactText(ready).contains("Collect"));
        assertTrue(Presentation.compactText(ready).contains("Knife (ready)"));
    }

    @Test
    public void detailsAndLiveGuidanceRetainAllPickupStepsAndBatchLimits()
    {
        Recommendation recommendation = recommendation(Collections.emptyList());
        String details = Presentation.detailedText(recommendation);
        assertTrue(details.contains("4 Logs"));
        assertTrue(details.contains("1 Knife"));
        assertTrue(details.contains("kitchen"));
        assertTrue(details.contains("If the spawn is absent"));
        assertTrue(details.contains("AFTER SETUP"));
        assertTrue(details.contains("Process at most 4 actions"));
        assertTrue(details.contains("Stop at level 15"));
        GuidanceChecklist live = new MethodGuidanceService(
                TestFixtures.farmingRunPlanner(new FarmingRunCatalog())).build(recommendation, null);
        assertTrue(live.action.contains("4 Logs"));
        assertTrue(live.action.contains("1 Knife"));
        assertTrue(live.action.contains("Stop at level 15"));
    }

    @Test
    public void pendingPreparationRemainsPaintableAtSupportedTextSizes()
    {
        for (SidebarTextSize size : SidebarTextSize.values())
        {
            OsrsStrategistPanel panel = new OsrsStrategistPanel((id, feedback) -> {}, null,
                    recommendation -> {}, () -> {}, () -> {}, "", value -> {});
            SidebarAccessibility.apply(panel, size);
            panel.updateRecommendations(Collections.singletonList(recommendation(Collections.emptyList())));
            assertTrue(panel.recommendationBody.getText().contains("1 Knife (prepare)"));
            assertTrue(panel.recommendationBody.getPreferredSize().height
                    >= panel.recommendationBody.getLineCount()
                    * panel.recommendationBody.getFontMetrics(panel.recommendationBody.getFont()).getHeight());
        }
    }

    private static Recommendation recommendation(List<ItemState> inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, 1);
        AccountSnapshot account = new AccountSnapshot("fixture", 0, 2, "ULTIMATE_IRONMAN",
                Membership.P2P, 0, 0, 0, levels, Collections.emptyMap());
        GameData data = GameData.builder(account).inventory(new ItemsState(inventory, true))
                .equipment(new ItemsState(Collections.emptyList(), true)).build();
        TrainingMethod method = new TrainingMethodCatalog().curatedFor(Skill.FLETCHING).stream()
                .map(CuratedTrainingMethod::method).filter(value -> value.id.equals("fletching_arrow_shafts"))
                .findFirst().orElseThrow(AssertionError::new);
        MethodReadiness readiness = new MethodReadinessService(new MethodIntelligenceCatalog(),
                new MethodPreparationService(new AccountResourcePlanner(null, new ResourceSourceCatalog())))
                .evaluate(data, method, false);
        assertTrue(readiness.actionable());
        TrainingPlan plan = new TrainingPlan(method, "Prepare arrow shafts", Confidence.VERIFIED,
                Collections.emptyList()).withReadiness(readiness);
        return new Recommendation("skill:fletching", "Train Fletching to 15", "Prepare arrow shafts",
                100, plan, Confidence.VERIFIED, 1, 15, readiness.guidance(15), Safety.skill(true, Skill.FLETCHING));
    }
}

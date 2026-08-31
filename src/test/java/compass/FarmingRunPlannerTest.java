package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FarmingRunPlannerTest
{
    private final FarmingRunPlanner planner = new FarmingRunPlanner(new FarmingRunCatalog());

    @Test
    public void plantedPatchIsGreenCompleteAndReadyPatchBecomesAction()
    {
        Map<String, ObservedFarmingPatchState> states = new HashMap<>();
        states.put("herb_falador", new ObservedFarmingPatchState(
                FarmingPatchCycleState.GROWING, 1L));
        states.put("herb_catherby", new ObservedFarmingPatchState(
                FarmingPatchCycleState.READY, 1L));

        GameData data = data(20, states);
        GuidanceChecklist checklist = planner.build(data, "skill:farming");

        GuidanceStep falador = find(checklist, "herb_falador");
        GuidanceStep catherby = find(checklist, "herb_catherby");
        assertNotNull(falador);
        assertNotNull(catherby);
        assertEquals(GuidanceStepState.COMPLETE, falador.getState());
        assertEquals(GuidanceStepState.ACTION, catherby.getState());
        assertTrue(falador.getDetail().contains("Planted"));
    }

    private static GuidanceStep find(GuidanceChecklist checklist, String id)
    {
        for (GuidanceStep step : checklist.getSteps())
        {
            if (id.equals(step.getId())) return step;
        }
        return null;
    }

    private static GameData data(
            int farming,
            Map<String, ObservedFarmingPatchState> states)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.FARMING, farming);
        AccountSnapshot account = new AccountSnapshot(
                "Tester", 0, "Main", MembershipStatus.P2P, 1,
                farming, 0L, levels, xp);
        return GameData.builder(account)
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .accessMemory(AccessMemorySnapshot.empty())
                .farmingRuns(new FarmingRunSnapshot(states))
                .build();
    }
}

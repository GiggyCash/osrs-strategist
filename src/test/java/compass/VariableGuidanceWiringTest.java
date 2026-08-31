package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Arrays;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VariableGuidanceWiringTest
{
    @Test
    public void expandedFoundryIdGetsVariableGuidance()
    {
        VariableMethodGuidanceService service = new VariableMethodGuidanceService();
        Guidance guidance = service.build(
                data(Skill.SMITHING, 70,
                        item(ItemID.RUNITE_BAR, "Runite bar", 14),
                        item(ItemID.ADAMANTITE_BAR, "Adamantite bar", 14)),
                Skill.SMITHING,
                70,
                80,
                plan("smithing_giants_foundry", Skill.SMITHING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("28 bars"));
        assertFalse(guidance.getAction().matches(".*[0-9]+ swords.*"));
    }

    @Test
    public void expandedMahoganyHomesIdGetsVariableGuidance()
    {
        VariableMethodGuidanceService service = new VariableMethodGuidanceService();
        Guidance guidance = service.build(
                data(Skill.CONSTRUCTION, 70,
                        item(ItemID.PLANK_MAHOGANY, "Mahogany plank", 20)),
                Skill.CONSTRUCTION,
                70,
                80,
                plan("construction_mahogany_homes", Skill.CONSTRUCTION),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Expert contract"));
        assertTrue(guidance.getLocation().contains("Mahogany Homes"));
        assertTrue(guidance.getNote().contains("live contract"));
    }

    @Test
    public void recommendationGuidanceServiceActuallyRoutesVariableMethods()
    {
        RecommendationGuidanceService service = new RecommendationGuidanceService(
                null,
                new VariableMethodGuidanceService(),
                null);
        Guidance guidance = service.build(
                data(Skill.FIREMAKING, 60),
                Skill.FIREMAKING,
                60,
                70,
                plan("firemaking_wintertodt", Skill.FIREMAKING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("500 personal points"));
    }

    private static GameData data(Skill skill, int level)
    {
        return data(skill, level, new ItemState[0]);
    }

    private static GameData data(Skill skill, int level,
            ItemState... items)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values())
        {
            levels.put(s, 60);
            xp.put(s, Experience.getXpForLevel(60));
        }
        levels.put(skill, level);
        xp.put(skill, Experience.getXpForLevel(level));
        AccountSnapshot account = new AccountSnapshot(
                "Variable Wiring",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
        return GameData.builder(account)
                .bank(new ItemsState(Arrays.asList(items), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
    }

    private static ItemState item(int id, String name, int quantity)
    {
        return new ItemState(id, name, quantity);
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                Confidence.VERIFIED);
        return new TrainingPlan(
                method,
                "test",
                Confidence.VERIFIED,
                Collections.emptyList());
    }
}

package compass;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WildernessMethodTest
{
    private final TrainingMethodSelector selector =
            new TrainingMethodSelector(new TrainingMethodDatabase(), null, new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(), new MethodStrategyService());

    @Test
    public void wildernessMethodsAreHardFilteredUnlessEnabled()
    {
        GameData data = p2pData();
        TrainingPlan disabled = selector.select(
                data, Skill.AGILITY, 52, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, false);
        assertEquals("agility_rooftop", disabled.getMethod().getId());

        TrainingPlan enabled = selector.select(
                data, Skill.AGILITY, 52, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, true);
        assertEquals("agility_wilderness", enabled.getMethod().getId());
    }

    private static GameData p2pData()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        return GameData.builder(new AccountSnapshot("Wild Test", 0L, 0, "Main", Membership.P2P, 1, 1500, 0L, levels, xp)).build();
    }
}

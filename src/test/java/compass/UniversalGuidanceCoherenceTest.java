package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class UniversalGuidanceCoherenceTest
{
    @Test
    public void skillNameAloneCannotTurnSlayerMagicIntoHighAlchemy()
    {
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog()
        {
            @Override
            public List<ActionDef> actionsFor(Skill skill)
            {
                return Collections.singletonList(
                        new ActionDef(
                                Skill.MAGIC, "runelite:magic:high_alchemy",
                                "High Level Alchemy", 55, 65, "Magic",
                                MembershipStatus.P2P, -1));
            }
        };
        UniversalSkillActionGuidanceService service =
                new UniversalSkillActionGuidanceService(actions,
                        new UniversalActionRecipeResolver(),
                        new SkillingXpModifierService(),
                        new AccountResourcePlanner());

        assertNull(service.build(data(), Skill.MAGIC, 80, 81,
                plan("magic_slayer", "Magic through Slayer",
                        "Use Magic on the live Slayer assignment."), true));
    }

    private static GameData data()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 80);
            xp.put(skill, Experience.getXpForLevel(80));
        }
        AccountSnapshot account = new AccountSnapshot(
                "Coherence", 0, "Main", MembershipStatus.P2P,
                1, 1920, 0L, levels, xp);
        return GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .build();
    }

    private static TrainingPlan plan(String id, String name,
            String instructions)
    {
        TrainingMethod method = new TrainingMethod(
                id, Skill.MAGIC, 1, 99, name, instructions,
                10, 10, 10, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(),
                Confidence.VERIFIED);
        return new TrainingPlan(method, "test",
                Confidence.VERIFIED,
                Collections.emptyList());
    }
}

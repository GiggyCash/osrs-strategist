package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Regression coverage for banked F2P anvil guidance. */
public class F2pSmithingGuidanceTest
{
    @Test
    public void ironAnvilRouteUsesBarsAndNamesVarrockWestBank()
    {
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog()
        {
            @Override
            public List<ActionDef> actionsFor(Skill skill)
            {
                if (skill != Skill.SMITHING) return Collections.emptyList();
                return Arrays.asList(
                        new ActionDef(
                                Skill.SMITHING,
                                "runelite:smithing:iron_bar",
                                "Iron bar",
                                15,
                                12.5f,
                                null,
                                MembershipStatus.F2P),
                        new ActionDef(
                                Skill.SMITHING,
                                "runelite:smithing:iron_2h_sword",
                                "Iron 2h sword",
                                29,
                                75.0f,
                                null,
                                MembershipStatus.F2P));
            }
        };

        UniversalSkillActionGuidanceService service =
                new UniversalSkillActionGuidanceService(
                        actions,
                        new UniversalActionRecipeResolver(),
                        new SkillingXpModifierService(),
                        new AccountResourcePlanner());

        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }
        levels.put(Skill.SMITHING, 29);

        AccountSnapshot account = new AccountSnapshot(
                "Smithing test",
                1L,
                0,
                "MAIN",
                MembershipStatus.F2P,
                0,
                500,
                0L,
                levels,
                xp);
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .build();

        TrainingMethod method = new F2pBaselineMethodCatalog()
                .methodsFor(Skill.SMITHING)
                .stream()
                .map(CuratedTrainingMethod::getMethod)
                .filter(candidate -> candidate.supportsLevel(29))
                .findFirst()
                .orElseThrow(AssertionError::new);

        Guidance guidance = service.build(
                data,
                Skill.SMITHING,
                29,
                30,
                new TrainingPlan(method, "Regression test"),
                false);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Iron 2h sword"));
        assertFalse(guidance.getAction().contains("Iron bar smelt"));
        assertTrue(guidance.getSupplies().contains("Iron bar"));
        assertFalse(guidance.getSupplies().contains("Iron ore"));
        assertTrue(guidance.getLocation().contains("Varrock West Bank"));
        assertTrue(guidance.getLocation().contains("anvils just south of the bank"));
    }
}

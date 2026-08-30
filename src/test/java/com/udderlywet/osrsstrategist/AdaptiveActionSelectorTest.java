package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdaptiveActionSelectorTest
{
    private final AdaptiveActionSelector selector = new AdaptiveActionSelector();
    private final MethodExecutionProfile profile =
            new MethodExecutionProfileCatalog().forMethod("fletching_bows");

    @Test
    public void ironPrefersSuppliedWillowsOverUnsuppliedYews()
    {
        AccountSnapshot account = account(1);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(
                        Collections.singletonList(
                                new ItemStackSnapshot(1519, "Willow logs", 10000)),
                        1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();

        RuneLiteSkillActionDefinition selected = selector.select(
                data,
                profile,
                actions(),
                70,
                MembershipStatus.P2P,
                Experience.getXpForLevel(70),
                Experience.getXpForLevel(80),
                1.0,
                true);

        assertEquals("Willow longbow (u)", selected.getName());
    }

    @Test
    public void mainStillPrefersHigherXpYewRouteWhenItCanBuyInputs()
    {
        AccountSnapshot account = account(0);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(
                        Collections.singletonList(
                                new ItemStackSnapshot(1519, "Willow logs", 10000)),
                        1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();

        RuneLiteSkillActionDefinition selected = selector.select(
                data,
                profile,
                actions(),
                70,
                MembershipStatus.P2P,
                Experience.getXpForLevel(70),
                Experience.getXpForLevel(80),
                1.0,
                true);

        assertEquals("Yew longbow (u)", selected.getName());
    }

    @Test
    public void enabledUnobservedGroupStorageCannotBiasAnIronAction()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(4))
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(1519, "Willow logs", 10000)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .groupStorage(GroupStorageSnapshot.unknown())
                .build();

        RuneLiteSkillActionDefinition selected = selector.select(
                data, profile, actions(), 70, MembershipStatus.P2P,
                Experience.getXpForLevel(70),
                Experience.getXpForLevel(80), 1.0, true);

        assertEquals("Yew longbow (u)", selected.getName());
    }

    private static List<RuneLiteSkillActionDefinition> actions()
    {
        return Arrays.asList(
                new RuneLiteSkillActionDefinition(
                        Skill.FLETCHING,
                        "runelite:fletching:willow_longbow_u",
                        "Willow longbow (u)",
                        40,
                        41.5f,
                        null,
                        MembershipStatus.P2P,
                        -1),
                new RuneLiteSkillActionDefinition(
                        Skill.FLETCHING,
                        "runelite:fletching:yew_longbow_u",
                        "Yew longbow (u)",
                        65,
                        75.0f,
                        null,
                        MembershipStatus.P2P,
                        -1));
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        levels.put(Skill.FLETCHING, 70);
        xp.put(Skill.FLETCHING, Experience.getXpForLevel(70));
        return new AccountSnapshot(
                "Selector Test",
                typeCode,
                typeCode == 0 ? "Main" : "Ironman",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
    }
}

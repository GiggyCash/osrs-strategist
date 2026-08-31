package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class SustainableResourceValueServiceTest
{
    @Test
    public void ownedConsumablesStillHaveDifferentReplacementCostByMode()
    {
        ResourcePipelineRequest cannonballs = request("Cannonballs", 100,
                ResourceUseKind.RECURRING_CONSUMABLE,
                ResourceScarcity.ORDINARY, true);
        SustainableResourceValueService service =
                new SustainableResourceValueService();

        ResourcePipelineAssessment main = service.assess(
                context(0, bank("Cannonballs", 500)), cannonballs);
        ResourcePipelineAssessment iron = service.assess(
                context(1, bank("Cannonballs", 500)), cannonballs);

        assertEquals(ResourcePipelineState.READY_CURRENT_SUPPLY,
                main.getState());
        assertEquals(ResourcePipelineState.READY_CURRENT_SUPPLY,
                iron.getState());
        assertTrue(main.getScoreAdjustment() > iron.getScoreAdjustment());
        assertTrue(main.getEvidence().contains("GE substitute"));
        assertTrue(iron.getEvidence().contains("self-source"));
    }

    @Test
    public void unopenedBankIsUnknownInsteadOfEmpty()
    {
        ResourcePipelineAssessment value =
                new SustainableResourceValueService().assess(context(1, null),
                        request("Prayer potion", 4,
                                ResourceUseKind.ONE_OFF_CONSUMABLE,
                                ResourceScarcity.SCARCE, false));

        assertEquals(ResourcePipelineState.UNKNOWN_STORAGE, value.getState());
        assertTrue(value.getEvidence().contains("not been observed"));
    }

    @Test
    public void uimNeverCountsConventionalBankAsReadySupply()
    {
        ResourcePipelineAssessment value =
                new SustainableResourceValueService().assess(
                        context(2, bank("Prayer potion", 100)),
                        request("Prayer potion", 4,
                                ResourceUseKind.RECURRING_CONSUMABLE,
                                ResourceScarcity.SCARCE, false));

        assertEquals(0, value.getObservedQuantity());
        assertEquals(ResourcePipelineState.ACQUISITION_NEEDED,
                value.getState());
        assertTrue(value.getEvidence().contains("inventory/retrieval"));
    }

    @Test
    public void reservedResourceHasHigherOpportunityCostWithoutChangingIdentity()
    {
        SustainableResourceValueService service =
                new SustainableResourceValueService();
        StrategyContext context = context(1, bank("Law rune", 100));
        ResourcePipelineAssessment ordinary = service.assess(context,
                request("Law rune", 10, ResourceUseKind.ONE_OFF_CONSUMABLE,
                        ResourceScarcity.ORDINARY, false));
        ResourcePipelineAssessment reserved = service.assess(context,
                request("Law rune", 10, ResourceUseKind.ONE_OFF_CONSUMABLE,
                        ResourceScarcity.RESERVED_FOR_GOAL, false));

        assertTrue(ordinary.getScoreAdjustment()
                > reserved.getScoreAdjustment());
    }

    @Test
    public void portfolioPreservesUnlikeResourceQuantitiesSeparately()
    {
        SustainableResourceValueService service =
                new SustainableResourceValueService();
        ResourcePortfolioAssessment value = service.assessAll(
                context(0, new ItemsState(Arrays.asList(
                        new ItemState(1, "Law rune", 50),
                        new ItemState(2, "Prayer potion", 3)), 1L)),
                Arrays.asList(
                        new ResourcePipelineRequest(
                                new ResourceNeed(1, "Law rune", 20),
                                ResourceUseKind.ONE_OFF_CONSUMABLE,
                                ResourceScarcity.ORDINARY, true),
                        new ResourcePipelineRequest(
                                new ResourceNeed(2, "Prayer potion", 4),
                                ResourceUseKind.ONE_OFF_CONSUMABLE,
                                ResourceScarcity.SCARCE, true)));

        assertEquals(2, value.getResources().size());
        assertEquals(20, value.getResources().get(0).getRequiredQuantity());
        assertEquals(4, value.getResources().get(1).getRequiredQuantity());
    }

    private static ResourcePipelineRequest request(String name, int quantity,
            ResourceUseKind use, ResourceScarcity scarcity, boolean tradeable)
    {
        return new ResourcePipelineRequest(new ResourceNeed(1, name, quantity),
                use, scarcity, tradeable);
    }

    private static ItemsState bank(String name, int quantity)
    {
        return new ItemsState(Arrays.asList(
                new ItemState(1, name, quantity)), 1L);
    }

    private static StrategyContext context(int type, ItemsState bank)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Resource", 88L, type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        GameData.Builder data = GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()));
        if (bank != null) data.bank(bank);
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }
}

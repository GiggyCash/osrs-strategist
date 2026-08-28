package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class ContextualGearValueServiceTest
{
    @Test
    public void acquisitionPropertiesDifferentiateMainAndIronWithoutItemBonus()
    {
        GearProgressionEntry entry = budgetMelee();
        ContextualGearValueService service = new ContextualGearValueService();
        GearUpgradeValueRequest mainRequest = request(entry,
                GearStorageDisposition.UNKNOWN, GearAcquisitionBurden.LOW);
        GearUpgradeValueRequest ironRequest = request(entry,
                GearStorageDisposition.UNKNOWN, GearAcquisitionBurden.HIGH);

        ContextualGearValueAssessment main = service.assess(
                context(0, true), mainRequest);
        ContextualGearValueAssessment iron = service.assess(
                context(1, true), ironRequest);

        assertEquals(GearUpgradeValueState.WORTH_CONSIDERING, main.getState());
        assertTrue(main.getScoreAdjustment() > iron.getScoreAdjustment());
        assertTrue(main.getAcquisitionRoute().isTradeable());
    }

    @Test
    public void uimStorabilityCanReverseAnOtherwiseIdenticalUpgrade()
    {
        GearProgressionEntry entry = budgetMelee();
        ContextualGearValueService service = new ContextualGearValueService();
        ContextualGearValueAssessment storable = service.assess(context(2, true),
                request(entry, GearStorageDisposition.VERIFIED_STORABLE,
                        GearAcquisitionBurden.MODERATE));
        ContextualGearValueAssessment persistent = service.assess(context(2, true),
                request(entry, GearStorageDisposition.OCCUPIES_PERSISTENT_SLOT,
                        GearAcquisitionBurden.MODERATE));

        assertEquals(GearUpgradeValueState.WORTH_CONSIDERING,
                storable.getState());
        assertEquals(GearUpgradeValueState.DEFER, persistent.getState());
        assertTrue(storable.getScoreAdjustment()
                > persistent.getScoreAdjustment());
    }

    @Test
    public void unknownUimStorageCannotBecomeAuthoritativeAdvice()
    {
        ContextualGearValueAssessment value =
                new ContextualGearValueService().assess(context(2, true),
                        request(budgetMelee(), GearStorageDisposition.UNKNOWN,
                                GearAcquisitionBurden.MODERATE));

        assertEquals(GearUpgradeValueState.NEEDS_EVIDENCE, value.getState());
    }

    @Test
    public void unopenedBankCannotProveExactUpgradeMissing()
    {
        ContextualGearValueAssessment value =
                new ContextualGearValueService().assess(context(0, false),
                        request(budgetMelee(), GearStorageDisposition.UNKNOWN,
                                GearAcquisitionBurden.LOW));

        assertEquals(GearUpgradeValueState.NEEDS_EVIDENCE, value.getState());
        assertTrue(value.getEvidence().contains("Open the bank"));
    }

    private static GearUpgradeValueRequest request(GearProgressionEntry entry,
            GearStorageDisposition storage, GearAcquisitionBurden burden)
    {
        return new GearUpgradeValueRequest(entry, "Dragon boots",
                GearMarginalBenefit.MEANINGFUL, GearReplacementHorizon.LONG,
                storage, burden, false);
    }

    private static GearProgressionEntry budgetMelee()
    {
        return new GearProgressionCatalog().forStyle(CombatStyle.MELEE_SLASH)
                .stream().filter(entry -> entry.getTier() == GearBudgetTier.BUDGET)
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static StrategyContext context(int type, boolean bankObserved)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Gear value", 99L, type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0L, levels, xp);
        StrategyDataBundle.Builder data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()));
        if (bankObserved)
            data.bank(new BankSnapshot(Collections.emptyList(), 1L));
        return new StrategyContext(data.build(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.GEAR_TARGET, false, false, new PreferenceProfile());
    }
}

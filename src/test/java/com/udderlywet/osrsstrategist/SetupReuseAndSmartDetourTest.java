package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SetupReuseAndSmartDetourTest
{
    @Test
    public void observedMatchingSetupProducesBoundedReuseValue()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(101, "Observed weapon", 1))))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(202, "Observed supply", 30))))
                .build();
        ActivitySetupProfile profile = ActivitySetupProfile.builder()
                .requiresEquipped(101).requiresInventory(202)
                .region("catacombs").spellbook("ancient")
                .setupMinutes(12).build();

        SetupReuseAssessment result = new SetupReuseService().assess(profile,
                data, new CurrentSetupEvidence("Catacombs", "Ancient"));

        assertEquals(RecommendationConfidence.VERIFIED,
                result.getConfidence());
        assertEquals(4, result.getMatchedProperties());
        assertEquals(4, result.getRequiredProperties());
        assertEquals(12, result.getMinutesAvoided());
        assertEquals(1.0, result.getNormalizedValue(), 0.0);
    }

    @Test
    public void unavailableRegionAndSpellbookDoNotMasqueradeAsReuse()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(101, "Observed weapon", 1))))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(202, "Observed supply", 30))))
                .build();
        ActivitySetupProfile profile = ActivitySetupProfile.builder()
                .requiresEquipped(101).requiresInventory(202)
                .region("catacombs").spellbook("ancient")
                .setupMinutes(12).build();

        SetupReuseAssessment result = new SetupReuseService().assess(profile,
                data, CurrentSetupEvidence.unknown());

        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                result.getConfidence());
        assertEquals(2, result.getMatchedProperties());
        assertEquals(6, result.getMinutesAvoided());
        assertEquals(0.5, result.getNormalizedValue(), 0.0);
    }

    @Test
    public void shortHighValueNearbyDetourClearsThreshold()
    {
        SmartDetourProfile detour = SmartDetourProfile.builder()
                .readiness(RecommendationConfidence.VERIFIED)
                .detourMinutes(8).sessionMinutes(60)
                .travelMinutesSaved(6).setupMinutesSaved(5)
                .goalValue(0.7).accountValue(0.5)
                .resourceValue(0.3).interruptionCost(0.1).build();

        WorthDoingNowAssessment result = new SmartDetourService()
                .assess(detour);

        assertEquals(WorthDoingNowState.DO_NOW, result.getState());
        assertTrue(result.getNetUtility() > 0.0);
    }

    @Test
    public void longUnrelatedInterruptionWaitsAndUnknownPrepCannotLead()
    {
        SmartDetourProfile interruption = SmartDetourProfile.builder()
                .readiness(RecommendationConfidence.VERIFIED)
                .detourMinutes(30).sessionMinutes(40)
                .travelMinutesSaved(1).setupMinutesSaved(0)
                .goalValue(0.0).accountValue(0.1)
                .interruptionCost(0.9).build();
        SmartDetourProfile unknown = SmartDetourProfile.builder()
                .readiness(RecommendationConfidence.CHECK_NEEDED)
                .ordinaryPreparationKnown(false)
                .detourMinutes(5).sessionMinutes(40)
                .goalValue(0.8).build();

        assertTrue(new SmartDetourService().assess(interruption).getState()
                != WorthDoingNowState.DO_NOW);
        assertEquals(WorthDoingNowState.WAIT,
                new SmartDetourService().assess(unknown).getState());
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResourceReadinessServiceTest
{
    private final ResourceReadinessService service = new ResourceReadinessService();

    @Test
    public void combinesInventoryAndKnownBankAcrossAlternativeIds()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .inventory(new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(100, "A", 1))))
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(101, "B", 2)), 1L))
                .build();
        ResourceRequirement requirement = new ResourceRequirement(
                "test", "Alternatives", 3, 100, 101);
        RequirementCheck check = service.evaluate(data, requirement);
        assertEquals(RequirementState.VERIFIED, check.getState());
        assertTrue(check.getEvidence().contains("3"));
    }

    @Test
    public void missingBankStaysUnknownInsteadOfPretendingResourceIsAbsent()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();
        RequirementCheck check = service.evaluate(
                data, new ResourceRequirement("test", "Thing", 1, 100));
        assertEquals(RequirementState.CHECK_NEEDED, check.getState());
        assertTrue(check.getEvidence().contains("bank has not been observed"));
    }

    @Test
    public void verifiedAlternateStorageCountsWithoutInventoryItem()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(null)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();
        RequirementCheck check = service.evaluate(
                data,
                new ResourceRequirement("test", "Rake", 1, 100),
                CapabilityState.VERIFIED,
                "Stored in Tool Leprechaun");
        assertEquals(RequirementState.VERIFIED, check.getState());
    }
}

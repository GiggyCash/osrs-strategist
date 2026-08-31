package compass;

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
        GameData data = GameData.builder(null)
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(100, "A", 1))))
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(101, "B", 2)), 1L))
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
        GameData data = GameData.builder(null)
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        RequirementCheck check = service.evaluate(
                data, new ResourceRequirement("test", "Thing", 1, 100));
        assertEquals(RequirementState.CHECK_NEEDED, check.getState());
        assertTrue(check.getEvidence().contains("bank has not been observed"));
    }

    @Test
    public void verifiedAlternateStorageCountsWithoutInventoryItem()
    {
        GameData data = GameData.builder(null)
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        RequirementCheck check = service.evaluate(
                data,
                new ResourceRequirement("test", "Rake", 1, 100),
                CapabilityState.VERIFIED,
                "Stored in Tool Leprechaun");
        assertEquals(RequirementState.VERIFIED, check.getState());
    }
}

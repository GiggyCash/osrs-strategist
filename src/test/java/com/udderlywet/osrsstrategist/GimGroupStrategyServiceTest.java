package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GimGroupStrategyServiceTest
{
    private static final int SHARED_ITEM = 12345;
    private final GimGroupStrategyService service =
            new GimGroupStrategyService();

    @Test
    public void freshEnabledExactStockCanPreventDuplicateAcquisition()
    {
        GroupResourceAssessment result = service.assess(context(4, true,
                new GroupStorageSnapshot(true, Collections.singletonList(
                        new ItemStackSnapshot(SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));

        assertEquals(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                result.getState());
        assertEquals(RecommendationConfidence.VERIFIED,
                result.getConfidence());
        assertTrue(result.satisfiesNeed());
        assertEquals(1.0, result.getDuplicateGrindAvoidance(), 0.0);
        assertTrue(result.strategicValue("group:test").hasTypedEvidence());
    }

    @Test
    public void partialConsumableStockHasPartialRatherThanWinnerValue()
    {
        GroupResourceAssessment result = service.assess(context(6, true,
                new GroupStorageSnapshot(true, Collections.singletonList(
                        new ItemStackSnapshot(SHARED_ITEM, "Shared supply", 4)))),
                need(10, false));

        assertEquals(GroupResourceState.SHARED_STOCK_PARTIAL,
                result.getState());
        assertFalse(result.satisfiesNeed());
        assertTrue(result.getDuplicateGrindAvoidance() > 0.0);
        assertTrue(result.getDuplicateGrindAvoidance() < 0.5);
    }

    @Test
    public void staleDisabledAndNonGroupEvidenceNeverClaimsSharedReadiness()
    {
        GroupStorageSnapshot stale = new GroupStorageSnapshot(true,
                Collections.singletonList(new ItemStackSnapshot(
                        SHARED_ITEM, "Shared tool", 1)),
                System.currentTimeMillis()
                        - GroupStorageSnapshot.FRESH_FOR_MILLIS - 1L);

        GroupResourceAssessment staleResult = service.assess(
                context(4, true, stale), need(1, true));
        GroupResourceAssessment disabled = service.assess(
                context(4, false, new GroupStorageSnapshot(true,
                        Collections.singletonList(new ItemStackSnapshot(
                                SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));
        GroupResourceAssessment main = service.assess(
                context(0, true, new GroupStorageSnapshot(true,
                        Collections.singletonList(new ItemStackSnapshot(
                                SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));

        assertEquals(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                staleResult.getState());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                staleResult.getConfidence());
        assertEquals(GroupResourceState.GROUP_STORAGE_DISABLED,
                disabled.getState());
        assertEquals(GroupResourceState.NOT_A_GROUP_ACCOUNT, main.getState());
        assertFalse(staleResult.strategicValue("group:test")
                .hasTypedEvidence());
    }

    @Test
    public void freshItemsDoNotInventTeammateInfrastructure()
    {
        StrategyContext context = context(5, true,
                new GroupStorageSnapshot(true, Collections.singletonList(
                        new ItemStackSnapshot(SHARED_ITEM, "Shared tool", 1))));

        SharedInfrastructureAssessment result = service
                .assessTeammateInfrastructure(context);

        assertEquals(CapabilityState.UNKNOWN, result.getState());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                result.getConfidence());
    }

    private static GroupResourceNeed need(int quantity, boolean reusable)
    {
        return new GroupResourceNeed("Shared requirement",
                new HashSet<>(Collections.singletonList(SHARED_ITEM)),
                quantity, reusable);
    }

    private static StrategyContext context(int typeCode, boolean enabled,
            GroupStorageSnapshot groupStorage)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 50);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Group member", 47L,
                typeCode, AccountMode.fromTypeCode(typeCode).name(),
                MembershipStatus.P2P, 1, levels.size() * 50, 0L,
                levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .groupStorage(groupStorage).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, enabled, false, false,
                new PreferenceProfile());
    }
}

package compass;

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
                new ItemsState(true, Collections.singletonList(
                        new ItemState(SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));

        assertEquals(GroupResourceState.SHARED_STOCK_SATISFIES_NEED,
                result.getState());
        assertEquals(Confidence.VERIFIED,
                result.getConfidence());
        assertTrue(result.satisfiesNeed());
        assertEquals(1.0, result.getDuplicateGrindAvoidance(), 0.0);
        assertTrue(result.strategicValue("group:test").hasTypedEvidence());
    }

    @Test
    public void partialConsumableStockHasPartialRatherThanWinnerValue()
    {
        GroupResourceAssessment result = service.assess(context(6, true,
                new ItemsState(true, Collections.singletonList(
                        new ItemState(SHARED_ITEM, "Shared supply", 4)))),
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
        ItemsState stale = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        SHARED_ITEM, "Shared tool", 1)),
                System.currentTimeMillis()
                        - ItemsState.FRESH_FOR_MILLIS - 1L);

        GroupResourceAssessment staleResult = service.assess(
                context(4, true, stale), need(1, true));
        GroupResourceAssessment disabled = service.assess(
                context(4, false, new ItemsState(true,
                        Collections.singletonList(new ItemState(
                                SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));
        GroupResourceAssessment main = service.assess(
                context(0, true, new ItemsState(true,
                        Collections.singletonList(new ItemState(
                                SHARED_ITEM, "Shared tool", 1)))),
                need(1, true));

        assertEquals(GroupResourceState.GROUP_STORAGE_UNKNOWN,
                staleResult.getState());
        assertEquals(Confidence.CHECK_NEEDED,
                staleResult.getConfidence());
        assertEquals(GroupResourceState.GROUP_STORAGE_DISABLED,
                disabled.getState());
        assertEquals(GroupResourceState.NOT_A_GROUP_ACCOUNT, main.getState());
        assertFalse(staleResult.strategicValue("group:test")
                .hasTypedEvidence());
    }

private static GroupResourceNeed need(int quantity, boolean reusable)
    {
        return new GroupResourceNeed("Shared requirement",
                new HashSet<>(Collections.singletonList(SHARED_ITEM)),
                quantity, reusable);
    }

    private static StrategyContext context(int typeCode, boolean enabled,
            ItemsState groupStorage)
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
                Membership.P2P, 1, levels.size() * 50, 0L,
                levels, xp);
        GameData data = GameData.builder(account)
                .groupStorage(groupStorage).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, enabled, false, false,
                new PreferenceProfile());
    }
}

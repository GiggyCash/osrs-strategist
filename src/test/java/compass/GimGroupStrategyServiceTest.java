package compass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

/** Regression coverage for Group Storage value in the method pipeline. */
public class GimGroupStrategyServiceTest
{
    private static final int SHARED_ITEM = 12345;

    @Test
    public void freshEnabledExactStockCanPreventDuplicateAcquisition()
    {
        StrategicValue value = value(context(4, true, storage(
                "Shared tool", 1)), "Shared tool", 1, true);

        assertTrue(value.hasTypedEvidence());
        assertTrue(value.getResourceFit() == 1.0);
    }

    @Test
    public void partialConsumableStockHasPartialRatherThanWinnerValue()
    {
        StrategicValue value = value(context(6, true, storage(
                "Shared supply", 4)), "Shared supply", 10, false);

        assertTrue(value.getResourceFit() > 0.0);
        assertTrue(value.getResourceFit() < 0.5);
    }

    @Test
    public void staleDisabledAndNonGroupEvidenceNeverClaimsSharedReadiness()
    {
        ItemsState stale = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        SHARED_ITEM, "Shared tool", 1)),
                System.currentTimeMillis() - ItemsState.FRESH_FOR_MILLIS - 1L);

        assertFalse(value(context(4, true, stale), "Shared tool", 1, true)
                .hasTypedEvidence());
        assertFalse(value(context(4, false, storage("Shared tool", 1)),
                "Shared tool", 1, true).hasTypedEvidence());
        assertFalse(value(context(0, true, storage("Shared tool", 1)),
                "Shared tool", 1, true).hasTypedEvidence());
    }

    private static StrategicValue value(StrategyContext context, String name,
            int quantity, boolean reusable)
    {
        return AdaptiveMilestoneGuidanceService.sharedResourceValue(
                context.data(), context.usesGroupStorage(), name, quantity,
                reusable);
    }

    private static ItemsState storage(String name, int quantity)
    {
        return new ItemsState(true, Collections.singletonList(
                new ItemState(SHARED_ITEM, name, quantity)));
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
                Membership.P2P, 1, levels.size() * 50, 0L, levels, xp);
        GameData data = GameData.builder(account)
                .groupStorage(groupStorage).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, enabled, false, false,
                new PreferenceProfile());
    }
}

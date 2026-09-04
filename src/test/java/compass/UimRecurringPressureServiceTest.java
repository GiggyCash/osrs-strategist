package compass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UimRecurringPressureServiceTest
{
    @Test
    public void unchangedFullInventoryCannotManufactureRepeatedPressure()
    {
        UimRecurringPressureService service =
                new UimRecurringPressureService();
        StrategyContext first = context(10_000);

        UimRecurringPressureAssessment one = service.observe(first);
        UimRecurringPressureAssessment duplicate = service.observe(first);

        assertEquals(1, one.getDistinctObservedLayouts());
        assertFalse(one.isRepeated());
        assertEquals(1, duplicate.getDistinctObservedLayouts());
        assertFalse(duplicate.isRepeated());
        assertTrue(one.getBlockedFamilies().contains("pvm"));
        assertTrue(one.getBlockedFamilies().contains("minigames"));
    }

    @Test
    public void distinctLayoutsBlockingMultipleFamiliesBecomeRecurringEvidence()
    {
        UimRecurringPressureService service =
                new UimRecurringPressureService();
        service.observe(context(10_000));
        UimRecurringPressureAssessment repeated = service.observe(
                context(20_000));

        assertEquals(2, repeated.getDistinctObservedLayouts());
        assertTrue(repeated.isRepeated());
    }

    @Test
    public void requirementFreeSkillingAndQuestPressureCanRecur()
    {
        UimRecurringPressureService service =
                new UimRecurringPressureService();
        service.observe(skillingAndQuestContext(30_000));
        UimRecurringPressureAssessment repeated = service.observe(
                skillingAndQuestContext(40_000));

        assertTrue(repeated.getBlockedFamilies().contains("skilling"));
        assertTrue(repeated.getBlockedFamilies().contains("questing"));
        assertTrue(repeated.isRepeated());
    }

    private static StrategyContext context(int firstItemId)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 80);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Pressure", 404L, 2,
                "Ultimate Ironman", Membership.P2P, 1, 1, 0L,
                levels, xp);
        List<ItemState> inventory = new ArrayList<>();
        for (int slot = 0; slot < 24; slot++)
            inventory.add(new ItemState(firstItemId + slot,
                    "Setup " + slot, 1, slot));
        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put("pvm:tztok_jad", new PvmReadiness("pvm:tztok_jad",
                false, Confidence.CHECK_NEEDED,
                Collections.singletonList("Observed loadout incomplete")));
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(inventory, true))
                .pvm(new PvmSnapshot(readiness))
                .minigames(new MinigameSnapshot(new HashSet<>(
                        Collections.singletonList("tithe-farm")),
                        Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }

    private static StrategyContext skillingAndQuestContext(int firstItemId)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 50);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Pressure", 505L, 2,
                "Ultimate Ironman", Membership.P2P, 1, 1, 0L,
                levels, xp);
        List<ItemState> inventory = new ArrayList<>();
        for (int slot = 0; slot < 28; slot++)
            inventory.add(new ItemState(firstItemId + slot,
                    "Setup " + slot, 1, slot));
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Waterfall Quest", QuestStatus.NOT_STARTED);
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(inventory, true))
                .quests(new QuestSnapshot(quests))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FarmingAccessEvaluatorTest
{
    private final FarmingAccessEvaluator evaluator =
            new FarmingAccessEvaluator(new FarmingAccessCatalog());

    @Test
    public void p2pInfersOpenWorldPatchesButNotLockedQuestPatches()
    {
        FarmingSnapshot result = evaluator.evaluate(
                account(MembershipStatus.P2P),
                new QuestSnapshot(Collections.emptyMap()),
                AccessMemorySnapshot.empty(),
                null
        );

        assertTrue(result.isPatchReachable("falador"));
        assertTrue(result.isPatchReachable("catherby"));
        assertFalse(result.isPatchReachable("troll_stronghold"));
    }

    @Test
    public void completedQuestProvesQuestGatedPatch()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("My Arm's Big Adventure", QuestStatus.COMPLETE);

        FarmingSnapshot result = evaluator.evaluate(
                account(MembershipStatus.P2P),
                new QuestSnapshot(quests),
                AccessMemorySnapshot.empty(),
                null
        );

        assertTrue(result.isPatchReachable("troll_stronghold"));
    }

    @Test
    public void previousDirectObservationAlsoProvesPatchAccess()
    {
        Map<String, Long> memory = new HashMap<>();
        memory.put("farming.patch.troll_stronghold", 1L);

        FarmingSnapshot result = evaluator.evaluate(
                account(MembershipStatus.P2P),
                new QuestSnapshot(Collections.emptyMap()),
                new AccessMemorySnapshot(memory),
                null
        );

        assertTrue(result.isPatchReachable("troll_stronghold"));
    }

    @Test
    public void f2pDoesNotInferMembersFarmingPatches()
    {
        FarmingSnapshot result = evaluator.evaluate(
                account(MembershipStatus.F2P),
                new QuestSnapshot(Collections.emptyMap()),
                AccessMemorySnapshot.empty(),
                null
        );

        assertFalse(result.isPatchReachable("falador"));
        assertFalse(result.isPatchReachable("troll_stronghold"));
    }

    private static AccountSnapshot account(MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 1);
            xp.put(skill, 0);
        }

        return new AccountSnapshot(
                "Tester",
                0,
                "Main",
                membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                1,
                0L,
                levels,
                xp
        );
    }
}

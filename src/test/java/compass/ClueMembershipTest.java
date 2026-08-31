package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Membership regressions for clues and stale recurring observations. */
public class ClueMembershipTest
{
    @Test
    public void f2pHidesHardClueOpportunity()
    {
        GameData data = GameData.builder(account(MembershipStatus.F2P))
                .clue(new ClueSnapshot(true, "hard", System.currentTimeMillis(),
                        Confidence.VERIFIED))
                .build();

        assertFalse(new OpportunityEngine().evaluate(data).stream()
                .anyMatch(o -> o.getType() == OpportunityType.CLUE));
    }

    @Test
    public void f2pKeepsBeginnerClueOpportunity()
    {
        GameData data = GameData.builder(account(MembershipStatus.F2P))
                .clue(new ClueSnapshot(true, "beginner", System.currentTimeMillis(),
                        Confidence.VERIFIED))
                .build();

        assertTrue(new OpportunityEngine().evaluate(data).stream()
                .anyMatch(o -> o.getType() == OpportunityType.CLUE));
    }

    @Test
    public void p2pKeepsHardClueOpportunity()
    {
        GameData data = GameData.builder(account(MembershipStatus.P2P))
                .clue(new ClueSnapshot(true, "hard", System.currentTimeMillis(),
                        Confidence.VERIFIED))
                .build();

        assertTrue(new OpportunityEngine().evaluate(data).stream()
                .anyMatch(o -> o.getType() == OpportunityType.CLUE));
    }

    @Test
    public void tierObservationAloneIsNotReportedReady()
    {
        GameData data = GameData.builder(
                account(MembershipStatus.P2P))
                .clue(new ClueSnapshot(true, "hard", 1L,
                        Confidence.VERIFIED)).build();

        Opportunity clue = new OpportunityEngine().evaluate(data).stream()
                .filter(value -> value.getType() == OpportunityType.CLUE)
                .findFirst().get();

        assertFalse(clue.isReady());
        assertEquals(Confidence.CHECK_NEEDED,
                clue.getConfidence());
        assertTrue(clue.getPreparation().get(0)
                .contains("Open the clue scroll"));
    }

    @Test
    public void exactStepReplacesGenericPreparationChecklist()
    {
        ClueStepSnapshot step = new ClueStepSnapshot("emote step",
                "Perform the emote.", "Catherby bank",
                Collections.singletonList("Maple longbow"), false, false,
                null, false, "outside catherby bank");
        GameData data = GameData.builder(
                account(MembershipStatus.P2P))
                .clue(new ClueSnapshot(true, "medium", 1L,
                        Confidence.VERIFIED, step)).build();

        Opportunity clue = new OpportunityEngine().evaluate(data).stream()
                .filter(value -> value.getType() == OpportunityType.CLUE)
                .findFirst().get();

        assertTrue(clue.getTitle().contains("emote step"));
        assertTrue(clue.getPreparation().contains("Maple longbow"));
        assertTrue(clue.getPreparation().stream()
                .anyMatch(value -> value.contains("STASH")));
        assertFalse(clue.getPreparation().stream()
                .anyMatch(value -> value.contains("when needed")));
    }

    @Test
    public void f2pDoesNotLeakPreviouslyObservedMembersTimers()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:birdhouse", 0L);
        timers.put("opportunity:herb-run", 0L);

        GameData data = GameData.builder(account(MembershipStatus.F2P))
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers))
                .build();

        List<Opportunity> opportunities = new OpportunityEngine().evaluate(data);
        assertTrue(opportunities.isEmpty());
    }

    @Test
    public void clueTierMembershipRuleIsCentralized()
    {
        assertTrue(ClueTier.BEGINNER.isAvailableFor(MembershipStatus.F2P));
        assertFalse(ClueTier.EASY.isAvailableFor(MembershipStatus.F2P));
        assertFalse(ClueTier.MEDIUM.isAvailableFor(MembershipStatus.F2P));
        assertFalse(ClueTier.HARD.isAvailableFor(MembershipStatus.F2P));
        assertFalse(ClueTier.ELITE.isAvailableFor(MembershipStatus.F2P));
        assertFalse(ClueTier.MASTER.isAvailableFor(MembershipStatus.F2P));
        assertTrue(ClueTier.HARD.isAvailableFor(MembershipStatus.P2P));
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
                "Membership Test", 0, "Main", membership,
                1, 1, 0L, levels, xp);
    }
}

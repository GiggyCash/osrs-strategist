package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnknownMembershipCandidateIsolationTest
{
    @Test
    public void unknownMembershipReceivesOnlyExplicitF2pCandidateRecords()
    {
        AccountSnapshot account = account(0);

        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Pandemonium", QuestStatus.NOT_STARTED);
        quests.put("The Ides of Milk", QuestStatus.NOT_STARTED);
        GameData questData = GameData.builder(account)
                .quests(new QuestSnapshot(quests)).build();
        assertFalse(contains(new QuestCandidateProvider(new QuestPriorityCatalog())
                .candidates(context(questData)), "Pandemonium"));
        assertTrue(contains(new QuestCandidateProvider(new QuestPriorityCatalog())
                .candidates(context(questData)), "The Ides of Milk"));

        GameData minigameData = GameData.builder(account)
                .minigames(new MinigameSnapshot(
                        new HashSet<>(java.util.Arrays.asList("castle-wars", "tempoross")),
                        Collections.emptyMap())).build();
        java.util.List<Recommendation> minigames = new MinigameCandidateProvider(
                new MinigameCatalog()).candidates(context(minigameData));
        assertTrue(contains(minigames, "Castle Wars"));
        assertFalse(contains(minigames, "Tempoross"));

        Map<String, PvmReadiness> readiness = new HashMap<>();
        readiness.put("pvm:obor", ready("pvm:obor"));
        readiness.put("pvm:zulrah", ready("pvm:zulrah"));
        java.util.List<Recommendation> pvm = new PvmCandidateProvider()
                .candidates(context(GameData.builder(account)
                        .pvm(new PvmSnapshot(readiness)).build()));
        assertTrue(contains(pvm, "Obor"));
        assertFalse(contains(pvm, "Zulrah"));

        StrategyContext broad = context(GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .economy(new AccountEconomySnapshot(0, 0,
                        Confidence.VERIFIED))
                .diaries(new DiarySnapshot(Collections.singletonMap("Varrock", 0),
                        Collections.singletonMap("Varrock", 10)))
                .combatAchievements(new CombatAchievementSnapshot(0, 0))
                .build());
        assertTrue(new DiaryCandidateProvider().candidates(broad).isEmpty());
        assertTrue(new CombatAchievementCandidateProvider().candidates(broad).isEmpty());
        assertTrue(new ProgressionUpgradeCandidateProvider().candidates(broad).isEmpty());

        for (Recommendation candidate : new GearCandidateProvider(
                new GearProgressionCatalog()).candidates(broad))
            assertTrue(candidate.getId().startsWith("gear:f2p-"));
        for (Recommendation candidate : new MoneyMakingCandidateProvider(
                new MoneyMakingCatalog()).candidates(broad))
            assertTrue(candidate.getId().startsWith("money:f2p-"));

        StrategyContext ironUnknown = context(GameData.builder(account(1))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build());
        assertTrue(new ResourceDetourCandidateProvider()
                .candidates(ironUnknown).isEmpty());
    }

    @Test
    public void unknownMembershipDoesNotCreateMembersOpportunity()
    {
        Map<String, Long> timers = new HashMap<>();
        timers.put("opportunity:herb-run", 0L);
        GameData data = GameData.builder(account(0))
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers)).build();
        assertTrue(new OpportunityEngine().evaluate(data).isEmpty());

        GameData f2p = GameData.builder(account(0, MembershipStatus.F2P))
                .recurringOpportunities(new RecurringOpportunitySnapshot(timers)).build();
        assertTrue(new OpportunityEngine().evaluate(f2p).isEmpty());
    }

    @Test
    public void unknownCluesUseF2pBoundaryInProviderAndOpportunityEngine()
    {
        ClueSnapshot medium = new ClueSnapshot(true, "medium", 0L,
                Confidence.VERIFIED);
        GameData unknown = GameData.builder(account(0))
                .clue(medium).build();
        assertTrue(new ClueCandidateProvider().candidates(context(unknown)).isEmpty());
        assertTrue(new OpportunityEngine().evaluate(unknown).isEmpty());

        GameData p2p = GameData.builder(
                        account(0, MembershipStatus.P2P))
                .clue(medium).build();
        assertFalse(new ClueCandidateProvider().candidates(context(p2p)).isEmpty());
        assertTrue(new OpportunityEngine().evaluate(p2p).stream()
                .anyMatch(value -> value.getType() == OpportunityType.CLUE));

        GameData beginnerUnknown = GameData.builder(account(0))
                .clue(new ClueSnapshot(true, "beginner", 0L,
                        Confidence.VERIFIED)).build();
        assertFalse(new ClueCandidateProvider().candidates(
                context(beginnerUnknown)).isEmpty());
        assertTrue(new OpportunityEngine().evaluate(beginnerUnknown).stream()
                .anyMatch(value -> value.getType() == OpportunityType.CLUE));
    }

    private static PvmReadiness ready(String id)
    {
        return new PvmReadiness(id, true, Confidence.VERIFIED,
                Collections.emptyList());
    }

    private static boolean contains(java.util.List<Recommendation> candidates, String text)
    {
        return candidates.stream().anyMatch(value -> value.getTitle().contains(text));
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL, GoalType.GEAR_TARGET,
                true, false, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(int typeCode)
    {
        return account(typeCode, MembershipStatus.UNKNOWN);
    }

    private static AccountSnapshot account(int typeCode, MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 60); xp.put(skill, 0); }
        return new AccountSnapshot("Unknown", typeCode, "Unknown",
                membership, 1, 1200, 0L, levels, xp);
    }
}

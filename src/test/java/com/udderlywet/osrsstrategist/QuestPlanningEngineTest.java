package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestPlanningEngineTest
{
    private final QuestCandidateProvider provider = new QuestCandidateProvider(
            new QuestPriorityCatalog(), new QuestKnowledgeCatalog(),
            new QuestRequirementResolver());

    @Test
    public void fullyModeledF2pQuestCanBecomeActionable()
    {
        StrategyCandidate candidate = only(data(account(MembershipStatus.F2P,
                40, 40, 40), "Rune Mysteries", QuestStatus.NOT_STARTED,
                Collections.emptyList()));
        assertEquals(RecommendationConfidence.VERIFIED, candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction().contains("Start Rune Mysteries"));
        assertTrue(candidate.getGuidance().getLocation().contains("Duke Horacio"));
    }

    @Test
    public void ownedQuestItemsResolveButMissingItemsGiveConcretePreparation()
    {
        List<ItemStackSnapshot> allMeat = Arrays.asList(
                item("Raw bear meat"), item("Raw rat meat"),
                item("Raw beef"), item("Raw chicken"));
        StrategyCandidate ready = only(data(account(MembershipStatus.P2P,
                40, 40, 40), "Druidic Ritual", QuestStatus.NOT_STARTED, allMeat));
        assertEquals(RecommendationConfidence.VERIFIED, ready.getConfidence());

        StrategyCandidate missing = only(data(account(MembershipStatus.P2P,
                40, 40, 40), "Druidic Ritual", QuestStatus.NOT_STARTED,
                Collections.singletonList(item("Raw chicken"))));
        assertEquals(RecommendationConfidence.CHECK_NEEDED, missing.getConfidence());
        assertTrue(missing.getGuidance().getAction().contains("Obtain"));
        assertFalse(missing.getGuidance().getAction().trim().isEmpty());
    }

    @Test
    public void prerequisiteAndUnobservedAccessRemainConcreteChecks()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Bone Voyage", QuestStatus.NOT_STARTED);
        quests.put("The Dig Site", QuestStatus.NOT_STARTED);
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 60, 60, 60))
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(Arrays.asList(
                        new ItemStackSnapshot(1, "Vodka", 2),
                        item("Marrentill potion (unf)")), 1L)).build();
        StrategyCandidate boneVoyage = provider.candidates(context(data)).stream()
                .filter(value -> value.getTitle().contains("Bone Voyage"))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                boneVoyage.getConfidence());
        assertTrue(boneVoyage.getGuidance().getAction().contains("The Dig Site"));
    }

    @Test
    public void irreversibleRewardQuestIsExcludedForOneDefencePure()
    {
        assertTrue(provider.candidates(context(data(account(MembershipStatus.F2P,
                60, 1, 1), "Dragon Slayer I", QuestStatus.NOT_STARTED,
                Collections.emptyList()))).isEmpty());
    }

    @Test
    public void observedInventoryDoesNotTurnUnobservedBankIntoMissingItems()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 40, 40, 40))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Druidic Ritual", QuestStatus.NOT_STARTED)))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        item("Raw chicken")))).build();
        StrategyCandidate candidate = only(data);
        assertTrue(candidate.getGuidance().getAction()
                .contains("Verify ownership"));
    }

    private static StrategyCandidate only(StrategyDataBundle data)
    {
        List<StrategyCandidate> candidates = new QuestCandidateProvider(
                new QuestPriorityCatalog(), new QuestKnowledgeCatalog(),
                new QuestRequirementResolver()).candidates(context(data));
        assertEquals(1, candidates.size());
        return candidates.get(0);
    }

    private static StrategyDataBundle data(AccountSnapshot account,
            String quest, QuestStatus status, List<ItemStackSnapshot> bank)
    {
        return StrategyDataBundle.builder(account)
                .quests(new QuestSnapshot(Collections.singletonMap(quest, status)))
                .bank(new BankSnapshot(bank, 1L)).build();
    }

    private static StrategyContext context(StrategyDataBundle data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.QUEST_CAPE, false, false, false,
                new PreferenceProfile());
    }

    private static ItemStackSnapshot item(String name)
    {
        return new ItemStackSnapshot(1, name, 1);
    }

    private static AccountSnapshot account(MembershipStatus membership,
            int attack, int strength, int defence)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 50); xp.put(skill, 0); }
        levels.put(Skill.ATTACK, attack); levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence); levels.put(Skill.RANGED, attack);
        levels.put(Skill.MAGIC, attack); levels.put(Skill.HITPOINTS, 70);
        return new AccountSnapshot("Quest", 0, "Main", membership,
                1, 1000, 0L, levels, xp);
    }
}

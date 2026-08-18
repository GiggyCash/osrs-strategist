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

    @Test
    public void corpusCoversCommonTransportEquipmentAndCombatUnlockChains()
    {
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        assertTrue(catalog.all().size() >= 20);
        assertTrue(catalog.definitionFor("Animal Magnetism").getUnlocks()
                .contains("Ava's devices"));
        assertTrue(catalog.definitionFor("Fairytale II - Cure a Queen").getUnlocks()
                .contains("Fairy ring transportation"));
        assertTrue(catalog.definitionFor("Desert Treasure I").getPrerequisites()
                .contains("Troll Stronghold"));
        assertTrue(catalog.definitionFor("Monkey Madness I").getPrerequisites()
                .contains("The Grand Tree"));
    }

    @Test
    public void targetQuestPromotesItsMissingEncodedPrerequisite()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Monkey Madness I", QuestStatus.NOT_STARTED);
        quests.put("The Grand Tree", QuestStatus.NOT_STARTED);
        quests.put("Tree Gnome Village", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(Collections.emptyList(), 1L)).build();

        StrategyCandidate prerequisite = provider.candidates(context(data)).stream()
                .filter(candidate -> candidate.getTitle().contains("The Grand Tree"))
                .findFirst().orElseThrow(AssertionError::new);
        StrategyCandidate target = provider.candidates(context(data)).stream()
                .filter(candidate -> candidate.getTitle().contains("Monkey Madness I"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(prerequisite.getScore() > target.getScore());
        assertTrue(target.getGuidance().getAction().contains("The Grand Tree"));
    }

    @Test
    public void practicalBetaCorpusCoversMajorEarlyAndMidgameChains()
    {
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        assertTrue(catalog.all().size() >= 38);
        assertTrue(catalog.definitionFor("Lunar Diplomacy").getPrerequisites()
                .contains("Shilo Village"));
        assertTrue(catalog.definitionFor("Regicide").getPrerequisites()
                .contains("Underground Pass"));
        assertTrue(catalog.definitionFor("My Arm's Big Adventure").getPrerequisites()
                .contains("Eadgar's Ruse"));
        assertTrue(catalog.definitionFor("In Aid of the Myreque").getPrerequisites()
                .contains("In Search of the Myreque"));
    }

    @Test
    public void deepPlagueChainPromotesImmediateEncodedQuest()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Regicide", QuestStatus.NOT_STARTED);
        quests.put("Underground Pass", QuestStatus.NOT_STARTED);
        quests.put("Biohazard", QuestStatus.COMPLETE);
        quests.put("Plague City", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(Collections.emptyList(), 1L)).build();
        List<StrategyCandidate> candidates = provider.candidates(context(data));
        StrategyCandidate prerequisite = candidates.stream()
                .filter(value -> value.getTitle().contains("Underground Pass"))
                .findFirst().orElseThrow(AssertionError::new);
        StrategyCandidate target = candidates.stream()
                .filter(value -> value.getTitle().contains("Regicide"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(prerequisite.getScore() > target.getScore());
        assertTrue(target.getGuidance().getAction().contains("Underground Pass"));
    }

    @Test
    public void expandedCorpusCoversKingdomMorytaniaDesertAndPirateChains()
    {
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        assertTrue(catalog.all().size() >= 65);
        assertTrue(catalog.definitionFor("King's Ransom").getPrerequisites()
                .contains("Holy Grail"));
        assertTrue(catalog.definitionFor("A Taste of Hope").getPrerequisites()
                .contains("Darkness of Hallowvale"));
        assertTrue(catalog.definitionFor("Shadow of the Storm").getPrerequisites()
                .contains("The Golem"));
        assertTrue(catalog.definitionFor("Cabin Fever").getPrerequisites()
                .contains("Rum Deal"));
        assertTrue(catalog.definitionFor("Royal Trouble").getPrerequisites()
                .contains("Throne of Miscellania"));
        assertTrue(catalog.definitionFor("Watchtower").getUnlocks()
                .contains("Watchtower Teleport"));
    }

    @Test
    public void kingsRansomPromotesHolyGrailBeforeTarget()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("King's Ransom", QuestStatus.NOT_STARTED);
        quests.put("Black Knights' Fortress", QuestStatus.COMPLETE);
        quests.put("Holy Grail", QuestStatus.NOT_STARTED);
        quests.put("Merlin's Crystal", QuestStatus.COMPLETE);
        quests.put("Murder Mystery", QuestStatus.COMPLETE);
        quests.put("One Small Favour", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new BankSnapshot(Collections.emptyList(), 1L)).build();
        List<StrategyCandidate> candidates = provider.candidates(context(data));
        StrategyCandidate target = candidates.stream()
                .filter(value -> value.getTitle().contains("King's Ransom"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(target.getGuidance().getAction().contains("Holy Grail"));
        assertTrue(candidates.stream().anyMatch(value ->
                value.getTitle().contains("Holy Grail")));
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

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
        Recommendation candidate = only(data(account(MembershipStatus.F2P,
                40, 40, 40), "Rune Mysteries", QuestStatus.NOT_STARTED,
                Collections.emptyList()));
        assertEquals(Confidence.VERIFIED, candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction().contains("Start Rune Mysteries"));
        assertTrue(candidate.getGuidance().getLocation().contains("Duke Horacio"));
        assertTrue(candidate.getGuidance().getSupplies().contains("Quest Helper"));
    }

    @Test
    public void ownedQuestItemsResolveButMissingItemsGiveConcretePreparation()
    {
        List<ItemState> allMeat = Arrays.asList(
                item("Raw bear meat"), item("Raw rat meat"),
                item("Raw beef"), item("Raw chicken"));
        Recommendation ready = only(data(account(MembershipStatus.P2P,
                40, 40, 40), "Druidic Ritual", QuestStatus.NOT_STARTED, allMeat));
        assertEquals(Confidence.VERIFIED, ready.getConfidence());

        Recommendation missing = only(data(account(MembershipStatus.P2P,
                40, 40, 40), "Druidic Ritual", QuestStatus.NOT_STARTED,
                Collections.singletonList(item("Raw chicken"))));
        assertEquals(Confidence.CHECK_NEEDED, missing.getConfidence());
        assertTrue(missing.getGuidance().getAction().contains("Obtain"));
        assertFalse(missing.getGuidance().getAction().trim().isEmpty());
    }

    @Test
    public void prerequisiteAndUnobservedAccessRemainConcreteChecks()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Bone Voyage", QuestStatus.NOT_STARTED);
        quests.put("The Dig Site", QuestStatus.NOT_STARTED);
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 60, 60, 60))
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(Arrays.asList(
                        new ItemState(1, "Vodka", 2),
                        item("Marrentill potion (unf)")), 1L)).build();
        Recommendation boneVoyage = provider.candidates(context(data)).stream()
                .filter(value -> value.getTitle().contains("Bone Voyage"))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(Confidence.CHECK_NEEDED,
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
    public void questDerivedSkillPreparationKeepsProtectedSkillSafety()
    {
        Map<Skill, Integer> requirements = new EnumMap<>(Skill.class);
        requirements.put(Skill.DEFENCE, 40);
        QuestDefinition definition = new QuestDefinition("Safe parent", true,
                Collections.emptyList(), requirements, Collections.emptyList(),
                0, Collections.emptyList(), "Varrock",
                Collections.singletonList("A useful unlock"),
                Collections.emptyMap());
        AccountSnapshot pure = account(MembershipStatus.F2P, 60, 60, 1);
        QuestResolution resolution = new QuestRequirementResolver().resolve(
                definition, context(GameData.builder(pure).build()));

        assertEquals(Confidence.CHECK_NEEDED,
                resolution.getConfidence());
        assertTrue(resolution.getGuidance().getAction().contains("Train Defence"));
        assertFalse(new CandidateSafetyPolicy().isAllowed(
                resolution.getSafetyEvidence(), context(
                        GameData.builder(pure).build())));
    }

    @Test
    public void observedInventoryDoesNotTurnUnobservedBankIntoMissingItems()
    {
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 40, 40, 40))
                .quests(new QuestSnapshot(Collections.singletonMap(
                        "Druidic Ritual", QuestStatus.NOT_STARTED)))
                .inventory(new ItemsState(Collections.singletonList(
                        item("Raw chicken")))).build();
        Recommendation candidate = only(data);
        assertTrue(candidate.getGuidance().getAction()
                .contains("Verify ownership"));
    }

    @Test
    public void exactUimQuestSlotRequirementChangesReadiness()
    {
        QuestDefinition definition = new QuestDefinition("Slot test", false,
                Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), ItemRequirementExpression.itemClass(
                        ItemRequirementClass.EMPTY_INVENTORY_SPACE, 5,
                        ItemRequirementScope.CARRIED), 0,
                Collections.emptyList(), "Verified start",
                Collections.emptyList(), Collections.emptyMap());
        QuestRequirementResolver resolver = new QuestRequirementResolver();

        QuestResolution blocked = resolver.resolve(definition,
                context(GameData.builder(uimAccount())
                        .inventory(new ItemsState(slots(28), true))
                        .equipment(new ItemsState(
                                Collections.emptyList())).build()));
        assertEquals(Confidence.CHECK_NEEDED,
                blocked.getConfidence());
        assertTrue(blocked.getGuidance().getAction()
                .contains("requires 5 free inventory slots; only 0"));
        assertFalse(blocked.getGuidance().getAction().toLowerCase()
                .contains("make space"));

        QuestResolution ready = resolver.resolve(definition,
                context(GameData.builder(uimAccount())
                        .inventory(new ItemsState(slots(23), true))
                        .equipment(new ItemsState(
                                Collections.emptyList())).build()));
        assertEquals(Confidence.VERIFIED,
                ready.getConfidence());
        assertTrue(ready.getGuidance().getAction().contains("Start Slot test"));
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
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();

        Recommendation prerequisite = provider.candidates(context(data)).stream()
                .filter(candidate -> candidate.getTitle().contains("The Grand Tree"))
                .findFirst().orElseThrow(AssertionError::new);
        Recommendation target = provider.candidates(context(data)).stream()
                .filter(candidate -> candidate.getTitle().contains("Monkey Madness I"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(prerequisite.getScore() > target.getScore());
        assertTrue(target.getGuidance().getAction().contains("The Grand Tree"));
    }

    @Test
    public void lateGameBossQuestNamesItsImmediateStructuredPrerequisite()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Desert Treasure II - The Fallen Empire", QuestStatus.NOT_STARTED);
        quests.put("Desert Treasure I", QuestStatus.COMPLETE);
        quests.put("Secrets of the North", QuestStatus.COMPLETE);
        quests.put("Enakhra's Lament", QuestStatus.COMPLETE);
        quests.put("Temple of the Eye", QuestStatus.NOT_STARTED);
        quests.put("The Garden of Death", QuestStatus.COMPLETE);
        quests.put("Below Ice Mountain", QuestStatus.COMPLETE);
        quests.put("His Faithful Servants", QuestStatus.COMPLETE);
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 90, 90, 90))
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();

        Recommendation target = provider.candidates(context(data)).stream()
                .filter(candidate -> candidate.getTitle().contains("Desert Treasure II"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(target.getGuidance().getAction().contains("Temple of the Eye"));
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
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
        List<Recommendation> candidates = provider.candidates(context(data));
        Recommendation prerequisite = candidates.stream()
                .filter(value -> value.getTitle().contains("Underground Pass"))
                .findFirst().orElseThrow(AssertionError::new);
        Recommendation target = candidates.stream()
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
        GameData data = GameData.builder(
                        account(MembershipStatus.P2P, 70, 70, 70))
                .quests(new QuestSnapshot(quests))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
        List<Recommendation> candidates = provider.candidates(context(data));
        Recommendation target = candidates.stream()
                .filter(value -> value.getTitle().contains("King's Ransom"))
                .findFirst().orElseThrow(AssertionError::new);
        assertTrue(target.getGuidance().getAction().contains("Holy Grail"));
        assertTrue(candidates.stream().anyMatch(value ->
                value.getTitle().contains("Holy Grail")));
    }

    private static Recommendation only(GameData data)
    {
        List<Recommendation> candidates = new QuestCandidateProvider(
                new QuestPriorityCatalog(), new QuestKnowledgeCatalog(),
                new QuestRequirementResolver()).candidates(context(data));
        assertEquals(1, candidates.size());
        return candidates.get(0);
    }

    private static GameData data(AccountSnapshot account,
            String quest, QuestStatus status, List<ItemState> bank)
    {
        return GameData.builder(account)
                .quests(new QuestSnapshot(Collections.singletonMap(quest, status)))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(bank, 1L)).build();
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.QUEST_CAPE, false, false, false,
                new PreferenceProfile());
    }

    private static ItemState item(String name)
    {
        return new ItemState(1, name, 1);
    }

    private static List<ItemState> slots(int occupied)
    {
        List<ItemState> result = new java.util.ArrayList<>();
        for (int slot = 0; slot < occupied; slot++)
            result.add(new ItemState(10_000 + slot,
                    "Setup item " + slot, 1, slot));
        return result;
    }

    private static AccountSnapshot uimAccount()
    {
        AccountSnapshot base = account(MembershipStatus.P2P, 70, 70, 70);
        return new AccountSnapshot("Quest UIM", 2, "Ultimate Ironman",
                MembershipStatus.P2P, 1, base.getTotalLevel(),
                base.getTotalExperience(), base.getSkillLevels(),
                base.getSkillExperience());
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

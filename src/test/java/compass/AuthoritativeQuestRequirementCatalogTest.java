package compass;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AuthoritativeQuestRequirementCatalogTest
{
    @Test
    public void everyCurrentRuneLiteQuestHasStructuredPlannerData()
    {
        QuestKnowledgeCatalog catalog = new QuestKnowledgeCatalog();
        assertEquals(Quest.values().length, catalog.all().size());
        for (Quest quest : Quest.values())
            assertNotNull(quest.getName(), catalog.definitionFor(quest.getName()));
    }

    @Test
    public void importedFactsPromoteRealDeepPrerequisites()
    {
        QuestDefinition bloodMoon = new QuestKnowledgeCatalog()
                .definitionFor("The Blood Moon Rises");
        assertEquals(Integer.valueOf(74),
                bloodMoon.getSkillRequirements().get(Skill.SLAYER));
        assertTrue(bloodMoon.getPrerequisites().contains("Sins of the Father"));
        assertFalse(bloodMoon.hasFieldUncertainty());
        assertFalse(bloodMoon.getStartLocation().isEmpty());
        assertTrue(bloodMoon.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertFalse(bloodMoon.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void whileGuthixSleepsKeepsCompletionRequirementsConcise()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("While Guthix Sleeps");
        assertEquals(Integer.valueOf(49),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertTrue(quest.getAccessChecks().contains(
                "Verify Warriors' Guild access: Attack plus Strength totals "
                        + "at least 130, or either skill is 99"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void swanSongKeepsCombatEvidenceWithoutDuplicatingTypedAccess()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Swan Song");
        assertEquals(100, quest.getQuestPointsRequired());
        assertEquals(Integer.valueOf(66),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Sea Troll Queen (level 170)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void fremennikExilesPromotesSourceRequirementsIntoTypedGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Fremennik Exiles");
        assertEquals(4, quest.getPrerequisites().size());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "The Fremennik Isles", "Lunar Diplomacy",
                "Mountain Daughter", "Heroes' Quest")));
        assertEquals(6, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(65),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(60),
                quest.getSkillRequirements().get(Skill.SLAYER));
        assertEquals(Integer.valueOf(60),
                quest.getSkillRequirements().get(Skill.SMITHING));
        assertEquals(Integer.valueOf(60),
                quest.getSkillRequirements().get(Skill.FISHING));
        assertEquals(Integer.valueOf(55),
                quest.getSkillRequirements().get(Skill.RUNECRAFT));
        assertEquals(Integer.valueOf(60),
                quest.getSkillRequirements().get(Skill.MINING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("The Jormungand (level 363)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void pathOfGlouphriePromotesSourceRequirementsIntoTypedGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Path of Glouphrie");
        assertEquals(3, quest.getPrerequisites().size());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "The Eyes of Glouphrie", "Waterfall Quest",
                "Tree Gnome Village")));
        assertEquals(5, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(60),
                quest.getSkillRequirements().get(Skill.STRENGTH));
        assertEquals(Integer.valueOf(56),
                quest.getSkillRequirements().get(Skill.SLAYER));
        assertEquals(Integer.valueOf(56),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertEquals(Integer.valueOf(47),
                quest.getSkillRequirements().get(Skill.RANGED));
        assertEquals(Integer.valueOf(45),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Warped Terrorbirds (level 138)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void tearsOfGuthixPromotesCompletionSkillsIntoTypedGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Tears of Guthix");
        assertEquals(43, quest.getQuestPointsRequired());
        assertEquals(Integer.valueOf(49),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.MINING));
        assertTrue(quest.getAccessChecks().contains(
                "Use a lit sapphire lantern and a safe light-source route "
                        + "through Lumbridge Swamp Caves"));
    }

    @Test
    public void gettingAheadPromotesMandatoryStartSkillsIntoTypedGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Getting Ahead");
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(26),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Headless Beast (level-82)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void ethicallyAcquiredAntiquitiesPromotesCompleteStartContract()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Ethically Acquired Antiquities");
        assertEquals(2, quest.getPrerequisites().size());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "Children of the Sun", "Shield of Arrav")));
        assertEquals(Integer.valueOf(25),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertTrue(quest.getAccessChecks().isEmpty());
    }

    @Test
    public void deviousMindsDoesNotDuplicateTypedAccessRequirements()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Devious Minds");
        assertEquals(4, quest.getPrerequisites().size());
        assertEquals(3, quest.getSkillRequirements().size());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Abyssal creatures level 41+")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void curseOfArravDoesNotDuplicateTypedAccessRequirements()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Curse of Arrav");
        assertEquals(2, quest.getPrerequisites().size());
        assertEquals(6, quest.getSkillRequirements().size());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Arrav (level 339)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void troubledTortugansDoesNotDuplicateTypedAccessRequirements()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Troubled Tortugans");
        assertEquals(java.util.Collections.singletonList("Pandemonium"),
                quest.getPrerequisites());
        assertEquals(6, quest.getSkillRequirements().size());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Shellbane gryphon (level 235)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void tribalTotemUsesOnlyItsTypedThievingRequirement()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Tribal Totem");
        assertEquals(Integer.valueOf(21),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getAccessChecks().isEmpty());
    }

    @Test
    public void fishingContestDoesNotDuplicateItsTypedSkillRequirement()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Fishing Contest");
        assertEquals(Integer.valueOf(10),
                quest.getSkillRequirements().get(Skill.FISHING));
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Required items:"));
    }

    @Test
    public void hopespearsWillDistinguishesStartedFromCompletedPrerequisites()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Hopespear's Will");
        assertEquals(java.util.Collections.singletonList("The Restless Ghost"),
                quest.getStartedPrerequisites());
        assertEquals(3, quest.getPrerequisites().size());
        assertFalse(quest.getPrerequisites().contains("The Restless Ghost"));
        assertEquals(Integer.valueOf(50),
                quest.getSkillRequirements().get(Skill.PRAYER));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Strongbones (level 184)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void fenkenstrainUsesPartialRestlessGhostProgressWithoutPhantomSmithing()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Creature of Fenkenstrain");
        assertEquals(java.util.Collections.singletonList("Priest in Peril"),
                quest.getPrerequisites());
        assertEquals(java.util.Collections.singletonList("The Restless Ghost"),
                quest.getStartedPrerequisites());
        assertFalse(quest.getSkillRequirements().containsKey(Skill.SMITHING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(25),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertTrue(quest.getAccessChecks().contains(
                "Verify a safe route past level 72 monsters"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void emptyLordTypesStartedQuestsWithoutHardGatingConditionalSkills()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Curse of the Empty Lord");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getStartedPrerequisites().containsAll(
                java.util.Arrays.asList(
                        "Desert Treasure I", "The Restless Ghost")));
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().contains(
                "If the ring of visibility is not already owned, obtain it "
                        + "with 53 Thieving"));
        assertTrue(quest.getAccessChecks().contains(
                "Verify 31 Prayer only if Edgeville Monastery second-floor "
                        + "access is needed"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void generalsShadowSeparatesStartedQuestFromTypedRingOwnership()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The General's Shadow");
        assertEquals(java.util.Collections.singletonList("Desert Treasure I"),
                quest.getStartedPrerequisites());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "Curse of the Empty Lord", "Fight Arena")));
        assertTrue(quest.getItemRequirements().stream().anyMatch(
                item -> item.getName().equals("Ring of visibility")));
        assertEquals(java.util.Collections.singletonList(
                "Verify a legal setup for the prayer-disabled, safespottable "
                        + "Ghost of Bouncer fight"), quest.getAccessChecks());
    }

    @Test
    public void ragAndBoneTwoDoesNotHardGatePartialQuestOrOptionalDefence()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Rag and Bone Man II");
        assertEquals(java.util.Collections.singletonList(
                "Creature of Fenkenstrain"), quest.getStartedPrerequisites());
        assertFalse(quest.getPrerequisites().contains(
                "Creature of Fenkenstrain"));
        assertFalse(quest.getSkillRequirements().containsKey(Skill.DEFENCE));
        assertEquals(Integer.valueOf(40),
                quest.getSkillRequirements().get(Skill.SLAYER));
        assertTrue(quest.getAccessChecks().contains(
                "Verify a legal basilisk route; 20 Defence is needed only "
                        + "to wield a mirror shield"));
        assertTrue(quest.getAccessChecks().contains(
                "Verify legal setups for the high-level bone targets"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void elementalWorkshopOneUsesCurrentLevelTwentySkillContract()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Elemental Workshop I");
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.MINING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.SMITHING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Earth elemental (level 35)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void coldWarDoesNotConfuseMahoganyPlankWithWoodcuttingRequirement()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Cold War");
        assertFalse(quest.getSkillRequirements().containsKey(
                Skill.WOODCUTTING));
        assertEquals(5, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(34),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertTrue(quest.getAccessChecks().contains(
                "Verify access to a crafting table 3"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Icelords (level 51)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void recipeSubquestAliasesResolveToRuneLiteIdentities()
    {
        QuestDefinition king = new QuestKnowledgeCatalog().definitionFor(
                "Recipe for Disaster - King Awowogei");
        assertNotNull(king);
        assertTrue(king.getPrerequisites().contains("Monkey Madness I"));
        assertEquals(Integer.valueOf(70),
                king.getSkillRequirements().get(Skill.COOKING));
    }

    @Test
    public void knownSkillShortfallBecomesActionBeforeUncertainFields()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Prying Times");
        Map<String, QuestStatus> states = new HashMap<>();
        states.put("Prying Times", QuestStatus.NOT_STARTED);
        states.put("Pandemonium", QuestStatus.COMPLETE);
        states.put("The Knight's Sword", QuestStatus.COMPLETE);
        GameData data = GameData.builder(account(1))
                .quests(new QuestSnapshot(states))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
        StrategyContext context = new StrategyContext(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.QUEST_CAPE, false, false,
                new PreferenceProfile());
        QuestResolution result = TestFixtures.questRequirementResolver().resolve(quest, context);
        assertTrue(result.getGuidance().getAction().contains("Smithing"));
    }

    private static AccountSnapshot account(int level)
    {
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, level); xp.put(skill, 0); }
        return new AccountSnapshot("Player", 0L, 0, "Main", Membership.P2P, 0, level * Skill.values().length, 0, levels, xp);
    }
}

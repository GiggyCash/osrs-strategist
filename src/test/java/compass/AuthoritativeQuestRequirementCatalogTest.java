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
    public void familyCrestUsesConcretePreparationWithoutDuplicateSkillDump()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Family Crest");
        assertEquals(4, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(59),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain shrimps, salmon, tuna, bass, swordfish"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Chronozon (level 170)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void giantDwarfKeepsVariableCompanyTaskActionable()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Giant Dwarf");
        assertEquals(4, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(33),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain 200 coins"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "after choosing a company, source its assigned ores and bars"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void wantedUsesDirectGraphAndAlternativePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Wanted!");
        assertEquals(4, quest.getPrerequisites().size());
        assertEquals(32, quest.getQuestPointsRequired());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain either 10,000 coins or"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "20 unnoted rune or pure essence at once"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Black Knight (level 32)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void defenderOfVarrockUsesDirectGraphAndConcretePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Defender of Varrock");
        assertEquals(8, quest.getPrerequisites().size());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(55),
                quest.getSkillRequirements().get(Skill.SMITHING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a barronite deposit"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("armoured zombies (level 85)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void gardenOfTranquillityRestoresCompletePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Garden of Tranquillity");
        assertEquals(java.util.Collections.singletonList(
                "Creature of Fenkenstrain"), quest.getPrerequisites());
        assertEquals(Integer.valueOf(25),
                quest.getSkillRequirements().get(Skill.FARMING));
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a ring of Charos"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "pestle and mortar, filled plant pot"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "bottomless compost bucket does not work"));
        assertFalse(quest.getAccessChecks().get(0).contains("…"));
    }

    @Test
    public void legendsQuestRestoresCompletePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Legends' Quest");
        assertEquals(5, quest.getPrerequisites().size());
        assertEquals(10, quest.getSkillRequirements().size());
        assertEquals(107, quest.getQuestPointsRequired());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain two gold bars"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "standard-spellbook runes for one Charge Orb cast"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "the long path requires a second Charge Orb cast"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Nezikchened (level 187)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void princeAliRescueKeepsAlternativesAndOptionalCombat()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Prince Ali Rescue");
        assertTrue(quest.isFreeToPlay());
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain soft clay, three balls of wool"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "yellow dye or two onions and 5 coins"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Jail guard (level 26, optional)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void nightAtTheTheatreKeepsEncounterSafetyExplicit()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("A Night at the Theatre");
        assertEquals(java.util.Collections.singletonList("A Taste of Hope"),
                quest.getPrerequisites());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain an Ivandis or Blisterwood flail"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Hespori (Level 302)")
                        && check.contains("optional, if a kill count")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void anotherSliceOfHamKeepsCombatStyleConstraint()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Another Slice of H.A.M.");
        assertEquals(3, quest.getPrerequisites().size());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(25),
                quest.getSkillRequirements().get(Skill.PRAYER));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a light source and Magic or Ranged combat gear"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "cannot be fought with Melee"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Sigmund (level 64)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void eaglesPeakKeepsOptionalCombatSeparate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Eagles' Peak");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(Integer.valueOf(27),
                quest.getSkillRequirements().get(Skill.HUNTER));
        assertEquals("Obtain yellow dye, swamp tar, and 50 coins",
                quest.getAccessChecks().get(0));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Kebbit (level 13) (optional)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void enlightenedJourneyRestoresBothTripSetups()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Enlightened Journey");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(3, quest.getSkillRequirements().size());
        assertEquals(20, quest.getQuestPointsRequired());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain for Trip 1"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "For Trip 2 obtain at least ten normal logs"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "only if growing the quest-provided willow sapling"));
        assertFalse(quest.getAccessChecks().get(0).contains("…"));
    }

    @Test
    public void makingFriendsWithMyArmKeepsConditionalPreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Making Friends with My Arm");
        assertEquals(4, quest.getPrerequisites().size());
        assertEquals(4, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(66),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a saw, bolt of cloth, five mahogany planks"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "climbing boots unless using Trollheim teleports"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "Agility above 68 is beneficial"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Mother (level 198)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void mountainDaughterKeepsConditionalPreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Mountain Daughter");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a rope, pickaxe, axe, plank"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "Dramen staff does not work"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "boostable and not required to start"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("The Kendal (level 70)")
                        && check.contains("can be safespotted")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void anotherCooksQuestKeepsConcretePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Another Cook's Quest");
        assertEquals(java.util.Collections.singletonList("Cook's Assistant"),
                quest.getPrerequisites());
        assertEquals(Integer.valueOf(10),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain an eye of newt, Greenman's ale"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "any fruit blast except a premade fr' blast"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "10 Cooking is not boostable"));
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
    public void trollRomanceUsesOnlyItsSourceBackedAgilityRequirement()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Troll Romance");
        assertFalse(quest.getSkillRequirements().containsKey(
                Skill.WOODCUTTING));
        assertEquals(1, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(28),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Arrg (level 113)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void doricsQuestTreatsOresAsItemsNotAMiningGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Doric's Quest");
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("6 clay")
                        && check.contains("4 copper ores")
                        && check.contains("2 iron ores")));
    }

    @Test
    public void eyesOfGlouphrieTreatsMudRuneAsAnItemNotASkillGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Eyes of Glouphrie");
        assertFalse(quest.getSkillRequirements().containsKey(
                Skill.RUNECRAFT));
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(46),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertEquals(Integer.valueOf(5),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Required items:")
                        && check.contains("Mud rune")));
        assertTrue(quest.getAccessChecks().contains(
                "46 Magic is required during the quest but not to start it"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void fremennikIslesUsesOnlyItsMandatoryConstructionGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Fremennik Isles");
        assertFalse(quest.getSkillRequirements().containsKey(Skill.AGILITY));
        assertEquals(1, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertTrue(quest.getAccessChecks().contains(
                "Resolve the Mining-level-dependent ore requirement"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Ice Troll King")));
    }

    @Test
    public void taiBwoWannaiTrioSeparatesSkillsFromPotionAcquisition()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Tai Bwo Wannai Trio");
        assertEquals(3, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(5),
                quest.getSkillRequirements().get(Skill.FISHING));
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(Integer.valueOf(15),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertFalse(quest.getSkillRequirements().containsKey(Skill.HERBLORE));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Agility potion(4)")));
        assertTrue(quest.getAccessChecks().contains(
                "15 Agility, 30 Cooking, and 5 Fishing are required during "
                        + "the quest but not to start it"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void finalDawnUsesDirectQuestGraphAndConcretePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Final Dawn");
        assertEquals(java.util.Arrays.asList(
                "The Heart of Darkness", "Perilous Moons"),
                quest.getPrerequisites());
        assertEquals(3, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(66),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain Emissary robes"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Augur Metzli (lvl-396)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void heartOfDarknessUsesDirectQuestGraphAndConcretePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Heart of Darkness");
        assertEquals(java.util.Collections.singletonList("Twilight's Promise"),
                quest.getPrerequisites());
        assertEquals(4, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(55),
                quest.getSkillRequirements().get(Skill.MINING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain 30 coins"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Amoxliatl (level 263)")));
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

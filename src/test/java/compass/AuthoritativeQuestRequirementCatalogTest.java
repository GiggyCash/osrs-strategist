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
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain combat equipment, a mirror shield, two kegs of beer"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "buy the beer from Thora the Barkeep before starting"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "Fremennik shield—not a Neitiznot shield"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "for 150,000 coins or free with a Ring of Charos(a)"));
        assertTrue(quest.getAccessChecks().get(2).contains(
                "three Lunar bars or a pickaxe to mine three Lunar ores"));
        assertTrue(quest.getAccessChecks().get(2).contains(
                "60 Mining is boostable and not required to start"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("The Jormungand (level 363)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
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
                check -> check.contains("Headless Beast (level 82)")));
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
    public void piratePeteDoesNotTurnItemUseIntoSkillGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Pirate Pete");
        assertEquals(java.util.Collections.singletonList(
                "Recipe for Disaster - Another Cook's Quest"),
                quest.getPrerequisites());
        assertEquals(1, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(31),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertFalse(quest.getSkillRequirements().containsKey(Skill.CRAFTING));
        assertFalse(quest.getSkillRequirements().containsKey(Skill.SMITHING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain at least three raw cod and three bread"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "fishcakes can burn"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "31 Cooking is boostable"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("5 Mudskippers (level 30/31)")
                        && check.contains("Crab (level 21/23)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void kingAwowogeiKeepsDirectGraphAndAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - King Awowogei");
        assertEquals(java.util.Arrays.asList(
                "Monkey Madness I",
                "Recipe for Disaster - Another Cook's Quest"),
                quest.getPrerequisites());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(70),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(Integer.valueOf(48),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain an M'speak amulet"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "or 3,000 coins for three monkey talismans"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "70 Cooking is boostable"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "neither skill requirement is needed to start"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Big Snake (level 84)")
                        && check.contains("Monkey guard (level 167)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void skrachUglogweeKeepsAcquisitionAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Skrach Uglogwee");
        assertEquals(java.util.Arrays.asList(
                "Big Chompy Bird Hunting",
                "Recipe for Disaster - Another Cook's Quest"),
                quest.getPrerequisites());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(41),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a non-blessed axe"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "bought from Rantz for at most 600 coins"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "Brutal arrows work with a comp ogre bow"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "both skill requirements are boostable and not needed to start"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Jubbly bird (Level 11)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void lumbridgeGuideKeepsDirectGraphAndTravelAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Lumbridge Guide");
        assertEquals(7, quest.getPrerequisites().size());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "Big Chompy Bird Hunting", "Biohazard", "Demon Slayer",
                "Murder Mystery", "Nature Spirit",
                "Recipe for Disaster - Another Cook's Quest",
                "Witch's House")));
        assertFalse(quest.getPrerequisites().contains("Plague City"));
        assertFalse(quest.getPrerequisites().contains("Priest in Peril"));
        assertFalse(quest.getPrerequisites().contains("The Restless Ghost"));
        assertFalse(quest.getPrerequisites().contains("Cook's Assistant"));
        assertEquals(Integer.valueOf(40),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a bucket of milk, egg, pot of flour, and cake tin"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "40 Cooking is boostable but required to start"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "fairy ring D I S with a Lunar or Dramen staff"));
    }

    @Test
    public void evilDaveDoesNotTurnStewIntoACookingGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Evil Dave");
        assertEquals(java.util.Arrays.asList(
                "Gertrude's Cat",
                "Recipe for Disaster - Another Cook's Quest",
                "Shadow of the Storm"), quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Cook's Assistant"));
        assertFalse(quest.getPrerequisites().contains("Demon Slayer"));
        assertFalse(quest.getPrerequisites().contains("The Golem"));
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain any cat except an overgrown cat"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "up to nine bowls of stew"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "kittens and lazy cats are not recommended"));
    }

    @Test
    public void goblinGeneralsKeepIngredientAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Wartface & Bentnoze");
        assertEquals(java.util.Arrays.asList(
                "Goblin Diplomacy",
                "Recipe for Disaster - Another Cook's Quest"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Cook's Assistant"));
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(1, quest.getAccessChecks().size());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain bread, orange slices or an orange and knife"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "blue, black, green, or purple dye"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "spice is available at Ardougne market"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "charcoal is sold in Shilo Village"));
    }

    @Test
    public void mountainDwarfRestoresTruncatedPreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Mountain Dwarf");
        assertEquals(java.util.Arrays.asList(
                "Fishing Contest",
                "Recipe for Disaster - Another Cook's Quest"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Cook's Assistant"));
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a bucket of milk, pot of flour, egg, bowl of water"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "two extra ales and two extra coins"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "falls to 100 with a Ring of Charos(a) or 150 with a chocolate bar"));
        assertTrue(quest.getAccessChecks().get(1).startsWith(
                "Obtain gloves or Telekinetic Grab runes"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Icefiend (level 13)")
                        && check.contains("only if you lack ice gloves")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void sirAmikSeparatesStartedQuestAndDirectCompletions()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Sir Amik Varze");
        assertEquals(java.util.Arrays.asList(
                "Family Crest", "Heroes' Quest",
                "Recipe for Disaster - Another Cook's Quest",
                "Shilo Village", "Underground Pass", "Waterfall Quest"),
                quest.getPrerequisites());
        assertEquals(java.util.Collections.singletonList("Legends' Quest"),
                quest.getStartedPrerequisites());
        assertEquals(107, quest.getQuestPointsRequired());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a bucket of milk or Bottomless milk bucket"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "without 20 Farming"));
        assertTrue(quest.getAccessChecks().get(1).startsWith(
                "Obtain a Dramen or Lunar staff"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "anti-dragon shield mitigates most damage to under 10"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Black dragon (level 227)")
                        && check.contains("Evil Chicken (level varies)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void culinaromancerKeepsDirectFinaleReadiness()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster - Culinaromancer");
        assertEquals(10, quest.getPrerequisites().size());
        assertTrue(quest.getPrerequisites().containsAll(java.util.Arrays.asList(
                "Desert Treasure I", "Horror from the Deep",
                "Recipe for Disaster - Mountain Dwarf",
                "Recipe for Disaster - Wartface & Bentnoze",
                "Recipe for Disaster - Pirate Pete",
                "Recipe for Disaster - Lumbridge Guide",
                "Recipe for Disaster - Evil Dave",
                "Recipe for Disaster - Skrach Uglogwee",
                "Recipe for Disaster - Sir Amik Varze",
                "Recipe for Disaster - King Awowogei")));
        assertEquals(175, quest.getQuestPointsRequired());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain ice gloves for Flambeed"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "Menaphite remedy or restore potions for Karamel"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "defeat every boss without Prayer"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Agrith-Na-Na (level 146)")
                        && check.contains("Culinaromancer (level 75)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")));
    }

    @Test
    public void recipeForDisasterWrapperDelegatesToReviewedFinaleGraph()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recipe for Disaster");
        assertEquals(java.util.Collections.singletonList(
                "Recipe for Disaster - Culinaromancer"),
                quest.getPrerequisites());
        assertTrue(quest.getStartedPrerequisites().isEmpty());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(0, quest.getQuestPointsRequired());
        assertTrue(quest.getItemRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().isEmpty());
    }

    @Test
    public void whileGuthixSleepsRestoresTruncatedPreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("While Guthix Sleeps");
        assertEquals(10, quest.getPrerequisites().size());
        assertEquals(7, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(72),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertEquals(Integer.valueOf(49),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertEquals(180, quest.getQuestPointsRequired());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a Ring of Charos(a) or 500 coins"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "four Astral Contact casts"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "Snare or Entangle, Weaken or Enfeeble"));
        assertTrue(quest.getAccessChecks().get(2).startsWith(
                "Obtain during the quest mort myre fungus"));
        assertTrue(quest.getAccessChecks().get(2).contains(
                "carried Zombie axe blocks that option"));
        assertEquals("Verify Warriors' Guild access: Attack plus Strength "
                        + "totals at least 130, or either skill is 99",
                quest.getAccessChecks().get(3));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Balance Elemental (level 524)")
                        && check.contains("2 Tormented Demons (level 450)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void swanSongRestoresTruncatedPreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Swan Song");
        assertEquals(java.util.Arrays.asList(
                "Garden of Tranquillity", "One Small Favour"),
                quest.getPrerequisites());
        assertEquals(6, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(66),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertEquals(Integer.valueOf(40),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(100, quest.getQuestPointsRequired());
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain 10 mist runes, 10 lava runes, 5 blood runes"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "all six skill requirements are boostable and not needed to start"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "five iron bars or mine them during the quest"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "small fishing net, and brown apron are obtainable"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("11 Sea trolls")
                        && check.contains("can drain Prayer")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void kingdomDividedUsesDirectGraphAndStartGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("A Kingdom Divided");
        assertEquals(java.util.Arrays.asList(
                "The Ascent of Arceuus", "The Depths of Despair",
                "The Forsaken Tower", "The Queen of Thieves",
                "Tale of the Righteous"), quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Druidic Ritual"));
        assertFalse(quest.getPrerequisites().contains("Client of Kourend"));
        assertFalse(quest.getPrerequisites().contains("X Marks the Spot"));
        assertEquals(7, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(54),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertEquals(Integer.valueOf(35),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain access to the normal spellbook"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "dark essence block or a pickaxe and chisel"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "all seven skill requirements are non-boostable and required to start"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Judge of Yama (Level 168)")
                        && check.contains("Xamphur (level 239)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void grimTalesDoesNotTurnHerbloreIntoAQuestGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Grim Tales");
        assertEquals(java.util.Collections.singletonList("Witch's House"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Druidic Ritual"));
        assertEquals(5, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(71),
                quest.getSkillRequirements().get(Skill.WOODCUTTING));
        assertEquals(Integer.valueOf(45),
                quest.getSkillRequirements().get(Skill.FARMING));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain two Tarromin potion (unf)"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "unless the Farming portion of Barbarian Training is complete"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "all five skill requirements are boostable and not needed to start"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "optional if willing to take 17 damage"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Glod (level 138)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void slugMenaceRestoresAltarAndCombatAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Slug Menace");
        assertEquals(java.util.Arrays.asList("Sea Slug", "Wanted!"),
                quest.getPrerequisites());
        assertEquals(4, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.RUNECRAFT));
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.SLAYER));
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain and carry a Commorb to start"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "preferably 15–20 because they can shatter"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "Runecraft and Slayer explicitly required to start"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "four elemental talismans or tiaras plus a mind talisman or tiara"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "use a Runecraft cape, or use Guardians of the Rift"));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Slug Prince (level 62)")
                        && check.contains("must be defeated with Melee")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void greatBrainRobberyRestoresConditionalWorkshopAndCombatReadiness()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Great Brain Robbery");
        assertEquals(java.util.Arrays.asList(
                        "Creature of Fenkenstrain",
                        "Cabin Fever",
                        "Recipe for Disaster - Pirate Pete"),
                quest.getPrerequisites());
        assertEquals(3, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(16),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertEquals(Integer.valueOf(50),
                quest.getSkillRequirements().get(Skill.PRAYER));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("fishbowl helmet with a diving apparatus")
                        && check.contains("Medallion of the Deep")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("10 wooden cats after partial quest progress")
                        && check.contains("where account rules permit")
                        && check.contains("furs from Hunter creatures do not work")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("4 oak planks")
                        && check.contains("Clockmaking space")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Imcando hammer does not work")
                        && check.contains("18 total when making the wooden cats")
                        && check.contains("Ring of Charos")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Barrelchest (level 190)")
                        && check.contains("disable prayers")
                        && check.contains("safespotted without taking damage")
                        && check.contains("four Sorebones (level 57)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.contains("UNIQ--ref")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void forgettableTalePreservesVariableItemCheckWithoutGuessing()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Forgettable Tale...");
        assertEquals(java.util.Arrays.asList("The Giant Dwarf", "Fishing Contest"),
                quest.getPrerequisites());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(22),
                quest.getSkillRequirements().get(Skill.COOKING));
        assertEquals(Integer.valueOf(17),
                quest.getSkillRequirements().get(Skill.FARMING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Keldagrim has no range")
                        && check.contains("cook plain barley into malt elsewhere")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Keldagrim has no Tool Leprechaun")
                        && check.contains("Barbarian Training")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("ale yeast")
                        && check.contains("a kebab and 2 beers")
                        && check.contains("beer glass is obtainable")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Check which random item")
                        && check.contains("does not enumerate the possible items")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Both skill requirements are boostable")
                        && check.contains("not required to start")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.contains("UNIQ--ref")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void gettingAheadRestoresOnSiteSupplyAlternatives()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Getting Ahead");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(26),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("grizzly bear and bear cub west of the farm")
                        && check.contains("two black bears east")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Kebos Lowlands mine")
                        && check.contains("bronze pickaxe spawn")
                        && check.contains("water source and bucket spawn")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Imcando hammer")
                        && check.contains("Amy's saw")
                        && check.contains("extra nails are recommended")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("needle with thread or a costume needle")
                        && check.contains("thread is unnecessary")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Both skill requirements are non-boostable")
                        && check.contains("required to start")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.equals("Combat encounter: Headless Beast (level 82)")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Required items:")));
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
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("27 pots, 27 jugs of vinegar")
                        && check.contains("27 sets of any logs")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("dusty key when below 70 Agility")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("ogre composite bow with brutal arrows")
                        && check.contains("Crumble Undead")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("mirror shield or V's shield")
                        && check.contains("verified shield workaround")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("40 Slayer is boostable")
                        && check.contains("not required to start")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Check the quest's 27 combat targets")
                        && check.contains("does not enumerate the full target list")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void ratcatchersUsesStartedKeldagrimAccessAndConditionalSupplies()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Ratcatchers");
        assertEquals(java.util.Collections.singletonList(
                "Icthlarin's Little Helper"), quest.getPrerequisites());
        assertEquals(java.util.Collections.singletonList(
                "The Giant Dwarf"), quest.getStartedPrerequisites());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("overgrown cats cannot be used")
                        && check.contains("Hooknosed Jack")
                        && check.contains("strong cat combat stats")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("4 cheese or 4 poisoned cheese")
                        && check.contains("Clock Tower Basement")
                        && check.contains("cleaned kwuarm")
                        && check.contains("Zahur can clean it for 200 coins")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("cleaned marrentill")
                        && check.contains("bottomless milk bucket")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Catspeak amulet even after Dragon Slayer II")
                        && check.contains("pot of weeds")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("101 coins, or 51 coins")
                        && check.contains("Ring of Charos(a)")
                        && check.contains("one free inventory space")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("cat must fight the King rat")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void recruitmentDriveUsesTypedGraphWithoutRawDuplicateGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Recruitment Drive");
        assertEquals(java.util.Arrays.asList(
                        "Black Knights' Fortress", "Druidic Ritual"),
                quest.getPrerequisites());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(java.util.Collections.singletonList(
                        "Combat encounter: Sir Leye (level 20)"),
                quest.getAccessChecks());
        assertEquals(
                "Talk to Sir Amik Varze on the second floor (UK; third floor US) "
                        + "of the White Knights' Castle's western tower in Falador.",
                quest.getStartLocation());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void rovingElvesKeepsCharterRouteOutsideMandatorySkillGates()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Roving Elves");
        assertEquals(java.util.Arrays.asList("Regicide", "Waterfall Quest"),
                quest.getPrerequisites());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Glarial's pebble")
                        && check.contains("Waterfall Dungeon key")
                        && check.contains("spade and rope")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("56 Agility")
                        && check.contains("boostable and not required to start")
                        && check.contains("bypass that level with a charter ship")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Moss Guardian (level 84)")
                        && check.contains("restricted-item rules")
                        && check.contains("can be flinched")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void knightsSwordKeepsF2pPreparationFreeOfMemberDetours()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Knight's Sword");
        assertTrue(quest.isFreeToPlay());
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(Integer.valueOf(10),
                quest.getSkillRequirements().get(Skill.MINING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("full redberry pie")
                        && check.contains("2 iron bars")
                        && check.contains("pickaxe available in Rimmington")
                        && check.contains("mine 1 blurite ore")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("personal blurite sword")
                        && check.contains("4 iron bars total")
                        && check.contains("2 blurite ore total")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Reach or boost to 10 Mining")
                        && check.contains("started without that level")
                        && check.contains("level 57 ice warriors")
                        && check.contains("level 53 ice giants")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Members")
                        || check.contains("Falador easy diary")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void scorpionCatcherKeepsDungeonRoutesAndJailerConditional()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Scorpion Catcher");
        assertEquals(java.util.Collections.singletonList(
                "Alfred Grimhand's Barcrawl"), quest.getPrerequisites());
        assertEquals(Integer.valueOf(31),
                quest.getSkillRequirements().get(Skill.PRAYER));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("dusty key obtainable during the quest")
                        && check.contains("Legends' Guild Shop of Useful Items")
                        && check.contains("boostable 70 Agility")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("31 Prayer")
                        && check.contains("not boostable")
                        && check.contains("not required to start")
                        && check.contains("level 172 black demons")
                        && check.contains("level 64 poison spiders")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Combat encounter only when obtaining")
                        && check.contains("Jailer (level 47)")
                        && check.contains("avoids this fight")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void handInTheSandUsesTypedStartGatesAndActionableSupplies()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Hand in the Sand");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(2, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(17),
                quest.getSkillRequirements().get(Skill.THIEVING));
        assertEquals(Integer.valueOf(49),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("about 150 coins")
                        && check.contains("empty vial")
                        && check.contains("white berries")
                        && check.contains("lantern lens")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("beer during the quest")
                        && check.contains("Port Sarim Food Store")
                        && check.contains("Betty sells for 20 coins")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("bucket of sand")
                        && check.contains("bringing a bucket")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Both skill requirements are non-boostable")
                        && check.contains("required to start")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void shadesOfMorttonUsesDirectGraphAndSoloTemplePreparation()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Shades of Mort'ton");
        assertEquals(java.util.Collections.singletonList("Priest in Peril"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("Druidic Ritual"));
        assertEquals(3, quest.getSkillRequirements().size());
        assertEquals(Integer.valueOf(5),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.CRAFTING));
        assertEquals(Integer.valueOf(15),
                quest.getSkillRequirements().get(Skill.HERBLORE));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("at least 1,000 coins")
                        && check.contains("2 Tarromin potion (unf)")
                        && check.contains("2 ashes")
                        && check.contains("logs or an axe")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("unpopulated temple")
                        && check.contains("Flamtaer hammer")
                        && check.contains("Razmire sells one")
                        && !check.contains("Grand Exchange")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("All three skill requirements are boostable")
                        && check.contains("not required to start")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("temple-rebuilding onslaught")
                        && check.contains("nearby players")));
        assertTrue(quest.getAccessChecks().contains(
                "Combat encounters: 5 Loar Shades (level 40)"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void seaSlugOffersSelfSourcedSwampPasteWithoutRawSkillMarkup()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Sea Slug");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.FIREMAKING));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("mixing flour with swamp tar")
                        && check.contains("Trader crewmember for 75 coins")
                        && check.contains("Rasolo")
                        && check.contains("Khazard General Store")));
        assertTrue(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("Reach or boost to 30 Firemaking")
                        && check.contains("started without that level")));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void depthsOfDespairUsesDirectClientGraphAndTypedStartGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Depths of Despair");
        assertEquals(java.util.Collections.singletonList("Client of Kourend"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("X Marks the Spot"));
        assertEquals(Integer.valueOf(18),
                quest.getSkillRequirements().get(Skill.AGILITY));
        assertEquals(java.util.Arrays.asList(
                        "18 Agility is non-boostable and required to start the quest",
                        "Combat encounter: Sand Snake (level 36)"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void ascentOfArceuusUsesDirectClientGraphAndTypedStartGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("The Ascent of Arceuus");
        assertEquals(java.util.Collections.singletonList("Client of Kourend"),
                quest.getPrerequisites());
        assertFalse(quest.getPrerequisites().contains("X Marks the Spot"));
        assertEquals(Integer.valueOf(12),
                quest.getSkillRequirements().get(Skill.HUNTER));
        assertEquals(java.util.Arrays.asList(
                        "12 Hunter is non-boostable and required to start the quest",
                        "Combat encounters: 5 Tormented Souls (level 16) "
                                + "and the Trapped Soul (level 30)"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void clockTowerKeepsObtainableWaterOptionAndRunPastHazard()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Clock Tower");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(java.util.Arrays.asList(
                        "Bring a bucket of water, a jug of water, ice gloves, "
                                + "or smiths gloves (i); a bucket of water is "
                                + "obtainable during the quest",
                        "Be able to run past level 53 ogres"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void hazeelCultKeepsCombatConditionalOnCerilsRoute()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Hazeel Cult");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(java.util.Collections.singletonList(
                        "If taking Ceril's side, be able to defeat Alomone "
                                + "(level 13)"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")));
    }

    @Test
    public void monksFriendKeepsAlternativeMaterialAndRunPastHazard()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Monk's Friend");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertTrue(quest.getSkillRequirements().isEmpty());
        assertEquals(java.util.Arrays.asList(
                        "Bring a jug of water and either regular logs or a "
                                + "regular plank",
                        "Be able to run past two level 14 thieves and one "
                                + "level 26 NPC"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void towerOfLifeUsesTypedConstructionGateAndExactBeerRequirement()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Tower of Life");
        assertTrue(quest.getPrerequisites().isEmpty());
        assertEquals(Integer.valueOf(10),
                quest.getSkillRequirements().get(Skill.CONSTRUCTION));
        assertEquals(java.util.Arrays.asList(
                        "10 Construction is non-boostable and required to "
                                + "start the quest",
                        "Bring a hammer or Imcando hammer, a saw or crystal "
                                + "saw, and a regular beer; other beers do "
                                + "not work"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void elementalWorkshopTwoKeepsBoostableUnknownStartContract()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("Elemental Workshop II");
        assertEquals(java.util.Collections.singletonList(
                        "Elemental Workshop I"),
                quest.getPrerequisites());
        assertEquals(Integer.valueOf(20),
                quest.getSkillRequirements().get(Skill.MAGIC));
        assertEquals(Integer.valueOf(30),
                quest.getSkillRequirements().get(Skill.SMITHING));
        assertEquals(java.util.Arrays.asList(
                        "20 Magic and 30 Smithing are boostable; whether "
                                + "either is required to start is unknown",
                        "Bring a hammer, the battered key from Elemental "
                                + "Workshop I, and either 2 elemental metals "
                                + "or 2 elemental ore plus 8 coal; bring a "
                                + "pickaxe if obtaining ore during the quest",
                        "For a Mind shield, use 3 elemental metals or 3 "
                                + "elemental ore plus 12 coal instead",
                        "If you do not bring elemental metal bars, be prepared "
                                + "for 2 Earth elementals (level 35)"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
    }

    @Test
    public void whatLiesBelowUsesDirectGraphAndTypedRunecraftStartGate()
    {
        QuestDefinition quest = new QuestKnowledgeCatalog()
                .definitionFor("What Lies Below");
        assertEquals(java.util.Collections.singletonList("Rune Mysteries"),
                quest.getPrerequisites());
        assertEquals(Integer.valueOf(35),
                quest.getSkillRequirements().get(Skill.RUNECRAFT));
        assertEquals(java.util.Arrays.asList(
                        "35 Runecraft is non-boostable and required to start "
                                + "the quest",
                        "Bring an empty bowl and 15 chaos runes",
                        "Have access to the Chaos Altar through a chaos or "
                                + "catalytic talisman or tiara, the Abyss, or "
                                + "Temple of the Eye",
                        "Combat encounters: 5 Outlaws (level 32) and King "
                                + "Roald (level 47)"),
                quest.getAccessChecks());
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
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
        assertTrue(quest.getAccessChecks().get(0).startsWith(
                "Obtain a knife or bladed weapon, small fishing net"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "adjacent Melee deals no damage to the monkey"));
        assertTrue(quest.getAccessChecks().get(0).contains(
                "15 Agility and 5 Fishing are boostable, but 30 Cooking is not"));
        assertTrue(quest.getAccessChecks().get(1).startsWith(
                "Obtain Karamjan rum or 30 coins"));
        assertTrue(quest.getAccessChecks().get(1).contains(
                "before leaving the island or the rum disappears"));
        assertTrue(quest.getAccessChecks().contains(
                "15 Agility, 30 Cooking, and 5 Fishing are required during "
                        + "the quest but not to start it"));
        assertFalse(quest.getAccessChecks().stream().anyMatch(
                check -> check.contains("…")
                        || check.startsWith("Access requirements:")
                        || check.startsWith("Required items:")));
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

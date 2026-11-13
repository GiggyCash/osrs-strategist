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

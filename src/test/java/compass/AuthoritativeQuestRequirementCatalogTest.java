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

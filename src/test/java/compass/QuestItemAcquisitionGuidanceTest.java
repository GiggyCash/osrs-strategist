package compass;

import java.util.Collections;
import java.util.EnumMap;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestItemAcquisitionGuidanceTest
{
    private final ItemRequirementEvaluator evaluator = new ItemRequirementEvaluator();

    @Test
    public void observedShortfallIsExposedStructurally()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Death rune", 5, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        GameData data = GameData.builder(account(0))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.singletonList(
                        item("Death rune", 2)), 1L)).build();

        ItemRequirementResult result = evaluator.evaluate(requirement, data, false);

        assertEquals(RequirementState.BLOCKED, result.getState());
        assertEquals(1, result.getMissingInputs().size());
        assertEquals("Death rune", result.getMissingInputs().get(0).getName());
        assertEquals(3, result.getMissingInputs().get(0).getQuantity());
        assertEquals("Get 3 × Death rune", result.getAction());
    }

    @Test
    public void unobservedEnabledGroupStorageDoesNotBecomeFakeGimShortfall()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Rope", 1, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        GameData data = GameData.builder(account(4))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .groupStorage(ItemsState.unknown()).build();

        ItemRequirementResult result = evaluator.evaluate(requirement, data, true);

        assertEquals(RequirementState.CHECK_NEEDED, result.getState());
        assertTrue(result.getMissingInputs().isEmpty());
        assertTrue(result.getAction().contains("Check whether you own"));
    }

    @Test
    public void verifiedMissingAlternativeChoosesOneConcreteBranch()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.anyOf(
                ItemRequirementExpression.item("Death rune", 5,
                        ItemRequirementScope.OWNED_OR_RETRIEVABLE),
                ItemRequirementExpression.item("Chaos rune", 10,
                        ItemRequirementScope.OWNED_OR_RETRIEVABLE));
        GameData data = GameData.builder(account(1))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();

        ItemRequirementResult result = evaluator.evaluate(requirement, data, false);

        assertEquals(RequirementState.BLOCKED, result.getState());
        assertEquals(1, result.getMissingInputs().size());
        assertEquals("Death rune", result.getMissingInputs().get(0).getName());
        assertEquals(5, result.getMissingInputs().get(0).getQuantity());
    }

    @Test
    public void ironQuestPreparationUsesAccountAwareSourceGuidance()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Death rune", 5, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        QuestDefinition quest = new QuestDefinition("Test quest", false,
                Collections.emptyList(), Collections.<Skill, Integer>emptyMap(),
                Collections.emptyList(), requirement, 0, Collections.emptyList(),
                "Test location", Collections.singletonList("Test unlock"),
                Collections.<Skill, Integer>emptyMap());
        GameData data = GameData.builder(account(1))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();

        QuestResolution result = new QuestRequirementResolver().resolve(
                quest, context(data, false));

        assertEquals(Confidence.CHECK_NEEDED,
                result.getConfidence());
        assertTrue(result.getGuidance().getAction().contains(
                "Self-source 5 × Death rune"));
        assertTrue(result.getGuidance().getSupplies().contains(
                "Confirmed shortfall: 5 × Death rune"));
        assertFalse(result.getGuidance().getAction().contains("Grand Exchange"));
    }

    @Test
    public void f2pIronQuestUsesOnlyAnExplicitF2pResourceRoute()
    {
        ItemRequirementExpression requirement = ItemRequirementExpression.item(
                "Raw beef", 1, ItemRequirementScope.OWNED_OR_RETRIEVABLE);
        QuestDefinition quest = new QuestDefinition("F2P test quest", true,
                Collections.emptyList(), Collections.<Skill, Integer>emptyMap(),
                Collections.emptyList(), requirement, 0, Collections.emptyList(),
                "Lumbridge", Collections.emptyList(),
                Collections.<Skill, Integer>emptyMap());
        GameData data = GameData.builder(
                        account(1, MembershipStatus.F2P))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();

        QuestResolution result = new QuestRequirementResolver().resolve(
                quest, context(data, false));

        assertTrue(result.getGuidance().getAction().contains("F2P cow"));
        assertFalse(result.getGuidance().getAction().contains("members"));
    }

    private static StrategyContext context(GameData data,
            boolean useGroupStorage)
    {
        return new StrategyContext(data, null, null, null, null,
                useGroupStorage, false, new PreferenceProfile());
    }

    private static AccountSnapshot account(int type)
    {
        return account(type, MembershipStatus.P2P);
    }

    private static AccountSnapshot account(int type,
            MembershipStatus membership)
    {
        EnumMap<Skill, Integer> levels = new EnumMap<>(Skill.class);
        EnumMap<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Player", type, "test",
                membership, 0, 1500, 0L, levels, xp);
    }

    private static ItemState item(String name, int quantity)
    {
        return new ItemState(-1, name, quantity);
    }
}

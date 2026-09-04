package compass;

import static org.junit.Assert.*;

import java.util.*;
import net.runelite.api.Skill;
import org.junit.Test;

/** Regression coverage for quest-path value now owned by goal provenance. */
public class QuestPathPlanningServiceTest
{
    private final GoalDependencyProvenanceService service =
            new GoalDependencyProvenanceService();

    @Test
    public void unfinishedPrerequisiteReceivesDependencyValue()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Lost City", QuestStatus.NOT_STARTED);
        statuses.put("Fairytale I - Growing Pains", QuestStatus.NOT_STARTED);
        statuses.put("Nature Spirit", QuestStatus.COMPLETE);
        Recommendation value = service.attach(quest("Lost City"),
                context(statuses, Membership.P2P, QuestTolerance.LOW,
                        GoalType.QUEST_CAPE, 99));

        assertNotNull(value.getGoalProvenance());
        assertTrue(value.getStrategicValue().getSharedDependencyValue() > 0.0);
        assertTrue(value.getStrategicValue().getEvidenceIds()
                .contains("quest-path:Lost City"));
    }

    @Test
    public void optionalQuestToleranceDoesNotChangeRequiredPathValue()
    {
        Map<String, QuestStatus> statuses = new LinkedHashMap<>();
        statuses.put("Lost City", QuestStatus.NOT_STARTED);
        statuses.put("Fairytale I - Growing Pains", QuestStatus.NOT_STARTED);
        statuses.put("Nature Spirit", QuestStatus.COMPLETE);
        double low = service.attach(quest("Lost City"), context(statuses,
                Membership.P2P, QuestTolerance.LOW, GoalType.QUEST_CAPE, 99))
                .getStrategicValue().getSharedDependencyValue();
        double high = service.attach(quest("Lost City"), context(statuses,
                Membership.P2P, QuestTolerance.HIGH, GoalType.QUEST_CAPE, 99))
                .getStrategicValue().getSharedDependencyValue();
        assertEquals(low, high, 0.0);
    }

    @Test
    public void membersGoalDoesNotCreateF2pQuestProvenance()
    {
        Map<String, QuestStatus> statuses = Collections.singletonMap(
                "Song of the Elves", QuestStatus.NOT_STARTED);
        Recommendation value = service.attach(quest("Song of the Elves"),
                context(statuses, Membership.F2P, QuestTolerance.NORMAL,
                        GoalType.PRIFDDINAS, 99));
        assertNotNull(value.getGoalProvenance());
        assertFalse(value.getStrategicValue().hasTypedEvidence());
    }

    @Test
    public void unrelatedRewardDoesNotCreateUnlockValue()
    {
        Map<String, QuestStatus> statuses = Collections.singletonMap(
                "Recipe for Disaster - Wartface & Bentnoze",
                QuestStatus.NOT_STARTED);
        Recommendation value = service.attach(
                quest("Recipe for Disaster - Wartface & Bentnoze"),
                context(statuses, Membership.P2P, QuestTolerance.LOW,
                        GoalType.BARROWS_GLOVES, 1));
        assertEquals(0.0, value.getStrategicValue().getUnlockValue(), 0.0);
    }

    @Test
    public void directQuestCapeWorkRetainsTypedPath()
    {
        Map<String, QuestStatus> statuses = Collections.singletonMap(
                "Lost City", QuestStatus.NOT_STARTED);
        Recommendation value = service.attach(quest("Lost City"),
                context(statuses, Membership.P2P, QuestTolerance.NORMAL,
                        GoalType.QUEST_CAPE, 99));
        assertEquals(GoalRelation.DIRECT,
                value.getGoalProvenance().getRelationship());
        assertEquals(Arrays.asList("Quest cape", "Lost City"),
                value.getGoalProvenance().getPath());
    }

    private static Recommendation quest(String name)
    {
        return new Recommendation("quest:" + Names.slug(name), name,
                "test", 1.0, Confidence.CHECK_NEEDED, null,
                Safety.unknown());
    }

    private static StrategyContext context(Map<String, QuestStatus> statuses,
            Membership membership, QuestTolerance tolerance, GoalType goal,
            int farmingLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, skill == Skill.FARMING ? farmingLevel : 99);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Quest planner", 31L,
                0, "MAIN", membership, membership == Membership.P2P ? 1 : 0,
                levels.size() * 99, 0L, levels, xp);
        GameData data = GameData.builder(account)
                .quests(new QuestSnapshot(statuses)).build();
        return new StrategyContext(data, StrategyMode.EFFICIENT,
                SessionIntent.PICK_FOR_ME, tolerance, goal,
                false, false, false, new PreferenceProfile());
    }
}

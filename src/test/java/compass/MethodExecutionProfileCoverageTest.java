package compass;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Prevents silent exact-planner coverage loss from method-id drift. */
public class MethodExecutionProfileCoverageTest
{
    @Test
    public void everyExecutionProfileMapsToARealTrainingMethod()
    {
        Set<String> methodIds = allMethodSkills().keySet();
        for (String profileId : new MethodExecutionProfileCatalog().all().keySet())
        {
            assertTrue(
                    "Execution profile points at missing training method: " + profileId,
                    methodIds.contains(profileId));
        }
    }

    @Test
    public void everyExecutionProfileMatchesAPinnedRuneLiteAction()
    {
        Map<String, Skill> methodSkills = allMethodSkills();
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog();
        List<String> missing = new ArrayList<>();
        for (MethodProfile profile :
                new MethodExecutionProfileCatalog().all().values())
        {
            Skill skill = methodSkills.get(profile.methodId);
            boolean matched = skill != null && actions.actionsFor(skill).stream()
                    .anyMatch(action -> AdaptiveActionSelector.matches(
                            action, profile.actionTerms));
            if (!matched) missing.add(profile.methodId);
        }
        assertTrue("Execution profiles match no RuneLite action: " + missing,
                missing.isEmpty());
    }

    @Test
    public void deterministicPlannerTouchesEveryNonCombatSkillWithStableActions()
    {
        Set<Skill> covered = new HashSet<>();
        Set<String> profileIds = new MethodExecutionProfileCatalog().all().keySet();
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog f2p = new F2pBaselineMethodCatalog();
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();

        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod method : expanded.methodsFor(skill))
                if (profileIds.contains(method.getMethod().getId())) covered.add(skill);
            for (CuratedTrainingMethod method : f2p.methodsFor(skill))
                if (profileIds.contains(method.getMethod().getId())) covered.add(skill);
            for (TrainingMethod method : legacy.methodsFor(skill))
                if (profileIds.contains(method.getId())) covered.add(skill);
        }

        // Combat, Slayer, and Sailing need target/task/activity-aware planners
        // rather than pretending their XP comes from one stable repeatable unit.
        Skill[] expected = {
                Skill.AGILITY, Skill.COOKING, Skill.CONSTRUCTION,
                Skill.CRAFTING, Skill.FARMING, Skill.FIREMAKING,
                Skill.FISHING, Skill.FLETCHING, Skill.HERBLORE,
                Skill.HUNTER, Skill.MAGIC, Skill.MINING, Skill.PRAYER,
                Skill.RUNECRAFT, Skill.SMITHING, Skill.THIEVING,
                Skill.WOODCUTTING
        };
        for (Skill skill : expected)
        {
            assertTrue("No deterministic execution profile for " + skill,
                    covered.contains(skill));
        }
    }

    private static Map<String, Skill> allMethodSkills()
    {
        Map<String, Skill> skills = new LinkedHashMap<>();
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog f2p = new F2pBaselineMethodCatalog();
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod method : expanded.methodsFor(skill))
                skills.put(method.getMethod().getId(), skill);
            for (CuratedTrainingMethod method : f2p.methodsFor(skill))
                skills.put(method.getMethod().getId(), skill);
            for (TrainingMethod method : legacy.methodsFor(skill))
                skills.put(method.getId(), skill);
        }
        return skills;
    }
}

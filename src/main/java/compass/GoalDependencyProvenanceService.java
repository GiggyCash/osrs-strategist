package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Resolves recommendation relationships from actual typed goal dependencies. */
@Singleton
public class GoalDependencyProvenanceService
{
    private final GoalGraph goalGraph;
    private final QuestKnowledgeCatalog quests;

    @Inject
    public GoalDependencyProvenanceService(
            GoalGraph goalGraph, QuestKnowledgeCatalog quests)
    {
        this.goalGraph = goalGraph == null ? new GoalGraph() : goalGraph;
        this.quests = quests == null ? new QuestKnowledgeCatalog() : quests;
    }

    public GoalDependencyProvenanceService()
    {
        this(new GoalGraph(), new QuestKnowledgeCatalog());
    }

    public Recommendation attach(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null) return null;
        var existing = recommendation.getGoalProvenance();
        if (context != null && existing != null
                && existing.proves(context.goal(),
                        recommendation.id))
            return recommendation;
        var provenance = resolve(recommendation, context);
        return recommendation.withGoalProvenance(provenance);
    }

    public GoalProvenance resolve(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null) return null;
        var goal = context.goal();
        if (goal == null || goal == GoalType.AUTOMATIC
                || goal == GoalType.CUSTOM) return null;

        var skill = recommendationSkill(recommendation);
        if (skill != null)
        {
            var path = skillPath(goal, skill, context);
            if (path == null) return null;
            return isDirectSkillGoal(goal, skill)
                    ? GoalProvenance.direct(goal,
                            recommendation.id, path)
                    : GoalProvenance.prerequisite(goal,
                            recommendation.id, path);
        }

        var quest = recommendationQuest(recommendation, context);
        if (quest != null)
        {
            var path = questPath(goal, quest, context);
            if (path == null) return null;
            boolean direct = goal == GoalType.QUEST_CAPE
                    || path.size() == 2;
            return direct
                    ? GoalProvenance.direct(goal,
                            recommendation.id, path)
                    : GoalProvenance.prerequisite(goal,
                            recommendation.id, path);
        }

        var direct = directActivityPath(goal, recommendation);
        return direct == null ? null : GoalProvenance.direct(
                goal, recommendation.id, direct);
    }

    public boolean isRequiredQuest(
            GoalType goal, String questName, StrategyContext context)
    {
        return questPath(goal, questName, context) != null;
    }

    /** Nearest still-unmet, proven skill requirement on the selected goal path. */
    public int nextRequiredSkillLevel(
            GoalType goal, Skill skill, StrategyContext context)
    {
        if (goal == null || skill == null || context == null
                || context.data() == null
                || context.data().account() == null) return 0;
        var current = context.data().account().level(skill);
        if (isDirectSkillGoal(goal, skill))
        {
            int target = goal == GoalType.BASE_70S ? 70
                    : goal == GoalType.SLAYER_85 ? 85 : 99;
            return current < target ? target : 0;
        }
        var nearest = Integer.MAX_VALUE;
        for (String quest : requiredQuestNames(goal, context))
        {
            var definition = quests.definitionFor(quest);
            if (definition == null) continue;
            int level = definition.getSkillRequirements()
                    .getOrDefault(skill, 0);
            if (level > current) nearest = Math.min(nearest, level);
        }
        return nearest == Integer.MAX_VALUE ? 0 : nearest;
    }

    public GoalQuestRewardForecast guaranteedRewardsBeforeManualTraining(
            StrategyContext context, Skill skill)
    {
        if (context == null || skill == null || context.data() == null
                || context.data().account() == null
                || context.data().quests() == null)
            return new GoalQuestRewardForecast(skill, 0,
                    Collections.emptyList());

        var currentLevel = context.data().account().level(skill);
        var goalQuests = requiredQuestNames(context.goal(), context);
        var experience = 0;
        List<String> sources = new ArrayList<>();
        for (String quest : goalQuests)
        {
            var status = statusOf(context, quest);
            if (status == QuestStatus.COMPLETE || status == QuestStatus.UNKNOWN)
                continue;
            var definition = quests.definitionFor(quest);
            if (definition == null
                    || definition.getSkillRequirements().getOrDefault(skill, 1)
                            > currentLevel
                    || !canReachWithoutTrainingSkill(definition, context,
                            skill, new HashSet<>()))
                continue;
            var reward = definition.getRewardXp().getOrDefault(skill, 0);
            if (reward <= 0) continue;
            experience += reward;
            sources.add(quest);
        }
        return new GoalQuestRewardForecast(skill, experience, sources);
    }

    private boolean canReachWithoutTrainingSkill(QuestDefinition definition,
            StrategyContext context, Skill skill, Set<String> active)
    {
        if (definition == null) return false;
        var key = Names.words(definition.getName());
        if (!active.add(key)) return false;
        for (Map.Entry<Skill, Integer> requirement
                : definition.getSkillRequirements().entrySet())
        {
            int current = context.data().account()
                    .level(requirement.getKey());
            var gap = requirement.getValue() - current;
            // The forecast may look through one short prerequisite grind, but
            // never treat a distant quest reward as near-term XP.
            if (gap > 0 && (requirement.getKey() == skill || gap > 10))
            {
                active.remove(key);
                return false;
            }
        }
        for (String prerequisite : definition.getPrerequisites())
        {
            var status = statusOf(context, prerequisite);
            if (status == QuestStatus.COMPLETE) continue;
            if (status == QuestStatus.UNKNOWN)
            {
                active.remove(key);
                return false;
            }
            if (!canReachWithoutTrainingSkill(quests.definitionFor(prerequisite),
                    context, skill, active))
            {
                active.remove(key);
                return false;
            }
        }
        active.remove(key);
        return true;
    }

    private List<String> questPath(
            GoalType goal, String questName, StrategyContext context)
    {
        if (goal == null || questName == null) return null;
        var targetStatus = statusOf(context, questName);
        if (targetStatus != QuestStatus.NOT_STARTED
                && targetStatus != QuestStatus.IN_PROGRESS) return null;
        if (goal == GoalType.QUEST_CAPE)
        {
            return list(goal.toString(), questName);
        }
        for (String root : goalGraph.questRootsFor(goal))
        {
            List<String> path = findQuestPath(root, questName,
                    new HashSet<>());
            if (path != null)
            {
                List<String> result = new ArrayList<>();
                result.add(goal.toString());
                if (goal == GoalType.BOWFA) result.add(Text.get(1722));
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> findQuestPath(
            String current, String target, Set<String> active)
    {
        if (Names.words(current).equals(Names.words(target)))
            return list(current);
        var key = Names.words(current);
        if (!active.add(key)) return null;
        var definition = quests.definitionFor(current);
        if (definition != null)
        {
            for (String prerequisite : definition.getPrerequisites())
            {
                List<String> child = findQuestPath(
                        prerequisite, target, active);
                if (child != null)
                {
                    List<String> result = new ArrayList<>();
                    result.add(current);
                    result.addAll(child);
                    active.remove(key);
                    return result;
                }
            }
        }
        active.remove(key);
        return null;
    }

    private List<String> skillPath(
            GoalType goal, Skill skill, StrategyContext context)
    {
        if (isDirectSkillGoal(goal, skill))
        {
            var target = goal == GoalType.BASE_70S ? 70 : 99;
            if (goal == GoalType.SLAYER_85) target = 85;
            return list(goal.toString(), target + " " + display(skill));
        }
        if (goal == GoalType.QUEST_CAPE)
        {
            for (Map.Entry<String, QuestStatus> entry
                    : context.data().quests().quests().entrySet())
            {
                if (entry.getValue() == QuestStatus.COMPLETE
                        || entry.getValue() == QuestStatus.UNKNOWN) continue;
                List<String> path = skillPathInQuest(entry.getKey(), skill,
                        context, new HashSet<>());
                if (path != null)
                {
                    List<String> result = new ArrayList<>();
                    result.add(goal.toString());
                    result.addAll(path);
                    return result;
                }
            }
            return null;
        }
        for (String root : goalGraph.questRootsFor(goal))
        {
            List<String> path = skillPathInQuest(root, skill, context,
                    new HashSet<>());
            if (path != null)
            {
                List<String> result = new ArrayList<>();
                result.add(goal.toString());
                if (goal == GoalType.BOWFA) result.add(Text.get(1722));
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> skillPathInQuest(String quest, Skill skill,
            StrategyContext context, Set<String> active)
    {
        var key = Names.words(quest);
        if (!active.add(key)) return null;
        var definition = quests.definitionFor(quest);
        if (definition == null)
        {
            active.remove(key);
            return null;
        }
        var questStatus = statusOf(context, quest);
        if (questStatus == QuestStatus.COMPLETE
                || questStatus == QuestStatus.UNKNOWN)
        {
            active.remove(key);
            return null;
        }
        var required = definition.getSkillRequirements().getOrDefault(skill, 0);
        var current = context.data().account().level(skill);
        List<String> best = null;
        if (required > current)
            best = list(quest, required + " " + display(skill));
        for (String prerequisite : definition.getPrerequisites())
        {
            List<String> child = skillPathInQuest(
                    prerequisite, skill, context, active);
            if (child != null)
            {
                List<String> result = new ArrayList<>();
                result.add(quest);
                result.addAll(child);
                best = strongerSkillPath(best, result);
            }
        }
        active.remove(key);
        return best;
    }

    private static List<String> strongerSkillPath(
            List<String> left, List<String> right)
    {
        if (left == null) return right;
        if (right == null) return left;
        return requirementLevel(right) > requirementLevel(left) ? right : left;
    }

    private static int requirementLevel(List<String> path)
    {
        if (path == null || path.isEmpty()) return 0;
        var value = path.get(path.size() - 1);
        var space = value.indexOf(' ');
        if (space <= 0) return 0;
        try { return Integer.parseInt(value.substring(0, space)); }
        catch (NumberFormatException ex) { return 0; }
    }

    private Set<String> requiredQuestNames(
            GoalType goal, StrategyContext context)
    {
        Set<String> result = new HashSet<>();
        if (goal == GoalType.QUEST_CAPE)
        {
            for (Map.Entry<String, QuestStatus> entry
                    : context.data().quests().quests().entrySet())
                if (entry.getValue() != QuestStatus.COMPLETE
                        && entry.getValue() != QuestStatus.UNKNOWN)
                    collectQuestTree(entry.getKey(), result);
            return result;
        }
        for (String root : goalGraph.questRootsFor(goal))
            collectQuestTree(root, result);
        return result;
    }

    private void collectQuestTree(String quest, Set<String> result)
    {
        var key = Names.words(quest);
        if (!result.add(quest)) return;
        var definition = quests.definitionFor(quest);
        if (definition == null) return;
        for (String prerequisite : definition.getPrerequisites())
        {
            var seen = false;
            for (String value : result)
                if (Names.words(value).equals(Names.words(prerequisite))) seen = true;
            if (!seen) collectQuestTree(prerequisite, result);
        }
    }

    private static boolean isDirectSkillGoal(GoalType goal, Skill skill)
    {
        if (skill == null) return false;
        return goal == GoalType.MAX || goal == GoalType.TOTAL_2000
                || goal == GoalType.BASE_70S
                || goal == GoalType.SLAYER_85 && skill == Skill.SLAYER;
    }

    private static List<String> directActivityPath(
            GoalType goal, Recommendation recommendation)
    {
        String identity = Names.words(recommendation.id + " "
                + recommendation.getTitle());
        switch (goal)
        {
            case FIRE_CAPE:
                return contains(identity, "fire cape", "tztok jad",
                        Text.get(1723))
                        ? list(goal.toString(), Text.get(1325))
                        : null;
            case BOWFA:
                return contains(identity, "bowfa", Text.get(1326))
                        ? list(goal.toString(), Text.get(264))
                        : null;
            case INFERNAL_CAPE:
                return contains(identity, "inferno", "infernal cape", "tzkal zuk")
                        ? list(goal.toString(), Text.get(1327)) : null;
            default:
                return null;
        }
    }

    private static Skill recommendationSkill(Recommendation recommendation)
    {
        var plan = recommendation.plan();
        return plan == null || plan.method() == null
                ? null : plan.method().getSkill();
    }

    private static String recommendationQuest(
            Recommendation recommendation, StrategyContext context)
    {
        var id = recommendation.id;
        if (id == null || !id.startsWith("quest:") || context.data() == null
                || context.data().quests() == null) return null;
        var slug = id.substring("quest:".length());
        for (String quest : context.data().quests().quests().keySet())
            if (slug(quest).equals(slug)) return quest;
        return null;
    }

    private static QuestStatus statusOf(
            StrategyContext context, String quest)
    {
        if (context == null || context.data() == null
                || context.data().quests() == null)
            return QuestStatus.UNKNOWN;
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
            if (Names.words(entry.getKey()).equals(Names.words(quest)))
                return entry.getValue();
        return QuestStatus.UNKNOWN;
    }

    private static String display(Skill skill)
    {
        var value = skill.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean contains(String value, String... tokens)
    {
        for (String token : tokens)
            if (value.contains(Names.words(token))) return true;
        return false;
    }

    private static String slug(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }


    private static List<String> list(String... values)
    {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }
}

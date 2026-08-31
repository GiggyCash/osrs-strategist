package com.udderlywet.osrsstrategist;

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
        GoalDependencyProvenance existing = recommendation.getGoalProvenance();
        if (context != null && existing != null
                && existing.proves(context.getActiveGoal(),
                        recommendation.getId()))
            return recommendation;
        GoalDependencyProvenance provenance = resolve(recommendation, context);
        return recommendation.withGoalProvenance(provenance);
    }

    public GoalDependencyProvenance resolve(
            Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null) return null;
        GoalType goal = context.getActiveGoal();
        if (goal == null || goal == GoalType.AUTOMATIC
                || goal == GoalType.CUSTOM) return null;

        Skill skill = recommendationSkill(recommendation);
        if (skill != null)
        {
            List<String> path = skillPath(goal, skill, context);
            if (path == null) return null;
            return isDirectSkillGoal(goal, skill)
                    ? GoalDependencyProvenance.direct(goal,
                            recommendation.getId(), path)
                    : GoalDependencyProvenance.prerequisite(goal,
                            recommendation.getId(), path);
        }

        String quest = recommendationQuest(recommendation, context);
        if (quest != null)
        {
            List<String> path = questPath(goal, quest, context);
            if (path == null) return null;
            boolean direct = goal == GoalType.QUEST_CAPE
                    || path.size() == 2;
            return direct
                    ? GoalDependencyProvenance.direct(goal,
                            recommendation.getId(), path)
                    : GoalDependencyProvenance.prerequisite(goal,
                            recommendation.getId(), path);
        }

        List<String> direct = directActivityPath(goal, recommendation);
        return direct == null ? null : GoalDependencyProvenance.direct(
                goal, recommendation.getId(), direct);
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
                || context.getData() == null
                || context.getData().getAccount() == null) return 0;
        int current = context.getData().getAccount().getSkillLevel(skill);
        if (isDirectSkillGoal(goal, skill))
        {
            int target = goal == GoalType.BASE_70S ? 70
                    : goal == GoalType.SLAYER_85 ? 85 : 99;
            return current < target ? target : 0;
        }
        int nearest = Integer.MAX_VALUE;
        for (String quest : requiredQuestNames(goal, context))
        {
            QuestDefinition definition = quests.definitionFor(quest);
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
        if (context == null || skill == null || context.getData() == null
                || context.getData().getAccount() == null
                || context.getData().getQuests() == null)
            return new GoalQuestRewardForecast(skill, 0,
                    Collections.emptyList());

        int currentLevel = context.getData().getAccount().getSkillLevel(skill);
        Set<String> goalQuests = requiredQuestNames(context.getActiveGoal(), context);
        int experience = 0;
        List<String> sources = new ArrayList<>();
        for (String quest : goalQuests)
        {
            QuestStatus status = statusOf(context, quest);
            if (status == QuestStatus.COMPLETE || status == QuestStatus.UNKNOWN)
                continue;
            QuestDefinition definition = quests.definitionFor(quest);
            if (definition == null
                    || definition.getSkillRequirements().getOrDefault(skill, 1)
                            > currentLevel
                    || !canReachWithoutTrainingSkill(definition, context,
                            skill, new HashSet<>()))
                continue;
            int reward = definition.getRewardXp().getOrDefault(skill, 0);
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
        String key = normalize(definition.getName());
        if (!active.add(key)) return false;
        for (Map.Entry<Skill, Integer> requirement
                : definition.getSkillRequirements().entrySet())
        {
            int current = context.getData().getAccount()
                    .getSkillLevel(requirement.getKey());
            int gap = requirement.getValue() - current;
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
            QuestStatus status = statusOf(context, prerequisite);
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
        QuestStatus targetStatus = statusOf(context, questName);
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
                if (goal == GoalType.BOWFA) result.add("Prifddinas access");
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> findQuestPath(
            String current, String target, Set<String> active)
    {
        if (normalize(current).equals(normalize(target)))
            return list(current);
        String key = normalize(current);
        if (!active.add(key)) return null;
        QuestDefinition definition = quests.definitionFor(current);
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
            int target = goal == GoalType.BASE_70S ? 70 : 99;
            if (goal == GoalType.SLAYER_85) target = 85;
            return list(goal.toString(), target + " " + display(skill));
        }
        if (goal == GoalType.QUEST_CAPE)
        {
            for (Map.Entry<String, QuestStatus> entry
                    : context.getData().getQuests().getQuests().entrySet())
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
                if (goal == GoalType.BOWFA) result.add("Prifddinas access");
                result.addAll(path);
                return result;
            }
        }
        return null;
    }

    private List<String> skillPathInQuest(String quest, Skill skill,
            StrategyContext context, Set<String> active)
    {
        String key = normalize(quest);
        if (!active.add(key)) return null;
        QuestDefinition definition = quests.definitionFor(quest);
        if (definition == null)
        {
            active.remove(key);
            return null;
        }
        QuestStatus questStatus = statusOf(context, quest);
        if (questStatus == QuestStatus.COMPLETE
                || questStatus == QuestStatus.UNKNOWN)
        {
            active.remove(key);
            return null;
        }
        int required = definition.getSkillRequirements().getOrDefault(skill, 0);
        int current = context.getData().getAccount().getSkillLevel(skill);
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
        String value = path.get(path.size() - 1);
        int space = value.indexOf(' ');
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
                    : context.getData().getQuests().getQuests().entrySet())
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
        String key = normalize(quest);
        if (!result.add(quest)) return;
        QuestDefinition definition = quests.definitionFor(quest);
        if (definition == null) return;
        for (String prerequisite : definition.getPrerequisites())
        {
            boolean seen = false;
            for (String value : result)
                if (normalize(value).equals(normalize(prerequisite))) seen = true;
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
        String identity = normalize(recommendation.getId() + " "
                + recommendation.getTitle());
        switch (goal)
        {
            case FIRE_CAPE:
                return contains(identity, "fire cape", "tztok jad",
                        "tzhaar fight cave")
                        ? list(goal.toString(), "Complete the TzHaar Fight Cave")
                        : null;
            case BOWFA:
                return contains(identity, "bowfa", "enhanced crystal weapon seed")
                        ? list(goal.toString(), Text.get(264))
                        : null;
            case INFERNAL_CAPE:
                return contains(identity, "inferno", "infernal cape", "tzkal zuk")
                        ? list(goal.toString(), "Complete the Inferno") : null;
            default:
                return null;
        }
    }

    private static Skill recommendationSkill(Recommendation recommendation)
    {
        TrainingPlan plan = recommendation.getTrainingPlan();
        return plan == null || plan.getMethod() == null
                ? null : plan.getMethod().getSkill();
    }

    private static String recommendationQuest(
            Recommendation recommendation, StrategyContext context)
    {
        String id = recommendation.getId();
        if (id == null || !id.startsWith("quest:") || context.getData() == null
                || context.getData().getQuests() == null) return null;
        String slug = id.substring("quest:".length());
        for (String quest : context.getData().getQuests().getQuests().keySet())
            if (slug(quest).equals(slug)) return quest;
        return null;
    }

    private static QuestStatus statusOf(
            StrategyContext context, String quest)
    {
        if (context == null || context.getData() == null
                || context.getData().getQuests() == null)
            return QuestStatus.UNKNOWN;
        for (Map.Entry<String, QuestStatus> entry
                : context.getData().getQuests().getQuests().entrySet())
            if (normalize(entry.getKey()).equals(normalize(quest)))
                return entry.getValue();
        return QuestStatus.UNKNOWN;
    }

    private static String display(Skill skill)
    {
        String value = skill.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean contains(String value, String... tokens)
    {
        for (String token : tokens)
            if (value.contains(normalize(token))) return true;
        return false;
    }

    private static String slug(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static List<String> list(String... values)
    {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }
}

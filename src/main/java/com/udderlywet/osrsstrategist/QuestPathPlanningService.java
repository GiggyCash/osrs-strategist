package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Orders quest work from typed dependency edges and compresses shared paths.
 * Optional-quest preference is deliberately absent: every returned quest is a
 * proven dependency of one of the supplied goals.
 */
@Singleton
public final class QuestPathPlanningService
{
    private final GoalGraph goalGraph;
    private final QuestKnowledgeCatalog quests;
    private final QuestRequirementResolver resolver;

    @Inject
    public QuestPathPlanningService(GoalGraph goalGraph,
            QuestKnowledgeCatalog quests, QuestRequirementResolver resolver)
    {
        this.goalGraph = goalGraph == null ? new GoalGraph() : goalGraph;
        this.quests = quests == null ? new QuestKnowledgeCatalog() : quests;
        this.resolver = resolver == null
                ? new QuestRequirementResolver() : resolver;
    }

    public QuestPathPlanningService()
    {
        this(new GoalGraph(), new QuestKnowledgeCatalog(),
                new QuestRequirementResolver());
    }

    public QuestPathPlan plan(StrategyContext context)
    {
        return plan(context, Collections.singleton(
                context == null ? GoalType.AUTOMATIC
                        : context.getActiveGoal()));
    }

    public QuestPathPlan plan(StrategyContext context,
            Collection<GoalType> selectedGoals)
    {
        if (context == null || context.data() == null
                || context.data().account() == null
                || context.data().quests() == null
                || selectedGoals == null)
            return new QuestPathPlan(Collections.emptyList());

        Map<String, MutableNode> nodes = new LinkedHashMap<>();
        for (GoalType goal : selectedGoals)
        {
            if (goal == null || goal == GoalType.AUTOMATIC
                    || goal == GoalType.CUSTOM) continue;
            if (goal == GoalType.QUEST_CAPE)
                addQuestCapeRoots(goal, context, nodes);
            else
                for (String root : goalGraph.questRootsFor(goal))
                    traverse(goal, root, context, nodes,
                            new ArrayList<>(), new HashSet<>());
        }

        Map<Skill, Integer> unmetSkillTargets = unmetSkillTargets(
                nodes, context);
        List<QuestPathStep> result = new ArrayList<>();
        for (MutableNode node : nodes.values())
        {
            QuestStatus status = statusOf(context, node.questName);
            if (status == QuestStatus.COMPLETE
                    || status == QuestStatus.UNKNOWN) continue;
            QuestDefinition definition = quests.definitionFor(node.questName);
            AccountSnapshot account = context.data().account();
            if (definition == null
                    || !QuestMembershipPolicy.isAvailable(
                            definition.getName(),
                            account.getMembershipStatus())
                    || !RestrictedQuestPolicy.isSafe(account,
                            definition.getName()))
                continue;
            QuestResolution resolution = definition == null ? null
                    : resolver.resolve(definition, context);
            Confidence readiness = resolution == null
                    ? Confidence.CHECK_NEEDED
                    : resolution.getConfidence();
            boolean prerequisitesComplete = definition != null;
            if (definition != null)
                for (String prerequisite : definition.getPrerequisites())
                    if (statusOf(context, prerequisite)
                            != QuestStatus.COMPLETE)
                        prerequisitesComplete = false;
            boolean eligible = prerequisitesComplete
                    && readiness != Confidence.BLOCKED;
            Map<Skill, Integer> rewards = guaranteedRewards(definition);
            result.add(new QuestPathStep(node.questName, status,
                    node.paths, node.unfinishedDependents,
                    readiness, eligible, node.depth, rewards,
                    rewardValue(rewards, unmetSkillTargets,
                            context.data().account())));
        }
        result.sort(Comparator
                .comparing(QuestPathStep::isEligibleNow).reversed()
                .thenComparing(step -> step.getStatus()
                        == QuestStatus.IN_PROGRESS, Comparator.reverseOrder())
                .thenComparing(step -> step.getReadiness()
                        == Confidence.VERIFIED,
                        Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(
                        QuestPathStep::getGoalCount).reversed())
                .thenComparing(Comparator.comparingInt(
                        (QuestPathStep step) -> step
                                .getUnfinishedDependents().size()).reversed())
                .thenComparing(Comparator.comparingDouble(
                        QuestPathStep::getGoalPathRewardValue).reversed())
                .thenComparing(Comparator.comparingInt(
                        QuestPathStep::getDepth).reversed())
                .thenComparing(QuestPathStep::getQuestName));
        return new QuestPathPlan(result);
    }

    private void addQuestCapeRoots(GoalType goal, StrategyContext context,
            Map<String, MutableNode> nodes)
    {
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
            if (entry.getValue() == QuestStatus.NOT_STARTED
                    || entry.getValue() == QuestStatus.IN_PROGRESS)
                traverse(goal, entry.getKey(), context, nodes,
                        new ArrayList<>(), new HashSet<>());
    }

    private void traverse(GoalType goal, String questName,
            StrategyContext context, Map<String, MutableNode> nodes,
            List<String> ancestors, Set<String> active)
    {
        String key = normalize(questName);
        if (!active.add(key)) return;
        QuestDefinition definition = quests.definitionFor(questName);
        if (definition == null)
        {
            active.remove(key);
            return;
        }
        AccountSnapshot account = context.data().account();
        if (!QuestMembershipPolicy.isAvailable(definition.getName(),
                account.getMembershipStatus())
                || !RestrictedQuestPolicy.isSafe(account,
                        definition.getName()))
        {
            active.remove(key);
            return;
        }
        List<String> path = new ArrayList<>();
        path.add(goal.toString());
        path.addAll(ancestors);
        path.add(definition.getName());
        MutableNode node = nodes.computeIfAbsent(key,
                ignored -> new MutableNode(definition.getName()));
        node.paths.put(goal, shortest(node.paths.get(goal), path));
        node.depth = Math.max(node.depth, ancestors.size());

        List<String> childAncestors = new ArrayList<>(ancestors);
        childAncestors.add(definition.getName());
        for (String prerequisite : definition.getPrerequisites())
        {
            QuestStatus status = statusOf(context, prerequisite);
            if (status != QuestStatus.COMPLETE
                    && status != QuestStatus.UNKNOWN)
            {
                MutableNode child = nodes.computeIfAbsent(
                        normalize(prerequisite),
                        ignored -> new MutableNode(prerequisite));
                if (!child.unfinishedDependents.contains(definition.getName()))
                    child.unfinishedDependents.add(definition.getName());
            }
            traverse(goal, prerequisite, context, nodes,
                    childAncestors, active);
        }
        active.remove(key);
    }

    private static List<String> shortest(
            List<String> current, List<String> candidate)
    {
        if (current == null || candidate.size() < current.size())
            return candidate;
        return current;
    }

    private Map<Skill, Integer> unmetSkillTargets(
            Map<String, MutableNode> nodes, StrategyContext context)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        AccountSnapshot account = context.data().account();
        for (MutableNode node : nodes.values())
        {
            QuestDefinition definition = quests.definitionFor(node.questName);
            if (definition == null) continue;
            for (Map.Entry<Skill, Integer> requirement
                    : definition.getSkillRequirements().entrySet())
                if (requirement.getValue()
                        > account.getSkillLevel(requirement.getKey()))
                    result.merge(requirement.getKey(), requirement.getValue(),
                            Math::max);
        }
        return result;
    }

    private static Map<Skill, Integer> guaranteedRewards(
            QuestDefinition definition)
    {
        if (definition == null) return Collections.emptyMap();
        for (String uncertainty : definition.getFieldUncertainties())
        {
            String value = normalize(uncertainty);
            if (value.contains("reward") || value.contains("irreversible xp"))
                return Collections.emptyMap();
        }
        return definition.getRewardXp();
    }

    private static double rewardValue(Map<Skill, Integer> rewards,
            Map<Skill, Integer> targets, AccountSnapshot account)
    {
        double value = 0.0;
        for (Map.Entry<Skill, Integer> reward : rewards.entrySet())
        {
            Integer target = targets.get(reward.getKey());
            if (target == null || reward.getValue() <= 0) continue;
            int currentLevel = account.getSkillLevel(reward.getKey());
            if (currentLevel >= target) continue;
            int currentXp = Math.max(account.getSkillExperience(reward.getKey()),
                    Experience.getXpForLevel(Math.max(1, currentLevel)));
            int targetXp = Experience.getXpForLevel(target);
            int gap = Math.max(1, targetXp - currentXp);
            value += Math.min(1.0, reward.getValue() / (double) gap);
        }
        return Math.min(1.0, value);
    }

    private static QuestStatus statusOf(
            StrategyContext context, String questName)
    {
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
            if (normalize(entry.getKey()).equals(normalize(questName)))
                return entry.getValue();
        return QuestStatus.UNKNOWN;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static final class MutableNode
    {
        private final String questName;
        private final Map<GoalType, List<String>> paths =
                new EnumMap<>(GoalType.class);
        private final List<String> unfinishedDependents = new ArrayList<>();
        private int depth;

        private MutableNode(String questName) { this.questName = questName; }
    }
}

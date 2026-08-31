package compass;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Builds a concise ordered plan from the same proven path used by DO NEXT. */
@Singleton
public final class StrategicPlanService
{
    private final QuestKnowledgeCatalog quests;

    @Inject
    public StrategicPlanService(QuestKnowledgeCatalog quests)
    {
        this.quests = quests == null ? new QuestKnowledgeCatalog() : quests;
    }

    public StrategicPlanService()
    {
        this(new QuestKnowledgeCatalog());
    }

    public StrategicPlan build(
            List<Recommendation> recommendations,
            StrategyContext context,
            long nowMillis)
    {
        if (recommendations == null || context == null
                || context.data() == null
                || context.data().account() == null
                || context.goal() == null
                || context.goal() == GoalType.AUTOMATIC
                || context.goal() == GoalType.CUSTOM)
            return null;

        Recommendation anchor = null;
        GoalProvenance provenance = null;
        for (Recommendation candidate : recommendations)
        {
            GoalProvenance value = candidate == null
                    ? null : candidate.getGoalProvenance();
            if (value != null && value.proves(
                    context.goal(), candidate.getId()))
            {
                anchor = candidate;
                provenance = value;
                break;
            }
        }
        if (anchor == null) return null;

        List<StrategicPlanStep> steps = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        var current = currentStep(anchor, provenance);
        steps.add(current);
        ids.add(current.getId());

        var path = provenance.getPath();
        for (int i = path.size() - 2; i >= 0; i--)
        {
            var label = path.get(i);
            StrategicPlanStep step = dependencyStep(label,
                    context.goal());
            if (ids.add(step.getId())) steps.add(step);
        }
        return new StrategicPlan(context.goal(),
                context.data().account(), steps, 0, nowMillis);
    }

    private StrategicPlanStep currentStep(
            Recommendation recommendation,
            GoalProvenance provenance)
    {
        var training = recommendation.plan();
        if (training != null && training.method() != null
                && training.method().getSkill() != null
                && recommendation.getTargetLevel() > 0)
        {
            var skill = training.method().getSkill();
            var currentTarget = recommendation.getCurrentExecutionTargetLevel();
            return new StrategicPlanStep(
                    "skill:" + skill.name().toLowerCase(Locale.ROOT) + ":"
                            + currentTarget,
                    GoalNodeKind.SKILL_LEVEL,
                    display(skill) + " " + recommendation.getCurrentLevel()
                            + " → " + currentTarget,
                    provenance.compactPath(),
                    PlanCompletionCondition.skillLevel(
                            skill, currentTarget),
                    recommendation.getId());
        }

        var quest = questName(recommendation);
        if (quest != null)
            return new StrategicPlanStep(
                    "quest:" + slug(quest), GoalNodeKind.QUEST,
                    quest, provenance.compactPath(),
                    PlanCompletionCondition.questComplete(quest),
                    recommendation.getId());

        return new StrategicPlanStep(
                "action:" + slug(recommendation.getId()),
                GoalNodeKind.ACTIVITY,
                recommendation.getTitle(), provenance.compactPath(),
                PlanCompletionCondition.none(), recommendation.getId());
    }

    private StrategicPlanStep dependencyStep(String label, GoalType goal)
    {
        var definition = quests.definitionFor(label);
        if (definition != null)
            return new StrategicPlanStep(
                    "quest:" + slug(definition.getName()), GoalNodeKind.QUEST,
                    definition.getName(), Text.get(1298) + goal,
                    PlanCompletionCondition.questComplete(definition.getName()),
                    "quest:" + slug(definition.getName()));

        var target = label.equalsIgnoreCase(goal.toString());
        return new StrategicPlanStep(
                (target ? "goal:" : "dependency:") + slug(label),
                target ? GoalNodeKind.META : GoalNodeKind.ACCESS,
                label,
                target ? "Selected target" : Text.get(1299) + goal,
                PlanCompletionCondition.none(), null);
    }

    private String questName(Recommendation recommendation)
    {
        if (recommendation == null || recommendation.getId() == null
                || !recommendation.getId().startsWith("quest:")) return null;
        String title = recommendation.getTitle() == null ? ""
                : recommendation.getTitle();
        title = title.replaceFirst(Text.get(1300), "");
        var separator = title.indexOf(": ");
        if (separator > 0) title = title.substring(0, separator);
        var definition = quests.definitionFor(title);
        return definition == null ? null : definition.getName();
    }

    private static String display(Skill skill)
    {
        String value = skill.name().toLowerCase(Locale.ROOT)
                .replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

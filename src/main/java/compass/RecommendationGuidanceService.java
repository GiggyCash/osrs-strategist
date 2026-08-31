package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Builds concrete, account-aware instructions for skill recommendations.
 *
 * <p>Guidance resolves in layers: special burn-aware routes, curated exact
 * execution profiles, variable-XP activity planners, then the universal
 * RuneLite action fallback. Every layer refuses to invent data it cannot prove.</p>
 */
@Singleton
public class RecommendationGuidanceService
{
    private static final String LOW_LEVEL_FISH_METHOD = get(1735);
    private static final double LOW_LEVEL_BURN_BUFFER = 2.5;

    private static final List<CookingStage> F2P_EARLY_COOKING = Arrays.asList(
            new CookingStage(1, 5, "sardine", "Raw sardine", 40),
            new CookingStage(5, 15, "herring", "Raw herring", 50),
            new CookingStage(15, 20, "trout", "Raw trout", 70),
            new CookingStage(20, 25, "pike", "Raw pike", 80),
            new CookingStage(25, 30, "salmon", "Raw salmon", 90)
    );

    private final AdaptiveMilestoneGuidanceService adaptiveGuidance;
    private final VariableMethodGuidanceService variableGuidance;
    private final UniversalSkillActionGuidanceService universalGuidance;

    @Inject
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance,
            VariableMethodGuidanceService variableGuidance,
            UniversalSkillActionGuidanceService universalGuidance)
    {
        this.adaptiveGuidance = adaptiveGuidance;
        this.variableGuidance = variableGuidance;
        this.universalGuidance = universalGuidance;
    }

    /** Compatibility constructor retained for focused tests and older callers. */
    public RecommendationGuidanceService(
            AdaptiveMilestoneGuidanceService adaptiveGuidance)
    {
        this(adaptiveGuidance,
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    /** Compatibility constructor for focused tests and older callers. */
    public RecommendationGuidanceService()
    {
        this(new AdaptiveMilestoneGuidanceService(),
                new VariableMethodGuidanceService(),
                new UniversalSkillActionGuidanceService());
    }

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan)
    {
        return build(data, skill, currentLevel, targetLevel,
                trainingPlan, true);
    }

    public Guidance build(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        Guidance uimBronze = uimF2pBronzeGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimBronze != null) return uimBronze;
        Guidance uimCooking = uimF2pCookingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimCooking != null) return uimCooking;
        Guidance uimRunecraft = uimF2pRunecraftGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimRunecraft != null) return uimRunecraft;
        Guidance uimThieving = uimThievingGuidance(
                data, skill, targetLevel, trainingPlan);
        if (uimThieving != null) return uimThieving;

        Guidance cooking = earlyCookingGuidance(
                data, skill, currentLevel, targetLevel,
                trainingPlan, useGroupStorage);
        if (cooking != null) return cooking;

        Guidance exact = adaptiveGuidance == null
                ? null
                : adaptiveGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (exact != null) return exact;

        Guidance variable = variableGuidance == null
                ? null
                : variableGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
        if (variable != null) return variable;

        return universalGuidance == null
                ? null
                : universalGuidance.build(
                        data, skill, currentLevel, targetLevel,
                        trainingPlan, useGroupStorage);
    }

    private static Guidance uimF2pBronzeGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.SMITHING || plan == null
                || plan.method() == null
                || !get(1632).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                get(662)
                        + targetLevel + ".",
                get(673),
                get(684),
                get(686),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pCookingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.COOKING || plan == null
                || plan.method() == null
                || !get(1857).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        return new Guidance(
                get(687)
                        + targetLevel + ".",
                get(688),
                get(689),
                get(690),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimF2pRunecraftGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.RUNECRAFT || plan == null
                || plan.method() == null
                || !get(1858).equals(
                        plan.method().id)
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        var level = data.account().level(Skill.RUNECRAFT);
        String rune = level >= 20 ? "body" : level >= 14 ? "fire"
                : level >= 9 ? "earth" : level >= 5 ? "water"
                : level >= 2 ? "mind" : "air";
        String altar = level >= 20 ? get(691)
                : level >= 14 ? get(1440)
                : level >= 9 ? get(1441)
                : level >= 5 ? get(1442)
                : level >= 2 ? get(1443)
                : get(1444);
        return new Guidance(
                get(663)
                        + altar + ", craft " + rune
                        + get(664)
                        + targetLevel + ".",
                "Bring the " + rune + get(1285) + rune
                        + get(665),
                get(666) + altar + ".",
                get(667),
                MethodBankingBehavior.LOCAL_PROCESSING);
    }

    private static Guidance uimThievingGuidance(
            GameData data, Skill skill, int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || skill != Skill.THIEVING || plan == null
                || plan.method() == null
                || AccountMode.fromTypeCode(
                        data.account().modeCode())
                        != AccountMode.ULTIMATE_IRONMAN)
            return null;
        var id = plan.method().id;
        if (get(1859).equals(id))
            return new Guidance(
                    get(668)
                            + targetLevel + ".",
                    get(669),
                    get(670),
                    get(671),
                    MethodBankingBehavior.NONE);
        if (get(1860).equals(id))
            return new Guidance(
                    get(672)
                            + targetLevel + ".",
                    get(674),
                    get(675),
                    get(676),
                    MethodBankingBehavior.NONE);
        return null;
    }

    private Guidance earlyCookingGuidance(
            GameData data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan trainingPlan,
            boolean useGroupStorage)
    {
        if (data == null
                || data.account() == null
                || skill != Skill.COOKING
                || trainingPlan == null
                || trainingPlan.method() == null
                || !LOW_LEVEL_FISH_METHOD.equals(trainingPlan.method().id))
        {
            return null;
        }

        if (currentLevel < 1 || currentLevel >= 30 || targetLevel > 30)
        {
            return null;
        }

        List<StagePlan> stages = buildStages(
                data.account(), currentLevel, targetLevel);
        if (stages.isEmpty()) return null;

        var action = actionGuidance(stages);
        String supplies = supplyGuidance(
                data, data.account(), stages, useGroupStorage);
        var location = locationGuidance(data.quests());
        var note = get(677);

        return new Guidance(
                action, supplies, location, note);
    }

    private static List<StagePlan> buildStages(
            AccountSnapshot account,
            int currentLevel,
            int targetLevel)
    {
        List<StagePlan> plans = new ArrayList<>();
        var currentXp = account.xp(Skill.COOKING);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }

        var stageStartXp = currentXp;
        for (CookingStage stage : F2P_EARLY_COOKING)
        {
            if (stage.endLevel <= currentLevel
                    || stage.startLevel >= targetLevel)
            {
                continue;
            }

            var stageTargetLevel = Math.min(stage.endLevel, targetLevel);
            var stageTargetXp = Experience.getXpForLevel(stageTargetLevel);
            var xpNeeded = Math.max(0, stageTargetXp - stageStartXp);
            var successfulCooks = divideRoundUp(xpNeeded, stage.xpEach);
            int rawNeeded = Math.max(
                    successfulCooks,
                    (int) Math.ceil(successfulCooks * LOW_LEVEL_BURN_BUFFER));

            if (successfulCooks > 0)
            {
                plans.add(new StagePlan(
                        stage, stageTargetLevel, successfulCooks, rawNeeded));
            }

            stageStartXp = stageTargetXp;
            if (stageTargetLevel >= targetLevel) break;
        }
        return plans;
    }

    private static String actionGuidance(List<StagePlan> stages)
    {
        var text = new StringBuilder();
        for (int i = 0; i < stages.size(); i++)
        {
            var stage = stages.get(i);
            if (i > 0) text.append(" Then ");
            text.append("cook ")
                    .append(stage.stage.foodName)
                    .append(" to level ")
                    .append(stage.targetLevel)
                    .append(" (about ")
                    .append(stage.successfulCooks)
                    .append(get(1861))
                    .append(stage.successfulCooks == 1 ? "" : "s")
                    .append(").");
        }
        return capitalize(text.toString());
    }

    private static String supplyGuidance(
            GameData data,
            AccountSnapshot account,
            List<StagePlan> stages,
            boolean useGroupStorage)
    {
        var mode = AccountMode.fromTypeCode(account.modeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() == null)
        {
            return "Plan for " + requiredSummary(stages)
                    + get(680);
        }
        var items = new ItemIndex(data, useGroupStorage);
        List<String> ownedParts = new ArrayList<>();
        List<String> missingParts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            var verified = items.quantity(stage.stage.rawItemName);
            var missing = Math.max(0, stage.rawNeeded - verified);
            ownedParts.add(verified + " "
                    + stage.stage.rawItemName.toLowerCase());
            if (missing > 0)
                missingParts.add(missing + " "
                        + stage.stage.rawItemName.toLowerCase());
        }

        var text = new StringBuilder();
        text.append("Plan for ").append(requiredSummary(stages));
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            text.append(get(1445));
        else text.append(". Verified: ");
        text.append(joinNatural(ownedParts)).append(".");

        if (missingParts.isEmpty())
        {
            text.append(mode == AccountMode.ULTIMATE_IRONMAN
                    ? get(678) : get(681));
            return text.toString();
        }
        var missing = joinNatural(missingParts);
        if (mode == AccountMode.ULTIMATE_IRONMAN)
            text.append(" Acquire ").append(missing).append(get(679));
        else if (mode.usesGrandExchange())
        {
            text.append(" Buy ").append(missing).append(get(1446));
        }
        else if (mode.isGroupIronman())
        {
            text.append(" Source ").append(missing)
                    .append(useGroupStorage
                            ? get(1447)
                            : ".");
        }
        else
        {
            text.append(" Source ").append(missing).append(get(682));
        }
        return text.toString();
    }

    private static String requiredSummary(List<StagePlan> stages)
    {
        List<String> parts = new ArrayList<>();
        for (StagePlan stage : stages)
        {
            parts.add("about " + stage.rawNeeded + " "
                    + stage.stage.rawItemName.toLowerCase());
        }
        return joinNatural(parts);
    }

    private static String locationGuidance(QuestSnapshot quests)
    {
        if (quests != null
                && quests.statusOf(get(1862)) == QuestStatus.COMPLETE)
        {
            return get(683);
        }

        return get(685);
    }

    private static String joinNatural(List<String> parts)
    {
        if (parts == null || parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);

        var text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++)
        {
            if (i > 0)
            {
                text.append(i == parts.size() - 1 ? ", and " : ", ");
            }
            text.append(parts.get(i));
        }
        return text.toString();
    }

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static final class CookingStage
    {
        private final int startLevel;
        private final int endLevel;
        private final String foodName;
        private final String rawItemName;
        private final int xpEach;

        private CookingStage(
                int startLevel,
                int endLevel,
                String foodName,
                String rawItemName,
                int xpEach)
        {
            this.startLevel = startLevel;
            this.endLevel = endLevel;
            this.foodName = foodName;
            this.rawItemName = rawItemName;
            this.xpEach = xpEach;
        }
    }

    private static final class StagePlan
    {
        private final CookingStage stage;
        private final int targetLevel;
        private final int successfulCooks;
        private final int rawNeeded;

        private StagePlan(
                CookingStage stage,
                int targetLevel,
                int successfulCooks,
                int rawNeeded)
        {
            this.stage = stage;
            this.targetLevel = targetLevel;
            this.successfulCooks = successfulCooks;
            this.rawNeeded = rawNeeded;
        }
    }
}

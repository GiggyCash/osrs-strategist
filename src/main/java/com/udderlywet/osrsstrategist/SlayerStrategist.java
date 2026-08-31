package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;

/**
 * Property-driven Slayer master and current-task decisions.
 *
 * <p>The service never assigns a task verdict merely because of its ID. IDs
 * select reviewed metadata; XP value, resource value, burden, attention, risk,
 * point economy, readiness and live intent determine the decision.</p>
 */
@Singleton
public class SlayerStrategist
{
    private final SlayerMasterCatalog masters;
    private final SlayerTaskProfileCatalog mechanics;
    private final SlayerTaskStrategicCatalog strategy;
    private final SlayerGuidanceService guidanceService;
    private final SlayerRewardAdvisor rewardAdvisor;

    @Inject
    public SlayerStrategist(SlayerMasterCatalog masters,
            SlayerTaskProfileCatalog mechanics,
            SlayerTaskStrategicCatalog strategy,
            SlayerGuidanceService guidanceService,
            SlayerRewardAdvisor rewardAdvisor)
    {
        this.masters = masters == null ? new SlayerMasterCatalog() : masters;
        this.mechanics = mechanics == null
                ? new SlayerTaskProfileCatalog() : mechanics;
        this.strategy = strategy == null
                ? new SlayerTaskStrategicCatalog(this.mechanics) : strategy;
        this.guidanceService = guidanceService == null
                ? new SlayerGuidanceService(this.mechanics) : guidanceService;
        this.rewardAdvisor = rewardAdvisor == null
                ? new SlayerRewardAdvisor() : rewardAdvisor;
    }

    public SlayerStrategist()
    {
        this(new SlayerMasterCatalog(), new SlayerTaskProfileCatalog(),
                null, null, null);
    }

    public SlayerDecisionResult assess(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null) return null;
        AccountSnapshot account = context.data().account();
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || !AccountBuildPolicy.allowsSkill(account, Skill.SLAYER))
            return null;

        SlayerSnapshot slayer = context.data().slayer();
        if (slayer == null
                || slayer.getAssignmentState() == SlayerAssignmentState.UNKNOWN)
            return unknownAssignment();
        if (slayer.getAssignmentState() == SlayerAssignmentState.CHOICE_PENDING)
            return chooseMortimerOffer(context, slayer);
        if (slayer.getAssignmentState() == SlayerAssignmentState.NO_TASK)
            return chooseMaster(context, slayer);
        return evaluateTask(context, slayer);
    }

    private SlayerDecisionResult chooseMortimerOffer(StrategyContext context,
            SlayerSnapshot slayer)
    {
        SlayerMasterProfile master = masters.byId("mortimer");
        for (SlayerTaskOffer offer : slayer.getTaskOffers())
        {
            if (offer.getTaskName() == null || offer.getModifierName() == null
                    || strategy.profileFor(offer.getTaskName()) == null)
                return unresolvedMortimerChoice(master);
        }
        SlayerTaskOffer choice = slayer.getTaskOffers().stream()
                .max(Comparator.comparingDouble(o -> offerValue(o, context)))
                .orElse(null);
        if (choice == null) return unresolvedMortimerChoice(master);
        SlayerTaskStrategicProfile profile = strategy.profileFor(
                choice.getTaskName());
        double value = offerValue(choice, context);
        String modifier = describeModifier(choice);
        String reason = get(815)
                + get(826);
        Guidance guidance = new Guidance(
                "Select " + choice.getTaskName() + " with " + modifier
                        + get(837),
                get(848),
                get(859),
                reason);
        return new SlayerDecisionResult(SlayerAssignmentState.CHOICE_PENDING,
                null, master, profile, 62.0 + value,
                Confidence.VERIFIED, reason, guidance, null,
                null, choice);
    }

    private SlayerDecisionResult unresolvedMortimerChoice(
            SlayerMasterProfile master)
    {
        String reason = get(870);
        return new SlayerDecisionResult(SlayerAssignmentState.CHOICE_PENDING,
                SlayerTaskDecision.PREP_FIRST, master, null, 34.0,
                Confidence.CHECK_NEEDED, reason,
                new Guidance(
                        get(881),
                        get(889),
                        get(890),
                        reason));
    }

    private double offerValue(SlayerTaskOffer offer, StrategyContext context)
    {
        SlayerTaskStrategicProfile profile = strategy.profileFor(
                offer.getTaskName());
        if (profile == null) return -1000.0;
        double value = taskValue(profile, context);
        String modifier = normalize(offer.getModifierName());
        double magnitude = Math.min(3.0, Math.max(.5,
                offer.getModifierValue() / 10.0));
        double direction = offer.isNegativeModifier() ? -1.0 : 1.0;
        if (modifier.contains("xp"))
        {
            double fit = context.getStrategyMode() == StrategyMode.EFFICIENT
                    || context.getActiveGoal() == GoalType.SLAYER_85
                    ? 3.0 : 1.8;
            value += direction * magnitude * fit;
        }
        else if (modifier.contains("superior"))
            value += direction * magnitude
                    * (context.accountMode().isIronLike() ? 3.2 : 2.3);
        else if (modifier.contains("point"))
            value += direction * magnitude
                    * (slayerPoints(context) < 150 ? 3.0 : 1.6);
        else if (modifier.contains("clue"))
            value += direction * magnitude
                    * (context.accountMode().isIronLike() ? 2.4 : 1.2);
        else if (modifier.contains("quant"))
        {
            double taskFit = profile.getXpQuality() + profile.getResourceValue()
                    - profile.getCompletionBurden();
            double sessionFit = context.getSessionIntent() == SessionIntent.LONG_SESSION
                    ? 1.0 : context.getSessionIntent() == SessionIntent.QUICK_20_MIN
                            ? -1.0 : 0.0;
            value += direction * magnitude * (taskFit + sessionFit) * .7;
        }
        return value;
    }

    private static int slayerPoints(StrategyContext context)
    {
        SlayerSnapshot slayer = context.data().slayer();
        return slayer == null ? 0 : slayer.getPoints();
    }

    private static String describeModifier(SlayerTaskOffer offer)
    {
        String prefix = offer.isNegativeModifier() ? "the reduced " : "the ";
        return prefix + offer.getModifierName()
                + (offer.getModifierValue() > 0
                        ? " modifier (" + offer.getModifierValue() + ")"
                        : " modifier");
    }

    private SlayerDecisionResult chooseMaster(StrategyContext context,
            SlayerSnapshot slayer)
    {
        SlayerRewardAdvice rewardAdvice = rewardAdvisor.recommend(context, slayer);
        if (rewardAdvice != null) return rewardPurchase(rewardAdvice, slayer);

        List<SlayerMasterProfile> eligible = masters.eligible(context);
        SlayerMasterProfile choice = eligible.stream()
                .max(Comparator.comparingDouble(p -> masterScore(p, context, slayer)))
                .orElse(null);
        if (choice == null) return unknownAssignment();

        double score = masterScore(choice, context, slayer);
        int next = slayer.getTaskStreak() == null
                ? -1 : slayer.getTaskStreak() + 1;
        String milestone = next > 0 && SlayerPointEconomy.isBonusCompletion(next)
                ? " This is task " + next + get(816)
                : "";
        String reason = get(817)
                + get(818) + milestone;
        Guidance guidance = new Guidance(
                get(1494) + choice.getDisplayName()
                        + get(819),
                get(820),
                choice.getLocation() + ".",
                reason + (choice.isWilderness()
                        ? get(821)
                        : ""));
        return new SlayerDecisionResult(SlayerAssignmentState.NO_TASK, null,
                choice, null, score, Confidence.VERIFIED,
                reason, guidance);
    }

    private static SlayerDecisionResult rewardPurchase(
            SlayerRewardAdvice advice, SlayerSnapshot slayer)
    {
        SlayerReward reward = advice.getReward();
        int remaining = slayer.getPoints() - reward.getPointCost();
        String reason = advice.getReason()
                + get(822)
                + remaining + get(823);
        Guidance guidance = new Guidance(
                "Buy " + reward.getDisplayName() + " for "
                        + reward.getPointCost() + get(824),
                get(825)
                        + slayer.getPoints() + get(827),
                get(828),
                reason);
        return new SlayerDecisionResult(SlayerAssignmentState.NO_TASK, null,
                null, null, advice.getScore(), Confidence.VERIFIED,
                reason, guidance, null, reward);
    }

    private SlayerDecisionResult evaluateTask(StrategyContext context,
            SlayerSnapshot slayer)
    {
        GameData data = context.data();
        SlayerTaskProfile taskMechanics = mechanics.profileFor(slayer.getTaskName());
        SlayerTaskStrategicProfile taskStrategy = strategy.profileFor(
                slayer.getTaskName());
        SlayerMasterProfile master = masters.match(slayer.getMasterName());

        if (unsafeWilderness(context, slayer, master,
                strategy.isWildernessBound(slayer.getTaskName())))
            return wildernessAlternative(slayer, taskStrategy, master);

        Guidance base = guidanceService.build(data,
                data.account().getSkillLevel(Skill.SLAYER),
                Math.min(99, data.account().getSkillLevel(Skill.SLAYER) + 1),
                context.isUseGroupStorage());
        if (taskMechanics == null || taskStrategy == null || base == null)
            return unreviewedTask(slayer, master, taskStrategy, base);

        if (taskStrategy.isDirectEncounter())
        {
            PvmReadiness readiness = alternativeReadiness(data, taskStrategy);
            if (readiness != null && readiness.isReadyForRecommendation())
                return bossAlternative(slayer, master, taskStrategy);
            return encounterPreparation(slayer, master, taskStrategy,
                    readiness);
        }

        PvmReadiness alternative = alternativeReadiness(data, taskStrategy);
        if (alternative != null && alternative.isReadyForRecommendation()
                && alternativeWorthUsing(context, taskStrategy))
            return bossAlternative(slayer, master, taskStrategy);

        double value = taskValue(taskStrategy, context);
        boolean milestone = slayer.getTaskStreak() != null
                && SlayerPointEconomy.isBonusCompletion(slayer.getTaskStreak() + 1);
        Integer weight = master == null ? null
                : taskStrategy.weightFor(master.getId());

        // Decide whether the assignment is worth keeping before asking the
        // player to disturb their setup. A bad task should be skipped or
        // blocked, not preceded by pointless gear and supply preparation.
        if (!milestone && value <= -1.0 && weight != null && weight >= 8
                && slayer.hasKnownFreeBlockSlot()
                && slayer.getPoints() >= master.getBlockCost())
            return block(slayer, master, taskStrategy, value, weight);

        if (!milestone && value < 0.5
                && SlayerPointEconomy.hasSustainableSkipBalance(
                        slayer.getPoints(), master == null
                                ? SlayerPointEconomy.SKIP_COST
                                : master.getCancelCost()))
            return skip(slayer, master, taskStrategy, value);

        ItemIndex items = new ItemIndex(data,
                context.isUseGroupStorage());
        if (!taskMechanics.getRequiredProtection().isEmpty()
                && firstReadyItem(items, taskMechanics.getRequiredProtection(),
                        taskStrategy.getRequiredItemUse()) == null)
            return preparation(slayer, master, taskStrategy, base,
                    taskStrategy.getRequiredItemUse() == SlayerRequiredItemUse.EQUIPPED
                            ? get(829)
                            : get(830));

        String weapon = observedCombatWeapon(data.equipment());
        if (weapon == null)
            return preparation(slayer, master, taskStrategy, base,
                    get(831));
        CombatStyle observedStyle = weaponStyle(weapon);
        if (taskStrategy.getRequiredCombatStyle() != null
                && observedStyle != taskStrategy.getRequiredCombatStyle())
            return preparation(slayer, master, taskStrategy, base,
                    "The equipped " + weapon + get(1495)
                            + styleName(taskStrategy.getRequiredCombatStyle())
                            + get(1496));

        if (requiresCarriedHealing(taskStrategy)
                && carriedHealingName(data.inventory()) == null)
            return supplyPreparation(context, slayer, master, taskStrategy,
                    base);

        base = concreteTaskGuidance(base, taskMechanics, taskStrategy,
                items, weapon, observedStyle, slayer, data.inventory());

        String reason = milestone
                ? get(832)
                : get(833);
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.DO, master, taskStrategy, 58.0 + value,
                Confidence.VERIFIED, reason, base);
    }

    private double masterScore(SlayerMasterProfile master,
            StrategyContext context, SlayerSnapshot slayer)
    {
        double score = master.getExperiencePotential() * 40.0
                + master.getSupplyValue() * 12.0
                + master.getNormalPoints() * .6
                - master.getSetupBurden() * 8.0
                - master.getLocationConstraint() * 8.0;
        if (context.getStrategyMode() == StrategyMode.EFFICIENT)
            score += master.getExperiencePotential() * 18.0
                    - master.getSetupBurden() * 4.0;
        else if (context.getStrategyMode() == StrategyMode.RELAXED)
            score -= master.getSetupBurden() * 10.0
                    + master.getLocationConstraint() * 8.0;

        if (context.getSessionIntent() == SessionIntent.QUICK_20_MIN)
            score -= master.getSetupBurden() * 15.0
                    + master.getLocationConstraint() * 8.0;
        else if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
            score += master.getExperiencePotential() * 6.0;

        if (context.accountMode().isIronLike())
            score += master.getSupplyValue() * 10.0;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
            score -= master.getSetupBurden() * 8.0;
        if (master.isWilderness()
                && AccountModePolicy.isRiskSensitive(context.accountMode()))
            score -= 100.0;

        if (context.getActiveGoal() == GoalType.SLAYER_85)
            score += master.getExperiencePotential() * 14.0;
        else if (context.getActiveGoal() == GoalType.MAX)
            score += master.getExperiencePotential() * 5.0;

        if (slayer.getTaskStreak() != null)
        {
            int next = slayer.getTaskStreak() + 1;
            if (SlayerPointEconomy.isBonusCompletion(next))
                score += master.pointsForCompletion(next)
                        - master.getNormalPoints();
        }
        score += context.preferenceProfile().weightFor(
                "slayer:master:" + master.getId()) * 10.0;
        score += reviewedPoolValue(master, context) * 1.25;
        return score;
    }

    /**
     * Uses only reviewed task/master weights. Unknown or conditional long-tail
     * assignments contribute nothing instead of being assigned guessed value.
     */
    private double reviewedPoolValue(SlayerMasterProfile master,
            StrategyContext context)
    {
        double weightedValue = 0.0;
        int reviewedWeight = 0;
        for (SlayerTaskStrategicProfile task : strategy.all())
        {
            Integer weight = task.weightFor(master.getId());
            if (weight == null || weight <= 0) continue;
            weightedValue += weight * taskValue(task, context);
            reviewedWeight += weight;
        }
        // A thin partial table must not masquerade as the master's full pool.
        if (reviewedWeight < 50) return 0.0;
        return weightedValue / reviewedWeight;
    }

    private static double taskValue(SlayerTaskStrategicProfile task,
            StrategyContext context)
    {
        double resourceMultiplier = context.accountMode().isIronLike()
                ? 1.7 : 1.2;
        double value = task.getXpQuality() * 2.3
                + task.getResourceValue() * resourceMultiplier
                - task.getCompletionBurden() * 1.5
                - task.getSetupBurden() * .8;
        if (context.getStrategyMode() == StrategyMode.EFFICIENT)
            value += task.getXpQuality() * .7
                    - task.getCompletionBurden() * .35;
        if (context.getStrategyMode() == StrategyMode.RELAXED)
        {
            value -= task.getSetupBurden() * .35;
            if (task.getAttention() == AttentionLevel.LOW
                    || task.getAttention() == AttentionLevel.AFK) value += 4.5;
        }
        if (context.getSessionIntent() == SessionIntent.QUICK_20_MIN)
            value -= task.getCompletionBurden() * 1.1
                    + task.getSetupBurden() * .35;
        if (context.getSessionIntent() == SessionIntent.AFK)
            value += task.getAttention() == AttentionLevel.AFK ? 5.0
                    : task.getAttention() == AttentionLevel.LOW ? 4.0 : -3.0;
        if (context.getSessionIntent() == SessionIntent.LONG_SESSION)
            value += task.getXpQuality() * .4;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
            value -= task.getSetupBurden() * .6;
        if (context.getActiveGoal() == GoalType.SLAYER_85)
            value += task.getXpQuality() * .8;
        if (task.getInherentRisk() == RiskLevel.MEDIUM) value -= 3.0;
        else if (task.getInherentRisk() == RiskLevel.HIGH) value -= 8.0;
        else if (task.getInherentRisk() == RiskLevel.IRREVERSIBLE) value -= 20.0;
        return value;
    }

    private static boolean unsafeWilderness(StrategyContext context,
            SlayerSnapshot slayer, SlayerMasterProfile master,
            boolean taskIsWildernessBound)
    {
        boolean wilderness = master != null && master.isWilderness();
        wilderness |= taskIsWildernessBound;
        String area = normalize(slayer.getTaskLocation());
        wilderness |= area.contains("wilderness") || area.contains("revenant caves");
        return wilderness && (!context.isAllowWildernessMethods()
                || AccountModePolicy.isRiskSensitive(context.accountMode()));
    }

    private static SlayerDecisionResult wildernessAlternative(
            SlayerSnapshot slayer, SlayerTaskStrategicProfile task,
            SlayerMasterProfile master)
    {
        if (slayer.getPoints() < SlayerPointEconomy.SKIP_COST)
        {
            Guidance guidance = new Guidance(
                    get(834),
                    get(835),
                    get(1497),
                    "Only " + slayer.getPoints() + get(836));
            return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                    SlayerTaskDecision.PREP_FIRST, master, task, 48.0,
                    Confidence.CHECK_NEEDED,
                    get(838),
                    guidance);
        }
        Guidance guidance = new Guidance(
                get(839),
                get(1498) + slayer.getPoints()
                        + get(840),
                get(841),
                get(842));
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.ALTERNATIVE, master, task, 60.0,
                Confidence.VERIFIED,
                get(843),
                guidance);
    }

    private static SlayerDecisionResult unreviewedTask(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            Guidance base)
    {
        Guidance guidance = base == null
                ? new Guidance(
                        get(844),
                        get(845),
                        get(846),
                        get(847))
                : new Guidance(
                        get(849),
                        base.getSupplies(), base.getLocation(),
                        get(850) + base.getNote());
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 40.0,
                Confidence.CHECK_NEEDED,
                get(851),
                guidance);
    }

    private static SlayerDecisionResult preparation(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            Guidance base, String reason)
    {
        Guidance guidance = new Guidance(
                get(1499) + slayer.getTaskName()
                        + get(852),
                base.getSupplies(), base.getLocation(), base.getNote());
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 48.0,
                Confidence.CHECK_NEEDED, reason, guidance);
    }

    private static SlayerDecisionResult supplyPreparation(
            StrategyContext context, SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            Guidance base)
    {
        GameData data = context.data();
        String storedFood = storedHealingName(data, context.isUseGroupStorage());
        String action;
        String supplies;
        AccountMode mode = context.accountMode();
        if (storedFood != null)
        {
            action = get(1500) + storedFood
                    + " for one " + slayer.getTaskName()
                    + get(853);
            supplies = "Keep " + storedFood
                    + get(854);
        }
        else if (mode == AccountMode.MAIN)
        {
            action = get(855)
                    + slayer.getTaskName() + ".";
            supplies = get(856);
        }
        else
        {
            String route = selfSourcedFoodRoute(data.account());
            action = route + get(857)
                    + slayer.getTaskName() + ".";
            supplies = get(858);
        }
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 50.0,
                Confidence.CHECK_NEEDED,
                get(860),
                new Guidance(action, supplies,
                        base.getLocation(), base.getNote()));
    }

    private static SlayerDecisionResult bossAlternative(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task)
    {
        Guidance guidance = new Guidance(
                get(1501) + task.getAlternativeName()
                        + get(1502) + slayer.getTaskName()
                        + get(861),
                get(862),
                task.getAlternativeLocation() + ".",
                get(863));
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.ALTERNATIVE, master, task, 64.0,
                Confidence.VERIFIED,
                get(864),
                guidance, task.getAlternativeName());
    }

    private static SlayerDecisionResult encounterPreparation(
            SlayerSnapshot slayer, SlayerMasterProfile master,
            SlayerTaskStrategicProfile task, PvmReadiness readiness)
    {
        String missing = readiness == null || readiness.getMissingRequirements().isEmpty()
                ? get(865)
                : "Resolve: " + String.join(", ", readiness.getMissingRequirements()) + ".";
        Guidance guidance = new Guidance(
                "Prepare for " + task.getAlternativeName()
                        + get(866) + missing,
                get(867),
                task.getAlternativeLocation() + ".",
                get(868));
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 49.0,
                Confidence.CHECK_NEEDED,
                get(869),
                guidance);
    }

    private static SlayerDecisionResult block(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            double value, int weight)
    {
        Guidance guidance = new Guidance(
                "Block " + slayer.getTaskName() + " in "
                        + master.getDisplayName() + get(871),
                "This costs " + master.getBlockCost() + get(872),
                get(1503) + master.getDisplayName()
                        + get(1504),
                get(1505) + weight + get(873));
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.BLOCK, master, task, 59.0 - value,
                Confidence.VERIFIED,
                get(874),
                guidance);
    }

    private static SlayerDecisionResult skip(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            double value)
    {
        String who = master == null ? "any Slayer master"
                : master.getDisplayName();
        int cost = master == null ? SlayerPointEconomy.SKIP_COST
                : master.getCancelCost();
        Guidance guidance = new Guidance(
                "Spend " + cost + get(1506) + slayer.getTaskName()
                        + get(1507),
                get(1498) + slayer.getPoints()
                        + get(1508) + cost
                        + get(1509),
                get(1510) + who + ".",
                get(875));
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.SKIP, master, task, 56.0 - value,
                Confidence.VERIFIED,
                get(876),
                guidance);
    }

    private static SlayerDecisionResult unknownAssignment()
    {
        return new SlayerDecisionResult(SlayerAssignmentState.UNKNOWN,
                SlayerTaskDecision.PREP_FIRST, null, null, 32.0,
                Confidence.CHECK_NEEDED,
                get(877),
                new Guidance(
                        get(878),
                        get(879),
                        get(880),
                        get(882)));
    }

    private static PvmReadiness alternativeReadiness(GameData data,
            SlayerTaskStrategicProfile task)
    {
        if (data == null || data.pvm() == null || task == null
                || task.getAlternativeActivityId() == null) return null;
        PvmReadiness readiness = data.pvm().readinessFor(
                task.getAlternativeActivityId());
        if (readiness == null && task.getAlternativeActivityId().startsWith("pvm:"))
            readiness = data.pvm().readinessFor(
                    task.getAlternativeActivityId().substring(4));
        return readiness;
    }

    private static boolean alternativeWorthUsing(StrategyContext context,
            SlayerTaskStrategicProfile task)
    {
        if (AccountModePolicy.isRiskSensitive(context.accountMode()))
            return false;
        return context.getActiveGoal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS
                || context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY
                || (context.accountMode().isIronLike()
                    && task.getResourceValue() <= 2);
    }

    private static Guidance concreteTaskGuidance(
            Guidance base, SlayerTaskProfile mechanics,
            SlayerTaskStrategicProfile strategy, ItemIndex items,
            String weapon, CombatStyle observedStyle, SlayerSnapshot slayer,
            ItemsState inventory)
    {
        String required = firstReadyItem(items,
                mechanics.getRequiredProtection(), strategy.getRequiredItemUse());
        StringBuilder supplies = new StringBuilder("Equip ").append(weapon);
        if (required != null && !required.equalsIgnoreCase(weapon))
            supplies.append(" and keep ").append(required)
                    .append(strategy.getRequiredItemUse()
                            == SlayerRequiredItemUse.EQUIPPED
                            ? " equipped" : get(1511));
        String healing = carriedHealingName(inventory);
        supplies.append(". ");
        if (healing != null)
            supplies.append(get(1512)).append(healing)
                    .append(get(883));
        else
            supplies.append(get(884));
        String action = get(1513) + slayer.getRemaining() + " "
                + slayer.getTaskName() + " using " + weapon + " ("
                + styleName(observedStyle) + ").";
        String technique = concreteTechnique(mechanics.getStyleGuidance());
        if (!technique.isEmpty()) action += " " + technique;
        return new Guidance(action, supplies.toString(),
                base.getLocation(), base.getNote());
    }

    /**
     * Once live equipment has selected a weapon and style, do not append an
     * older catalog sentence that delegates that same choice back to the
     * player. Task-specific execution such as a safespot or finishing item is
     * still retained when it does not contradict the observed loadout.
     */
    private static String concreteTechnique(String guidance)
    {
        String normalized = normalize(guidance);
        if (normalized.isEmpty()
                || normalized.startsWith("use any ")
                || normalized.startsWith("choose ")
                || normalized.startsWith(get(1514))
                || normalized.startsWith(get(1515))
                || normalized.startsWith(get(1516))
                || normalized.startsWith(get(1517))
                || normalized.contains(get(1518))
                || normalized.contains(get(1519)))
            return "";
        return guidance.trim();
    }

    private static boolean requiresCarriedHealing(
            SlayerTaskStrategicProfile task)
    {
        return task.getInherentRisk() != RiskLevel.LOW
                || task.getCompletionBurden() >= 3
                || task.getSetupBurden() >= 3;
    }

    private static String carriedHealingName(ItemsState inventory)
    {
        return inventory == null ? null : healingName(inventory.getItems());
    }

    private static String storedHealingName(GameData data,
            boolean useGroupStorage)
    {
        if (data == null || data.account() == null) return null;
        AccountMode mode = AccountMode.fromTypeCode(
                data.account().getAccountTypeCode());
        if (mode != AccountMode.ULTIMATE_IRONMAN && data.bank() != null)
        {
            String found = healingName(data.bank().getItems());
            if (found != null) return found;
        }
        if (useGroupStorage && mode.isGroupIronman()
                && data.groupStorage() != null
                && data.groupStorage().isObserved())
        {
            String found = healingName(data.groupStorage().getItems());
            if (found != null) return found;
        }
        if (data.storage() != null)
        {
            for (java.util.Map.Entry<StorageCapability, List<ItemState>>
                    entry : data.storage().getObservedContents().entrySet())
            {
                if (!data.storage().verified(entry.getKey())) continue;
                if (mode == AccountMode.ULTIMATE_IRONMAN
                        && UimStorageMechanics.isRestrictedRetrieval(
                                entry.getKey()))
                    continue;
                String found = healingName(entry.getValue());
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Conservative edible-name evidence; raw and burnt items never count. */
    private static String healingName(Iterable<ItemState> observed)
    {
        if (observed == null) return null;
        String best = null;
        int bestRank = 0;
        for (ItemState item : observed)
        {
            if (item == null || item.getQuantity() <= 0) continue;
            String name = normalize(item.getName());
            if (name.startsWith("raw ") || name.startsWith("burnt ")) continue;
            int rank = healingRank(name);
            if (rank > bestRank)
            {
                bestRank = rank;
                best = item.getName();
            }
        }
        return best;
    }

    private static int healingRank(String name)
    {
        if (name.equals(get(1520))
                || name.equals("dark crab") || name.equals("anglerfish")
                || name.equals("manta ray")) return 5;
        if (name.equals("shark") || name.equals("sea turtle")
                || name.equals(get(1521))
                || name.startsWith("saradomin brew(")) return 4;
        if (name.equals("karambwan") || name.equals("tuna potato")
                || name.equals("swordfish") || name.equals("monkfish")) return 3;
        if (name.equals("lobster") || name.equals("bass")
                || name.endsWith(" pizza")) return 2;
        if (name.equals("salmon") || name.equals("trout")
                || name.equals("cake") || name.equals("chocolate cake")
                || name.equals("slice of cake")
                || name.equals("chocolate slice")) return 1;
        return 0;
    }

    private static String selfSourcedFoodRoute(AccountSnapshot account)
    {
        int fishing = account == null ? 1 : account.getSkillLevel(Skill.FISHING);
        int cooking = account == null ? 1 : account.getSkillLevel(Skill.COOKING);
        if (fishing >= 50 && cooking >= 45)
            return get(885);
        if (fishing >= 40 && cooking >= 40)
            return get(886);
        if (fishing >= 20 && cooking >= 15)
            return get(887);
        return get(888);
    }

    private static String firstReadyItem(ItemIndex items,
            List<String> candidates, SlayerRequiredItemUse use)
    {
        for (String candidate : candidates)
        {
            boolean ready = use == SlayerRequiredItemUse.EQUIPPED
                    ? items.equipped(candidate) : items.has(candidate);
            if (ready) return candidate;
        }
        return null;
    }

    private static String observedCombatWeapon(ItemsState equipment)
    {
        if (equipment == null) return null;
        for (ItemState item : equipment.getEquippedItems())
        {
            if (item == null || item.getSlotIndex()
                    != EquipmentInventorySlot.WEAPON.getSlotIdx()) continue;
            String name = normalize(item.getName());
            if (containsAny(name, "scimitar", "sword", "whip", "mace",
                    "axe", "halberd", "spear", "hasta", "fang", "scythe",
                    "maul", "bludgeon", "lance", "bow", "crossbow",
                    "blowpipe", "ballista", "atlatl", "staff", "wand",
                    "trident", "sceptre", "scepter", "shadow"))
                return item.getName();
        }
        return null;
    }

    private static CombatStyle weaponStyle(String weapon)
    {
        String name = normalize(weapon);
        if (containsAny(name, "bow", "crossbow", "blowpipe", "ballista",
                "atlatl")) return CombatStyle.RANGED;
        if (containsAny(name, "staff", "wand", "trident", "sceptre",
                "scepter", "shadow")) return CombatStyle.MAGIC;
        if (containsAny(name, "mace", "maul", "bludgeon"))
            return CombatStyle.MELEE_CRUSH;
        if (containsAny(name, "spear", "hasta", "fang", "lance"))
            return CombatStyle.MELEE_STAB;
        return CombatStyle.MELEE_SLASH;
    }

    private static String styleName(CombatStyle style)
    {
        if (style == null) return "combat";
        switch (style)
        {
            case MAGIC: return "Magic";
            case RANGED: return "Ranged";
            case MELEE_CRUSH: return "Melee crush";
            case MELEE_STAB: return "Melee stab";
            case MELEE_SLASH: return "Melee slash";
            default: return "hybrid";
        }
    }

    private static boolean containsAny(String value, String... terms)
    {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

package compass;
import static net.runelite.api.Skill.*;
import static java.lang.Math.*;
import static compass.Text.get;

import java.util.*;
import javax.inject.*;
import net.runelite.api.*;

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
    private final SlayerRewardAdvisor rewardAdvisor;

    @Inject
    public SlayerStrategist(SlayerMasterCatalog masters,
            SlayerTaskProfileCatalog mechanics,
            SlayerTaskStrategicCatalog strategy,
            SlayerRewardAdvisor rewardAdvisor)
    {
        this.masters = masters == null ? new SlayerMasterCatalog() : masters;
        this.mechanics = mechanics == null
                ? new SlayerTaskProfileCatalog() : mechanics;
        this.strategy = strategy == null
                ? new SlayerTaskStrategicCatalog(this.mechanics) : strategy;
        this.rewardAdvisor = rewardAdvisor == null
                ? new SlayerRewardAdvisor() : rewardAdvisor;
    }

    public SlayerStrategist()
    {
        this(new SlayerMasterCatalog(), new SlayerTaskProfileCatalog(),
                null, null);
    }

    public SlayerDecisionResult assess(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null) return null;
        var account = context.data().account();
        if (account.membership() != Membership.P2P
                || !AccountBuildPolicy.allowsSkill(account, SLAYER))
            return null;

        var slayer = context.data().slayer();
        if (slayer == null
                || slayer.getAssignmentState() == SlayerState.UNKNOWN)
            return unknownAssignment();
        if (slayer.getAssignmentState() == SlayerState.CHOICE_PENDING)
            return chooseMortimerOffer(context, slayer);
        if (slayer.getAssignmentState() == SlayerState.NO_TASK)
            return chooseMaster(context, slayer);
        return evaluateTask(context, slayer);
    }

    private SlayerDecisionResult chooseMortimerOffer(StrategyContext context,
            SlayerSnapshot slayer)
    {
        var master = masters.byId("mortimer");
        for (SlayerTaskOffer offer : slayer.getTaskOffers())
        {
            if (offer.taskName == null || offer.getModifierName() == null
                    || strategy.profileFor(offer.taskName) == null)
                return unresolvedMortimerChoice(master);
        }
        SlayerTaskOffer choice = slayer.getTaskOffers().stream()
                .max(Comparator.comparingDouble(o -> offerValue(o, context)))
                .orElse(null);
        if (choice == null) return unresolvedMortimerChoice(master);
        SlayerStrategy profile = strategy.profileFor(
                choice.taskName);
        var value = offerValue(choice, context);
        var modifier = describeModifier(choice);
        String reason = get(815)
                + get(826);
        Guidance guidance = new Guidance(
                "Select " + choice.taskName + " with " + modifier
                        + get(837),
                get(848),
                get(859),
                reason);
        return new SlayerDecisionResult(SlayerState.CHOICE_PENDING,
                null, master, profile, 62.0 + value,
                Confidence.VERIFIED, reason, guidance, null,
                null, choice);
    }

    private SlayerDecisionResult unresolvedMortimerChoice(
            SlayerMasterProfile master)
    {
        var reason = get(870);
        return new SlayerDecisionResult(SlayerState.CHOICE_PENDING,
                SlayerDecision.PREP_FIRST, master, null, 34.0,
                Confidence.CHECK_NEEDED, reason,
                new Guidance(
                        get(881),
                        get(889),
                        get(890),
                        reason), null, null, null);
    }

    private double offerValue(SlayerTaskOffer offer, StrategyContext context)
    {
        SlayerStrategy profile = strategy.profileFor(
                offer.taskName);
        if (profile == null) return -1000.0;
        var value = taskValue(profile, context);
        var modifier = Names.lower(offer.getModifierName());
        double magnitude = min(3.0, max(.5,
                offer.getModifierValue() / 10.0));
        var direction = offer.isNegativeModifier() ? -1.0 : 1.0;
        if (modifier.contains("xp"))
        {
            double fit = context.mode() == StrategyMode.EFFICIENT
                    || context.goal() == GoalType.SLAYER_85
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
            double taskFit = profile.xpQuality + profile.getResourceValue()
                    - profile.completionBurden;
            double sessionFit = context.intent() == SessionIntent.LONG_SESSION
                    ? 1.0 : context.intent() == SessionIntent.QUICK_20_MIN
                            ? -1.0 : 0.0;
            value += direction * magnitude * (taskFit + sessionFit) * .7;
        }
        return value;
    }

    private static int slayerPoints(StrategyContext context)
    {
        var slayer = context.data().slayer();
        return slayer == null ? 0 : slayer.getPoints();
    }

    private static String describeModifier(SlayerTaskOffer offer)
    {
        var prefix = offer.isNegativeModifier() ? "the reduced " : "the ";
        return prefix + offer.getModifierName()
                + (offer.getModifierValue() > 0
                        ? " modifier (" + offer.getModifierValue() + ")"
                        : " modifier");
    }

    private SlayerDecisionResult chooseMaster(StrategyContext context,
            SlayerSnapshot slayer)
    {
        var rewardAdvice = rewardAdvisor.recommend(context, slayer);
        if (rewardAdvice != null) return rewardPurchase(rewardAdvice, slayer);

        var eligible = masters.eligible(context);
        SlayerMasterProfile choice = eligible.stream()
                .max(Comparator.comparingDouble(p -> masterScore(p, context, slayer)))
                .orElse(null);
        if (choice == null) return unknownAssignment();

        var score = masterScore(choice, context, slayer);
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
                choice.location + ".",
                reason + (choice.wilderness
                        ? get(821)
                        : ""));
        return new SlayerDecisionResult(SlayerState.NO_TASK, null,
                choice, null, score, Confidence.VERIFIED,
                reason, guidance, null, null, null);
    }

    private static SlayerDecisionResult rewardPurchase(
            SlayerRewardAdvice advice, SlayerSnapshot slayer)
    {
        var reward = advice.getReward();
        var remaining = slayer.getPoints() - reward.getPointCost();
        String reason = advice.reason
                + get(822)
                + remaining + get(823);
        Guidance guidance = new Guidance(
                "Buy " + reward.getDisplayName() + " for "
                        + reward.getPointCost() + get(824),
                get(825)
                        + slayer.getPoints() + get(827),
                get(828),
                reason);
        return new SlayerDecisionResult(SlayerState.NO_TASK, null,
                null, null, advice.score, Confidence.VERIFIED,
                reason, guidance, null, reward, null);
    }

    private SlayerDecisionResult evaluateTask(StrategyContext context,
            SlayerSnapshot slayer)
    {
        var data = context.data();
        var taskMechanics = mechanics.profileFor(slayer.taskName);
        SlayerStrategy taskStrategy = strategy.profileFor(
                slayer.taskName);
        var master = masters.match(slayer.getMasterName());

        if (unsafeWilderness(context, slayer, master,
                strategy.isWildernessBound(slayer.taskName)))
            return wildernessAlternative(slayer, taskStrategy, master);

        Guidance base = taskGuidance(data, taskMechanics,
                context.usesGroupStorage());
        if (taskMechanics == null || taskStrategy == null || base == null)
            return unreviewedTask(slayer, master, taskStrategy, base);

        if (taskStrategy.isDirectEncounter())
        {
            var readiness = alternativeReadiness(data, taskStrategy);
            if (readiness != null && readiness.isReadyForRecommendation())
                return bossAlternative(slayer, master, taskStrategy);
            return encounterPreparation(slayer, master, taskStrategy,
                    readiness);
        }

        var alternative = alternativeReadiness(data, taskStrategy);
        if (alternative != null && alternative.isReadyForRecommendation()
                && alternativeWorthUsing(context, taskStrategy))
            return bossAlternative(slayer, master, taskStrategy);

        var value = taskValue(taskStrategy, context);
        boolean milestone = slayer.getTaskStreak() != null
                && SlayerPointEconomy.isBonusCompletion(slayer.getTaskStreak() + 1);
        Integer weight = master == null ? null
                : taskStrategy.weightFor(master.id);

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
                context.usesGroupStorage());
        if (!taskMechanics.requiredProtection.isEmpty()
                && firstReadyItem(items, taskMechanics.requiredProtection,
                        taskStrategy.getRequiredItemUse()) == null)
            return preparation(slayer, master, taskStrategy, base,
                    taskStrategy.getRequiredItemUse() == SlayerRequiredItemUse.EQUIPPED
                            ? get(829)
                            : get(830));

        var weapon = observedCombatWeapon(data.equipment());
        if (weapon == null)
            return preparation(slayer, master, taskStrategy, base,
                    get(831));
        var observedStyle = weaponStyle(weapon);
        if (taskStrategy.getRequiredCombatStyle() != null
                && observedStyle != taskStrategy.getRequiredCombatStyle())
            return preparation(slayer, master, taskStrategy, base,
                    "The equipped " + weapon + get(1495)
                            + styleName(taskStrategy.getRequiredCombatStyle())
                            + get(1496));

        if (requiresCarriedHealing(taskStrategy)
                && items.bestInventoryName(SlayerStrategist::healingRank) == null)
            return supplyPreparation(context, slayer, master, taskStrategy,
                    base);

        base = concreteTaskGuidance(base, taskMechanics, taskStrategy,
                items, weapon, observedStyle, slayer, data.inventory());

        String reason = milestone
                ? get(832)
                : get(833);
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.DO, master, taskStrategy, 58.0 + value,
                Confidence.VERIFIED, reason, base, null, null, null);
    }

    private double masterScore(SlayerMasterProfile master,
            StrategyContext context, SlayerSnapshot slayer)
    {
        double score = master.experiencePotential * 40.0
                + master.getSupplyValue() * 12.0
                + master.getNormalPoints() * .6
                - master.setupBurden * 8.0
                - master.getLocationConstraint() * 8.0;
        if (context.mode() == StrategyMode.EFFICIENT)
            score += master.experiencePotential * 18.0
                    - master.setupBurden * 4.0;
        else if (context.mode() == StrategyMode.RELAXED)
            score -= master.setupBurden * 10.0
                    + master.getLocationConstraint() * 8.0;

        if (context.intent() == SessionIntent.QUICK_20_MIN)
            score -= master.setupBurden * 15.0
                    + master.getLocationConstraint() * 8.0;
        else if (context.intent() == SessionIntent.LONG_SESSION)
            score += master.experiencePotential * 6.0;

        if (context.accountMode().isIronLike())
            score += master.getSupplyValue() * 10.0;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
            score -= master.setupBurden * 8.0;
        if (master.wilderness
                && AccountModePolicy.isRiskSensitive(context.accountMode()))
            score -= 100.0;

        if (context.goal() == GoalType.SLAYER_85)
            score += master.experiencePotential * 14.0;
        else if (context.goal() == GoalType.MAX)
            score += master.experiencePotential * 5.0;

        if (slayer.getTaskStreak() != null)
        {
            var next = slayer.getTaskStreak() + 1;
            if (SlayerPointEconomy.isBonusCompletion(next))
                score += master.pointsForCompletion(next)
                        - master.getNormalPoints();
        }
        score += context.preferenceProfile().weightFor(
                "slayer:master:" + master.id) * 10.0;
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
        var weightedValue = 0.0;
        var reviewedWeight = 0;
        for (SlayerStrategy task : strategy.all())
        {
            var weight = task.weightFor(master.id);
            if (weight == null || weight <= 0) continue;
            weightedValue += weight * taskValue(task, context);
            reviewedWeight += weight;
        }
        // A thin partial table must not masquerade as the master's full pool.
        if (reviewedWeight < 50) return 0.0;
        return weightedValue / reviewedWeight;
    }

    private static double taskValue(SlayerStrategy task,
            StrategyContext context)
    {
        double resourceMultiplier = context.accountMode().isIronLike()
                ? 1.7 : 1.2;
        double value = task.xpQuality * 2.3
                + task.getResourceValue() * resourceMultiplier
                - task.completionBurden * 1.5
                - task.setupBurden * .8;
        if (context.mode() == StrategyMode.EFFICIENT)
            value += task.xpQuality * .7
                    - task.completionBurden * .35;
        if (context.mode() == StrategyMode.RELAXED)
        {
            value -= task.setupBurden * .35;
            if (task.attention == AttentionLevel.LOW
                    || task.attention == AttentionLevel.AFK) value += 4.5;
        }
        if (context.intent() == SessionIntent.QUICK_20_MIN)
            value -= task.completionBurden * 1.1
                    + task.setupBurden * .35;
        if (context.intent() == SessionIntent.AFK)
            value += task.attention == AttentionLevel.AFK ? 5.0
                    : task.attention == AttentionLevel.LOW ? 4.0 : -3.0;
        if (context.intent() == SessionIntent.LONG_SESSION)
            value += task.xpQuality * .4;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
            value -= task.setupBurden * .6;
        if (context.goal() == GoalType.SLAYER_85)
            value += task.xpQuality * .8;
        if (task.inherentRisk == RiskLevel.MEDIUM) value -= 3.0;
        else if (task.inherentRisk == RiskLevel.HIGH) value -= 8.0;
        else if (task.inherentRisk == RiskLevel.IRREVERSIBLE) value -= 20.0;
        return value;
    }

    private static boolean unsafeWilderness(StrategyContext context,
            SlayerSnapshot slayer, SlayerMasterProfile master,
            boolean taskIsWildernessBound)
    {
        var wilderness = master != null && master.wilderness;
        wilderness |= taskIsWildernessBound;
        var area = Names.lower(slayer.getTaskLocation());
        wilderness |= area.contains("wilderness") || area.contains("revenant caves");
        return wilderness && (!context.allowsWilderness()
                || AccountModePolicy.isRiskSensitive(context.accountMode()));
    }

    private static SlayerDecisionResult wildernessAlternative(
            SlayerSnapshot slayer, SlayerStrategy task,
            SlayerMasterProfile master)
    {
        if (slayer.getPoints() < SlayerPointEconomy.SKIP_COST)
        {
            Guidance guidance = new Guidance(
                    get(834),
                    get(835),
                    get(1497),
                    "Only " + slayer.getPoints() + get(836));
            return new SlayerDecisionResult(SlayerState.ASSIGNED,
                    SlayerDecision.PREP_FIRST, master, task, 48.0,
                    Confidence.CHECK_NEEDED,
                    get(838),
                    guidance, null, null, null);
        }
        Guidance guidance = new Guidance(
                get(839),
                get(1498) + slayer.getPoints()
                        + get(840),
                get(841),
                get(842));
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.ALTERNATIVE, master, task, 60.0,
                Confidence.VERIFIED,
                get(843),
                guidance, null, null, null);
    }

    private static SlayerDecisionResult unreviewedTask(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task,
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
                        base.supplies, base.location,
                        get(850) + base.note);
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.PREP_FIRST, master, task, 40.0,
                Confidence.CHECK_NEEDED,
                get(851),
                guidance, null, null, null);
    }

    private static SlayerDecisionResult preparation(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task,
            Guidance base, String reason)
    {
        Guidance guidance = new Guidance(
                get(1499) + slayer.taskName
                        + get(852),
                base.supplies, base.location, base.note);
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.PREP_FIRST, master, task, 48.0,
                Confidence.CHECK_NEEDED, reason, guidance, null, null, null);
    }

    private static SlayerDecisionResult supplyPreparation(
            StrategyContext context, SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task,
            Guidance base)
    {
        var data = context.data();
        var storedFood = new ItemIndex(data, context.usesGroupStorage())
                .bestName(SlayerStrategist::healingRank);
        String action;
        String supplies;
        var mode = context.accountMode();
        if (storedFood != null)
        {
            action = get(1500) + storedFood
                    + " for one " + slayer.taskName
                    + get(853);
            supplies = "Keep " + storedFood
                    + get(854);
        }
        else if (mode == AccountMode.MAIN)
        {
            action = get(855)
                    + slayer.taskName + ".";
            supplies = get(856);
        }
        else
        {
            var route = selfSourcedFoodRoute(data.account());
            action = route + get(857)
                    + slayer.taskName + ".";
            supplies = get(858);
        }
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.PREP_FIRST, master, task, 50.0,
                Confidence.CHECK_NEEDED,
                get(860),
                new Guidance(action, supplies,
                        base.location, base.note), null, null, null);
    }

    private static SlayerDecisionResult bossAlternative(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task)
    {
        Guidance guidance = new Guidance(
                get(1501) + task.getAlternativeName()
                        + get(1502) + slayer.taskName
                        + get(861),
                get(862),
                task.getAlternativeLocation() + ".",
                get(863));
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.ALTERNATIVE, master, task, 64.0,
                Confidence.VERIFIED,
                get(864),
                guidance, task.getAlternativeName(), null, null);
    }

    private static SlayerDecisionResult encounterPreparation(
            SlayerSnapshot slayer, SlayerMasterProfile master,
            SlayerStrategy task, PvmReadiness readiness)
    {
        String missing = readiness == null || readiness.missingRequirements.isEmpty()
                ? get(865)
                : "Resolve: " + String.join(", ", readiness.missingRequirements) + ".";
        Guidance guidance = new Guidance(
                "Prepare for " + task.getAlternativeName()
                        + get(866) + missing,
                get(867),
                task.getAlternativeLocation() + ".",
                get(868));
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.PREP_FIRST, master, task, 49.0,
                Confidence.CHECK_NEEDED,
                get(869),
                guidance, null, null, null);
    }

    private static SlayerDecisionResult block(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task,
            double value, int weight)
    {
        Guidance guidance = new Guidance(
                "Block " + slayer.taskName + " in "
                        + master.getDisplayName() + get(871),
                "This costs " + master.getBlockCost() + get(872),
                get(1503) + master.getDisplayName()
                        + get(1504),
                get(1505) + weight + get(873));
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.BLOCK, master, task, 59.0 - value,
                Confidence.VERIFIED,
                get(874),
                guidance, null, null, null);
    }

    private static SlayerDecisionResult skip(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerStrategy task,
            double value)
    {
        String who = master == null ? get(1968)
                : master.getDisplayName();
        int cost = master == null ? SlayerPointEconomy.SKIP_COST
                : master.getCancelCost();
        Guidance guidance = new Guidance(
                "Spend " + cost + get(1506) + slayer.taskName
                        + get(1507),
                get(1498) + slayer.getPoints()
                        + get(1508) + cost
                        + get(1509),
                get(1510) + who + ".",
                get(875));
        return new SlayerDecisionResult(SlayerState.ASSIGNED,
                SlayerDecision.SKIP, master, task, 56.0 - value,
                Confidence.VERIFIED,
                get(876),
                guidance, null, null, null);
    }

    private static SlayerDecisionResult unknownAssignment()
    {
        return new SlayerDecisionResult(SlayerState.UNKNOWN,
                SlayerDecision.PREP_FIRST, null, null, 32.0,
                Confidence.CHECK_NEEDED,
                get(877),
                new Guidance(
                        get(878),
                        get(879),
                        get(880),
                        get(882)), null, null, null);
    }

    private static PvmReadiness alternativeReadiness(GameData data,
            SlayerStrategy task)
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
            SlayerStrategy task)
    {
        if (AccountModePolicy.isRiskSensitive(context.accountMode()))
            return false;
        return context.goal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS
                || context.goal() == GoalType.GEAR_TARGET
                || context.goal() == GoalType.RAID_READY
                || (context.accountMode().isIronLike()
                    && task.getResourceValue() <= 2);
    }

    private static Guidance concreteTaskGuidance(
            Guidance base, SlayerTaskProfile mechanics,
            SlayerStrategy strategy, ItemIndex items,
            String weapon, CombatStyle observedStyle, SlayerSnapshot slayer,
            ItemsState inventory)
    {
        String required = firstReadyItem(items,
                mechanics.requiredProtection, strategy.getRequiredItemUse());
        var supplies = new StringBuilder("Equip ").append(weapon);
        if (required != null && !required.equalsIgnoreCase(weapon))
            supplies.append(" and keep ").append(required)
                    .append(strategy.getRequiredItemUse()
                            == SlayerRequiredItemUse.EQUIPPED
                            ? " equipped" : get(1511));
        var healing = items.bestInventoryName(SlayerStrategist::healingRank);
        supplies.append(". ");
        if (healing != null)
            supplies.append(get(1512)).append(healing)
                    .append(get(883));
        else
            supplies.append(get(884));
        String action = get(1513) + slayer.getRemaining() + " "
                + slayer.taskName + " using " + weapon + " ("
                + styleName(observedStyle) + ").";
        var technique = concreteTechnique(mechanics.getStyleGuidance());
        if (!technique.isEmpty()) action += " " + technique;
        return new Guidance(action, supplies.toString(),
                base.location, base.note);
    }

    /** Shared preparation view used by every current-task decision. */
    private static Guidance taskGuidance(GameData data,
            SlayerTaskProfile profile, boolean useGroupStorage)
    {
        if (profile == null) return null;
        var account = data.account();
        var items = new ItemIndex(data, useGroupStorage);
        String supplies = get(761);
        if (!profile.requiredProtection.isEmpty())
        {
            String owned = null;
            for (String candidate : profile.requiredProtection)
                if (items.has(candidate)) { owned = candidate; break; }
            String choices = String.join(" or ", profile.requiredProtection);
            if (owned != null) supplies = get(1456) + owned + get(762);
            else if (AccountMode.fromTypeCode(account.modeCode())
                    == AccountMode.ULTIMATE_IRONMAN)
            {
                var restricted = 0;
                for (String candidate : profile.requiredProtection)
                    restricted += items.restrictedQuantity(candidate);
                supplies = restricted > 0
                        ? get(763) + choices + get(764)
                        : get(766) + choices + get(767);
            }
            else if (!items.primaryOwnershipObserved())
                supplies = get(768) + choices + ".";
            else if (AccountMode.fromTypeCode(account.modeCode()).isIronLike())
                supplies = get(769) + choices + ".";
            else supplies = get(770) + choices + get(771);
        }

        var slayer = data.slayer();
        String location = slayer != null
                && !Names.lower(slayer.getTaskLocation()).isEmpty()
                ? get(1457) + slayer.getTaskLocation() + get(772)
                : !Names.lower(profile.getPreferredLocation()).isEmpty()
                        ? profile.getPreferredLocation()
                        : slayer != null
                                && !Names.lower(slayer.getMasterName()).isEmpty()
                                ? get(1458) + slayer.getMasterName() + get(773)
                                : get(774);
        var note = new StringBuilder();
        if (!Names.lower(profile.getMechanicsNote()).isEmpty())
            note.append(profile.getMechanicsNote()).append(' ');
        if (profile.getMultiTargetMagicEligibility() == Capability.VERIFIED)
            note.append(get(777));
        if (profile.getCannonEligibility() == Capability.UNKNOWN)
            note.append(get(778));
        if (profile.isWildernessVariantKnown()) note.append(get(779));
        if (AccountMode.fromTypeCode(account.modeCode()).isIronLike()
                && !profile.getIronObjectives().isEmpty())
            note.append(get(1953)).append(String.join(", ",
                    profile.getIronObjectives())).append(". ");
        if (!Names.lower(profile.getTaskDecisionGuidance()).isEmpty())
            note.append(profile.getTaskDecisionGuidance()).append(' ');
        note.append(get(775));
        return new Guidance(null, supplies, location, note.toString());
    }

    /**
     * Once live equipment has selected a weapon and style, do not append an
     * older catalog sentence that delegates that same choice back to the
     * player. Task-specific execution such as a safespot or finishing item is
     * still retained when it does not contradict the observed loadout.
     */
    private static String concreteTechnique(String guidance)
    {
        var normalized = Names.lower(guidance);
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
            SlayerStrategy task)
    {
        return task.inherentRisk != RiskLevel.LOW
                || task.completionBurden >= 3
                || task.setupBurden >= 3;
    }
    private static int healingRank(String name)
    {
        if (name.startsWith("raw ") || name.startsWith("burnt ")) return 0;
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
        var fishing = account == null ? 1 : account.level(FISHING);
        var cooking = account == null ? 1 : account.level(COOKING);
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
            if (item == null || item.slotIndex
                    != EquipmentInventorySlot.WEAPON.getSlotIdx()) continue;
            var name = Names.lower(item.getName());
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
        var name = Names.lower(weapon);
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

}

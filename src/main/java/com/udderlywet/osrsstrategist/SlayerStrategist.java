package com.udderlywet.osrsstrategist;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null) return null;
        AccountSnapshot account = context.getData().getAccount();
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || !AccountBuildPolicy.allowsSkill(account, Skill.SLAYER))
            return null;

        SlayerSnapshot slayer = context.getData().getSlayer();
        if (slayer == null
                || slayer.getAssignmentState() == SlayerAssignmentState.UNKNOWN)
            return unknownAssignment();
        if (slayer.getAssignmentState() == SlayerAssignmentState.NO_TASK)
            return chooseMaster(context, slayer);
        return evaluateTask(context, slayer);
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
                ? " This is task " + next + ", so its verified milestone point value is part of the choice."
                : "";
        String reason = "Chosen from eligible masters by the reviewed assignment-pool value, task XP potential, point value, "
                + "supply value, setup burden and location constraints." + milestone;
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Get your next Slayer assignment from " + choice.getDisplayName()
                        + ". Return to Compass after the assignment appears so the task can be evaluated.",
                "Do not pre-buy a task loadout. Keep only the transport needed to reach the master; task protection and supplies are decided after assignment.",
                choice.getLocation() + ".",
                reason + (choice.isWilderness()
                        ? " This master is eligible only because Wilderness methods are explicitly enabled."
                        : ""));
        return new SlayerDecisionResult(SlayerAssignmentState.NO_TASK, null,
                choice, null, score, RecommendationConfidence.VERIFIED,
                reason, guidance);
    }

    private static SlayerDecisionResult rewardPurchase(
            SlayerRewardAdvice advice, SlayerSnapshot slayer)
    {
        SlayerReward reward = advice.getReward();
        int remaining = slayer.getPoints() - reward.getPointCost();
        String reason = advice.getReason()
                + " Live varbit evidence shows the reward is locked, and the purchase leaves "
                + remaining + " points, including a 30-point cancellation reserve.";
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Buy " + reward.getDisplayName() + " for "
                        + reward.getPointCost() + " Slayer points, then return to Compass for the next master.",
                "No task loadout is needed. The live snapshot shows "
                        + slayer.getPoints() + " points and confirms this reward is not unlocked.",
                "Open the Slayer Rewards interface with any Slayer master.",
                reason);
        return new SlayerDecisionResult(SlayerAssignmentState.NO_TASK, null,
                null, null, advice.getScore(), RecommendationConfidence.VERIFIED,
                reason, guidance, null, reward);
    }

    private SlayerDecisionResult evaluateTask(StrategyContext context,
            SlayerSnapshot slayer)
    {
        StrategyDataBundle data = context.getData();
        SlayerTaskProfile taskMechanics = mechanics.profileFor(slayer.getTaskName());
        SlayerTaskStrategicProfile taskStrategy = strategy.profileFor(
                slayer.getTaskName());
        SlayerMasterProfile master = masters.match(slayer.getMasterName());

        if (unsafeWilderness(context, slayer, master))
            return wildernessAlternative(slayer, taskStrategy, master);

        RecommendationGuidance base = guidanceService.build(data,
                data.getAccount().getSkillLevel(Skill.SLAYER),
                Math.min(99, data.getAccount().getSkillLevel(Skill.SLAYER) + 1),
                context.isUseGroupStorage());
        if (taskMechanics == null || taskStrategy == null || base == null)
            return unreviewedTask(slayer, master, taskStrategy, base);

        ObservedItemIndex items = new ObservedItemIndex(data,
                context.isUseGroupStorage());
        if (!taskMechanics.getRequiredProtection().isEmpty()
                && firstReadyItem(items, taskMechanics.getRequiredProtection(),
                        taskStrategy.getRequiredItemUse()) == null)
            return preparation(slayer, master, taskStrategy, base,
                    taskStrategy.getRequiredItemUse() == SlayerRequiredItemUse.EQUIPPED
                            ? "A catalogued mandatory task item is not observed in an equipped slot."
                            : "A catalogued mandatory task item is not observed as directly usable.");

        String weapon = observedCombatWeapon(data.getEquipment());
        if (weapon == null)
            return preparation(slayer, master, taskStrategy, base,
                    "No recognised combat weapon is observed in the live weapon slot.");
        CombatStyle observedStyle = weaponStyle(weapon);
        if (taskStrategy.getRequiredCombatStyle() != null
                && observedStyle != taskStrategy.getRequiredCombatStyle())
            return preparation(slayer, master, taskStrategy, base,
                    "The equipped " + weapon + " does not satisfy the task's "
                            + styleName(taskStrategy.getRequiredCombatStyle())
                            + "-only damage requirement.");

        PvmReadiness alternative = alternativeReadiness(data, taskStrategy);
        if (alternative != null && alternative.isReadyForRecommendation()
                && alternativeWorthUsing(context, taskStrategy))
            return bossAlternative(slayer, master, taskStrategy);

        base = concreteTaskGuidance(base, taskMechanics, taskStrategy,
                items, weapon, observedStyle, slayer, data.getInventory());

        double value = taskValue(taskStrategy, context);
        boolean milestone = slayer.getTaskStreak() != null
                && SlayerPointEconomy.isBonusCompletion(slayer.getTaskStreak() + 1);
        Integer weight = master == null ? null
                : taskStrategy.weightFor(master.getId());

        if (!milestone && value <= -1.0 && weight != null && weight >= 8
                && slayer.hasKnownFreeBlockSlot()
                && slayer.getPoints() >= master.getBlockCost())
            return block(slayer, master, taskStrategy, value, weight);

        if (!milestone && value < 0.5
                && SlayerPointEconomy.hasSustainableSkipBalance(
                        slayer.getPoints()))
            return skip(slayer, master, taskStrategy, value);

        String reason = milestone
                ? "Complete this safe task: the next completion is a verified Slayer point milestone, so preserving the streak outweighs an ordinary skip."
                : "Do this task because its XP, resources, effort, setup, attention and session-fit properties clear the keep threshold.";
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.DO, master, taskStrategy, 58.0 + value,
                RecommendationConfidence.VERIFIED, reason, base);
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

        if (context.getAccountMode().isIronLike())
            score += master.getSupplyValue() * 10.0;
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
            score -= master.getSetupBurden() * 8.0;
        if (master.isWilderness()
                && AccountModePolicy.isRiskSensitive(context.getAccountMode()))
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
        score += context.getPreferenceProfile().weightFor(
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
        double resourceMultiplier = context.getAccountMode().isIronLike()
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
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
            value -= task.getSetupBurden() * .6;
        if (context.getActiveGoal() == GoalType.SLAYER_85)
            value += task.getXpQuality() * .8;
        if (task.getInherentRisk() == RiskLevel.MEDIUM) value -= 3.0;
        else if (task.getInherentRisk() == RiskLevel.HIGH) value -= 8.0;
        else if (task.getInherentRisk() == RiskLevel.IRREVERSIBLE) value -= 20.0;
        return value;
    }

    private static boolean unsafeWilderness(StrategyContext context,
            SlayerSnapshot slayer, SlayerMasterProfile master)
    {
        boolean wilderness = master != null && master.isWilderness();
        String area = normalize(slayer.getTaskLocation());
        wilderness |= area.contains("wilderness") || area.contains("revenant caves");
        return wilderness && (!context.isAllowWildernessMethods()
                || AccountModePolicy.isRiskSensitive(context.getAccountMode()));
    }

    private static SlayerDecisionResult wildernessAlternative(
            SlayerSnapshot slayer, SlayerTaskStrategicProfile task,
            SlayerMasterProfile master)
    {
        if (slayer.getPoints() < SlayerPointEconomy.SKIP_COST)
        {
            RecommendationGuidance guidance = new RecommendationGuidance(
                    "Do not enter the Wilderness. Ask Turael/Aya whether this exact task is eligible for safe replacement; accept only if the dialogue explicitly offers it.",
                    "No Wilderness loadout is needed for this verification. Do not risk carried valuables.",
                    "Turael/Aya in Burthorpe.",
                    "Only " + slayer.getPoints() + " Slayer points are observed, so Compass cannot claim a 30-point cancellation is available. Turael cannot replace tasks from his own assignment list.");
            return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                    SlayerTaskDecision.PREP_FIRST, master, task, 48.0,
                    RecommendationConfidence.CHECK_NEEDED,
                    "Risk policy blocks the assignment, but the safe replacement path needs one explicit eligibility check.",
                    guidance);
        }
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Do not enter the Wilderness. Spend 30 Slayer points to cancel this assignment, then obtain a non-Wilderness task.",
                "The live snapshot shows " + slayer.getPoints()
                        + " points, enough for the verified cancellation cost. No dangerous loadout is required.",
                "Open Slayer rewards with any Slayer master.",
                "The live assignment or master is Wilderness-bound and account risk policy rejects it before loadout selection.");
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.ALTERNATIVE, master, task, 60.0,
                RecommendationConfidence.VERIFIED,
                "Use a safe assignment replacement instead of post-hoc Wilderness warnings.",
                guidance);
    }

    private static SlayerDecisionResult unreviewedTask(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            RecommendationGuidance base)
    {
        RecommendationGuidance guidance = base == null
                ? new RecommendationGuidance(
                        "Check the assignment with an enchanted gem, Slayer ring, Slayer helmet, or the assigning master, then reopen Compass.",
                        "Do not assume protection, weapon or supply requirements for an unreviewed task.",
                        "Use the live assignment interface before travelling.",
                        "The count is observed, but Compass lacks reviewed strategic or mechanical metadata for a safe verdict.")
                : new RecommendationGuidance(
                        "Review the shown task mechanics and verify a legal weapon, protection and supplies before beginning the assignment.",
                        base.getSupplies(), base.getLocation(),
                        "The assignment is decoded, but no reviewed strategic profile exists; Compass will not invent a do/skip/block verdict. " + base.getNote());
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 40.0,
                RecommendationConfidence.CHECK_NEEDED,
                "Task identity alone is not sufficient evidence for a strategic verdict.",
                guidance);
    }

    private static SlayerDecisionResult preparation(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            RecommendationGuidance base, String reason)
    {
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Prepare the mandatory item for " + slayer.getTaskName()
                        + ", then return to the live task before killing anything.",
                base.getSupplies(), base.getLocation(), base.getNote());
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.PREP_FIRST, master, task, 48.0,
                RecommendationConfidence.CHECK_NEEDED, reason, guidance);
    }

    private static SlayerDecisionResult bossAlternative(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task)
    {
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Use the observed-ready " + task.getAlternativeName()
                        + " alternative for this " + slayer.getTaskName()
                        + " assignment. Recheck Compass when the carried setup changes.",
                "Keep the equipment and carried supplies already verified by the encounter-readiness check; do not substitute an unverified loadout.",
                task.getAlternativeLocation() + ".",
                "The boss/alternative is selected only because live PvM readiness is VERIFIED; merely having the Slayer task is not readiness evidence.");
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.ALTERNATIVE, master, task, 64.0,
                RecommendationConfidence.VERIFIED,
                "A verified-ready alternative contributes more goal/resource value than the ordinary route.",
                guidance, task.getAlternativeName());
    }

    private static SlayerDecisionResult block(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            double value, int weight)
    {
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Block " + slayer.getTaskName() + " in "
                        + master.getDisplayName() + "'s Slayer rewards list, then get a replacement assignment.",
                "This costs " + master.getBlockCost() + " Slayer points. The live snapshot proves enough points and an unused block slot.",
                "Use the Slayer rewards interface for " + master.getDisplayName()
                        + "; blocks are master-specific.",
                "Assignment weight " + weight + " and poor property score make a reusable block more valuable than repeated 30-point skips.");
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.BLOCK, master, task, 59.0 - value,
                RecommendationConfidence.VERIFIED,
                "High assignment weight, low strategic value, enough points and a verified free slot justify a block.",
                guidance);
    }

    private static SlayerDecisionResult skip(SlayerSnapshot slayer,
            SlayerMasterProfile master, SlayerTaskStrategicProfile task,
            double value)
    {
        String who = master == null ? "any Slayer master"
                : master.getDisplayName();
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Spend 30 Slayer points to cancel " + slayer.getTaskName()
                        + ", then get a replacement assignment.",
                "The live snapshot shows " + slayer.getPoints()
                        + " points, enough for the verified 30-point cancellation cost.",
                "Open Slayer rewards with " + who + ".",
                "The task's XP, resources, length, setup and session fit fall below the keep threshold; no block is claimed without known weight and slot evidence.");
        return new SlayerDecisionResult(SlayerAssignmentState.ASSIGNED,
                SlayerTaskDecision.SKIP, master, task, 56.0 - value,
                RecommendationConfidence.VERIFIED,
                "Low property value and sufficient points justify one cancellation.",
                guidance);
    }

    private static SlayerDecisionResult unknownAssignment()
    {
        return new SlayerDecisionResult(SlayerAssignmentState.UNKNOWN,
                SlayerTaskDecision.PREP_FIRST, null, null, 32.0,
                RecommendationConfidence.CHECK_NEEDED,
                "Live state does not prove whether the account has an assignment.",
                new RecommendationGuidance(
                        "Check your assignment once with an enchanted gem, Slayer ring, Slayer helmet, or a Slayer master, then reopen Compass.",
                        "Do not prepare a task-specific loadout until the assignment name and count are visible.",
                        "Use the nearest safe Slayer task-check option already available to the account.",
                        "Unknown assignment state is kept unknown; Compass does not treat missing evidence as no task."));
    }

    private static PvmReadiness alternativeReadiness(StrategyDataBundle data,
            SlayerTaskStrategicProfile task)
    {
        if (data == null || data.getPvm() == null || task == null
                || task.getAlternativeActivityId() == null) return null;
        PvmReadiness readiness = data.getPvm().readinessFor(
                task.getAlternativeActivityId());
        if (readiness == null && task.getAlternativeActivityId().startsWith("pvm:"))
            readiness = data.getPvm().readinessFor(
                    task.getAlternativeActivityId().substring(4));
        return readiness;
    }

    private static boolean alternativeWorthUsing(StrategyContext context,
            SlayerTaskStrategicProfile task)
    {
        if (AccountModePolicy.isRiskSensitive(context.getAccountMode()))
            return false;
        return context.getActiveGoal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS
                || context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY
                || (context.getAccountMode().isIronLike()
                    && task.getResourceValue() <= 2);
    }

    private static RecommendationGuidance concreteTaskGuidance(
            RecommendationGuidance base, SlayerTaskProfile mechanics,
            SlayerTaskStrategicProfile strategy, ObservedItemIndex items,
            String weapon, CombatStyle observedStyle, SlayerSnapshot slayer,
            InventorySnapshot inventory)
    {
        String required = firstReadyItem(items,
                mechanics.getRequiredProtection(), strategy.getRequiredItemUse());
        StringBuilder supplies = new StringBuilder("Equip ").append(weapon);
        if (required != null && !required.equalsIgnoreCase(weapon))
            supplies.append(" and keep ").append(required)
                    .append(strategy.getRequiredItemUse()
                            == SlayerRequiredItemUse.EQUIPPED
                            ? " equipped" : " carried or equipped as required");
        supplies.append(". ");
        if (inventory == null || inventory.getItems().isEmpty())
            supplies.append("No consumable inventory is observed; begin only a short safe trip and bank before supplies run out.");
        else
            supplies.append("Keep the currently carried inventory; Compass has not inferred doses, charges, or healing value from item names.");
        String action = "Kill the remaining " + slayer.getRemaining() + " "
                + slayer.getTaskName() + " using " + weapon + " ("
                + styleName(observedStyle) + "). " + mechanics.getStyleGuidance();
        return new RecommendationGuidance(action, supplies.toString(),
                base.getLocation(), base.getNote());
    }

    private static String firstReadyItem(ObservedItemIndex items,
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

    private static String observedCombatWeapon(EquipmentSnapshot equipment)
    {
        if (equipment == null) return null;
        for (ItemStackSnapshot item : equipment.getEquippedItems())
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

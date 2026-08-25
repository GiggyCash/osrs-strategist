package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/**
 * Bounded traversal across the production quest, gear, training, resource,
 * PvM, minigame, clue and STASH providers.
 */
@Singleton
public final class UniversalDependencyPlanner
{
    private static final int DEFAULT_MAX_DEPTH = 12;
    private static final int DEFAULT_MAX_NODES = 160;
    private static final Pattern SKILL_LEVEL = Pattern.compile(
            "(?i)^([a-z ]+?)\\s+(\\d+)$");

    private final GearAcquisitionCatalog gear = new GearAcquisitionCatalog();
    private final QuestKnowledgeCatalog quests = new QuestKnowledgeCatalog();
    private final PvmPreparationProfileCatalog pvm =
            new PvmPreparationProfileCatalog();
    private final ResourceSourceCatalog resources = new ResourceSourceCatalog();
    private final TrainingMethodSelector training = new TrainingMethodSelector(
            new TrainingMethodDatabase(), null,
            new ExpandedTrainingMethodCatalog(), new F2pBaselineMethodCatalog(),
            new TrainingMethodPolicy());
    private final StashUnitCatalog stash = new StashUnitCatalog();
    private final StashDependencyPlanner stashPlanner =
            new StashDependencyPlanner();
    private final DiaryTaskCatalog diaries = new DiaryTaskCatalog();
    private final TransportCatalog transports = new TransportCatalog();
    private final int maxDepth;
    private final int maxNodes;

    public UniversalDependencyPlanner()
    {
        this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);
    }

    UniversalDependencyPlanner(int maxDepth, int maxNodes)
    {
        this.maxDepth = Math.max(1, maxDepth);
        this.maxNodes = Math.max(4, maxNodes);
    }

    public UniversalDependencyResolution resolveGoal(GoalType goal,
            StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        GoalType safe = goal == null ? GoalType.MAX : goal;
        String root = traversal.add("goal:" + normalize(safe.name()),
                GoalNodeKind.META, "Advance " + safe.name().toLowerCase()
                        .replace('_', ' '), RecommendationConfidence.CHECK_NEEDED,
                0, 1, null);
        switch (safe)
        {
            case BOWFA:
                traversal.gear("Bow of faerdhinen", root, 1,
                        new LinkedHashSet<>());
                break;
            case BARROWS_GLOVES:
                traversal.gear("Barrows gloves", root, 1,
                        new LinkedHashSet<>());
                break;
            case INFERNAL_CAPE:
                traversal.gear("Infernal cape", root, 1,
                        new LinkedHashSet<>());
                break;
            case PRIFDDINAS:
                traversal.quest("Song of the Elves", root, 1,
                        new LinkedHashSet<>());
                break;
            case QUEST_CAPE:
                traversal.add("quest-catalogue:current", GoalNodeKind.QUEST,
                        "Select the first incomplete, safe quest prerequisite from the current catalogue",
                        RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
                break;
            case DIARY_CAPE:
                traversal.add("diary:all-current", GoalNodeKind.DIARY,
                        "Select the first incomplete task prerequisite from all current diary tiers",
                        RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
                break;
            case RAID_READY:
                traversal.pvm("pvm:tombs_of_amascut", "Tombs of Amascut",
                        root, 1);
                break;
            case SLAYER_85:
                traversal.skill(Skill.SLAYER, 85, root, 1);
                break;
            case BASE_70S:
                if (context != null && context.getData() != null
                        && context.getData().getAccount() != null)
                    for (Skill skill : Skill.values())
                        if (context.getData().getAccount().getSkillLevel(skill) < 70)
                            traversal.skill(skill, 70, root, 1);
                break;
            case GEAR_TARGET:
                traversal.add("gear:target-unselected", GoalNodeKind.GEAR,
                        "Select the encounter and practical gear target before resolving acquisition",
                        RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
                break;
            default:
                traversal.add("preparation:" + normalize(safe.name()),
                        GoalNodeKind.PREPARATION_ACTION,
                        "Select the first observed actionable prerequisite for this goal",
                        RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
                break;
        }
        return traversal.finish();
    }

    public UniversalDependencyResolution resolveGear(String item,
            StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        traversal.gear(item, null, 0, new LinkedHashSet<>());
        return traversal.finish();
    }

    public UniversalDependencyResolution resolveQuest(String quest,
            StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        traversal.quest(quest, null, 0, new LinkedHashSet<>());
        return traversal.finish();
    }

    public UniversalDependencyResolution resolveClueStash(STASHUnit unit,
            StashUnitState state, StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        StashUnitDefinition definition = stash.get(unit);
        String clue = traversal.add("clue:emote:" + normalize(unit.name()),
                GoalNodeKind.CLUE, "Prepare the active emote clue step",
                RecommendationConfidence.CHECK_NEEDED, 0, 1, null);
        StashBuildPlan plan = stashPlanner.plan(definition, state, context);
        String parent = clue;
        int depth = 1;
        for (StashDependencyStep step : plan.getSteps())
        {
            String id = step.getKind().name().toLowerCase(Locale.ROOT) + ":"
                    + normalize(step.getAction());
            parent = traversal.add(id, step.getKind(), step.getAction(),
                    step.getConfidence(), depth++, 1, parent);
        }
        return traversal.finish();
    }

    public UniversalDependencyResolution resolveDiary(String region,
            DiaryTier tier, StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        String root = traversal.add("diary:" + normalize(region) + ":"
                        + tier.name().toLowerCase(Locale.ROOT),
                GoalNodeKind.DIARY,
                "Complete the " + tier.name().toLowerCase(Locale.ROOT) + " "
                        + region + " diary",
                RecommendationConfidence.CHECK_NEEDED, 0, 1, null);
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null
                || context.getData().getAccount().getMembershipStatus()
                        != MembershipStatus.P2P)
        {
            traversal.add("access:membership:diary", GoalNodeKind.ACCESS,
                    "Verify active membership before planning Achievement Diary tasks",
                    RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
            return traversal.finish();
        }
        if ("Wilderness".equalsIgnoreCase(region)
                && !context.isAllowWildernessMethods())
        {
            traversal.add("access:wilderness:diary", GoalNodeKind.ACCESS,
                    "Enable and accept Wilderness risk before routing this diary",
                    RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
            return traversal.finish();
        }

        List<DiaryTaskDefinition> tasks = diaries.forTier(region, tier);
        DiaryTaskDefinition selected = null;
        for (DiaryTaskDefinition task : tasks)
            if (taskHasUnmetRequirement(task, context))
            {
                selected = task;
                break;
            }
        if (selected == null && !tasks.isEmpty()) selected = tasks.get(0);
        if (selected == null)
        {
            traversal.add("preparation:diary-missing-evidence",
                    GoalNodeKind.PREPARATION_ACTION,
                    "Check the diary interface; no current task evidence is available",
                    RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
            return traversal.finish();
        }

        String task = traversal.add(selected.getId(), GoalNodeKind.ACTIVITY,
                "Open the diary interface and check whether this task is incomplete: "
                        + selected.getTask(),
                RecommendationConfidence.CHECK_NEEDED, 1, 1, root);
        Set<String> path = new LinkedHashSet<>();
        for (DiaryTaskRequirement requirement : selected.getRequirements())
        {
            switch (requirement.getKind())
            {
                case SKILL:
                    traversal.skill(requirement.getSkill(), requirement.getLevel(),
                            task, 2);
                    break;
                case QUEST:
                    if (requirement.isStartedOnly()
                            && diaryQuestStarted(requirement.getQuest(), context))
                        traversal.add("quest-access:"
                                        + normalize(requirement.getQuest()),
                                GoalNodeKind.QUEST,
                                requirement.getQuest()
                                        + " start-state access verified",
                                RecommendationConfidence.VERIFIED, 2, 1, task);
                    else
                        traversal.quest(requirement.getQuest(), task, 2, path);
                    break;
                case COMBAT_LEVEL:
                    traversal.add("preparation:combat-level:"
                                    + requirement.getLevel(),
                            GoalNodeKind.PREPARATION_ACTION,
                            "Verify combat level " + requirement.getLevel(),
                            RecommendationConfidence.CHECK_NEEDED, 2, 1, task);
                    break;
                case QUEST_POINTS:
                    traversal.add("preparation:quest-points:"
                                    + requirement.getLevel(),
                            GoalNodeKind.PREPARATION_ACTION,
                            "Verify " + requirement.getLevel() + " quest points",
                            RecommendationConfidence.CHECK_NEEDED, 2, 1, task);
                    break;
                case ALTERNATIVE_CHECK:
                    traversal.add("preparation:diary-alternative:"
                                    + normalize(selected.getTask()),
                            GoalNodeKind.PREPARATION_ACTION,
                            requirement.getCheck(),
                            RecommendationConfidence.CHECK_NEEDED, 2, 1, task);
                    break;
                default:
                    break;
            }
        }
        if (selected.isTransportRelevant())
            traversal.add("transport:diary-task:" + normalize(selected.getTask()),
                    GoalNodeKind.TRANSPORTATION,
                    "Verify the transport unlock and route for: "
                            + selected.getTask(),
                    RecommendationConfidence.CHECK_NEEDED, 2, 1, task);
        return traversal.finish();
    }

    public UniversalDependencyResolution resolveTransport(String transportId,
            StrategyContext context)
    {
        Traversal traversal = new Traversal(context);
        traversal.transport(transportId, null, 0, new LinkedHashSet<>());
        return traversal.finish();
    }

    private static boolean taskHasUnmetRequirement(DiaryTaskDefinition task,
            StrategyContext context)
    {
        AccountSnapshot account = context.getData().getAccount();
        QuestSnapshot quests = context.getData().getQuests();
        for (DiaryTaskRequirement requirement : task.getRequirements())
        {
            if (requirement.getKind() == DiaryTaskRequirement.Kind.SKILL
                    && account.getSkillLevel(requirement.getSkill())
                            < requirement.getLevel()) return true;
            if (requirement.getKind() == DiaryTaskRequirement.Kind.QUEST)
            {
                QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                        : quests.statusOf(requirement.getQuest());
                if (status != QuestStatus.COMPLETE
                        && !(requirement.isStartedOnly()
                        && status == QuestStatus.IN_PROGRESS)) return true;
            }
            if (requirement.getKind()
                    == DiaryTaskRequirement.Kind.ALTERNATIVE_CHECK) return true;
        }
        return false;
    }

    private static boolean diaryQuestStarted(String quest,
            StrategyContext context)
    {
        QuestSnapshot snapshot = context == null || context.getData() == null
                ? null : context.getData().getQuests();
        if (snapshot == null) return false;
        QuestStatus status = snapshot.statusOf(quest);
        return status == QuestStatus.IN_PROGRESS
                || status == QuestStatus.COMPLETE;
    }

    private final class Traversal
    {
        private final StrategyContext context;
        private final Map<String, UniversalDependencyNode> nodes =
                new LinkedHashMap<>();
        private boolean cycle;
        private boolean depth;
        private boolean nodeLimit;

        private Traversal(StrategyContext context) { this.context = context; }

        private String add(String id, GoalNodeKind kind, String action,
                RecommendationConfidence confidence, int nodeDepth,
                int quantity, String parent)
        {
            if (nodeDepth > maxDepth) { depth = true; return parent; }
            UniversalDependencyNode existing = nodes.get(id);
            if (existing != null)
            {
                existing.addParent(parent);
                return existing.getId();
            }
            if (nodes.size() >= maxNodes) { nodeLimit = true; return parent; }
            UniversalDependencyNode node = new UniversalDependencyNode(id, kind,
                    action, confidence, nodeDepth, quantity);
            node.addParent(parent);
            nodes.put(id, node);
            return id;
        }

        private void gear(String item, String parent, int nodeDepth,
                Set<String> path)
        {
            if (item == null || item.trim().isEmpty()) return;
            String key = "gear:" + normalize(item);
            if (!path.add(key)) { cycle = true; return; }
            String gearNode = add(key, GoalNodeKind.GEAR,
                    "Acquire " + item + " for its selected encounter/context",
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth, 1, parent);
            GearAcquisitionRoute route = gear.forItem(item);
            if (route == null) { path.remove(key); return; }
            for (GearAcquisitionStep step : route.getSteps())
            {
                switch (step.getKind())
                {
                    case QUEST:
                        quest(step.getTarget(), gearNode, nodeDepth + 1, path);
                        break;
                    case SKILL:
                        skillText(step.getTarget(), gearNode, nodeDepth + 1);
                        break;
                    case BOSS:
                        pvm("pvm:" + normalize(step.getTarget()), step.getTarget(),
                                gearNode, nodeDepth + 1);
                        break;
                    case MINIGAME:
                        add("minigame:" + normalize(step.getTarget()),
                                GoalNodeKind.MINIGAME, step.getAction(),
                                RecommendationConfidence.CHECK_NEEDED,
                                nodeDepth + 1, 1, gearNode);
                        break;
                    case RESOURCE:
                        resource(step.getTarget(), 1, gearNode, nodeDepth + 1);
                        break;
                    case SHOP:
                        add("shop:" + normalize(step.getTarget()),
                                GoalNodeKind.SHOP, step.getAction(),
                                RecommendationConfidence.CHECK_NEEDED,
                                nodeDepth + 1, 1, gearNode);
                        break;
                    case VERIFY:
                        if (gear.forItem(step.getTarget()) != null)
                            gear(step.getTarget(), gearNode, nodeDepth + 1, path);
                        else
                            add("preparation:" + normalize(step.getTarget()),
                                    GoalNodeKind.PREPARATION_ACTION,
                                    step.getAction(),
                                    RecommendationConfidence.CHECK_NEEDED,
                                    nodeDepth + 1, 1, gearNode);
                        break;
                    default:
                        break;
                }
            }
            path.remove(key);
        }

        private void quest(String name, String parent, int nodeDepth,
                Set<String> path)
        {
            if (name == null || name.trim().isEmpty()) return;
            String key = "quest:" + normalize(name);
            if (!path.add(key)) { cycle = true; return; }
            QuestDefinition definition = quests.definitionFor(name);
            RecommendationConfidence confidence = questComplete(name)
                    ? RecommendationConfidence.VERIFIED
                    : RecommendationConfidence.CHECK_NEEDED;
            String questNode = add(key, GoalNodeKind.QUEST,
                    (confidence == RecommendationConfidence.VERIFIED
                            ? "Completed " : "Complete ") + name,
                    confidence, nodeDepth, 1, parent);
            if (definition == null || confidence == RecommendationConfidence.VERIFIED)
            {
                path.remove(key);
                return;
            }
            if (!definition.isFreeToPlay() && !p2p())
            {
                add("access:membership:" + normalize(name), GoalNodeKind.ACCESS,
                        "Verify active membership before planning " + name,
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, questNode);
                path.remove(key);
                return;
            }
            for (String prerequisite : definition.getPrerequisites())
                if (!questComplete(prerequisite))
                    quest(prerequisite, questNode, nodeDepth + 1, path);
            for (Map.Entry<Skill, Integer> requirement
                    : definition.getSkillRequirements().entrySet())
                if (level(requirement.getKey()) < requirement.getValue())
                    skill(requirement.getKey(), requirement.getValue(), questNode,
                            nodeDepth + 1);
            for (QuestDefinition.QuestItemRequirement item
                    : definition.getItemRequirements())
                resource(item.getName(), item.getQuantity(), questNode,
                        nodeDepth + 1);
            expression(definition.getItemRequirementExpression(), questNode,
                    nodeDepth + 1);
            for (String check : definition.getAccessChecks())
            {
                String access = add("access:" + normalize(name + " " + check),
                        GoalNodeKind.ACCESS, check,
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, questNode);
                if (containsAny(check, "route", "transport", "travel", "access"))
                    add("transport:" + normalize(check),
                            GoalNodeKind.TRANSPORTATION,
                            "Verify the reusable route: " + check,
                            RecommendationConfidence.CHECK_NEEDED,
                            nodeDepth + 2, 1, access);
                if (containsAny(check, "defeat", "combat", "boss", "enemy"))
                    add("preparation:combat:" + normalize(check),
                            GoalNodeKind.PREPARATION_ACTION,
                            "Prepare the legal encounter setup: " + check,
                            RecommendationConfidence.CHECK_NEEDED,
                            nodeDepth + 2, 1, access);
            }
            path.remove(key);
        }

        private void skillText(String text, String parent, int nodeDepth)
        {
            Matcher matcher = SKILL_LEVEL.matcher(text == null ? "" : text.trim());
            if (matcher.matches())
            {
                Skill skill = skillByName(matcher.group(1));
                if (skill != null)
                {
                    skill(skill, Integer.parseInt(matcher.group(2)), parent,
                            nodeDepth);
                    return;
                }
            }
            add("skill:" + normalize(text), GoalNodeKind.SKILL,
                    "Verify skill requirement: " + text,
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth, 1, parent);
        }

        private void skill(Skill skill, int target, String parent, int nodeDepth)
        {
            int current = level(skill);
            RecommendationConfidence confidence = current >= target
                    ? RecommendationConfidence.VERIFIED
                    : RecommendationConfidence.CHECK_NEEDED;
            String node = add("skill-level:" + normalize(skill.getName()) + ":" + target,
                    GoalNodeKind.SKILL_LEVEL,
                    current >= target ? skill.getName() + " level verified"
                            : "Train " + skill.getName() + " from " + current
                                    + " to " + target,
                    confidence, nodeDepth, 1, parent);
            if (current >= target || context == null || context.getData() == null)
                return;
            TrainingPlan plan = training.select(context.getData(), skill, current,
                    context.getStrategyMode(), context.getSessionIntent(),
                    context.isAllowWildernessMethods());
            if (plan == null || plan.getMethod() == null) return;
            TrainingMethod method = plan.getMethod();
            String methodNode = add("training-method:" + normalize(method.getId()),
                    GoalNodeKind.TRAINING_METHOD,
                    "Use " + method.getName() + ": " + method.getInstructions(),
                    plan.getConfidence(), nodeDepth + 1, 1, node);
            for (String requirement : method.getRequirements())
                add("preparation:method:" + normalize(requirement),
                        GoalNodeKind.PREPARATION_ACTION,
                        "Verify method setup: " + requirement,
                        RecommendationConfidence.CHECK_NEEDED,
                        nodeDepth + 2, 1, methodNode);
        }

        private void expression(ItemRequirementExpression value, String parent,
                int nodeDepth)
        {
            if (value == null) return;
            if (value.getKind() == ItemRequirementExpression.Kind.ITEM)
            {
                if (!value.getItemNames().isEmpty())
                    resource(value.getItemNames().get(0), value.getQuantity(),
                            parent, nodeDepth);
                return;
            }
            if (value.getKind() == ItemRequirementExpression.Kind.ITEM_CLASS
                    || value.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED
                    || value.getKind() == ItemRequirementExpression.Kind.ANY_OF)
            {
                add("preparation:item:" + normalize(value.label()),
                        GoalNodeKind.PREPARATION_ACTION,
                        "Verify item alternative: " + value.label(),
                        RecommendationConfidence.CHECK_NEEDED,
                        nodeDepth, 1, parent);
                return;
            }
            for (ItemRequirementExpression child : value.getChildren())
                expression(child, parent, nodeDepth);
        }

        private void resource(String name, int quantity, String parent,
                int nodeDepth)
        {
            String item = add("item:" + normalize(name), GoalNodeKind.ITEM,
                    "Obtain " + quantity + " × " + name,
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth, quantity,
                    parent);
            String resource = add("resource:" + normalize(name),
                    GoalNodeKind.RESOURCE,
                    "Resolve the confirmed " + name + " shortfall",
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                    quantity, item);
            List<String> suggestions = p2p() && context != null
                    ? resources.suggestions(name, context.getAccountMode(),
                            context.isAllowWildernessMethods())
                    : Collections.emptyList();
            String action;
            if (!suggestions.isEmpty()) action = suggestions.get(0);
            else if (context != null && context.getAccountMode().usesGrandExchange())
                action = "Compare a Grand Exchange purchase with the time value of self-sourcing after price and GP are observed";
            else if (context != null && context.getAccountMode()
                    == AccountMode.ULTIMATE_IRONMAN)
                action = "Self-source the shortfall just in time without relying on a conventional bank";
            else
                action = "Verify an account-safe self-source for the shortfall";
            add("preparation:source:" + normalize(name),
                    GoalNodeKind.PREPARATION_ACTION, action,
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth + 2,
                    quantity, resource);
        }

        private void pvm(String id, String name, String parent, int nodeDepth)
        {
            String node = add(id, GoalNodeKind.PVM_ENCOUNTER,
                    "Prepare for " + name + " without claiming mechanical readiness",
                    RecommendationConfidence.CHECK_NEEDED, nodeDepth, 1, parent);
            PvmPreparationProfile profile = pvm.forActivity(id);
            if (profile == null) return;
            int index = 0;
            for (String check : profile.getChecks())
                add("preparation:pvm:" + normalize(id) + ":" + index++,
                        GoalNodeKind.PREPARATION_ACTION, check,
                        RecommendationConfidence.CHECK_NEEDED,
                        nodeDepth + 1, 1, node);
        }

        private void transport(String transportId, String parent, int nodeDepth,
                Set<String> path)
        {
            TransportDefinition definition = transports.get(transportId);
            String key = "transport:" + normalize(transportId);
            if (!path.add(key)) { cycle = true; return; }
            boolean verified = transportVerified(transportId);
            String name = definition == null ? transportId : definition.getName();
            String node = add(key, GoalNodeKind.TRANSPORTATION,
                    verified ? name + " route verified"
                            : "Unlock and verify " + name,
                    verified ? RecommendationConfidence.VERIFIED
                            : RecommendationConfidence.CHECK_NEEDED,
                    nodeDepth, 1, parent);
            if (verified)
            {
                path.remove(key);
                return;
            }
            if (definition == null)
            {
                add("preparation:transport-evidence:" + normalize(transportId),
                        GoalNodeKind.PREPARATION_ACTION,
                        "Verify this transport route in live account state before relying on it",
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, node);
                path.remove(key);
                return;
            }
            if (definition.isMembersOnly() && !p2p())
            {
                add("access:membership:" + key, GoalNodeKind.ACCESS,
                        "Verify active membership before planning " + name,
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, node);
                path.remove(key);
                return;
            }
            if (definition.isWilderness()
                    && (!context.isAllowWildernessMethods() || hardcore()))
            {
                add("access:wilderness:" + key, GoalNodeKind.ACCESS,
                        hardcore()
                                ? "Do not route this Wilderness transport for a Hardcore account without a separate explicit risk decision"
                                : "Enable and accept Wilderness risk before routing this transport",
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, node);
                path.remove(key);
                return;
            }
            if (definition.getQuest() != null
                    && !questAccessSatisfied(definition))
                quest(definition.getQuest(), node, nodeDepth + 1, path);
            if (definition.getSkill() != null
                    && level(definition.getSkill()) < definition.getLevel())
                skill(definition.getSkill(), definition.getLevel(), node,
                        nodeDepth + 1);
            if (definition.getPohFurniture() != null
                    && !pohFurnitureVerified(definition.getPohFurniture()))
                add("preparation:poh-furniture:"
                                + normalize(definition.getPohFurniture()),
                        GoalNodeKind.PREPARATION_ACTION,
                        "Check the live POH and build the required "
                                + definition.getPohFurniture()
                                + " only if it is not already installed",
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, node);
            if (definition.getItemOrAccessCheck() != null)
                add("preparation:transport:" + normalize(transportId),
                        GoalNodeKind.PREPARATION_ACTION,
                        definition.getItemOrAccessCheck(),
                        RecommendationConfidence.CHECK_NEEDED, nodeDepth + 1,
                        1, node);
            path.remove(key);
        }

        private boolean transportVerified(String id)
        {
            TransportSnapshot snapshot = context == null
                    || context.getData() == null ? null
                    : context.getData().getTransport();
            return snapshot != null && snapshot.hasVerifiedRoute(id);
        }

        private boolean questAccessSatisfied(TransportDefinition definition)
        {
            QuestSnapshot snapshot = context == null
                    || context.getData() == null ? null
                    : context.getData().getQuests();
            if (snapshot == null) return false;
            QuestStatus status = snapshot.statusOf(definition.getQuest());
            return status == QuestStatus.COMPLETE
                    || definition.isQuestStartSufficient()
                    && status == QuestStatus.IN_PROGRESS;
        }

        private boolean pohFurnitureVerified(String furniture)
        {
            PohSnapshot snapshot = context == null || context.getData() == null
                    ? null : context.getData().getPoh();
            return snapshot != null && snapshot.furnitureState(furniture)
                    == CapabilityState.VERIFIED;
        }

        private boolean hardcore()
        {
            if (context == null) return false;
            return context.getAccountMode() == AccountMode.HARDCORE_IRONMAN
                    || context.getAccountMode()
                    == AccountMode.HARDCORE_GROUP_IRONMAN;
        }

        private boolean questComplete(String name)
        {
            QuestSnapshot snapshot = context == null || context.getData() == null
                    ? null : context.getData().getQuests();
            return snapshot != null && snapshot.statusOf(name)
                    == QuestStatus.COMPLETE;
        }

        private int level(Skill skill)
        {
            return context == null || context.getData() == null
                    || context.getData().getAccount() == null ? 1
                    : context.getData().getAccount().getSkillLevel(skill);
        }

        private boolean p2p()
        {
            return context != null && context.getData() != null
                    && context.getData().getAccount() != null
                    && context.getData().getAccount().getMembershipStatus()
                    == MembershipStatus.P2P;
        }

        private UniversalDependencyResolution finish()
        {
            return new UniversalDependencyResolution(
                    new ArrayList<>(nodes.values()), cycle, depth, nodeLimit);
        }
    }

    private static Skill skillByName(String name)
    {
        if (name == null) return null;
        for (Skill skill : Skill.values())
            if (skill.getName().equalsIgnoreCase(name.trim())) return skill;
        return null;
    }

    private static boolean containsAny(String text, String... values)
    {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String value : values) if (normalized.contains(value)) return true;
        return false;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}

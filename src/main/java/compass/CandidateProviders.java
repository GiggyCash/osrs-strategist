package compass;
import lombok.*;
import static net.runelite.api.Skill.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import java.util.*;
import javax.inject.*;
import net.runelite.api.*;
import static compass.Text.get;

/** Provider of verified non-skill work that may compete with skill training. */
interface CandidateProvider
{
    List<Recommendation> candidates(StrategyContext context);

    /**
     * Generic queue entries owned by this richer workflow. The entries are
     * removed only when this provider actually emits a candidate, so missing
     * live evidence cannot silently erase an otherwise safe fallback.
     */
    default Set<String> supersededCandidateIds()
    {
        return emptySet();
    }
}

/** Lets an observed clue become actual DO NEXT work without making it spammy. */
@Singleton
class ClueCandidateProvider implements CandidateProvider
{
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null) return result;
        var clue = context.data().clue();
        if (clue == null || !clue.cluePresent) return result;

        var tier = ClueTier.fromText(clue.clueType);
        var account = context.data().account();
        Membership membership = account == null
                ? Membership.UNKNOWN
                : account.membership();
        if (!tier.isAvailableFor(membership)) return result;

        // Keep one stable activity id across clue tiers so learned preference,
        // snoozes, and older profiles continue to work after this richer model.
        var id = "clue:pending";
        var preferences = context.preferenceProfile();
        if (preferences.isOnCooldown(id)) return result;

        long age = max(0L,
                System.currentTimeMillis() - clue.getFirstSeenAtMillis());
        var ageHours = age / 3_600_000.0;
        double score = 39.0
                + tier.getPriorityBonus()
                + min(15.0, ageHours * 0.5)
                + preferences.weightFor(id) * 10.0;

        if (context.collectionist()) score += 6.0;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN) score -= 6.0;
        if (context.intent() == SessionIntent.QUICK_20_MIN) score += 4.0;
        if (context.intent() == SessionIntent.AFK) score -= 8.0;
        if (context.mode() == StrategyMode.EFFICIENT
                && (tier == ClueTier.BEGINNER || tier == ClueTier.EASY))
            score -= 7.0;

        String type = tier == ClueTier.UNKNOWN
                ? "clue"
                : tier.name().toLowerCase() + " clue";
        var step = clue.getCurrentStep();
        var reason = new StringBuilder();
        reason.append(get(1361)).append(type)
                .append(get(132));
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            reason.append(get(134));
        }
        if (step == null)
        {
            reason.append(get(135));
        }
        else
            reason.append(get(1362))
                    .append(step.getKind()).append(get(1363));

        boolean hardcore = context.accountMode() == AccountMode.HARDCORE_IRONMAN
                || context.accountMode() == AccountMode.HARDCORE_GROUP_IRONMAN;
        boolean wildernessHold = step != null && step.wilderness
                && (!context.allowsWilderness() || hardcore);
        if (wildernessHold)
        {
            score -= hardcore ? 30.0 : 18.0;
            reason.append(hardcore
                    ? get(136)
                    : get(137));
        }

        String title;
        String candidateId;
        Guidance guidance;
        Confidence confidence;
        if (step == null)
        {
            title = "Inspect " + type;
            candidateId = get(1643);
            guidance = new Guidance(
                    get(138),
                    null, "Inventory", get(139));
            confidence = Confidence.CHECK_NEEDED;
        }
        else if (wildernessHold)
        {
            title = "Hold " + type + get(1364);
            candidateId = get(1644);
            guidance = new Guidance(
                    context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                            ? get(140)
                            : get(141),
                    supplies(step), step.location,
                    get(133));
            confidence = Confidence.CHECK_NEEDED;
        }
        else
        {
            title = (step.requiresPreparation() ? "Prepare " : "Do ")
                    + type + ": " + step.getKind();
            candidateId = step.requiresPreparation()
                    ? get(1645) : id;
            guidance = new Guidance(step.getAction(),
                    supplies(step), step.location, note(step));
            // RuneLite proves the step, not every quest/access requirement.
            // Beginner steps are the only F2P-safe tier and can lead when no
            // additional setup, combat, light or Wilderness evidence remains.
            confidence = tier == ClueTier.BEGINNER
                    && !step.requiresPreparation()
                    ? Confidence.VERIFIED
                    : Confidence.CHECK_NEEDED;
        }

        result.add(new Recommendation(
                candidateId,
                title,
                reason.toString(),
                score,
                confidence,
                guidance,
                step != null && step.hasEnemy()
                        ? Safety.potentiallyIrreversible(
                                tier == ClueTier.BEGINNER)
                        : Safety.harmless(
                                tier == ClueTier.BEGINNER),
                StrategicValue.builder()
                        .accountModeFit(context.accountMode()
                                == AccountMode.ULTIMATE_IRONMAN ? -0.35 : 0.0)
                        .riskBurden(step != null && (step.wilderness
                                || step.hasEnemy()) ? 0.8 : 0.0)
                        .opportunityCost(context.intent()
                                == SessionIntent.AFK ? 0.7 : 0.15)
                        .evidence(step == null
                                ? get(1646)
                                : get(1647))
                        .build()
        ));
        return result;
    }

    private static String supplies(ClueStepSnapshot step)
    {
        if (step == null) return null;
        List<String> values = new ArrayList<>(step.itemRequirements);
        if (step.isRequiresSpade()) values.add("Spade");
        if (step.isRequiresLight()) values.add("Light source");
        if (step.hasEnemy()) values.add(get(1365) + step.getEnemy());
        if (values.isEmpty()) return null;
        return String.join(", ", values);
    }

    private static String note(ClueStepSnapshot step)
    {
        if (step == null) return null;
        List<String> values = new ArrayList<>();
        if (step.hasStashUnit())
            values.add("STASH: " + display(step.getStashUnit())
                    + get(1366));
        if (step.wilderness) values.add("Wilderness step");
        return values.isEmpty() ? null : String.join(". ", values) + ".";
    }

    private static String display(String value)
    {
        if (value == null || value.isEmpty()) return "";
        var lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

/** Surfaces observed near-complete Collection Log categories without guessing drops. */
@Singleton
class CollectionLogCandidateProvider implements CandidateProvider
{
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().collectionLog() == null) return result;

        var log = context.data().collectionLog();
        Set<String> categories = new HashSet<>(log.getCategoryTotals().keySet());
        categories.addAll(log.getCategoryCompleted().keySet());
        for (String category : categories)
        {
            var total = log.getCategoryTotal(category);
            var complete = log.getCategoryCompleted(category);
            if (total <= 0 || complete < 0 || complete >= total) continue;
            var missing = total - complete;
            if (!context.collectionist() && missing > 3) continue;

            var id = "collection-log:" + slug(category);
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var percent = complete * 100.0 / total;
            var score = 20.0 + min(20.0, percent * 0.20);
            if (missing == 1) score += 14.0;
            else if (missing == 2) score += 9.0;
            else if (missing == 3) score += 5.0;
            if (context.collectionist()) score += 9.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            result.add(new Recommendation(
                    id,
                    get(1649) + category,
                    complete + "/" + total + get(1369)
                            + missing + get(200),
                    score,
                    Confidence.CHECK_NEEDED,
                    null,
                    Safety.unknown()
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 3) return new ArrayList<>(result.subList(0, 3));
        return result;
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

/** Surfaces the next claimable Combat Achievement reward tier. */
@Singleton
class CombatAchievementCandidateProvider implements CandidateProvider
{
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().combatAchievements() == null
                || context.data().account() == null)
        {
            return result;
        }

        // F2P characters can complete a small subset of tasks, but cannot claim
        // tier rewards. Until Compass models those individual F2P tasks, a
        // reward-tier candidate would be misleading and is intentionally absent.
        if (!ContentAccessRules.hasVerifiedMembership(
                context.data().account().membership()))
        {
            return result;
        }

        var snapshot = context.data().combatAchievements();
        var next = snapshot.nextRewardTier();
        if (next == null) return result;

        var id = get(1651) + next.name().toLowerCase();
        if (context.preferenceProfile().isOnCooldown(id)) return result;

        var gap = max(0, next.getRewardPoints() - snapshot.getEarnedPoints());
        var score = 26.0;
        if (gap <= 20) score += 17.0;
        else if (gap <= 75) score += 10.0;
        else if (gap <= 200) score += 5.0;
        if (context.goal() == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
        {
            score += next.ordinal() <= CombatAchievementTier.ELITE.ordinal() ? 25.0 : 8.0;
        }
        score += context.preferenceProfile().weightFor(id) * 10.0;

        result.add(new Recommendation(
                id,
                get(1367) + pretty(next.name()),
                get(1368) + gap + " point"
                        + (gap == 1 ? "" : "s")
                        + get(131),
                score,
                Confidence.CHECK_NEEDED,
                null,
                Safety.potentiallyIrreversible(false)
        ));
        return result;
    }

    private static String pretty(String value)
    {
        var lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

/** Ranks the next unfinished tier across all 12 Achievement Diary regions. */
@Singleton
class DiaryCandidateProvider implements CandidateProvider
{
    private final DiaryTaskCatalog taskCatalog = new DiaryTaskCatalog();
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().diaries() == null
                || context.data().account() == null
                || !ContentAccessRules.hasVerifiedMembership(
                        context.data().account().membership()))
        {
            return result;
        }

        var diaries = context.data().diaries();
        for (String region : diaries.getRegions())
        {
            var next = nextIncomplete(diaries, region);
            if (next == null) continue;
            if ("Wilderness".equals(region) && !context.allowsWilderness())
            {
                continue;
            }

            String id = "diary:" + region.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-") + ":"
                    + next.name().toLowerCase();

            var score = tierScore(next);
            if (context.goal() == GoalType.DIARY_CAPE) score += 20.0;
            var observedTasks = diaries.completedIn(region);
            score += min(8.0, observedTasks * 0.15);
            score += context.preferenceProfile().weightFor(id) * 10.0;

            var tierTasks = taskCatalog.forTier(region, next);
            DiaryTaskDefinition ready = firstReadyIncomplete(
                    tierTasks, diaries, context);
            boolean tierObserved = tierTasks.stream().anyMatch(task ->
                    diaries.taskCompletion(task.getId()) != null);
            if (ready == null && tierObserved) continue;

            if (!tierObserved)
            {
                var verifyId = "verify:" + id;
                if (context.preferenceProfile().isOnCooldown(verifyId))
                    continue;
                result.add(new Recommendation(
                        verifyId,
                        "Check " + pretty(next.name()) + " " + region + " Diary",
                        get(205),
                        score,
                        Confidence.CHECK_NEEDED,
                        new Guidance(
                                "Open the " + region + get(206),
                                get(207),
                                get(1360) + region + ".",
                                get(208)),
                        Safety.harmless(false)
                ));
                continue;
            }

            if (context.preferenceProfile().isOnCooldown(ready.getId()))
                continue;

            result.add(new Recommendation(
                    ready.getId(),
                    "Complete a " + pretty(next.name()) + " " + region + " task",
                    get(209),
                    score,
                    Confidence.VERIFIED,
                    new Guidance(
                            ready.getTask(),
                            requirementSummary(ready),
                            region + get(210),
                            get(211)),
                    Safety.potentiallyIrreversible(false)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 5) return new ArrayList<>(result.subList(0, 5));
        return result;
    }

    private static DiaryTaskDefinition firstReadyIncomplete(
            List<DiaryTaskDefinition> tasks, DiarySnapshot snapshot,
            StrategyContext context)
    {
        for (DiaryTaskDefinition task : tasks)
            if (Boolean.FALSE.equals(snapshot.taskCompletion(task.getId()))
                    && requirementsMet(task, context)) return task;
        return null;
    }

    private static boolean requirementsMet(DiaryTaskDefinition task,
            StrategyContext context)
    {
        var account = context.data().account();
        var quests = context.data().quests();
        for (DiaryTaskRequirement requirement : task.requirements)
        {
            switch (requirement.getKind())
            {
                case SKILL:
                    if (account.level(requirement.getSkill())
                            < requirement.getLevel()) return false;
                    break;
                case QUEST:
                    QuestStatus status = quests == null ? QuestStatus.UNKNOWN
                            : quests.statusOf(requirement.getQuest());
                    if (status != QuestStatus.COMPLETE
                            && !(requirement.isStartedOnly()
                            && status == QuestStatus.IN_PROGRESS)) return false;
                    break;
                case COMBAT_LEVEL:
                case QUEST_POINTS:
                case ALTERNATIVE_CHECK:
                default:
                    return false;
            }
        }
        return true;
    }

    private static String requirementSummary(DiaryTaskDefinition task)
    {
        List<String> values = new ArrayList<>();
        for (DiaryTaskRequirement requirement : task.requirements)
        {
            if (requirement.getKind() == DiaryTaskRequirement.Kind.SKILL)
                values.add(requirement.getLevel() + " "
                        + requirement.getSkill().getName());
            else if (requirement.getKind() == DiaryTaskRequirement.Kind.QUEST)
                values.add(requirement.getQuest()
                        + (requirement.isStartedOnly() ? " started" : " complete"));
        }
        return values.isEmpty()
                ? get(212)
                : "Verified: " + String.join(", ", values) + ".";
    }

    private static DiaryTier nextIncomplete(DiarySnapshot diaries, String region)
    {
        for (DiaryTier tier : DiaryTier.values())
            if (!diaries.isTierComplete(region, tier)) return tier;
        return null;
    }

    private static double tierScore(DiaryTier tier)
    {
        switch (tier)
        {
            case EASY: return 42.0;
            case MEDIUM: return 39.0;
            case HARD: return 34.0;
            case ELITE:
            default: return 29.0;
        }
    }

    private static String pretty(String value)
    {
        var lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

/** Surfaces a practical next gear tier without pretending a universal BIS exists. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class GearCandidateProvider implements CandidateProvider
{
    private final GearProgressionCatalog catalog;
    private final GearAcquisitionCatalog acquisitionCatalog;

    public GearCandidateProvider(GearProgressionCatalog catalog)
    {
        this(catalog, new GearAcquisitionCatalog());
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null) return result;

        var account = context.data().account();
        var mode = context.accountMode();
        ItemIndex items = new ItemIndex(context.data(),
                context.usesGroupStorage());
        var f2pSafeOnly = account.membership() != Membership.P2P;
        var primaryStyle = primaryStyle(account);
        var targetTier = targetTier(account, f2pSafeOnly);

        for (GearProgressionEntry entry : catalog.all())
        {
            if (!ContentAccessRules.isContentAvailable(
                    account.membership(), entry.freeToPlay)) continue;
            if (!f2pSafeOnly && entry.tier == GearBudgetTier.F2P) continue;

            // A legal item on a Main can still be an account-ending suggestion
            // for a pure. Build policy is checked before style/tier ranking.
            if (!AccountBuildPolicy.allowsGearEntry(account, entry)) continue;

            if (entry.style != primaryStyle
                    && !(context.goal() == GoalType.RAID_READY
                    && entry.style == CombatStyle.HYBRID)) continue;
            if (entry.tier != targetTier
                    && !(context.goal() == GoalType.RAID_READY
                    && entry.style == CombatStyle.HYBRID)) continue;
            if (mode.isIronLike() && !entry.selfSourceFriendly) continue;
            if (mode == AccountMode.ULTIMATE_IRONMAN && !entry.uimFriendly) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && !entry.isHardcoreSafe()) continue;

            var id = "gear:" + entry.id;
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var score = 23.0;
            if (context.goal() == GoalType.GEAR_TARGET) score += 25.0;
            if (context.goal() == GoalType.RAID_READY
                    && entry.style == CombatStyle.HYBRID) score += 22.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            var build = AccountBuildPolicy.effectiveBuild(account);
            String buildNote = build == BuildType.STANDARD
                    ? ""
                    : get(1289) + AccountBuildPolicy.label(account) + ".";
            Guidance guidance = acquisitionGuidance(entry, mode,
                    items, context);
            String practical = practicalUpgrade(entry, items);

            result.add(new Recommendation(
                    id,
                    "Gear path: " + pretty(entry.tier) + " " + pretty(entry.style),
                    entry.getWeaponGuidance() + ". " + entry.note
                            + buildNote
                            + get(1290)
                            + practical + get(1291)
                            + entry.note,
                    score,
                    Confidence.CHECK_NEEDED,
                    guidance,
                    Safety.verifiedSafe(entry.freeToPlay)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 2) return new ArrayList<>(result.subList(0, 2));
        return result;
    }

    private Guidance acquisitionGuidance(
            GearProgressionEntry entry, AccountMode mode, ItemIndex items,
            StrategyContext context)
    {
        if (!items.primaryOwnershipObserved())
        {
            return new Guidance(
                    get(251),
                    get(255),
                    get(256),
                    get(257));
        }

        List<String> owned = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String target : entry.getRecommendedItems())
        {
            if (!isExactOwnershipTarget(target)) continue;
            if (items.has(target)) owned.add(target);
            else unresolved.add(target);
        }
        String next = unresolved.isEmpty()
                ? entry.getWeaponGuidance() : unresolved.get(0);
        var route = acquisitionCatalog.forItem(next);
        String action;
        if (route != null && !route.getSteps().isEmpty()
                && (!mode.usesGrandExchange() || !route.isTradeable()))
            action = route.getSteps().get(0).getAction();
        else if (mode.usesGrandExchange())
            action = get(258) + next
                    + get(259);
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
            action = get(260)
                    + next + get(261);
        else
            action = get(262) + next
                    + get(252);

        String supplies = get(1292)
                + (owned.isEmpty() ? "none" : String.join(", ", owned))
                + get(1293)
                + (unresolved.isEmpty() ? get(1294)
                : String.join(", ", unresolved)) + ".";
        String location = route == null
                ? get(253)
                : get(1295) + route.getSteps().get(0).getTarget()
                        + get(254);
        return new Guidance(action, supplies, location,
                entry.note + (route == null ? "" : " " + route.getValueRule()));
    }

    private static GearBudgetTier targetTier(AccountSnapshot account, boolean f2p)
    {
        if (f2p) return GearBudgetTier.F2P;
        int combatPeak = max(
                max(account.level(ATTACK), account.level(STRENGTH)),
                max(account.level(Skill.RANGED), account.level(Skill.MAGIC)));
        if (combatPeak >= 95) return GearBudgetTier.BIS;
        if (combatPeak >= 85) return GearBudgetTier.HIGH_END;
        if (combatPeak >= 70) return GearBudgetTier.MIDGAME;
        return GearBudgetTier.BUDGET;
    }

    private String practicalUpgrade(GearProgressionEntry entry,
            ItemIndex items)
    {
        for (String target : entry.getRecommendedItems())
            if (isExactOwnershipTarget(target) && !items.has(target)
                    && acquisitionCatalog.forItem(target) != null)
                return target + get(150);
        return entry.getWeaponGuidance();
    }

    static boolean isExactOwnershipTarget(String target)
    {
        if (target == null || target.trim().isEmpty()) return false;
        String value = target.toLowerCase(Locale.ROOT);
        return !value.contains(" or ") && !value.contains("/")
                && !value.contains("depending") && !value.contains("target-")
                && !value.contains(" mix") && !value.contains(" pieces")
                && !value.contains(" switch") && !value.contains(" as ")
                && !value.contains(" progression")
                && !value.contains("applicable");
    }

    private static CombatStyle primaryStyle(AccountSnapshot account)
    {
        var build = AccountBuildPolicy.effectiveBuild(account);
        if (build == BuildType.DEFENCE_PURE
                || build == BuildType.RANGE_TANK)
        {
            if (account.level(Skill.RANGED) > 1)
            {
                return CombatStyle.RANGED;
            }
        }

        int melee = max(account.level(ATTACK),
                account.level(STRENGTH));
        var ranged = account.level(Skill.RANGED);
        var magic = account.level(Skill.MAGIC);
        if (ranged >= melee && ranged >= magic) return CombatStyle.RANGED;
        if (magic >= melee) return CombatStyle.MAGIC;
        return CombatStyle.MELEE_SLASH;
    }

    private static String pretty(Enum<?> value)
    {
        var text = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

/** Turns verified POH absence into one concrete build or verification action. */
@Singleton
class InfrastructureCandidateProvider implements CandidateProvider
{
    private final InfrastructureMilestoneCatalog catalog;
    private final InfrastructureUnlockValueService values;
    private final UimRecurringPressureService recurringPressure;

    @Inject
    public InfrastructureCandidateProvider(
            InfrastructureMilestoneCatalog catalog,
            InfrastructureUnlockValueService values,
            UimRecurringPressureService recurringPressure)
    {
        this.catalog = catalog;
        this.values = values;
        this.recurringPressure = recurringPressure == null
                ? new UimRecurringPressureService() : recurringPressure;
    }

    public InfrastructureCandidateProvider(
            InfrastructureMilestoneCatalog catalog,
            InfrastructureUnlockValueService values)
    {
        this(catalog, values, new UimRecurringPressureService());
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null) return result;
        var account = context.data().account();
        if (account.membership() != Membership.P2P) return result;

        if (context.data().poh() == null)
        {
            result.add(verificationCandidate(context));
            return result;
        }

        UimRecurringPressureAssessment pressure =
                recurringPressure.observe(context);

        for (InfrastructureMilestone definition : catalog.all())
        {
            InfraAssessment assessment = values.assess(
                    definition.id, context);
            if (!assessment.canRecommendAcquisition()) continue;
            result.add(buildCandidate(definition, assessment, context,
                    pressure));
        }
        return result;
    }

    private static Recommendation verificationCandidate(
            StrategyContext context)
    {
        double modeValue = context.accountMode()
                == AccountMode.ULTIMATE_IRONMAN ? 0.9
                : AccountModePolicy.requiresSelfSourcing(
                        context.accountMode()) ? 0.55 : 0.25;
        return new Recommendation(
                get(1727),
                get(1424),
                get(301),
                34.0 + modeValue * 12.0,
                Confidence.CHECK_NEEDED,
                new Guidance(
                        get(309),
                        get(310),
                        get(1425),
                        get(311)),
                Safety.harmless(false),
                StrategicValue.builder()
                        .infrastructureValue(modeValue)
                        .accountModeFit(modeValue)
                        .evidence(get(1728))
                        .build());
    }

    private static Recommendation buildCandidate(
            InfrastructureMilestone definition,
            InfraAssessment assessment,
            StrategyContext context,
            UimRecurringPressureAssessment pressure)
    {
        double utility = assessment.strategicValue.ordinal()
                / (double) Priority.CRITICAL.ordinal();
        var score = 31.0 + utility * 26.0;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
            score += 8.0;
        else if (AccountModePolicy.requiresSelfSourcing(context.accountMode()))
            score += 3.0;
        if (context.intent() == SessionIntent.QUICK_20_MIN)
            score -= expensiveSetup(definition.id) ? 12.0 : 3.0;
        if (context.intent() == SessionIntent.AFK) score -= 8.0;
        if (context.mode() == StrategyMode.EFFICIENT) score += 2.0;
        boolean recurringRelief = pressure != null && pressure.isRepeated()
                && (definition.getBenefits().containsKey(
                        InfraBenefit.INVENTORY_RELIEF)
                || definition.getBenefits().containsKey(
                        InfraBenefit.STORAGE));
        if (recurringRelief) score += 12.0;

        String modeReason = context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                ? get(312)
                : AccountModePolicy.requiresSelfSourcing(context.accountMode())
                ? get(313)
                : get(314);
        if (recurringRelief)
            modeReason += get(315)
                    + String.join(" and ", pressure.getBlockedFamilies())
                    + get(316);
        return new Recommendation(
                get(1729) + definition.id,
                "Build " + definition.getName(),
                get(302)
                        + modeReason,
                score,
                Confidence.CHECK_NEEDED,
                new Guidance(
                        definition.getAction(), materials(definition.id),
                        get(1426),
                        get(303)),
                Safety.skill(false, CONSTRUCTION),
                StrategicValue.builder()
                        .infrastructureValue(utility)
                        .accountModeFit(context.accountMode()
                                == AccountMode.ULTIMATE_IRONMAN
                                ? utility : utility * 0.55)
                        .setupReuse(utility * 0.7)
                        .resourceFit(expensiveSetup(definition.id)
                                ? -0.75 : -0.25)
                        .unlockValue(recurringRelief ? 0.8 : 0.0)
                        .evidence("infrastructure:" + definition.id)
                        .evidence(recurringRelief
                                ? get(1730) : null)
                        .build());
    }

    private static boolean expensiveSetup(String id)
    {
        return get(1712).equals(id)
                || get(1711).equals(id)
                || get(1731).equals(id);
    }

    private static String materials(String id)
    {
        switch (id)
        {
            case "poh-costume-room": return "50,000 coins";
            case "poh-armour-case": return get(1427);
            case "poh-portal-chamber":
                return get(304);
            case "poh-superior-garden": return "75,000 coins";
            case "poh-restoration-pool":
                return get(305);
            case "poh-portal-nexus":
                return get(306);
            case "poh-spirit-tree":
                return get(1428);
            case "poh-basic-jewellery-box":
                return get(307);
            case "poh-fairy-ring":
                return get(308);
            default: return null;
        }
    }
}

/** Converts verified minigame unlocks into useful progression candidates. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class MinigameCandidateProvider implements CandidateProvider
{
    private final MinigameCatalog catalog;
    private final MinigameSetupCatalog setupCatalog;
    private final ItemRequirementEvaluator itemEvaluator;

    public MinigameCandidateProvider(MinigameCatalog catalog)
    {
        this(catalog, new MinigameSetupCatalog(), new ItemRequirementEvaluator());
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null
                || context.data().minigames() == null) return result;

        var account = context.data().account();
        var mode = context.accountMode();
        var snapshot = context.data().minigames();

        for (MinigameDefinition definition : catalog.all())
        {
            if (!snapshot.isUnlocked(definition.id)) continue;
            if (!definition.supports(mode)) continue;
            if (!ContentAccessRules.isContentAvailable(
                    account.membership(), definition.freeToPlay)) continue;
            if (definition.primarySkill != null
                    && account.level(definition.primarySkill)
                    < definition.getMinimumLevel()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && definition.riskLevel == RiskLevel.HIGH) continue;

            var id = "minigame:" + definition.id;
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var score = 28.0;
            if (definition.riskLevel == RiskLevel.NONE) score += 4.0;
            if (context.mode() == StrategyMode.RELAXED
                    && (definition.attention == AttentionLevel.LOW
                    || definition.attention == AttentionLevel.AFK)) score += 6.0;
            if (context.intent() == SessionIntent.LONG_SESSION) score += 2.0;
            if (context.collectionist()) score += 4.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            MinigameSetupProfile setup = setupCatalog.forActivity(
                    definition.id);
            ItemRequirementResult itemResult = setup == null ? null
                    : itemEvaluator.evaluate(setup.getItems(), context.data(),
                            context.usesGroupStorage());
            var verified = setup != null && itemResult.isSatisfied();
            Guidance guidance = setup == null
                    ? verificationGuidance(definition)
                    : "forestry".equals(definition.id)
                            ? forestryGuidance(account, verified, itemResult)
                            : new Guidance(
                            verified ? setup.instructions
                                    : itemResult.getAction() + " before " + definition.getName() + ".",
                            verified ? setup.supplies : itemResult.getAction(),
                            setup.location, definition.getRewardFocus() + ".");

            result.add(new Recommendation(
                    id,
                    definition.getName(),
                    definition.getRewardFocus()
                            + get(344),
                    score,
                    verified ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    guidance,
                    safetyFor(definition)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static Guidance verificationGuidance(
            MinigameDefinition definition)
    {
        var activity = definition.getName();
        return new Guidance(
                get(350) + activity
                        + get(1814),
                get(351),
                get(1492) + activity + ".",
                definition.getRewardFocus() + ".");
    }

    private static Guidance forestryGuidance(
            AccountSnapshot account, boolean verified,
            ItemRequirementResult itemResult)
    {
        var level = account.level(Skill.WOODCUTTING);
        var f2p = account.membership() != Membership.P2P;
        String tree;
        String location;
        if (level < 30)
        {
            tree = "oak trees";
            location = get(352);
        }
        else if (f2p || level < 45)
        {
            tree = "willow trees";
            location = get(353);
        }
        else if (level < 60)
        {
            tree = "maple trees";
            location = get(354);
        }
        else
        {
            tree = "yew trees";
            location = get(355);
        }
        boolean uim = AccountMode.fromTypeCode(account.modeCode())
                == AccountMode.ULTIMATE_IRONMAN;
        String loop = uim
                ? get(356) + tree
                        + get(357)
                : get(345) + tree
                        + get(346);
        return new Guidance(
                verified
                        ? loop
                        : itemResult.getAction() + get(1493),
                verified ? get(347)
                        : itemResult.getAction(),
                location + ".",
                get(348)
                        + (uim ? get(349) : ""));
    }

    private static Safety safetyFor(MinigameDefinition definition)
    {
        if (definition.riskLevel == RiskLevel.HIGH
                || definition.riskLevel == RiskLevel.IRREVERSIBLE)
            return Safety.potentiallyIrreversible(
                    definition.freeToPlay);
        if (definition.isCombatActivity())
            return Safety.potentiallyIrreversible(
                    definition.freeToPlay);
        if (definition.primarySkill != null)
            return Safety.skill(definition.freeToPlay,
                    definition.primarySkill);
        return Safety.harmless(definition.freeToPlay);
    }
}

/** Surfaces money/resource work only when cash pressure or a gear goal makes it relevant. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class MoneyMakingCandidateProvider implements CandidateProvider
{
    private final MoneyMakingCatalog catalog;

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null) return result;

        var account = context.data().account();
        var economy = context.data().economy();
        boolean explicitGearNeed = context.goal() == GoalType.GEAR_TARGET
                || context.goal() == GoalType.RAID_READY;
        boolean observedCashPressure = economy != null
                && economy.confidence == Confidence.VERIFIED
                && economy.coins < 1_000_000L;
        if (!explicitGearNeed && !observedCashPressure) return result;

        var mode = context.accountMode();
        for (MoneyMakingDefinition method : catalog.forAccount(mode))
        {
            if (!ContentAccessRules.isContentAvailable(
                    account.membership(), method.freeToPlay)) continue;
            if (method.primarySkill != null
                    && account.level(method.primarySkill) < method.getMinimumLevel()) continue;
            if (method.wilderness && !context.allowsWilderness()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && (method.wilderness
                    || method.riskLevel == RiskLevel.HIGH
                    || method.riskLevel == RiskLevel.IRREVERSIBLE)) continue;

            var id = method.id;
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var guidance = guidanceFor(method, context);
            // A catalog identity is not a recommendation. Price-sensitive,
            // encounter-dependent, or access-dependent methods stay hidden
            // until Compass can publish one coherent executable loop.
            if (guidance == null) continue;
            var score = 25.0;
            if (observedCashPressure) score += 12.0;
            if (explicitGearNeed) score += 7.0;
            if (method.riskLevel == RiskLevel.NONE) score += 4.0;
            if (method.attention == AttentionLevel.AFK
                    && context.intent() == SessionIntent.AFK) score += 8.0;
            if (method.attention == AttentionLevel.LOW
                    && context.mode() == StrategyMode.RELAXED) score += 5.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            String priceNote = method.isRequiresLivePrices()
                    ? get(378)
                    : "";
            result.add(new Recommendation(
                    id,
                    "Make money: " + method.getName(),
                    method.getDescription() + priceNote,
                    score,
                    Confidence.VERIFIED,
                    guidance,
                    safetyFor(method),
                    strategicValue(method, mode)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static Guidance guidanceFor(
            MoneyMakingDefinition method, StrategyContext context)
    {
        if (method == null || context == null) return null;
        if (!get(1802).equals(method.id)) return null;
        var mode = context.accountMode();
        if (!mode.isIronLike()
                || mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN
                || context.data().account().level(
                        Skill.AGILITY) < 60)
        {
            return null;
        }
        return new Guidance(
                get(379),
                get(380),
                get(381),
                get(382));
    }

    private static StrategicValue strategicValue(
            MoneyMakingDefinition method, AccountMode mode)
    {
        if (method != null && get(1802).equals(method.id)
                && mode != null && mode.isIronLike())
        {
            return StrategicValue.builder()
                    .accountModeFit(0.8)
                    .resourceFit(0.75)
                    .riskBurden(0.3)
                    .evidence(get(1803))
                    .evidence(get(1804))
                    .build();
        }
        return StrategicValue.neutral();
    }

    private static Safety safetyFor(MoneyMakingDefinition method)
    {
        if (method.riskLevel == RiskLevel.HIGH
                || method.riskLevel == RiskLevel.IRREVERSIBLE)
            return Safety.potentiallyIrreversible(
                    method.freeToPlay);
        if (method.primarySkill != null)
            return Safety.skill(method.freeToPlay,
                    method.primarySkill);
        return Safety.harmless(method.freeToPlay);
    }
}

/** Build-safe, account-aware progression upgrades that can interrupt raw XP. */
@Singleton
class ProgressionUpgradeCandidateProvider implements CandidateProvider
{
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return emptyList();
        }
        return new UpgradeScan(context).run();
    }

    /** Shared account evidence for every upgrade rule. */
    private static final class UpgradeScan
    {
        private final StrategyContext context;
        private final GameData data;
        private final AccountSnapshot account;
        private final AccountMode mode;
        private final ItemIndex items;
        private final List<Recommendation> result = new ArrayList<>();

        private UpgradeScan(StrategyContext context)
        {
            this.context = context;
            data = context.data();
            account = data.account();
            mode = context.accountMode();
            items = new ItemIndex(data, context.usesGroupStorage());
        }

        private List<Recommendation> run()
        {
            fighterTorso();
            abyssalWhip();
            dragonDefender();
            dragonScimitar();
            avaDevice();
            barrowsGloves();
            bowfa();
            anglerOutfit();
            questRewardGear();
            result.sort(Comparator.comparingDouble(
                    Recommendation::getScore).reversed());
            return result;
        }

        private boolean members()
        {
            return ContentAccessRules.hasVerifiedMembership(
                    account.membership());
        }

        private boolean owns(String... names)
        {
            if (items.has(names)) return true;
            if (mode != AccountMode.ULTIMATE_IRONMAN) return false;
            for (String name : names)
            {
                if (items.restrictedQuantity(name) > 0) return true;
            }
            return false;
        }

        private boolean eligible(String id)
        {
            return items.usableOwnershipObserved()
                    && !context.preferenceProfile().isOnCooldown(id);
        }

        private void add(String id, String title, String reason, double score,
                Confidence confidence, Guidance guidance,
                Safety safety)
        {
            result.add(new Recommendation(id, title, reason,
                    score + context.preferenceProfile().weightFor(id) * 10.0,
                    confidence, guidance, safety));
        }

        private boolean questComplete(String quest)
        {
            return data.quests() != null
                    && data.quests().statusOf(quest) == QuestStatus.COMPLETE;
        }

        private void questRewardGear()
        {
            if (!members() || data.quests() == null) return;
            questReward("salve-amulet", "Salve amulet", "Haunted Mine",
                    get(453), get(464), get(475),
                    items.has("Chisel"));
            questReward(get(1833), get(1834),
                    get(1394), get(486), get(497),
                    get(508), false);
            questReward("ibans-staff", "Iban's staff", get(1835),
                    get(519), get(530), get(541), false);
        }

        private void questReward(String suffix, String item, String quest,
                String action, String supplies, String note, boolean ready)
        {
            var id = "upgrade:" + suffix;
            // Retrieval-only UIM storage proves the item exists but does not
            // make it usable now; emit the retrieval action in that case.
            if (!eligible(id) || !questComplete(quest) || items.has(item)) return;
            boolean retrieval = mode == AccountMode.ULTIMATE_IRONMAN
                    && items.restrictedQuantity(item) > 0;
            add(id, (retrieval ? "Retrieve " : "Recover ") + item,
                    quest + get(457), 34.0,
                    ready && !retrieval ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    new Guidance(retrieval
                            ? get(455) + item + get(456) : action,
                            supplies + (mode == AccountMode.ULTIMATE_IRONMAN
                                    ? get(454) : ""),
                            get(458), note),
                    Safety.verifiedSafe(false));
        }

        private void dragonScimitar()
        {
            var id = get(1836);
            if (!members() || account.level(ATTACK) < 60
                    || !AccountBuildPolicy.allowsSkill(account, ATTACK)
                    || !eligible(id) || !questComplete(get(1837))
                    || owns("Dragon scimitar", "Abyssal whip",
                            get(1838), get(1395))) return;
            var cash = verifiedCoins(100_000L);
            String setup = mode == AccountMode.ULTIMATE_IRONMAN
                    ? get(459) : get(460);
            add(id, get(1396), get(461), 42.0,
                    cash ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(cash ? get(462) : get(463),
                            setup + (cash ? get(1397) : get(465))
                                    + get(466),
                            get(467), get(468)),
                    Safety.verifiedSafe(false));
        }

        private void avaDevice()
        {
            var id = get(1839);
            var ranged = account.level(Skill.RANGED);
            if (!members() || ranged < 30
                    || !AccountBuildPolicy.allowsSkill(account, Skill.RANGED)
                    || !eligible(id) || !questComplete(get(1840))
                    || owns("Ava's attractor", get(1841),
                            "Ava's assembler", get(1842),
                            "Dizana's quiver")) return;
            String device = ranged >= 50
                    ? get(1841) : "Ava's attractor";
            var replacement = ranged >= 50 ? get(469) : get(470);
            boolean ready = verifiedCoins(999L)
                    && (ranged < 50 || items.quantity("Steel arrow") >= 75);
            add(id, "Get " + device, get(471), 40.0,
                    ready ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(ready
                            ? get(472) + device + get(473)
                            : get(474) + device + ".",
                            replacement + (ready ? get(476)
                                    : get(477)),
                            get(478), get(479)),
                    Safety.verifiedSafe(false));
        }

        private void fighterTorso()
        {
            var id = get(1843);
            var build = AccountBuildPolicy.effectiveBuild(account);
            var defencePure = build == BuildType.DEFENCE_PURE;
            boolean protectedBuild = build == BuildType.SKILLER
                    || build == BuildType.F2P_SKILLER
                    || build == BuildType.PRAYER_SKILLER
                    || build == BuildType.TEN_HITPOINTS;
            if (!members() || account.level(DEFENCE) < 40
                    || protectedBuild
                    || (!defencePure && max(
                            account.level(ATTACK),
                            account.level(STRENGTH)) < 40)
                    || !eligible(id)
                    || owns("Fighter torso", get(1844),
                            get(1845), get(1398),
                            "Torva platebody", get(1399))) return;
            var score = mode.isIronLike() ? 48.0 : 37.0;
            if (defencePure) score += 8.0;
            if (goalIs(GoalType.GEAR_TARGET, GoalType.RAID_READY)) score += 10.0;
            add(id, get(1400), get(487), score,
                    Confidence.VERIFIED,
                    new Guidance(get(482), get(483)
                            + (defencePure ? get(480) : get(481)),
                            get(484), get(485)),
                    Safety.verifiedSafe(false));
        }

        private void abyssalWhip()
        {
            var id = get(1846);
            var attack = account.level(ATTACK);
            if (!members() || attack < 70
                    || !AccountBuildPolicy.allowsSkill(account, ATTACK)
                    || !eligible(id)
                    || owns("Abyssal whip", get(1847),
                            get(1848), get(1838),
                            get(1395), "Ghrazi rapier", "Osmumten's fang",
                            "Soulreaper axe", "Scythe of vitur")) return;
            var slayer = account.level(SLAYER);
            if (mode.usesGrandExchange())
            {
                add(id, get(1401), get(488), 41.0,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(489), get(490),
                                "Grand Exchange.", get(491)),
                        Safety.verifiedSafe(false));
                return;
            }
            if (slayer >= 85)
            {
                add(id, get(1401), get(492), 49.0,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(493), get(494),
                                get(495), get(496)),
                        Safety.verifiedSafe(false));
                return;
            }
            if (!goalIs(GoalType.MAX, GoalType.SLAYER_85,
                    GoalType.GEAR_TARGET, GoalType.RAID_READY)) return;
            add(id, get(1402), get(498),
                    max(24.0, 42.0 - (85 - slayer) * 0.8),
                    Confidence.VERIFIED,
                    new Guidance(get(1403) + slayer + get(499),
                            get(500), get(501), get(502)),
                    Safety.verifiedSafe(false));
        }

        private void dragonDefender()
        {
            var id = get(1849);
            var attack = account.level(ATTACK);
            var strength = account.level(STRENGTH);
            if (!members() || !eligible(id)
                    || account.level(DEFENCE) < 60 || attack < 60
                    || !AccountBuildPolicy.allowsSkill(account, ATTACK)
                    || !AccountBuildPolicy.allowsSkill(account, STRENGTH)
                    || (attack < 99 && strength < 99 && attack + strength < 130)
                    || owns("Dragon defender", get(1404),
                            get(1850), get(1405))) return;
            double score = 45.0 + (goalIs(GoalType.GEAR_TARGET,
                    GoalType.RAID_READY) ? 8.0 : 0.0);
            add(id, get(1406), get(507), score,
                    Confidence.VERIFIED,
                    new Guidance(get(503), get(504),
                            get(505), get(506)),
                    Safety.verifiedSafe(false));
        }

        private void barrowsGloves()
        {
            var id = get(1851);
            if (!members() || !eligible(id) || !questComplete(get(1198))
                    || owns("Barrows gloves", get(1852),
                            get(1853))) return;
            boolean elite = data.diaries() != null
                    && data.diaries().isTierComplete(get(1152),
                            DiaryTier.ELITE);
            var price = elite ? 104_000L : 130_000L;
            var economy = data.economy();
            boolean known = economy != null
                    && economy.confidence == Confidence.VERIFIED;
            var affordable = known && economy.coins >= price;
            String supplies = !known
                    ? get(509) + format(price) + " coins."
                    : !affordable
                    ? "You have " + format(economy.coins)
                            + get(1407) + format(price) + ". You are "
                            + format(price - economy.coins) + " coins short."
                    : get(1408) + format(price) + get(1854);
            var score = 48.0;
            if (goalIs(GoalType.BARROWS_GLOVES)) score += 35.0;
            if (goalIs(GoalType.GEAR_TARGET, GoalType.RAID_READY)) score += 10.0;
            add(id, get(1409), get(514), score,
                    affordable ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(get(510), supplies, get(511),
                            elite ? get(512) : get(513)),
                    Safety.verifiedSafe(false));
        }

        private void bowfa()
        {
            var id = "upgrade:bowfa";
            if (!members() || !goalIs(GoalType.BOWFA, GoalType.GEAR_TARGET,
                    GoalType.RAID_READY) || !questComplete(get(1721))
                    || !eligible(id)
                    || owns(get(1683), get(1345))) return;
            var seed = owns(get(1410));
            var shards = items.quantity("Crystal shard");
            var score = goalIs(GoalType.BOWFA) ? 78.0 : 54.0;
            if (seed)
            {
                boolean selfSing = account.level(SMITHING) >= 82
                        && account.level(CRAFTING) >= 82;
                var needed = selfSing ? 100 : 150;
                var shortfall = max(0, needed - shards);
                add(id, get(1411), get(515), score,
                        shortfall == 0 ? Confidence.VERIFIED
                                : Confidence.CHECK_NEEDED,
                        new Guidance(selfSing
                                ? get(516) + needed + get(517)
                                : get(518) + needed + get(520),
                                shortfall == 0
                                ? get(521) + needed + get(1412)
                                : get(1413) + shortfall + get(1414)
                                        + (shortfall == 1 ? "" : "s")
                                        + get(1415),
                                get(522), get(523)),
                        Safety.potentiallyIrreversible(false));
                return;
            }
            if (mode.usesGrandExchange())
            {
                add(id, get(1416), get(524), score,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(525), get(526),
                                get(527), get(528)),
                        Safety.potentiallyIrreversible(false));
                return;
            }
            boolean hardcore = mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN;
            boolean deathStorage = mode == AccountMode.ULTIMATE_IRONMAN
                    && data.storage() != null
                    && !data.storage().getDeathStorageItems().isEmpty();
            add(id, get(1417), get(529), score,
                    hardcore || deathStorage ? Confidence.CHECK_NEEDED
                            : Confidence.VERIFIED,
                    new Guidance(get(531), get(532), get(1418),
                            hardcore ? get(533)
                                    : deathStorage ? get(534)
                                    : get(535)),
                    Safety.potentiallyIrreversible(false));
        }

        private void anglerOutfit()
        {
            var id = get(1855);
            var fishing = account.level(FISHING);
            if (!members() || fishing < 15 || !eligible(id)) return;
            int pieces = (owns("Angler hat", get(1214)) ? 1 : 0)
                    + (owns("Angler top", get(1856)) ? 1 : 0)
                    + (owns("Angler waders", get(1215)) ? 1 : 0)
                    + (owns("Angler boots", get(1216)) ? 1 : 0);
            if (pieces >= 4) return;
            var xp = account.xp(FISHING);
            if (xp <= 0) xp = Experience.getXpForLevel(fishing);
            var remaining = max(0, Experience.getXpForLevel(99) - xp);
            double score = 16.0 + (context.collectionist() ? 30.0 : 0.0)
                    + (fishing >= 82 ? 17.0 : 0.0)
                    + (goalIs(GoalType.MAX) && remaining >= 5_000_000
                            ? 12.0 : 0.0)
                    + (goalIs(GoalType.GEAR_TARGET) ? 5.0 : 0.0)
                    + pieces * 2.0;
            if (score + context.preferenceProfile().weightFor(id) * 10.0
                    < 25.0) return;
            add(id, get(1420) + pieces + "/4)", get(540), score,
                    Confidence.VERIFIED,
                    new Guidance(get(536), get(537), get(538),
                            get(1419) + pieces + get(539)),
                    Safety.skill(false, FISHING));
        }

        private boolean goalIs(GoalType... goals)
        {
            for (GoalType goal : goals)
            {
                if (context.goal() == goal) return true;
            }
            return false;
        }

        private boolean verifiedCoins(long needed)
        {
            var economy = data.economy();
            return economy != null
                    && economy.confidence == Confidence.VERIFIED
                    && economy.coins >= needed;
        }
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}

/** Makes explicitly verified/realistic PvM assessments eligible for DO NEXT. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class PvmCandidateProvider implements CandidateProvider
{
    private final PvmActivityCatalog catalog;

    public PvmCandidateProvider()
    {
        this(new PvmActivityCatalog());
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().pvm() == null) return result;

        var mode = context.accountMode();
        var account = context.data().account();
        var preferences = context.preferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.data().pvm().getReadinessByActivity().entrySet())
        {
            var readiness = entry.getValue();
            if (readiness == null) continue;
            if (readiness.confidence == Confidence.BLOCKED) continue;

            var definition = catalog.match(entry.getKey());
            if (definition != null)
            {
                if (account == null || !ContentAccessRules.isContentAvailable(
                        account.membership(), definition.freeToPlay)) continue;
                if (definition.wilderness && !context.allowsWilderness()) continue;
                if ((mode == AccountMode.HARDCORE_IRONMAN
                        || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                        && !definition.isHardcoreSafeByDefault()) continue;
            }
            else continue; // Unknown/future metadata cannot prove beta readiness.

            String normalizedKey = entry.getKey().startsWith("pvm:")
                    ? entry.getKey().substring(4) : entry.getKey();
            var id = "pvm:" + normalizedKey;
            if (preferences.isOnCooldown(id)) continue;
            var ready = readiness.isReadyForRecommendation();
            var relevant = progressionRelevant(definition, context);
            // A generic hiscore identity is not a reason to boss. Preparation
            // competes globally only when a goal/task makes the encounter
            // relevant and a curated readiness floor can name concrete work.
            if (!ready && (!catalog.hasCuratedReadinessProfile(definition.id)
                    || !relevant)) continue;

            double score = (ready ? 48.0 : 32.0)
                    + preferences.weightFor(id) * 10.0;
            if (relevant) score += 14.0;
            if (definition != null)
            {
                if (definition.isRaid()) score += 4.0;
                if (definition.riskLevel == RiskLevel.HIGH
                        && AccountModePolicy.isRiskSensitive(mode)) score -= 8.0;
            }

            var title = definition == null ? entry.getKey() : definition.getName();
            String missing = readiness.missingRequirements.isEmpty()
                    ? "" : String.join("; ", readiness.missingRequirements);
            if (!ready && missing.trim().isEmpty()) continue;
            Guidance guidance = ready
                    ? readyGuidance(definition, title)
                    : new Guidance(
                            get(417) + title + ": " + missing + ".",
                            missing,
                            get(428),
                            get(433));
            result.add(new Recommendation(
                    id,
                    "Do " + title,
                    ready
                            ? get(434)
                            : get(435),
                    score,
                    ready ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    guidance,
                    Safety.potentiallyIrreversible(
                            definition.freeToPlay)
            ));
        }
        return result;
    }

    private static Guidance readyGuidance(
            PvmActivity definition, String title)
    {
        if (definition != null && "pvm:tztok_jad".equals(definition.id))
        {
            return new Guidance(
                    get(436),
                    get(437),
                    get(438),
                    get(439));
        }
        if (definition != null && "pvm:obor".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(418),
                    get(419),
                    get(420));
        if (definition != null && "pvm:bryophyta".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(421),
                    get(422),
                    get(423));
        if (definition != null && "pvm:scurrius".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(424),
                    get(1338),
                    get(425));
        return new Guidance(
                "Attempt " + title + get(426),
                get(427),
                get(429),
                get(430));
    }

    private static Guidance simpleReadyGuidance(String title,
            String action, String location, String supplies)
    {
        return new Guidance(action, supplies, location,
                get(431)
                        + title + get(432));
    }

    private static boolean progressionRelevant(PvmActivity definition,
            StrategyContext context)
    {
        if (definition == null || context == null) return false;
        var id = definition.id;
        var goal = context.goal();
        if (goal == GoalType.BOWFA)
            return id.contains("gauntlet");
        if (goal == GoalType.FIRE_CAPE)
            return id.endsWith("tztok_jad");
        if (goal == GoalType.INFERNAL_CAPE)
            return id.endsWith("tztok_jad") || id.endsWith("tzkal_zuk");
        if (goal == GoalType.RAID_READY)
            return definition.isRaid();
        if (goal == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
            return catalogChallengeEncounter(id);

        SlayerSnapshot slayer = context.data() == null
                ? null : context.data().slayer();
        if (slayer == null || !slayer.hasTask()) return false;
        var task = Names.words(slayer.taskName);
        var boss = Names.words(definition.getName());
        return boss.contains(task) || task.contains(boss)
                || (id.endsWith("kraken") && task.contains("kraken"))
                || (id.endsWith("cerberus") && task.contains("hellhound"))
                || (id.endsWith(get(1809)) && task.contains("hydra"))
                || (id.endsWith("araxxor") && task.contains("araxyte"));
    }

    private static boolean catalogChallengeEncounter(String id)
    {
        return id != null && (id.contains("gauntlet") || id.contains("raid")
                || id.contains(get(1810)) || id.contains(get(1811))
                || id.contains(get(1812)) || id.endsWith("tzkal_zuk")
                || id.endsWith("sol_heredit") || id.endsWith("nex"));
    }

}

/**
 * Turns RuneLite's complete live quest-state snapshot into ranked quest work.
 *
 * <p>Quest membership and restricted-build safety are hard gates. An unfinished
 * quest whose remaining prerequisites are not yet proven stays Check Needed and
 * therefore cannot occupy the primary DO NEXT slot.</p>
 */
@Singleton
class QuestCandidateProvider implements CandidateProvider
{
    private final QuestPriorityCatalog priorityCatalog;
    private final QuestKnowledgeCatalog knowledgeCatalog;
    private final QuestRequirementResolver requirementResolver;
    private final GoalDependencyProvenanceService goalProvenanceService;

    @Inject
    public QuestCandidateProvider(QuestPriorityCatalog priorityCatalog,
            QuestKnowledgeCatalog knowledgeCatalog,
            QuestRequirementResolver requirementResolver,
            GoalDependencyProvenanceService goalProvenanceService)
    {
        this.priorityCatalog = priorityCatalog;
        this.knowledgeCatalog = knowledgeCatalog;
        this.requirementResolver = requirementResolver;
        this.goalProvenanceService = goalProvenanceService == null
                ? new GoalDependencyProvenanceService() : goalProvenanceService;
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().quests() == null
                || context.data().account() == null)
        {
            return result;
        }

        var account = context.data().account();
        var membership = account.membership();
        var preferences = context.preferenceProfile();
        Set<String> neededPrerequisites = neededPrerequisites(
                context.data().quests());
        for (Map.Entry<String, QuestStatus> entry
                : context.data().quests().quests().entrySet())
        {
            var status = entry.getValue();
            if (status == null || status == QuestStatus.COMPLETE
                    || status == QuestStatus.UNKNOWN)
            {
                continue;
            }

            var questName = entry.getKey();
            if (!QuestMembershipPolicy.isAvailable(questName, membership))
            {
                continue;
            }

            // Quest XP is irreversible. Restricted builds fail closed: a quest
            // that is not on the curated safe list never reaches the queue.
            if (!RestrictedQuestPolicy.isSafe(account, questName))
            {
                continue;
            }

            var id = "quest:" + slug(questName);
            if (preferences.isOnCooldown(id)) continue;

            QuestPriorityCatalog.QuestPriority priority =
                    priorityCatalog.priorityFor(questName);
            var definition = knowledgeCatalog.definitionFor(questName);
            QuestResolution resolution = definition == null ? null
                    : requirementResolver.resolve(definition, context);
            boolean requiredForGoal = goalProvenanceService.isRequiredQuest(
                    context.goal(), questName, context);
            double score = requiredForGoal ? 42.0
                    : baseScore(context.questTolerance);
            String reason;

            if (status == QuestStatus.IN_PROGRESS)
            {
                score += 12.0;
                reason = get(546);
            }
            else
            {
                score -= 7.0;
                reason = get(547);
            }

            var build = AccountBuildPolicy.effectiveBuild(account);
            if (build != BuildType.STANDARD)
            {
                reason += get(548)
                        + AccountBuildPolicy.label(account) + " build.";
            }

            if (priority != null)
            {
                score += priority.getScoreBonus();
                reason += " " + priority.getReason() + ".";
            }

            if (neededPrerequisites.contains(Names.words(questName)))
            {
                score += 24.0;
                reason += get(549);
            }

            if (requiredForGoal)
                reason += get(550);

            score += preferences.weightFor(id) * 10.0;
            score += preferences.timedScoreAdjustmentFor(id);

            Confidence confidence = resolution == null
                    ? Confidence.CHECK_NEEDED
                    : resolution.confidence;
            Guidance guidance = resolution == null ? null
                    : resolution.guidance;
            if (resolution != null) reason += " " + resolution.reason + ".";

            String title = (status == QuestStatus.IN_PROGRESS ? "Continue " : "Quest: ")
                    + questName;
            if (resolution != null
                    && resolution.confidence == Confidence.CHECK_NEEDED
                    && guidance != null && guidance.getAction() != null
                    && !guidance.getAction().trim().isEmpty())
                title = "Prepare for " + questName + ": "
                        + guidance.getAction().replaceFirst("\\.$", "");
            result.add(new Recommendation(
                    id,
                    title,
                    reason,
                    score,
                    confidence,
                    guidance,
                    resolution == null
                            ? Safety.unknown()
                            : resolution.safetyEvidence
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 8)
        {
            return new ArrayList<>(result.subList(0, 8));
        }
        return result;
    }

    private static double baseScore(QuestTolerance tolerance)
    {
        if (tolerance == null) return 30.0;
        switch (tolerance)
        {
            case HIGH: return 47.0;
            case LOW: return 18.0;
            case NORMAL:
            default: return 31.0;
        }
    }

    private static String slug(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private Set<String> neededPrerequisites(QuestSnapshot quests)
    {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, QuestStatus> entry : quests.quests().entrySet())
        {
            if (entry.getValue() == QuestStatus.COMPLETE) continue;
            var definition = knowledgeCatalog.definitionFor(entry.getKey());
            if (definition == null) continue;
            for (String prerequisite : definition.prerequisites)
                if (quests.statusOf(prerequisite) != QuestStatus.COMPLETE)
                    result.add(Names.words(prerequisite));
        }
        return result;
    }

}

/**
 * Cross-skill detours that solve a real resource shortage while advancing a
 * second useful goal.
 *
 * <p>A detour must beat the direct acquisition route on more than novelty. This
 * provider therefore keeps scores modest unless the account is an Iron-style
 * account, the shortage is observed, and the detour also progresses a skill the
 * account still needs. VERIFIED detours include concrete guidance so they can
 * legitimately compete for DO NEXT instead of existing as decorative options.</p>
 */
@Singleton
class ResourceDetourCandidateProvider
        implements CandidateProvider
{
    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return result;
        }

        var account = context.data().account();
        if (!ContentAccessRules.hasVerifiedMembership(
                account.membership())) return result;
        var mode = context.accountMode();
        if (!mode.isIronLike() || mode == AccountMode.ULTIMATE_IRONMAN)
        {
            return result;
        }

        ItemIndex items = new ItemIndex(
                context.data(), context.usesGroupStorage());
        if (!items.resourceContainersObserved()) return result;

        plankDetours(context, account, items, result);
        result.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        return result;
    }

    private static void plankDetours(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!constructionRelevant(context.goal())) return;
        var construction = account.level(CONSTRUCTION);
        if (construction >= 70) return;

        int planks = items.quantity(
                "Plank", "Oak plank", "Teak plank", "Mahogany plank");
        if (planks >= 150) return;

        var fishing = account.level(FISHING);
        if (fishing >= 35 && fishing < 80)
        {
            var id = get(1957);
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                var score = 27.0;
                if (construction < 50) score += 5.0;
                if (fishing < 70) score += 5.0;
                if (context.intent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        get(599),
                        "Only " + planks + get(602),
                        get(603),
                        get(604)
                );
                result.add(new Recommendation(
                        id,
                        get(1296),
                        "Only " + planks + get(605),
                        score,
                        Confidence.VERIFIED,
                        guidance,
                        Safety.skill(false, FISHING)));
            }
        }

        var firemaking = account.level(FIREMAKING);
        int logs = items.quantity(
                "Logs", "Oak logs", "Willow logs", "Maple logs",
                "Yew logs", "Teak logs", "Mahogany logs");
        if (firemaking >= 50 && firemaking < 80 && logs < 100
                && account.membership() == Membership.P2P
                && context.accountMode() != AccountMode.HARDCORE_IRONMAN
                && context.accountMode() != AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            var id = get(1958);
            if (!context.preferenceProfile().isOnCooldown(id))
            {
                var score = 20.0;
                if (account.level(WOODCUTTING) < 60) score += 2.0;
                if (context.intent() == SessionIntent.LONG_SESSION)
                    score += 3.0;
                score += context.preferenceProfile().weightFor(id) * 10.0;

                Guidance guidance = new Guidance(
                        get(606),
                        "Only " + logs + get(1959) + planks + get(607),
                        get(608),
                        get(609)
                );
                result.add(new Recommendation(
                        id,
                        get(600),
                        get(601),
                        score,
                        Confidence.VERIFIED,
                        guidance,
                        Safety.skill(false, FIREMAKING)));
            }
        }
    }

    private static boolean constructionRelevant(GoalType goal)
    {
        if (goal == null) return true;
        switch (goal)
        {
            case AUTOMATIC:
            case MAX:
            case QUEST_CAPE:
            case BARROWS_GLOVES:
            case PRIFDDINAS:
            case DIARY_CAPE:
            case RAID_READY:
            case TOTAL_2000:
            case BASE_70S:
            case CUSTOM:
                return true;
            default:
                return false;
        }
    }
}

/** Exposes the single current Slayer state-machine action to the shared queue. */
@Singleton
class SlayerCandidateProvider implements CandidateProvider
{
    private final SlayerStrategist strategist;

    @Inject
    public SlayerCandidateProvider(SlayerStrategist strategist)
    {
        this.strategist = strategist == null ? new SlayerStrategist() : strategist;
    }

    public SlayerCandidateProvider()
    {
        this(new SlayerStrategist());
    }

    @Override
    public Set<String> supersededCandidateIds()
    {
        return singleton("skill:slayer");
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        var result = strategist.assess(context);
        if (result == null || result.guidance == null)
            return emptyList();

        var slayer = context.data().slayer();
        String id;
        String title;
        if (result.getRecommendedReward() != null)
        {
            id = "slayer:unlock:" + result.getRecommendedReward().id;
            title = "Unlock " + result.getRecommendedReward().getDisplayName();
        }
        else if (result.getAssignmentState() == SlayerState.UNKNOWN)
        {
            id = get(1970);
            title = get(1460);
        }
        else if (result.getAssignmentState() == SlayerState.CHOICE_PENDING)
        {
            id = get(1971);
            title = result.getRecommendedOffer() == null
                    ? get(1461)
                    : "Choose " + result.getRecommendedOffer().taskName
                            + " from Mortimer";
        }
        else if (result.getAssignmentState() == SlayerState.NO_TASK)
        {
            id = "slayer:get-task";
            title = get(1972) + result.getMaster().getDisplayName();
        }
        else
        {
            var task = slayer == null ? "Slayer task" : slayer.taskName;
            var decision = result.getDecision();
            switch (decision)
            {
                case BLOCK:
                    id = get(1973);
                    title = "Block " + task;
                    break;
                case SKIP:
                    id = get(1974);
                    title = "Skip " + task;
                    break;
                case PREP_FIRST:
                    id = get(1975);
                    title = "Prepare for " + task;
                    break;
                case ALTERNATIVE:
                    id = get(1976);
                    title = result.getSelectedAlternativeName() != null
                            ? "Use " + result.getSelectedAlternativeName()
                            : get(1462);
                    break;
                case DO:
                default:
                    id = "slayer:do-task";
                    title = task + " — do this task";
                    break;
            }
        }

        Safety safety = result.getDecision()
                == SlayerDecision.DO
                ? Safety.skill(false, SLAYER)
                : result.getDecision() == SlayerDecision.ALTERNATIVE
                    && result.getSelectedAlternativeName() != null
                    ? Safety.potentiallyIrreversible(false)
                    : Safety.verifiedSafe(false);
        StrategicValue strategicValue = strategicValue(result,
                context);
        return singletonList(new Recommendation(id, title,
                result.reason, result.score, result.confidence,
                result.guidance, safety, strategicValue));
    }

    private static StrategicValue strategicValue(
            SlayerDecisionResult result, StrategyContext context)
    {
        StrategicValue.Builder builder =
                StrategicValue.builder()
                        .evidence(get(1977));
        var task = result.getTaskProfile();
        if (task != null)
        {
            builder.resourceFit((task.getResourceValue() - 2.5) / 2.5)
                    .riskBurden(task.inherentRisk == RiskLevel.NONE
                            || task.inherentRisk == RiskLevel.LOW ? 0.0
                            : task.inherentRisk == RiskLevel.MEDIUM
                                    ? 0.5 : 1.0)
                    .setupReuse(max(0.0,
                            1.0 - task.setupBurden / 5.0));
        }
        if (context != null && context.goal() == GoalType.SLAYER_85)
            builder.unlockValue(1.0);
        if (result.getRecommendedReward() != null)
            builder.unlockValue(1.0).infrastructureValue(0.6)
                    .evidence(get(1978));
        return builder.build();
    }
}

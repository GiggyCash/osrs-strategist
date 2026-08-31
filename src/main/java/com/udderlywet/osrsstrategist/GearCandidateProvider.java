package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Surfaces a practical next gear tier without pretending a universal BIS exists. */
@Singleton
public class GearCandidateProvider implements CandidateProvider
{
    private final GearProgressionCatalog catalog;
    private final GearAcquisitionCatalog acquisitionCatalog;
    private final ContextualGearDecisionService decisionService =
            new ContextualGearDecisionService();

    @Inject
    public GearCandidateProvider(GearProgressionCatalog catalog,
            GearAcquisitionCatalog acquisitionCatalog)
    {
        this.catalog = catalog;
        this.acquisitionCatalog = acquisitionCatalog;
    }

    public GearCandidateProvider(GearProgressionCatalog catalog)
    {
        this(catalog, new GearAcquisitionCatalog());
    }

    @Override
    public String getId() { return "gear-candidates"; }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null) return result;

        AccountSnapshot account = context.data().account();
        AccountMode mode = context.accountMode();
        ItemIndex items = new ItemIndex(context.data(),
                context.isUseGroupStorage());
        boolean f2pSafeOnly = account.getMembershipStatus() != MembershipStatus.P2P;
        CombatStyle primaryStyle = primaryStyle(account);
        GearBudgetTier targetTier = targetTier(account, f2pSafeOnly);

        for (GearProgressionEntry entry : catalog.all())
        {
            if (!ContentAccessRules.isContentAvailable(
                    account.getMembershipStatus(), entry.isFreeToPlay())) continue;
            if (!f2pSafeOnly && entry.getTier() == GearBudgetTier.F2P) continue;

            // A legal item on a Main can still be an account-ending suggestion
            // for a pure. Build policy is checked before style/tier ranking.
            if (!AccountBuildPolicy.allowsGearEntry(account, entry)) continue;

            if (entry.getStyle() != primaryStyle
                    && !(context.getActiveGoal() == GoalType.RAID_READY
                    && entry.getStyle() == CombatStyle.HYBRID)) continue;
            if (entry.getTier() != targetTier
                    && !(context.getActiveGoal() == GoalType.RAID_READY
                    && entry.getStyle() == CombatStyle.HYBRID)) continue;
            if (mode.isIronLike() && !entry.isSelfSourceFriendly()) continue;
            if (mode == AccountMode.ULTIMATE_IRONMAN && !entry.isUimFriendly()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && !entry.isHardcoreSafe()) continue;

            String id = "gear:" + entry.getId();
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            double score = 23.0;
            if (context.getActiveGoal() == GoalType.GEAR_TARGET) score += 25.0;
            if (context.getActiveGoal() == GoalType.RAID_READY
                    && entry.getStyle() == CombatStyle.HYBRID) score += 22.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
            String buildNote = build == RestrictedBuildType.STANDARD
                    ? ""
                    : get(1289) + AccountBuildPolicy.label(account) + ".";
            Guidance guidance = acquisitionGuidance(entry, mode,
                    items, context);
            ContextualGearAssessment assessment = decisionService.assess(entry,
                    context);
            ContextualGearDecision practical = assessment.get(
                    GearDecisionKind.BEST_PRACTICAL_UPGRADE);
            ContextualGearDecision targetBest = assessment.get(
                    GearDecisionKind.TARGET_SPECIFIC_BEST);

            result.add(new Recommendation(
                    id,
                    "Gear path: " + pretty(entry.getTier()) + " " + pretty(entry.getStyle()),
                    entry.getWeaponGuidance() + ". " + entry.getNote()
                            + buildNote
                            + get(1290)
                            + practical.getValue() + get(1291)
                            + targetBest.getValue(),
                    score,
                    Confidence.CHECK_NEEDED,
                    guidance,
                    SafetyEvidence.verifiedSafe(entry.isFreeToPlay())
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
            if (!ContextualGearDecisionService
                    .isExactOwnershipTarget(target)) continue;
            if (items.has(target)) owned.add(target);
            else unresolved.add(target);
        }
        String next = unresolved.isEmpty()
                ? entry.getWeaponGuidance() : unresolved.get(0);
        GearAcquisitionRoute route = acquisitionCatalog.forItem(next);
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
                entry.getNote() + (route == null ? "" : " " + route.getValueRule()));
    }

    private static GearBudgetTier targetTier(AccountSnapshot account, boolean f2p)
    {
        if (f2p) return GearBudgetTier.F2P;
        int combatPeak = Math.max(
                Math.max(account.getSkillLevel(Skill.ATTACK), account.getSkillLevel(Skill.STRENGTH)),
                Math.max(account.getSkillLevel(Skill.RANGED), account.getSkillLevel(Skill.MAGIC)));
        if (combatPeak >= 95) return GearBudgetTier.BIS;
        if (combatPeak >= 85) return GearBudgetTier.HIGH_END;
        if (combatPeak >= 70) return GearBudgetTier.MIDGAME;
        return GearBudgetTier.BUDGET;
    }

    private static CombatStyle primaryStyle(AccountSnapshot account)
    {
        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        if (build == RestrictedBuildType.DEFENCE_PURE
                || build == RestrictedBuildType.RANGE_TANK)
        {
            if (account.getSkillLevel(Skill.RANGED) > 1)
            {
                return CombatStyle.RANGED;
            }
        }

        int melee = Math.max(account.getSkillLevel(Skill.ATTACK),
                account.getSkillLevel(Skill.STRENGTH));
        int ranged = account.getSkillLevel(Skill.RANGED);
        int magic = account.getSkillLevel(Skill.MAGIC);
        if (ranged >= melee && ranged >= magic) return CombatStyle.RANGED;
        if (magic >= melee) return CombatStyle.MAGIC;
        return CombatStyle.MELEE_SLASH;
    }

    private static String pretty(Enum<?> value)
    {
        String text = value.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Surfaces a practical next gear tier without pretending a universal BIS exists. */
@Singleton
public class GearCandidateProvider implements StrategyCandidateProvider
{
    private final GearProgressionCatalog catalog;
    private final GearAcquisitionCatalog acquisitionCatalog;

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
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null) return result;

        AccountSnapshot account = context.getData().getAccount();
        AccountMode mode = context.getAccountMode();
        ObservedItemIndex items = new ObservedItemIndex(context.getData(),
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
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;
            double score = 23.0;
            if (context.getActiveGoal() == GoalType.GEAR_TARGET) score += 25.0;
            if (context.getActiveGoal() == GoalType.RAID_READY
                    && entry.getStyle() == CombatStyle.HYBRID) score += 22.0;
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
            String buildNote = build == RestrictedBuildType.STANDARD
                    ? ""
                    : " Build protected: " + AccountBuildPolicy.label(account) + ".";
            RecommendationGuidance guidance = acquisitionGuidance(entry, mode,
                    items);

            result.add(new StrategyCandidate(
                    id,
                    "Gear path: " + pretty(entry.getTier()) + " " + pretty(entry.getStyle()),
                    entry.getWeaponGuidance() + ". " + entry.getNote()
                            + buildNote
                            + " Compare owned equipment, bank/storage, acquisition route, GP, and the target encounter before choosing a purchase or grind.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED,
                    guidance,
                    CandidateSafetyEvidence.verifiedSafe(entry.isFreeToPlay())
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 2) return new ArrayList<>(result.subList(0, 2));
        return result;
    }

    private RecommendationGuidance acquisitionGuidance(
            GearProgressionEntry entry, AccountMode mode, ObservedItemIndex items)
    {
        if (!items.bankObserved() && mode != AccountMode.ULTIMATE_IRONMAN)
        {
            return new RecommendationGuidance(
                    "Open the bank once to compare this gear path with verified ownership.",
                    "Bank ownership is currently unknown; no item is being called missing yet.",
                    "Any bank.",
                    "This remains a verification alternative, not a purchase instruction.");
        }

        List<String> owned = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (String target : entry.getRecommendedItems())
        {
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
            action = "Compare the live price and marginal benefit of " + next
                    + " before buying; keep the current item when the upgrade is not worth the detour.";
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
            action = "Verify a self-source route and inventory/storage consequence for "
                    + next + " before changing the current UIM setup.";
        else
            action = "Resolve the verified self-source acquisition path for " + next
                    + "; do not substitute a Grand Exchange purchase.";

        String supplies = "Observed matching targets: "
                + (owned.isEmpty() ? "none" : String.join(", ", owned))
                + ". Still to compare: "
                + (unresolved.isEmpty() ? "weapon/context comparison only"
                : String.join(", ", unresolved)) + ".";
        String location = route == null
                ? "Use the acquisition source attached to the selected concrete item; this tier alone does not prove a location."
                : "Route: " + route.getSteps().get(0).getTarget() + ".";
        return new RecommendationGuidance(action, supplies, location,
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

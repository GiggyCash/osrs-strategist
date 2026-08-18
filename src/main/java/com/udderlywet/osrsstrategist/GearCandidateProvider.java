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

    @Inject
    public GearCandidateProvider(GearProgressionCatalog catalog)
    {
        this.catalog = catalog;
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

            result.add(new StrategyCandidate(
                    id,
                    "Gear path: " + pretty(entry.getTier()) + " " + pretty(entry.getStyle()),
                    entry.getWeaponGuidance() + ". " + entry.getNote()
                            + buildNote
                            + " Strategist will compare owned equipment, bank/storage, acquisition route, GP, and target encounter before recommending an actual purchase or grind.",
                    score,
                    RecommendationConfidence.CHECK_NEEDED,
                    null,
                    CandidateSafetyEvidence.verifiedSafe(entry.isFreeToPlay())
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 2) return new ArrayList<>(result.subList(0, 2));
        return result;
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

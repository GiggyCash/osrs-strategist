package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Detects strong restricted-account signatures from live stats.
 *
 * <p>The detector is intentionally conservative. A developing main can briefly
 * resemble a pure, so only recognizable stat patterns are protected
 * automatically. Ambiguous builds such as many med/range-tank variants remain
 * STANDARD until a future explicit per-character build setting is added.</p>
 */
@Singleton
public class RestrictedBuildDetector
{
    public RestrictedBuildSuggestion suggest(AccountSnapshot account)
    {
        if (account == null)
        {
            return new RestrictedBuildSuggestion(
                    RestrictedBuildType.STANDARD,
                    RecommendationConfidence.CHECK_NEEDED,
                    "No account snapshot is available."
            );
        }

        int attack = account.getSkillLevel(Skill.ATTACK);
        int strength = account.getSkillLevel(Skill.STRENGTH);
        int defence = account.getSkillLevel(Skill.DEFENCE);
        int ranged = account.getSkillLevel(Skill.RANGED);
        int prayer = account.getSkillLevel(Skill.PRAYER);
        int magic = account.getSkillLevel(Skill.MAGIC);
        int hp = account.getSkillLevel(Skill.HITPOINTS);
        int highestNonCombat = highestNonCombat(account);
        int lowestNonCombat = lowestNonCombat(account);
        int offensivePeak = Math.max(Math.max(attack, strength),
                Math.max(ranged, magic));
        int combatPeak = Math.max(Math.max(offensivePeak, defence), prayer);

        boolean baselineOffence = attack <= 1 && strength <= 1
                && ranged <= 1 && magic <= 1;
        boolean levelThreeCombatStats = baselineOffence
                && defence <= 1 && prayer <= 1 && hp <= 10;

        if (levelThreeCombatStats && highestNonCombat >= 20)
        {
            RestrictedBuildType type = account.getMembershipStatus() != MembershipStatus.P2P
                    ? RestrictedBuildType.F2P_SKILLER
                    : RestrictedBuildType.SKILLER;
            return strong(type,
                    Text.get(583));
        }

        if (baselineOffence && defence <= 1 && hp <= 10
                && prayer >= 15 && highestNonCombat >= 20)
        {
            return strong(RestrictedBuildType.PRAYER_SKILLER,
                    Text.get(590));
        }

        if (allNonCombatAtBaseline(account) && combatPeak >= 40)
        {
            return strong(RestrictedBuildType.COMBAT_ONLY,
                    Text.get(591));
        }

        if (baselineOffence && defence >= 20)
        {
            return strong(RestrictedBuildType.DEFENCE_PURE,
                    Text.get(592));
        }

        if (hp <= 10 && (ranged >= 20 || magic >= 20 || prayer >= 20
                || highestNonCombat >= 50))
        {
            return strong(RestrictedBuildType.TEN_HITPOINTS,
                    Text.get(593));
        }

        if (attack <= 1 && defence <= 1 && strength >= 50)
        {
            return strong(RestrictedBuildType.OBSIDIAN_MAULER,
                    Text.get(594));
        }

        if (defence <= 1 && offensivePeak >= 40)
        {
            return strong(RestrictedBuildType.ONE_DEFENCE_PURE,
                    Text.get(595));
        }

        // Exact/near-exact Defence stopping points become meaningful only after
        // the account has substantial offensive progression. This avoids
        // classifying an ordinary low-level main simply because Defence happens
        // to be 13, 20, 40, 42, or 45 during normal levelling.
        if (offensivePeak >= 50)
        {
            if (defence >= 2 && defence <= 13)
            {
                return strong(RestrictedBuildType.LOW_DEFENCE_PURE,
                        Text.get(596));
            }
            if (defence >= 14 && defence <= 20)
            {
                return strong(RestrictedBuildType.INITIATE_PURE,
                        Text.get(597));
            }
            if (defence >= 39 && defence <= 40)
            {
                return strong(RestrictedBuildType.RUNE_PURE,
                        Text.get(584));
            }
            if (defence >= 41 && defence <= 42)
            {
                return strong(RestrictedBuildType.VOID_PURE,
                        Text.get(585));
            }
            if (defence >= 43 && defence <= 45
                    && attack >= 50 && strength >= 50)
            {
                return strong(RestrictedBuildType.ZERKER,
                        Text.get(586));
            }
        }

        // Range tanks and med builds have many valid cap combinations. Detect a
        // particularly strong range-tank signature, but do not infer a med build
        // from ordinary balanced combat stats.
        if (defence >= 70 && ranged >= 80 && magic >= 70
                && attack <= 60 && strength <= 70)
        {
            return suggestion(RestrictedBuildType.RANGE_TANK,
                    Text.get(587));
        }

        // A nearly untouched non-combat account with only modest combat is more
        // likely new than intentionally combat-only, hence the earlier 40+ gate.
        if (lowestNonCombat <= 1 && highestNonCombat <= 5 && combatPeak >= 30)
        {
            return suggestion(RestrictedBuildType.COMBAT_ONLY,
                    Text.get(588));
        }

        return new RestrictedBuildSuggestion(
                RestrictedBuildType.STANDARD,
                RecommendationConfidence.VERIFIED,
                Text.get(589)
        );
    }

    private static RestrictedBuildSuggestion strong(
            RestrictedBuildType type,
            String evidence)
    {
        return new RestrictedBuildSuggestion(
                type,
                RecommendationConfidence.VERIFIED,
                evidence
        );
    }

    private static RestrictedBuildSuggestion suggestion(
            RestrictedBuildType type,
            String evidence)
    {
        return new RestrictedBuildSuggestion(
                type,
                RecommendationConfidence.CHECK_NEEDED,
                evidence
        );
    }

    private static int highestNonCombat(AccountSnapshot account)
    {
        int highest = 1;
        for (Skill skill : Skill.values())
        {
            if (!isCombatSkill(skill))
            {
                highest = Math.max(highest, account.getSkillLevel(skill));
            }
        }
        return highest;
    }

    private static int lowestNonCombat(AccountSnapshot account)
    {
        int lowest = Integer.MAX_VALUE;
        for (Skill skill : Skill.values())
        {
            if (!isCombatSkill(skill))
            {
                lowest = Math.min(lowest, account.getSkillLevel(skill));
            }
        }
        return lowest == Integer.MAX_VALUE ? 1 : lowest;
    }

    private static boolean allNonCombatAtBaseline(AccountSnapshot account)
    {
        for (Skill skill : Skill.values())
        {
            if (!isCombatSkill(skill) && account.getSkillLevel(skill) > 1)
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isCombatSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case PRAYER:
            case MAGIC:
            case SLAYER:
                return true;
            default:
                return false;
        }
    }
}

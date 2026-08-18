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
                    "Combat stats remain at level-3 baselines while non-combat progression is established.");
        }

        if (baselineOffence && defence <= 1 && hp <= 10
                && prayer >= 15 && highestNonCombat >= 20)
        {
            return strong(RestrictedBuildType.PRAYER_SKILLER,
                    "Offensive combat and Defence remain at level-3 baselines while Prayer and non-combat skills are established.");
        }

        if (allNonCombatAtBaseline(account) && combatPeak >= 40)
        {
            return strong(RestrictedBuildType.COMBAT_ONLY,
                    "Combat progression is established while every tracked non-combat skill remains at baseline.");
        }

        if (baselineOffence && defence >= 20)
        {
            return strong(RestrictedBuildType.DEFENCE_PURE,
                    "Defence is established while Attack, Strength, Ranged, and Magic remain at baseline.");
        }

        if (hp <= 10 && (ranged >= 20 || magic >= 20 || prayer >= 20
                || highestNonCombat >= 50))
        {
            return strong(RestrictedBuildType.TEN_HITPOINTS,
                    "Hitpoints remain at 10 despite substantial account progress.");
        }

        if (attack <= 1 && defence <= 1 && strength >= 50)
        {
            return strong(RestrictedBuildType.OBSIDIAN_MAULER,
                    "Attack and Defence remain at 1 while Strength is heavily trained, matching an obby-mauler style restriction.");
        }

        if (defence <= 1 && offensivePeak >= 40)
        {
            return strong(RestrictedBuildType.ONE_DEFENCE_PURE,
                    "Defence is 1 while an offensive combat skill is 40+.");
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
                        "Offensive stats are established while Defence is held in the low-defence pure bracket at or below 13.");
            }
            if (defence >= 14 && defence <= 20)
            {
                return strong(RestrictedBuildType.INITIATE_PURE,
                        "Offensive stats are established while Defence is held at or below the 20-Defence bracket.");
            }
            if (defence >= 39 && defence <= 40)
            {
                return strong(RestrictedBuildType.RUNE_PURE,
                        "Offensive stats are established while Defence is held around the 40-Defence rune-pure breakpoint.");
            }
            if (defence >= 41 && defence <= 42)
            {
                return strong(RestrictedBuildType.VOID_PURE,
                        "Offensive stats are established while Defence is held around the 42-Defence Void breakpoint.");
            }
            if (defence >= 43 && defence <= 45
                    && attack >= 50 && strength >= 50)
            {
                return strong(RestrictedBuildType.ZERKER,
                        "Defence is held at or below 45 with established melee stats, matching a berserker-style cap.");
            }
        }

        // Range tanks and med builds have many valid cap combinations. Detect a
        // particularly strong range-tank signature, but do not infer a med build
        // from ordinary balanced combat stats.
        if (defence >= 70 && ranged >= 80 && magic >= 70
                && attack <= 60 && strength <= 70)
        {
            return suggestion(RestrictedBuildType.RANGE_TANK,
                    "High Defence/Ranged/Magic with deliberately lower melee stats resembles a range tank. Protecting exact melee caps still needs confirmation.");
        }

        // A nearly untouched non-combat account with only modest combat is more
        // likely new than intentionally combat-only, hence the earlier 40+ gate.
        if (lowestNonCombat <= 1 && highestNonCombat <= 5 && combatPeak >= 30)
        {
            return suggestion(RestrictedBuildType.COMBAT_ONLY,
                    "Combat is progressing while non-combat skills remain very low; confirm which stats should be protected.");
        }

        return new RestrictedBuildSuggestion(
                RestrictedBuildType.STANDARD,
                RecommendationConfidence.VERIFIED,
                "No strong restricted-build signature was detected."
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

package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Suggests niche account builds from stats but never silently enforces them.
 * A new normal account can resemble a pure, so confirmation remains required.
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

        boolean levelThreeCombatStats = attack <= 1 && strength <= 1
                && defence <= 1 && ranged <= 1 && prayer <= 1
                && magic <= 1 && hp <= 10;
        if (levelThreeCombatStats && highestNonCombat >= 20)
        {
            RestrictedBuildType type = account.getMembershipStatus() == MembershipStatus.F2P
                    ? RestrictedBuildType.F2P_SKILLER
                    : RestrictedBuildType.SKILLER;
            return suggestion(type,
                    "Combat stats remain at level-3-style baselines while at least one non-combat skill is 20+. Confirm before Strategist protects combat stats.");
        }

        int offensivePeak = Math.max(Math.max(attack, strength),
                Math.max(ranged, magic));
        if (defence <= 1 && offensivePeak >= 40)
        {
            return suggestion(RestrictedBuildType.ONE_DEFENCE_PURE,
                    "Defence is 1 while an offensive combat skill is 40+. This strongly resembles a 1-defence pure.");
        }

        if (defence >= 20 && attack <= 1 && strength <= 1
                && ranged <= 1 && magic <= 1)
        {
            return suggestion(RestrictedBuildType.DEFENCE_PURE,
                    "Defence is trained while Attack, Strength, Ranged, and Magic remain at baseline.");
        }

        if (defence >= 40 && defence <= 45
                && attack >= 50 && strength >= 50)
        {
            return suggestion(RestrictedBuildType.ZERKER,
                    "Defence is in the classic 40-45 bracket with established melee stats. Confirm the Defence cap before Strategist protects it.");
        }

        if (hp <= 10 && (ranged >= 20 || magic >= 20 || prayer >= 20
                || highestNonCombat >= 50))
        {
            return suggestion(RestrictedBuildType.TEN_HITPOINTS,
                    "Hitpoints remain at 10 despite substantial account progress. Confirm before Strategist avoids Hitpoints-generating methods.");
        }

        return new RestrictedBuildSuggestion(
                RestrictedBuildType.STANDARD,
                RecommendationConfidence.VERIFIED,
                "No strong restricted-build signature was detected."
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
            switch (skill)
            {
                case ATTACK:
                case STRENGTH:
                case DEFENCE:
                case HITPOINTS:
                case RANGED:
                case PRAYER:
                case MAGIC:
                    break;
                default:
                    highest = Math.max(highest, account.getSkillLevel(skill));
            }
        }
        return highest;
    }
}

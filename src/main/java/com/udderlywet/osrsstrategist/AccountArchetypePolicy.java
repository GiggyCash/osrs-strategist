package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/**
 * Prevents Strategist from casually ruining an apparent restricted build.
 *
 * <p>Restricted-build detection is advisory and conservative. When an account
 * matches one of these archetypes, protected combat skills are removed from
 * normal training recommendations. A future explicit profile override can take
 * precedence over automatic detection.</p>
 */
public final class AccountArchetypePolicy
{
    private AccountArchetypePolicy()
    {
    }

    public static boolean mayTrain(AccountArchetype archetype, Skill skill)
    {
        if (archetype == null || skill == null
                || archetype == AccountArchetype.STANDARD
                || archetype == AccountArchetype.UNKNOWN)
        {
            return true;
        }

        switch (archetype)
        {
            case SKILLER:
                return !isCombatSkill(skill);
            case ONE_DEFENCE_PURE:
                return skill != Skill.DEFENCE;
            case DEFENCE_PURE:
                return skill != Skill.ATTACK
                        && skill != Skill.STRENGTH
                        && skill != Skill.RANGED
                        && skill != Skill.MAGIC;
            case ZERKER:
                return skill != Skill.DEFENCE;
            default:
                return true;
        }
    }

    public static boolean isCombatSkill(Skill skill)
    {
        return skill == Skill.ATTACK
                || skill == Skill.STRENGTH
                || skill == Skill.DEFENCE
                || skill == Skill.RANGED
                || skill == Skill.PRAYER
                || skill == Skill.MAGIC
                || skill == Skill.HITPOINTS;
    }
}

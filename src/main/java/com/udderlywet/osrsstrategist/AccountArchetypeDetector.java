package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Conservative live-skill detector for optional restricted account builds.
 *
 * <p>Definitions intentionally avoid external lookups and only classify when
 * the observed combat stats strongly match a common restricted build. A player
 * can later override this explicitly in profile settings.</p>
 */
@Singleton
public class AccountArchetypeDetector
{
    public AccountArchetype detect(AccountSnapshot account)
    {
        if (account == null)
        {
            return AccountArchetype.UNKNOWN;
        }

        int attack = account.getSkillLevel(Skill.ATTACK);
        int strength = account.getSkillLevel(Skill.STRENGTH);
        int defence = account.getSkillLevel(Skill.DEFENCE);
        int ranged = account.getSkillLevel(Skill.RANGED);
        int prayer = account.getSkillLevel(Skill.PRAYER);
        int magic = account.getSkillLevel(Skill.MAGIC);
        int hitpoints = account.getSkillLevel(Skill.HITPOINTS);

        // Classic non-combat skiller shape. Hitpoints can be above 10 through
        // unusual historical/activity XP, so keep the rule conservative rather
        // than demanding exactly 10 HP.
        if (attack <= 1 && strength <= 1 && defence <= 1
                && ranged <= 1 && prayer <= 1 && magic <= 1
                && hitpoints <= 15)
        {
            return AccountArchetype.SKILLER;
        }

        // Defence pure: combat offence remains essentially untrained while
        // Defence is intentionally raised.
        if (attack <= 1 && strength <= 1 && ranged <= 1 && magic <= 1
                && defence >= 20)
        {
            return AccountArchetype.DEFENCE_PURE;
        }

        // One-defence pure: Defence is preserved at 1 while at least one
        // offensive combat skill is meaningfully trained.
        if (defence <= 1
                && (attack >= 20 || strength >= 20 || ranged >= 20 || magic >= 20))
        {
            return AccountArchetype.ONE_DEFENCE_PURE;
        }

        // Zerker builds traditionally preserve 45 Defence. Allow a narrow
        // tolerance for accounts that have not finished the final quest XP yet
        // or have accidentally gained one level beyond the canonical target.
        if (defence >= 40 && defence <= 46
                && (attack >= 40 || strength >= 50 || ranged >= 50 || magic >= 50))
        {
            return AccountArchetype.ZERKER;
        }

        return AccountArchetype.STANDARD;
    }
}

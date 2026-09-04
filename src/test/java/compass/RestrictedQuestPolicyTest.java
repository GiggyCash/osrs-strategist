package compass;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RestrictedQuestPolicyTest
{
    @Test
    public void oneDefencePureCanUseKnownSafeAttackQuestsButNotDragonSlayer()
    {
        AccountSnapshot pure = account(60, 70, 1, 80, 52, 80, 70, 50);
        assertTrue(RestrictedQuestPolicy.isSafe(pure, "Waterfall Quest"));
        assertTrue(RestrictedQuestPolicy.isSafe(pure, "Monkey Madness I"));
        assertFalse(RestrictedQuestPolicy.isSafe(pure, "Dragon Slayer I"));
        assertFalse(RestrictedQuestPolicy.isSafe(pure, "King's Ransom"));
    }

    @Test
    public void levelThreeSkillerRejectsPrayerAndCombatRewardQuests()
    {
        AccountSnapshot skiller = account(1, 1, 1, 1, 1, 1, 10, 70);
        assertTrue(RestrictedQuestPolicy.isSafe(skiller, "Cook's Assistant"));
        assertTrue(RestrictedQuestPolicy.isSafe(skiller, "Rune Mysteries"));
        assertFalse(RestrictedQuestPolicy.isSafe(skiller, "The Restless Ghost"));
        assertFalse(RestrictedQuestPolicy.isSafe(skiller, "Waterfall Quest"));
    }

    @Test
    public void prayerSkillerCanUseCuratedPrayerQuest()
    {
        AccountSnapshot prayerSkiller = account(1, 1, 1, 1, 31, 1, 10, 70);
        assertTrue(RestrictedQuestPolicy.isSafe(
                prayerSkiller, "The Restless Ghost"));
        assertTrue(RestrictedQuestPolicy.isSafe(
                prayerSkiller, "Ghosts Ahoy"));
    }

    private static AccountSnapshot account(
            int attack, int strength, int defence, int ranged,
            int prayer, int magic, int hp, int nonCombat)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, nonCombat);
            xp.put(skill, 0);
        }
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp);
        return new AccountSnapshot("Quest Build Test", 0L, 0, "Main", Membership.P2P, 1, 1000, 0L, levels, xp);
    }
}

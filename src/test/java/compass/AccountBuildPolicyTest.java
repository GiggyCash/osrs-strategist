package compass;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountBuildPolicyTest
{
    @Test
    public void oneDefencePureNeverGetsDefenceTraining()
    {
        AccountSnapshot pure = account(60, 80, 1, 80, 52, 80, 70, 30);
        assertEquals(BuildType.ONE_DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(pure));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.DEFENCE));
        assertTrue(AccountBuildPolicy.allowsSkill(pure, Skill.RANGED));
    }

    @Test
    public void defencePureOnlyAllowsDefenceAndPrayerCombatProgression()
    {
        AccountSnapshot pure = account(1, 1, 75, 1, 43, 1, 63, 30);
        assertEquals(BuildType.DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(pure));
        assertTrue(AccountBuildPolicy.allowsSkill(pure, Skill.DEFENCE));
        assertTrue(AccountBuildPolicy.allowsSkill(pure, Skill.PRAYER));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.ATTACK));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.STRENGTH));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.RANGED));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.MAGIC));
        assertFalse(AccountBuildPolicy.allowsSkill(pure, Skill.SLAYER));
    }

    @Test
    public void establishedLevelThreeSkillerProtectsAllCombatSkills()
    {
        AccountSnapshot skiller = account(1, 1, 1, 1, 1, 1, 10, 70);
        assertEquals(BuildType.SKILLER,
                AccountBuildPolicy.effectiveBuild(skiller));
        assertFalse(AccountBuildPolicy.allowsSkill(skiller, Skill.ATTACK));
        assertFalse(AccountBuildPolicy.allowsSkill(skiller, Skill.PRAYER));
        assertFalse(AccountBuildPolicy.allowsSkill(skiller, Skill.SLAYER));
        assertTrue(AccountBuildPolicy.allowsSkill(skiller, Skill.COOKING));
    }

    @Test
    public void tenHpBuildAllowsNonDamagingMagicButRejectsCombatMagic()
    {
        AccountSnapshot tenHp = account(1, 1, 1, 1, 20, 55, 10, 60);
        assertEquals(BuildType.TEN_HITPOINTS,
                AccountBuildPolicy.effectiveBuild(tenHp));

        TrainingMethod alch = method("magic_high_alch", Skill.MAGIC);
        TrainingMethod combat = method("magic_f2p_combat", Skill.MAGIC);
        assertTrue(AccountBuildPolicy.allowsMethod(tenHp, alch));
        assertFalse(AccountBuildPolicy.allowsMethod(tenHp, combat));
    }

    private static TrainingMethod method(String id, Skill skill)
    {
        return new TrainingMethod(
                id, skill, 1, 99, id, id,
                10, 10, 10, AttentionLevel.MODERATE, 10, 1,
                java.util.Collections.emptyList(),
                Confidence.VERIFIED);
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
        levels.put(Skill.SLAYER, nonCombat);

        return new AccountSnapshot("Build Test", 0L, 0, "Main", Membership.P2P, 1, 1000, 0L, levels, xp);
    }
}

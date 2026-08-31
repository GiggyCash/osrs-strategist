package compass;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CandidateSafetyPolicyTest
{
    private final CandidateSafetyPolicy policy = new CandidateSafetyPolicy();

    @Test
    public void protectedBuildsCannotReceiveUnprovenIrreversibleFamilies()
    {
        assertProtected(account(60, 60, 1, 60, 43, 60, 70, 40)); // 1 Defence
        assertProtected(account(1, 1, 60, 1, 43, 1, 70, 40)); // Defence pure
        assertProtected(account(1, 1, 1, 1, 1, 1, 10, 50)); // level 3 skiller
        assertProtected(account(1, 1, 1, 1, 20, 1, 10, 50)); // Prayer skiller
        assertProtected(account(1, 1, 1, 20, 20, 1, 10, 50)); // 10 HP
    }

    @Test
    public void harmlessWorkRemainsAvailableAndStandardMainIsUnrestricted()
    {
        StrategyContext skiller = context(account(1, 1, 1, 1, 1, 1, 10, 50));
        assertTrue(policy.isAllowed(recommendation("money:f2p-iron",
                SafetyEvidence.skill(true, Skill.MINING)), skiller));
        assertTrue(policy.isAllowed(recommendation("detour:tempoross-planks",
                SafetyEvidence.skill(false, Skill.FISHING)), skiller));

        StrategyContext main = context(account(70, 70, 70, 70, 70, 70, 70, 70));
        assertTrue(policy.isAllowed(recommendation("pvm:test"), main));
        assertTrue(policy.isAllowed(recommendation("combat-achievements:easy"), main));
    }

    private void assertProtected(AccountSnapshot account)
    {
        StrategyContext context = context(account);
        assertFalse(policy.isAllowed(recommendation("pvm:test"), context));
        assertFalse(policy.isAllowed(recommendation("combat-achievements:easy"), context));
        assertFalse(policy.isAllowed(recommendation("diary:test"), context));
        assertFalse(policy.isAllowed(recommendation("clue:test"), context));
        assertFalse(policy.isAllowed(recommendation("upgrade:test"), context));
        assertFalse(policy.isAllowed(recommendation("minigame:pest-control"), context));
        assertFalse(policy.isAllowed(recommendation("opportunity:tears-of-guthix"), context));
        assertTrue(policy.isAllowed(recommendation("opportunity:herb-run",
                SafetyEvidence.skill(false, Skill.FARMING)), context));
    }

    private static Recommendation recommendation(String id)
    {
        return recommendation(id, SafetyEvidence.unknown());
    }

    private static Recommendation recommendation(String id,
            SafetyEvidence evidence)
    {
        return new Recommendation(id, id, "test", 10, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance("Do it.", "Prepare.", "Safe area.", "Test."),
                evidence);
    }

    private static StrategyContext context(AccountSnapshot account)
    {
        return new StrategyContext(GameData.builder(account).build(),
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL, GoalType.MAX, false, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot account(int attack, int strength, int defence,
            int ranged, int prayer, int magic, int hp, int nonCombat)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, nonCombat); xp.put(skill, 0); }
        levels.put(Skill.ATTACK, attack); levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence); levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer); levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp); levels.put(Skill.SLAYER, 1);
        return new AccountSnapshot("Safety", 0, "Main", MembershipStatus.P2P,
                1, 1000, 0L, levels, xp);
    }
}

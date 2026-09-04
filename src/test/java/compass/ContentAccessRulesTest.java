package compass;

import java.util.Collections;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContentAccessRulesTest
{
    @Test
    public void f2pAllowsCoreFreeSkillsAndBlocksMembersSkills()
    {
        assertTrue(ContentAccessRules.isSkillAvailable(
                Skill.MINING,
                Membership.F2P
        ));
        assertTrue(ContentAccessRules.isSkillAvailable(
                Skill.RUNECRAFT,
                Membership.F2P
        ));

        assertFalse(ContentAccessRules.isSkillAvailable(
                Skill.FARMING,
                Membership.F2P
        ));
        assertFalse(ContentAccessRules.isSkillAvailable(
                Skill.HERBLORE,
                Membership.F2P
        ));
        assertFalse(ContentAccessRules.isSkillAvailable(
                Skill.SAILING,
                Membership.F2P
        ));
    }

    @Test
    public void p2pAllowsMembersSkills()
    {
        assertTrue(ContentAccessRules.isSkillAvailable(
                Skill.FARMING,
                Membership.P2P
        ));
        assertTrue(ContentAccessRules.isSkillAvailable(
                Skill.SAILING,
                Membership.P2P
        ));
    }

    @Test
    public void f2pBlocksMembersActivityInsideFreeSkill()
    {
        TrainingMethod motherlode = method(
                "mining_mlm",
                Skill.MINING,
                false
        );
        TrainingMethod normalOre = method(
                "mining_ore",
                Skill.MINING,
                false
        );

        assertFalse(ContentAccessRules.isMethodAvailable(
                motherlode,
                Membership.F2P
        ));
        assertTrue(ContentAccessRules.isMethodAvailable(
                normalOre,
                Membership.F2P
        ));
        assertTrue(ContentAccessRules.isMethodAvailable(
                motherlode,
                Membership.P2P
        ));
    }

    @Test
    public void explicitMembersOnlyFlagIsFutureProof()
    {
        TrainingMethod futureMembersMethod = method(
                "future_members_method",
                Skill.MINING,
                true
        );

        assertFalse(ContentAccessRules.isMethodAvailable(
                futureMembersMethod,
                Membership.F2P
        ));
        assertTrue(ContentAccessRules.isMethodAvailable(
                futureMembersMethod,
                Membership.P2P
        ));
    }

    private static TrainingMethod method(
            String id,
            Skill skill,
            boolean membersOnly)
    {
        return new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                1,
                1,
                1,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                Confidence.VERIFIED,
                membersOnly
        );
    }
}

package compass;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestMembershipPolicyTest
{
    @Test
    public void f2pAllowsCurrentFreeQuestsAndRejectsMembersQuests()
    {
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Cook's Assistant", MembershipStatus.F2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "The Ides of Milk", MembershipStatus.F2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Dragon Slayer I", MembershipStatus.F2P));
        assertFalse(QuestMembershipPolicy.isAvailable(
                "Pandemonium", MembershipStatus.F2P));
        assertFalse(QuestMembershipPolicy.isAvailable(
                "Recipe for Disaster", MembershipStatus.F2P));
    }

    @Test
    public void p2pDoesNotApplyF2pWhitelist()
    {
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Pandemonium", MembershipStatus.P2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Recipe for Disaster", MembershipStatus.P2P));
    }

    @Test
    public void f2pListTracksTwentyFourQuests()
    {
        assertTrue(QuestMembershipPolicy.freeToPlayQuestNames().size() >= 24);
    }
}

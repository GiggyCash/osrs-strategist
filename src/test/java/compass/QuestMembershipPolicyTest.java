package compass;

import org.junit.Test;
import net.runelite.api.Quest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestMembershipPolicyTest
{
    @Test
    public void f2pAllowsCurrentFreeQuestsAndRejectsMembersQuests()
    {
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Cook's Assistant", Membership.F2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "The Ides of Milk", Membership.F2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Dragon Slayer I", Membership.F2P));
        assertFalse(QuestMembershipPolicy.isAvailable(
                "Pandemonium", Membership.F2P));
        assertFalse(QuestMembershipPolicy.isAvailable(
                "Recipe for Disaster", Membership.F2P));
    }

    @Test
    public void p2pDoesNotApplyF2pWhitelist()
    {
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Pandemonium", Membership.P2P));
        assertTrue(QuestMembershipPolicy.isAvailable(
                "Recipe for Disaster", Membership.P2P));
    }

    @Test
    public void f2pListTracksTwentyFourQuests()
    {
        long count = java.util.Arrays.stream(Quest.values())
                .filter(quest -> QuestMembershipPolicy.isAvailable(
                        quest.getName(), Membership.F2P)).count();
        assertTrue(count >= 24);
    }
}

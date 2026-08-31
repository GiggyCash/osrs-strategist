package compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class TravelAwareMethodValueServiceTest
{
    @Test
    public void verifiedRouteChangesBestLocationThroughTravelBurden()
    {
        TrainingMethod method = method("farming_fruit_trees", true);
        TravelAwareMethodValueService service =
                new TravelAwareMethodValueService();

        TravelAwareMethodAssessment ordinary = service.assess(method,
                context(MembershipStatus.P2P, Collections.emptySet(), false));
        TravelAwareMethodAssessment routed = service.assess(method,
                context(MembershipStatus.P2P,
                        Collections.singleton("spirit-tree-gnome-stronghold"),
                        false));

        assertEquals("catherby-fruit-tree", ordinary.getLocation().getId());
        assertFalse(ordinary.isVerifiedRouteUsed());
        assertEquals("gnome-stronghold-fruit-tree",
                routed.getLocation().getId());
        assertTrue(routed.isVerifiedRouteUsed());
        assertTrue(routed.getScoreAdjustment() > ordinary.getScoreAdjustment());
    }

    @Test
    public void unverifiedRouteIsNeverDescribedAsAvailable()
    {
        TravelAwareMethodAssessment value =
                new TravelAwareMethodValueService().assess(
                        method("prayer_ectofuntus", true),
                        context(MembershipStatus.P2P,
                                Collections.emptySet(), false));

        assertFalse(value.isVerifiedRouteUsed());
        assertTrue(value.getEvidence().contains("No exact"));
        assertFalse(value.getEvidence().contains("lowers"));
    }

    @Test
    public void membershipAndWildernessAreFailClosedAtLocationLayer()
    {
        MethodLocationProfile profile = new MethodLocationProfile("custom",
                Collections.singletonList(new MethodLocationOption(
                        "members-wild", "Members Wilderness place", 1,
                        null, 1, true, true)), "test");
        TravelAwareMethodValueService service =
                new TravelAwareMethodValueService();

        assertNull(service.assess(profile, context(
                MembershipStatus.UNKNOWN, Collections.emptySet(), true)));
        assertNull(service.assess(profile, context(
                MembershipStatus.P2P, Collections.emptySet(), false)));
    }

    @Test
    public void completedExactQuestCanProveDestinationRouteWithoutGenericUnlock()
    {
        TravelAwareMethodAssessment value =
                new TravelAwareMethodValueService().assess(
                        method("farming_fruit_trees", true),
                        context(MembershipStatus.P2P, Collections.emptySet(),
                                false, "The Grand Tree"));

        assertEquals("gnome-stronghold-fruit-tree",
                value.getLocation().getId());
        assertTrue(value.isVerifiedRouteUsed());
    }

    @Test
    public void genericNetworkObservationDoesNotProveDestinationNode()
    {
        TravelAwareMethodAssessment value =
                new TravelAwareMethodValueService().assess(
                        method("farming_fruit_trees", true),
                        context(MembershipStatus.P2P,
                                Collections.singleton("spirit-trees"), false));

        assertEquals("catherby-fruit-tree", value.getLocation().getId());
        assertFalse(value.isVerifiedRouteUsed());
    }

    @Test
    public void ectophialNeedsBothQuestAndObservedUsableItem()
    {
        TravelRouteEvidenceService service = new TravelRouteEvidenceService();
        StrategyContext questOnly = context(MembershipStatus.P2P,
                Collections.emptySet(), false, "Ghosts Ahoy");

        assertFalse(service.verified("ectophial", questOnly));
        assertTrue(service.verified("ectophial", contextWithItemAndQuest(
                "Ectophial", "Ghosts Ahoy")));
    }

    private static TrainingMethod method(String id, boolean members)
    {
        return new TrainingMethod(id, Skill.FARMING, 1, 99, id, "Do it",
                10, 10, 10, AttentionLevel.MODERATE, 10, 1,
                Collections.emptyList(), Confidence.VERIFIED,
                members);
    }

    private static StrategyContext context(MembershipStatus membership,
            java.util.Set<String> routes, boolean wilderness)
    {
        return context(membership, routes, wilderness, null);
    }

    private static StrategyContext context(MembershipStatus membership,
            java.util.Set<String> routes, boolean wilderness,
            String completedQuest)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Travel", 77L, 0,
                "MAIN", membership, membership == MembershipStatus.P2P ? 1 : 0,
                70 * Skill.values().length, 0, levels, xp);
        Map<String, QuestStatus> quests = new HashMap<>();
        if (completedQuest != null)
            quests.put(completedQuest, QuestStatus.COMPLETE);
        GameData data = GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .transport(new TransportSnapshot(new HashSet<>(routes))).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, wilderness,
                new PreferenceProfile());
    }

    private static StrategyContext contextWithItemAndQuest(String item,
            String quest)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 70);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Travel", 78L, 0,
                "MAIN", MembershipStatus.P2P, 1,
                70 * Skill.values().length, 0, levels, xp);
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put(quest, QuestStatus.COMPLETE);
        GameData data = GameData.builder(account)
                .quests(new QuestSnapshot(quests))
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(1, item, 1))))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }
}

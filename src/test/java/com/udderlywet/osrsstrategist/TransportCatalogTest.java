package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import org.junit.Test;

public class TransportCatalogTest
{
    @Test
    public void coversEveryHighValueTransportFamilyWithReusableFanOut()
    {
        TransportCatalog catalog = new TransportCatalog();
        Set<TransportCategory> categories = catalog.all().stream()
                .map(TransportDefinition::getCategory)
                .collect(Collectors.toSet());

        assertEquals(EnumSet.allOf(TransportCategory.class), categories);
        assertTrue(catalog.all().size() >= 25);
        assertEquals(41, catalog.all().size());
        assertEquals(catalog.all().size(), catalog.all().stream()
                .map(TransportDefinition::getId).distinct().count());
        for (TransportDefinition definition : catalog.all())
        {
            assertFalse(definition.getName().trim().isEmpty());
            assertTrue(definition.getId(), definition.getFanOut() >= 2);
        }
    }

    @Test
    public void august2026AgilityShortcutsUseLiveLevelsAndDiaryCaveat()
    {
        TransportCatalog catalog = new TransportCatalog();
        assertEquals(83, catalog.get("pollnivneach-west-plateau").getLevel());
        assertEquals(72,
                catalog.get("water-obelisk-catherby-crossing").getLevel());
        assertTrue(catalog.get("water-obelisk-catherby-crossing")
                .getItemOrAccessCheck().contains("Diary"));
        assertEquals(TransportCategory.AGILITY_SHORTCUT,
                catalog.get("mos-le-harmless-island-stones").getCategory());
    }

    @Test
    public void verifiedLiveRouteShortCircuitsPreparation()
    {
        StrategyContext context = context(0, MembershipStatus.P2P, 80,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.singleton("fairy-rings"), true);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("fairy-rings", context);

        assertEquals(1, result.getNodes().size());
        assertEquals(RecommendationConfidence.VERIFIED,
                result.getNodes().get(0).getConfidence());
        assertNull(result.nextAction());
    }

    @Test
    public void unknownMembershipFailsClosedBeforeMembersTransportRequirements()
    {
        StrategyContext context = context(0, MembershipStatus.UNKNOWN, 99,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptySet(), true);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("fairy-rings", context);

        assertTrue(actions(result).contains("Verify active membership"));
        assertFalse(kinds(result).contains(GoalNodeKind.QUEST));
    }

    @Test
    public void pohFurnitureIsNeverInferredFromConstructionLevel()
    {
        StrategyContext unknown = context(0, MembershipStatus.P2P, 99,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptySet(), true);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("poh-fairy-ring", unknown);

        assertTrue(actions(result).contains("Check the live POH"));

        Map<String, CapabilityState> furniture = new HashMap<>();
        furniture.put("fairy-ring", CapabilityState.VERIFIED);
        StrategyContext observed = context(0, MembershipStatus.P2P, 99,
                Collections.emptyMap(), furniture, Collections.emptySet(), true);
        UniversalDependencyResolution observedResult =
                new UniversalDependencyPlanner().resolveTransport(
                        "poh-fairy-ring", observed);
        assertFalse(actions(observedResult).contains("Check the live POH"));
        assertNotNull(observedResult.nextAction());
    }

    @Test
    public void questStartCanSatisfyFairyRingQuestGateWithoutClaimingRouteVerified()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Fairytale II - Cure a Queen", QuestStatus.IN_PROGRESS);
        StrategyContext context = context(1, MembershipStatus.P2P, 70, quests,
                Collections.emptyMap(), Collections.emptySet(), true);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("fairy-rings", context);

        assertFalse(result.getNodes().stream().anyMatch(node ->
                "quest:fairytale-ii-cure-a-queen".equals(node.getId())));
        assertTrue(actions(result).contains("dramen or lunar staff"));
    }

    @Test
    public void hardcoreAccountsDoNotAutoRouteWildernessTeleports()
    {
        StrategyContext context = context(3, MembershipStatus.P2P, 99,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptySet(), true);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("ancient-magicks-teleports", context);

        assertTrue(actions(result).contains("Hardcore account"));
        assertFalse(kinds(result).contains(GoalNodeKind.QUEST));
    }

    @Test
    public void missingConstructionCreatesRealTrainingDependency()
    {
        StrategyContext context = context(2, MembershipStatus.P2P, 40,
                Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptySet(), false);
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveTransport("poh-spirit-tree", context);

        assertTrue(result.getNodes().stream().anyMatch(node ->
                node.getKind() == GoalNodeKind.SKILL_LEVEL
                        && node.getAction().contains("Construction")
                        && node.getAction().contains("75")));
        assertTrue(kinds(result).contains(GoalNodeKind.TRAINING_METHOD));
    }

    private static String actions(UniversalDependencyResolution result)
    {
        return result.getNodes().stream().map(UniversalDependencyNode::getAction)
                .collect(Collectors.joining(" | "));
    }

    private static Set<GoalNodeKind> kinds(UniversalDependencyResolution result)
    {
        return result.getNodes().stream().map(UniversalDependencyNode::getKind)
                .collect(Collectors.toSet());
    }

    private static StrategyContext context(int type,
            MembershipStatus membership, int level,
            Map<String, QuestStatus> questStates,
            Map<String, CapabilityState> furniture,
            Set<String> routes, boolean wilderness)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Transport", 912L, type,
                AccountMode.fromTypeCode(type).name(), membership, 1,
                level * Skill.values().length, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                // These route scenarios model a live, fully observed empty
                // inventory. UIM training-method generation must not treat an
                // omitted persisted snapshot as proof of free slots.
                .inventory(new InventorySnapshot(Collections.emptyList(), true))
                .quests(new QuestSnapshot(questStates))
                .transport(new TransportSnapshot(new HashSet<>(routes)))
                .poh(new PohSnapshot(CapabilityState.UNKNOWN, furniture))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL,
                GoalType.DIARY_CAPE, false, false, wilderness,
                new PreferenceProfile());
    }
}

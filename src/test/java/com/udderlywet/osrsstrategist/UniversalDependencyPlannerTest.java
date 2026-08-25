package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UniversalDependencyPlannerTest
{
    @Test
    public void twentyRealMultiDomainSimulationsMeetDeepChainThresholds()
    {
        UniversalDependencyPlanner planner = new UniversalDependencyPlanner();
        StrategyContext context = context(MembershipStatus.P2P, 20);
        List<NamedResolution> simulations = new ArrayList<>();
        simulations.add(goal("Bowfa goal", planner.resolveGoal(GoalType.BOWFA, context)));
        simulations.add(goal("Prifddinas goal", planner.resolveGoal(GoalType.PRIFDDINAS, context)));
        simulations.add(goal("Barrows gloves goal", planner.resolveGoal(GoalType.BARROWS_GLOVES, context)));
        simulations.add(goal("Infernal cape goal", planner.resolveGoal(GoalType.INFERNAL_CAPE, context)));
        simulations.add(goal("Song of the Elves", planner.resolveQuest("Song of the Elves", context)));
        simulations.add(goal("Dragon Slayer II", planner.resolveQuest("Dragon Slayer II", context)));
        simulations.add(goal("Desert Treasure II", planner.resolveQuest("Desert Treasure II - The Fallen Empire", context)));
        simulations.add(goal("Animal Magnetism", planner.resolveQuest("Animal Magnetism", context)));
        simulations.add(goal("Fremennik Isles", planner.resolveQuest("The Fremennik Isles", context)));
        simulations.add(goal("Beneath Cursed Sands", planner.resolveQuest("Beneath Cursed Sands", context)));
        simulations.add(goal("Ava assembler", planner.resolveGear("Ava's assembler", context)));
        simulations.add(goal("Bow of faerdhinen", planner.resolveGear("Bow of faerdhinen", context)));
        simulations.add(goal("Neitiznot faceguard", planner.resolveGear("Neitiznot faceguard", context)));
        simulations.add(goal("Ferocious gloves", planner.resolveGear("Ferocious gloves", context)));
        simulations.add(goal("Avernic defender", planner.resolveGear("Avernic defender", context)));
        simulations.add(goal("Arclight", planner.resolveGear("Arclight", context)));
        simulations.add(goal("Raiments", planner.resolveGear("Raiments of the Eye", context)));
        simulations.add(goal("Black mask", planner.resolveGear("Black mask", context)));
        simulations.add(goal("Zombie axe", planner.resolveGear("Zombie axe", context)));
        simulations.add(goal("Ancient staff", planner.resolveGear("Ancient staff", context)));

        assertEquals(20, simulations.size());
        int fiveEdges = 0;
        int sevenEdges = 0;
        StringBuilder failures = new StringBuilder();
        for (NamedResolution simulation : simulations)
        {
            UniversalDependencyResolution result = simulation.resolution;
            Set<GoalNodeKind> domains = new HashSet<>();
            for (UniversalDependencyNode node : result.getNodes())
                domains.add(node.getKind());
            if (result.getEdgeCount() >= 5) fiveEdges++;
            if (result.getEdgeCount() >= 7) sevenEdges++;
            if (result.getEdgeCount() < 3 || domains.size() < 3)
                failures.append('\n').append(simulation.name)
                        .append(" edges=").append(result.getEdgeCount())
                        .append(" domains=").append(domains);
            assertFalse(simulation.name + " exceeded node bound",
                    result.getNodes().size() > 160);
            assertNotNull(simulation.name + " has no actionable leaf",
                    result.nextAction());
        }
        assertEquals(failures.toString(), 0, failures.length());
        assertTrue("Only " + fiveEdges + " simulations reached 5+ edges",
                fiveEdges >= 10);
        assertTrue("Only " + sevenEdges + " simulations reached 7+ edges",
                sevenEdges >= 5);
    }

    @Test
    public void clueStashTraversesConstructionMaterialsAndTransportSafely()
    {
        UniversalDependencyResolution unknown = new UniversalDependencyPlanner()
                .resolveClueStash(STASHUnit.BRITTLE_ISLE,
                        StashUnitState.UNKNOWN, context(MembershipStatus.P2P, 99));
        assertKinds(unknown, GoalNodeKind.CLUE, GoalNodeKind.STASH,
                GoalNodeKind.PREPARATION_ACTION);
        assertTrue(unknown.nextAction().getAction().contains("Watson"));

        UniversalDependencyResolution unbuilt = new UniversalDependencyPlanner()
                .resolveClueStash(STASHUnit.BRITTLE_ISLE,
                        StashUnitState.NOT_BUILT, context(MembershipStatus.P2P, 20));
        assertKinds(unbuilt, GoalNodeKind.CLUE, GoalNodeKind.STASH,
                GoalNodeKind.SKILL_LEVEL, GoalNodeKind.TRAINING_METHOD);
    }

    @Test
    public void membershipUncertaintyStopsBeforeMembersQuestExpansion()
    {
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveQuest("Song of the Elves",
                        context(MembershipStatus.UNKNOWN, 99));
        assertEquals(2, result.getNodes().size());
        assertKinds(result, GoalNodeKind.QUEST, GoalNodeKind.ACCESS);
        assertTrue(result.nextAction().getAction().contains("membership"));
    }

    @Test
    public void depthAndNodeBoundsAreReportedWithoutBlankActions()
    {
        UniversalDependencyResolution result = new UniversalDependencyPlanner(3, 12)
                .resolveQuest("Song of the Elves", context(MembershipStatus.P2P, 1));
        assertTrue(result.getNodes().size() <= 12);
        assertTrue(result.isDepthLimited() || result.isNodeLimited());
        for (UniversalDependencyNode node : result.getNodes())
            assertFalse(node.getAction().trim().isEmpty());
    }

    @Test
    public void deterministicResourceRecipesPreserveYieldAndPartialOwnership()
    {
        StrategyContext context = context(MembershipStatus.P2P, 99,
                Collections.singletonList(new ItemStackSnapshot(
                        ItemID.STEEL_BAR, "Steel bar", 24)));
        UniversalDependencyResolution result = new UniversalDependencyPlanner()
                .resolveResource("Cannonball", 100, context);

        UniversalDependencyNode steel = result.getNodes().stream()
                .filter(node -> "resource:steel-bar".equals(node.getId()))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(1, steel.getQuantity());
        assertTrue(result.getNodes().stream().anyMatch(node ->
                "resource:iron-ore".equals(node.getId())
                        && node.getQuantity() == 1));
        assertTrue(result.getNodes().stream().anyMatch(node ->
                "resource:coal".equals(node.getId())
                        && node.getQuantity() == 2));
        assertKinds(result, GoalNodeKind.RESOURCE, GoalNodeKind.QUEST,
                GoalNodeKind.SKILL_LEVEL, GoalNodeKind.PREPARATION_ACTION);
    }

    @Test
    public void provenShortfallDoesNotSubtractOriginalOwnershipTwice()
    {
        StrategyContext context = context(MembershipStatus.P2P, 99,
                Collections.singletonList(new ItemStackSnapshot(
                        ItemID.MCANNONBALL, "Cannonball", 100)));
        UniversalDependencyPlanner planner = new UniversalDependencyPlanner();
        UniversalDependencyResolution total = planner.resolveResource(
                "Cannonball", 100, context);
        UniversalDependencyResolution shortfall =
                planner.resolveKnownResourceShortfall("Cannonball", 100,
                        context);

        assertEquals(RecommendationConfidence.VERIFIED,
                total.getNodes().stream().filter(node ->
                        "resource:cannonball".equals(node.getId()))
                        .findFirst().orElseThrow(AssertionError::new)
                        .getConfidence());
        assertTrue(shortfall.getNodes().stream().anyMatch(node ->
                "resource:steel-bar".equals(node.getId())
                        && node.getQuantity() == 25));
    }

    private static NamedResolution goal(String name,
            UniversalDependencyResolution resolution)
    {
        return new NamedResolution(name, resolution);
    }

    private static void assertKinds(UniversalDependencyResolution resolution,
            GoalNodeKind... expected)
    {
        Set<GoalNodeKind> actual = new HashSet<>();
        for (UniversalDependencyNode node : resolution.getNodes())
            actual.add(node.getKind());
        for (GoalNodeKind kind : expected)
            assertTrue("Missing " + kind + " from " + actual,
                    actual.contains(kind));
    }

    private static StrategyContext context(MembershipStatus membership, int level)
    {
        return context(membership, level, Collections.emptyList());
    }

    private static StrategyContext context(MembershipStatus membership, int level,
            List<ItemStackSnapshot> inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Deep chain", 88L, 1,
                "IRONMAN", membership, membership == MembershipStatus.P2P ? 1 : 0,
                level * Skill.values().length, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(inventory))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, false, new PreferenceProfile());
    }

    private static final class NamedResolution
    {
        private final String name;
        private final UniversalDependencyResolution resolution;

        private NamedResolution(String name,
                UniversalDependencyResolution resolution)
        {
            this.name = name;
            this.resolution = resolution;
        }
    }
}

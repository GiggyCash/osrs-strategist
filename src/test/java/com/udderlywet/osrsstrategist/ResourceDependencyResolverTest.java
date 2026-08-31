package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourceDependencyResolverTest
{
    @Test
    public void catalogContainsBroadDeterministicProductionGraph()
    {
        assertEquals(41, new ResourceDependencyCatalog().size());
    }
    @Test
    public void simpleAndMultiHopIronChainsResolveInPrerequisiteOrder()
    {
        DependencyResolution simple = resolver().resolve(
                context(1, false, null, null, null),
                new ResourceNeed(ItemID.STEEL_BAR, "Steel bar", 1));
        assertTrue(ids(simple).contains("resource:" + ItemID.IRON_ORE));
        assertTrue(ids(simple).contains("resource:" + ItemID.COAL));

        DependencyResolution multi = resolver().resolve(
                context(1, false, null, null, null),
                new ResourceNeed(ItemID.MCANNONBALL, "Cannonball", 4));
        assertTrue(ids(multi).contains("quest:dwarf-cannon"));
        assertTrue(ids(multi).contains("resource:" + ItemID.STEEL_BAR));
        assertTrue(ids(multi).contains("resource:" + ItemID.IRON_ORE));
        assertEquals("quest:dwarf-cannon", multi.nextAction().getId());
    }

    @Test
    public void branchingAndRepeatedDependenciesAreDeduplicated()
    {
        int root = 900001, leaf = 900002;
        ResourceDependencyDefinition definition = new ResourceDependencyDefinition(
                root, "Make root.", 1, Arrays.asList(
                DependencyRequirement.resource(new ResourceNeed(leaf, "Leaf", 1)),
                DependencyRequirement.resource(new ResourceNeed(leaf, "Leaf", 1)),
                DependencyRequirement.gear("Hammer")));
        DependencyResolution result = resolver(definition).resolve(
                context(1, false, null, null, null),
                new ResourceNeed(root, "Root", 1));
        assertEquals(1, count(ids(result), "resource:" + leaf));
        assertTrue(ids(result).contains("gear:hammer"));
    }

    @Test
    public void cyclesAndDepthLimitsTerminateDeterministically()
    {
        int a = 910001, b = 910002;
        ResourceDependencyDefinition first = definition(a, b, 1);
        ResourceDependencyDefinition second = definition(b, a, 1);
        DependencyResolution cycle = resolver(first, second).resolve(
                context(1, false, null, null, null),
                new ResourceNeed(a, "A", 1));
        assertTrue(cycle.isCycleDetected());
        assertTrue(cycle.getNodes().size() <= 4);

        DependencyResolution depth = new ResourceDependencyResolver(
                new ResourceAcquisitionPlanner(),
                new ResourceDependencyCatalog(Arrays.asList(first, second)), 1)
                .resolve(context(1, false, null, null, null),
                        new ResourceNeed(a, "A", 1));
        assertTrue(depth.isDepthLimited() || depth.isCycleDetected());
    }

    @Test
    public void broadAcyclicGraphsRespectIndependentNodeLimit()
    {
        java.util.ArrayList<ResourceDependencyDefinition> definitions =
                new java.util.ArrayList<>();
        java.util.ArrayList<DependencyRequirement> children =
                new java.util.ArrayList<>();
        int root = 915000;
        for (int i = 1; i <= 40; i++)
        {
            int child = root + i;
            children.add(DependencyRequirement.resource(
                    new ResourceNeed(child, "Child " + i, 1)));
            definitions.add(new ResourceDependencyDefinition(child,
                    "Resolve child " + i + ".", 1,
                    Collections.emptyList()));
        }
        definitions.add(new ResourceDependencyDefinition(root,
                "Resolve root.", 1, children));

        DependencyResolution result = new ResourceDependencyResolver(
                new ResourceAcquisitionPlanner(),
                new ResourceDependencyCatalog(definitions), 8, 12)
                .resolve(context(1, false, null, null, null),
                        new ResourceNeed(root, "Root", 1));

        assertTrue(result.isNodeLimited());
        assertTrue(result.getNodes().size() <= 12);
    }

    @Test
    public void mainStopsAtPurchaseWhileUnknownIronLeafStaysCheckNeeded()
    {
        ResourceNeed need = new ResourceNeed(920001, "Uncatalogued", 1);
        DependencyResolution main = resolver().resolve(
                context(0, false,
                        new ItemsState(Collections.emptyList(), 1L),
                        null, null), need);
        assertEquals(1, main.getNodes().size());
        assertTrue(main.nextAction().getAction().contains("GE"));

        DependencyResolution iron = resolver().resolve(
                context(1, false, null, null, null), need);
        assertEquals(Confidence.CHECK_NEEDED,
                iron.nextAction().getConfidence());
        assertFalse(iron.nextAction().getAction().trim().isEmpty());
    }

    @Test
    public void groupStorageCountsOnlyWhenEnabledAndObserved()
    {
        int id = 930001;
        ItemsState observed = new ItemsState(true,
                Collections.singletonList(new ItemState(id, "Part", 2)));
        ResourceNeed need = new ResourceNeed(id, "Part", 2);
        DependencyResolution usable = resolver().resolve(
                context(4, true, null, observed, null), need);
        assertEquals(Confidence.VERIFIED,
                usable.getNodes().get(0).getConfidence());

        DependencyResolution disabled = resolver().resolve(
                context(4, false, null, observed, null), need);
        assertEquals(Confidence.CHECK_NEEDED,
                disabled.nextAction().getConfidence());
        DependencyResolution unseen = resolver().resolve(
                context(4, true, null, ItemsState.unknown(), null), need);
        assertEquals(Confidence.CHECK_NEEDED,
                unseen.nextAction().getConfidence());
    }

    @Test
    public void uimIgnoresBankAndSurfacesRetrievalOnlyStorage()
    {
        int id = 940001;
        ItemsState bank = new ItemsState(Collections.singletonList(
                new ItemState(id, "UIM part", 2)), 1L);
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.LOOTING_BAG, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.LOOTING_BAG, Collections.singletonList(
                new ItemState(id, "UIM part", 2)));
        StorageSnapshot storage = new StorageSnapshot(states, contents);

        DependencyResolution result = resolver().resolve(
                context(2, false, bank, null, storage),
                new ResourceNeed(id, "UIM part", 2));
        assertEquals(Confidence.CHECK_NEEDED,
                result.nextAction().getConfidence());
        assertTrue(result.nextAction().getAction().contains("retrieval"));
    }

    @Test
    public void expensiveDetourIsRejectedForShortSession()
    {
        int root = 950001;
        ResourceDependencyDefinition expensive = new ResourceDependencyDefinition(
                root, "Long detour", 90, Collections.emptyList());
        DependencyResolution result = resolver(expensive).resolve(
                context(1, false, null, null, null),
                new ResourceNeed(root, "Expensive", 1));
        assertTrue(result.isOpportunityCostRejected());
        assertTrue(result.nextAction().getAction().contains("too much"));
    }

    @Test
    public void moltenGlassChainPromotesSeaweedBeforeProcessing()
    {
        DependencyResolution result = resolver().resolve(
                context(1, false, null, null, null),
                new ResourceNeed(ItemID.MOLTEN_GLASS, "Molten glass", 1));
        assertTrue(ids(result).contains("resource:" + ItemID.SODA_ASH));
        assertTrue(ids(result).contains("resource:" + ItemID.SEAWEED));
        assertEquals("resource:" + ItemID.BUCKET_SAND,
                result.nextAction().getId());
        assertTrue(result.getNodes().size() <= 8);
    }

    @Test
    public void observedFlaxCompletesOneBranchOfBowStringChain()
    {
        ItemsState inventory = new ItemsState(
                Collections.singletonList(new ItemState(
                        ItemID.FLAX, "Flax", 1)));
        StrategyContext context = context(1, false, null, null, null,
                inventory);
        DependencyResolution result = resolver().resolve(context,
                new ResourceNeed(ItemID.BOW_STRING, "Bow string", 1));
        assertEquals(Confidence.VERIFIED,
                result.getNodes().stream()
                        .filter(value -> value.getId().equals(
                                "resource:" + ItemID.FLAX))
                        .findFirst().orElseThrow(AssertionError::new)
                        .getConfidence());
    }

    @Test
    public void deterministicRecipeYieldsScaleChildQuantities()
    {
        ItemsState inventory = new ItemsState(Arrays.asList(
                new ItemState(ItemID.STEEL_BAR, "Steel bar", 24),
                new ItemState(ItemID.IRON_ORE, "Iron ore", 1),
                new ItemState(ItemID.COAL, "Coal", 2)));
        DependencyResolution result = resolver().resolve(
                context(1, false, null, null, null, inventory),
                new ResourceNeed(ItemID.MCANNONBALL, "Cannonball", 100));
        ResolvedDependencyNode steel = result.getNodes().stream()
                .filter(value -> value.getId().equals("resource:" + ItemID.STEEL_BAR))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(Confidence.CHECK_NEEDED, steel.getConfidence());
        assertEquals(25, steel.getRequiredQuantity());
    }

    @Test
    public void repeatedChildRequirementsAreSummedBeforeTraversal()
    {
        int root = 960001, leaf = 960002;
        ResourceDependencyDefinition combined = new ResourceDependencyDefinition(
                root, "Combine leaf parts.", 1, Arrays.asList(
                DependencyRequirement.resource(new ResourceNeed(leaf, "Leaf", 2)),
                DependencyRequirement.resource(new ResourceNeed(leaf, "Leaf", 3))));
        ItemsState inventory = new ItemsState(
                Collections.singletonList(new ItemState(leaf, "Leaf", 4)));
        DependencyResolution result = resolver(combined).resolve(
                context(1, false, null, null, null, inventory),
                new ResourceNeed(root, "Root", 1));
        ResolvedDependencyNode node = result.getNodes().stream()
                .filter(value -> value.getId().equals("resource:" + leaf))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(5, node.getRequiredQuantity());
    }

    @Test
    public void sharedLeafAcrossBranchesIsDeduplicatedAndSummed()
    {
        int root = 970001, left = 970002, right = 970003, leaf = 970004;
        ResourceDependencyDefinition rootDefinition = new ResourceDependencyDefinition(
                root, "Assemble root.", 1, Arrays.asList(
                DependencyRequirement.resource(new ResourceNeed(left, "Left", 1)),
                DependencyRequirement.resource(new ResourceNeed(right, "Right", 1))));
        ResourceDependencyDefinition leftDefinition = definition(left, leaf, 1);
        ResourceDependencyDefinition rightDefinition = new ResourceDependencyDefinition(
                right, "Make right.", 1, Collections.singletonList(
                DependencyRequirement.resource(new ResourceNeed(leaf, "Leaf", 2))));
        DependencyResolution result = resolver(rootDefinition,
                leftDefinition, rightDefinition).resolve(
                context(1, false, null, null, null),
                new ResourceNeed(root, "Root", 1));
        assertEquals(1, count(ids(result), "resource:" + leaf));
        ResolvedDependencyNode node = result.getNodes().stream()
                .filter(value -> value.getId().equals("resource:" + leaf))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(3, node.getRequiredQuantity());
    }

    private static ResourceDependencyDefinition definition(int item, int child,
            int cost)
    {
        return new ResourceDependencyDefinition(item, "Make " + item, cost,
                Collections.singletonList(DependencyRequirement.resource(
                        new ResourceNeed(child, "Child", 1))));
    }

    private static ResourceDependencyResolver resolver(
            ResourceDependencyDefinition... definitions)
    {
        ResourceDependencyCatalog catalog = definitions.length == 0
                ? new ResourceDependencyCatalog()
                : new ResourceDependencyCatalog(Arrays.asList(definitions));
        return new ResourceDependencyResolver(new ResourceAcquisitionPlanner(),
                catalog, 8);
    }

    private static StrategyContext context(int type, boolean groupEnabled,
            ItemsState bank, ItemsState group,
            StorageSnapshot storage)
    {
        return context(type, groupEnabled, bank, group, storage,
                new ItemsState(Collections.emptyList()));
    }

    private static StrategyContext context(int type, boolean groupEnabled,
            ItemsState bank, ItemsState group,
            StorageSnapshot storage, ItemsState inventory)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 50); xp.put(skill, 0); }
        AccountSnapshot account = new AccountSnapshot("Graph", type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P,
                1, 1000, 0L, levels, xp);
        GameData data = GameData.builder(account)
                .inventory(inventory)
                .bank(bank).groupStorage(group).storage(storage)
                .quests(new QuestSnapshot(Collections.emptyMap())).build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.QUICK_20_MIN, QuestTolerance.NORMAL, GoalType.MAX,
                groupEnabled, false, false, new PreferenceProfile());
    }

    private static List<String> ids(DependencyResolution result)
    {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (ResolvedDependencyNode node : result.getNodes()) ids.add(node.getId());
        return ids;
    }

    private static int count(List<String> values, String target)
    {
        int count = 0;
        for (String value : values) if (target.equals(value)) count++;
        return count;
    }
}

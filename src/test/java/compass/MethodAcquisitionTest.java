package compass;

import java.util.*;
import java.util.function.Consumer;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class MethodAcquisitionTest
{
    private final MethodPreparationService preparation = new MethodPreparationService(
            new AccountResourcePlanner(null, new ResourceSourceCatalog()));

    @Test
    public void safePickupIsPreparationNotCarriedOwnershipForEveryKnownMode()
    {
        for (int mode : new int[] {0, 1, 2, 3, 4, 5, 6})
        {
            MethodPreparation result = prepare(MethodIntelligenceTest.data(mode, -1, empty(), empty()), knife());
            assertEquals(MethodPreparation.State.VERIFIED, result.state);
            assertEquals(MethodPreparation.Kind.ACQUIRE, result.steps.get(0).kind);
            assertTrue(result.explanation().contains("Lumbridge Castle"));
            assertTrue(result.explanation().contains("If the spawn is absent"));
            assertEquals(0, result.purchaseCost);
        }
    }

    @Test
    public void knownStockWinsOverAcquisition()
    {
        MethodPreparation carried = prepare(MethodIntelligenceTest.data(1, -1,
                Collections.singletonList(new ItemState(946, "Knife", 1)), empty()), knife());
        assertEquals(MethodPreparation.State.READY, carried.state);
        assertEquals(MethodPreparation.Kind.CARRIED, carried.steps.get(0).kind);
        MethodPreparation banked = prepare(MethodIntelligenceTest.data(1, -1, empty(),
                Collections.singletonList(new ItemState(946, "Knife", 1))), knife());
        assertEquals(MethodPreparation.Kind.RETRIEVE, banked.steps.get(0).kind);
    }

    @Test
    public void unknownBankAndEnabledUnobservedGroupStockCannotBecomeEmpty()
    {
        GameData unknownBank = GameData.builder(MethodIntelligenceTest.account(1))
                .inventory(new ItemsState(empty(), true)).equipment(new ItemsState(empty(), true))
                .bank(ItemsState.unknown()).build();
        assertFalse(prepare(unknownBank, knife()).feasible());
        GameData group = MethodIntelligenceTest.data(4, -1, empty(), empty());
        assertFalse(preparation.evaluate(group, knife(), true, true, false).feasible());
        assertTrue(preparation.evaluate(group, knife(), false, true, false).feasible());
    }

    @Test
    public void acquisitionRequiresObservedInventoryAndEquipmentForEveryMode()
    {
        for (int mode : new int[] {0, 1, 2, 3, 4, 5, 6, -1})
            for (int missing = 0; missing < 3; missing++)
            {
                ItemsState known = new ItemsState(empty());
                GameData data = GameData.builder(MethodIntelligenceTest.account(mode))
                        .inventory(missing == 0 ? ItemsState.unknown() : known)
                        .equipment(missing == 1 ? ItemsState.unknown() : known)
                        .bank(known).build();
                assertEquals(mode >= 0 && missing == 2, prepare(data, knife()).feasible());
            }
    }

    @Test
    public void unsupportedRemainderCannotExposeAPartialAcquisitionPlan()
    {
        List<MethodInput> needs = Arrays.asList(new MethodInput("Knife", 946, 1),
                new MethodInput("Oak logs", 1521, 4));
        MethodPreparation result = prepare(MethodIntelligenceTest.data(1, -1, empty(), empty()), needs);
        assertFalse(result.feasible());
        assertFalse(result.steps.stream().anyMatch(step -> step.kind == MethodPreparation.Kind.ACQUIRE));
    }

    @Test
    public void purchaseFailureCanFallBackToAReviewedFreeSource()
    {
        MarketPriceService prices = new MarketPriceService(null)
        {
            @Override public MarketPriceQuote quote(String name)
            { return new MarketPriceQuote(946, "Knife", 100); }
        };
        MethodPreparationService service = new MethodPreparationService(new AccountResourcePlanner(
                prices, new ResourceSourceCatalog()));
        assertEquals(MethodPreparation.Kind.ACQUIRE, service.evaluate(
                MethodIntelligenceTest.data(0, 0, empty(), empty()), knife(), false, true, true).steps.get(0).kind);
        assertEquals(MethodPreparation.Kind.BUY, service.evaluate(
                MethodIntelligenceTest.data(0, 100, empty(), empty()), knife(), false, true, true).steps.get(0).kind);
    }

    @Test
    public void pickupContractRequiresSafeAccessAndNoIrreversibleExperience()
    {
        rejects(source -> source.access = null);
        rejects(source -> source.kind = null);
        rejects(source -> source.risk = null);
        rejects(source -> source.membersOnly = null);
        rejects(source -> source.experienceSkills = null);
        rejects(source -> source.experienceSkills = Collections.singletonList(Skill.ATTACK));
        rejects(source -> source.sourceRevision = 0);
    }

    @Test
    public void exactIdentityAndMembershipAreRequired()
    {
        MethodAcquisitionCatalog.Source source = definitions()[0];
        source.membersOnly = true;
        MethodAcquisitionCatalog catalog = new MethodAcquisitionCatalog(new MethodAcquisitionCatalog.Source[] {source});
        assertNull(catalog.step(new MethodInput("Wrong name", 946, 1), Membership.P2P));
        assertNull(catalog.step(knife().get(0), Membership.F2P));
        assertNull(catalog.step(knife().get(0), Membership.UNKNOWN));
        assertNotNull(catalog.step(knife().get(0), Membership.P2P));
    }

    private MethodPreparation prepare(GameData data, List<MethodInput> needs)
    { return preparation.evaluate(data, needs, false, true, true); }

    private static List<MethodInput> knife()
    { return Collections.singletonList(new MethodInput("Knife", 946, 1)); }

    private static List<ItemState> empty() { return Collections.emptyList(); }

    private static MethodAcquisitionCatalog.Source[] definitions()
    { return BundledCatalogLoader.array("/content/catalogs/method-acquisition.json", MethodAcquisitionCatalog.Source[].class); }

    private static void rejects(Consumer<MethodAcquisitionCatalog.Source> mutation)
    {
        MethodAcquisitionCatalog.Source[] definitions = definitions();
        mutation.accept(definitions[0]);
        assertThrows(IllegalStateException.class, () -> new MethodAcquisitionCatalog(definitions));
    }
}

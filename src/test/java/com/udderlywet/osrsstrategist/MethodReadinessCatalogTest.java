package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MethodReadinessCatalogTest
{
    private final RequirementEvidenceEngine evidence =
            new RequirementEvidenceEngine((FarmingAccessEvaluator) null);

    @Test
    public void wineSuppliesBecomeFullyVerifiedFromObservedItems()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(0, 50, 50))
                .inventory(inventory(
                        item("Grapes", 14),
                        item("Jug of water", 14)))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();

        List<RequirementCheck> checks = evidence.evaluate(
                data, method("cooking_wines"));

        assertEquals(2, checks.size());
        assertTrue(checks.stream().allMatch(
                check -> check.getState() == RequirementState.VERIFIED));
    }

    @Test
    public void fletchingLogAlternativesRespectCurrentFletchingLevel()
    {
        StrategyDataBundle tooHigh = StrategyDataBundle.builder(account(0, 20, 50))
                .inventory(inventory(
                        item("Knife", 1),
                        item("Magic logs", 100)))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();
        List<RequirementCheck> highChecks = evidence.evaluate(
                tooHigh, method("fletching_bows"));
        RequirementCheck highLogs = check(highChecks, "Usable bow logs");
        assertEquals(RequirementState.CHECK_NEEDED, highLogs.getState());

        StrategyDataBundle usable = StrategyDataBundle.builder(account(0, 20, 50))
                .inventory(inventory(
                        item("Knife", 1),
                        item("Oak logs", 100)))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();
        List<RequirementCheck> usableChecks = evidence.evaluate(
                usable, method("fletching_bows"));
        assertEquals(RequirementState.VERIFIED,
                check(usableChecks, "Usable bow logs").getState());
    }

    @Test
    public void namedRequirementsStillIgnoreNormalBankForUim()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(2, 50, 50))
                .inventory(inventory())
                .bank(new BankSnapshot(Arrays.asList(
                        item("Grapes", 100), item("Jug of water", 100)), 1L))
                .build();

        List<RequirementCheck> checks = evidence.evaluate(
                data, method("cooking_wines"));
        assertTrue(checks.stream().allMatch(
                check -> check.getState() == RequirementState.CHECK_NEEDED));
    }

    @Test
    public void constructionProfileChecksRealQuantitiesBeforeAccess()
    {
        StrategyDataBundle data = StrategyDataBundle.builder(account(0, 70, 70))
                .inventory(inventory(
                        item("Oak plank", 8),
                        item("Hammer", 1),
                        item("Saw", 1)))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();

        List<RequirementCheck> checks = evidence.evaluate(
                data, method("construction_oak_larders"));
        assertEquals(4, checks.size());
        assertEquals(RequirementState.VERIFIED,
                check(checks, "Oak planks").getState());
        assertEquals(RequirementState.VERIFIED,
                check(checks, "Hammer").getState());
        assertEquals(RequirementState.VERIFIED,
                check(checks, "Saw").getState());
        assertEquals(RequirementState.CHECK_NEEDED,
                check(checks, "POH kitchen with an oak-larder build space").getState());
    }

    @Test
    public void curlyApostropheItemNamesNormalizeForStableMatching()
    {
        NamedResourceRequirement requirement = new NamedResourceRequirement(
                "red-eggs", "Red spiders' eggs", 1,
                ItemNameRule.exact("Red spiders' eggs"));
        StrategyDataBundle data = StrategyDataBundle.builder(account(0, 70, 70))
                .inventory(inventory(item("Red spiders’ eggs", 5)))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .build();

        assertEquals(RequirementState.VERIFIED,
                new ResourceReadinessService().evaluate(data, requirement).getState());
    }

    @Test
    public void catalogCoversGrowingSetOfHighValueItemDrivenMethods()
    {
        MethodReadinessCatalog catalog = new MethodReadinessCatalog();
        assertTrue(catalog.size() >= 20);
        assertNotNull(catalog.forMethod("cooking_wines"));
        assertNotNull(catalog.forMethod("fletching_bows"));
        assertNotNull(catalog.forMethod("firemaking_f2p_logs"));
        assertNotNull(catalog.forMethod("herblore_prayer_potions"));
        assertNotNull(catalog.forMethod("construction_oak_larders"));
        assertNotNull(catalog.forMethod("fishing_f2p_fly"));
        assertNotNull(catalog.forMethod("slayer_cannon_tasks"));
    }

    private static TrainingMethod method(String id)
    {
        ExpandedTrainingMethodCatalog catalog = new ExpandedTrainingMethodCatalog();
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod method : catalog.methodsFor(skill))
            {
                if (id.equals(method.getMethod().getId()))
                    return method.getMethod();
            }
        }
        throw new AssertionError("Missing expanded method: " + id);
    }

    private static RequirementCheck check(
            List<RequirementCheck> checks, String label)
    {
        return checks.stream()
                .filter(value -> label.equals(value.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing check: " + label));
    }

    private static InventorySnapshot inventory(ItemStackSnapshot... items)
    {
        return new InventorySnapshot(
                items == null ? Collections.emptyList() : Arrays.asList(items));
    }

    private static ItemStackSnapshot item(String name, int quantity)
    {
        // Named-resource tests intentionally do not depend on a specific numeric
        // gameval ID. Live snapshots still retain the real ID for exact checks.
        return new ItemStackSnapshot(name.hashCode(), name, quantity);
    }

    private static AccountSnapshot account(
            int accountTypeCode,
            int fletching,
            int generalLevel)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, generalLevel);
            xp.put(skill, 0);
        }
        levels.put(Skill.FLETCHING, fletching);
        return new AccountSnapshot(
                "Test", accountTypeCode,
                AccountMode.fromTypeCode(accountTypeCode).name(),
                MembershipStatus.MEMBER,
                generalLevel,
                generalLevel,
                0L,
                levels,
                xp);
    }
}

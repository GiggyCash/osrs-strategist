package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AdaptiveMilestoneGuidanceServiceTest
{
    @Test
    public void highAlchemyCountsEveryModeledRuneAndMainShortfall()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(0, Membership.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Arrays.asList(
                        item(561, "Nature rune", 10),
                        item(554, "Fire rune", 50)), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("High Level Alchemy"));
        assertTrue(guidance.getSupplies().contains("Nature rune"));
        assertTrue(guidance.getSupplies().contains("Fire rune"));
        assertTrue(guidance.getSupplies().contains(
                "Do not assume the shortfall should be bought"));
    }

    @Test
    public void curseGuidanceCountsVerifiedSpellRunesAndSplashSetup()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "curse", "Curse", 19, 29));
        AccountSnapshot account = account(0, Membership.P2P,
                Skill.MAGIC, 19, Experience.getXpForLevel(19));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 19, 20,
                plan("magic_f2p_curse", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Curse"));
        assertTrue(guidance.getSupplies().contains("Body rune"));
        assertTrue(guidance.getSupplies().contains("Earth rune"));
        assertTrue(guidance.getSupplies().contains("Water rune"));
        assertTrue(guidance.getSupplies().contains("-64 Magic attack"));
    }

    @Test
    public void fireStrikeSplashingNamesAutocastAndExactRuneInputs()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "fire_strike", "Fire Strike", 13, 11.5f));
        AccountSnapshot account = account(0, Membership.P2P,
                Skill.MAGIC, 13, Experience.getXpForLevel(13));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 13, 14,
                plan("magic_f2p_fire_strike_splash", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Fire Strike"));
        assertTrue(guidance.getSupplies().contains("Air rune"));
        assertTrue(guidance.getSupplies().contains("Fire rune"));
        assertTrue(guidance.getSupplies().contains("Mind rune"));
        assertTrue(guidance.getSupplies().contains("autocast"));
        assertTrue(guidance.getLocation().contains("Varrock Palace"));
    }

    @Test
    public void equippedFireStaffRemovesFireRuneShortfallFromHighAlchemy()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(0, Membership.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Collections.singletonList(
                        item(561, "Nature rune", 5000)), 1L))
                .equipment(new ItemsState(Collections.singletonList(
                        item(1387, "Staff of fire", 1))))
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains(
                "Fire rune supplied by Staff of fire"));
        assertFalse(guidance.getSupplies().contains("Buy "));
    }

    @Test
    public void bankedFireStaffDoesNotPretendToBeEquippedRuneSource()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(0, Membership.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Arrays.asList(
                        item(561, "Nature rune", 5000),
                        item(1387, "Staff of fire", 1)), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertFalse(guidance.getSupplies().contains(
                "Fire rune supplied by Staff of fire"));
        assertTrue(guidance.getSupplies().contains("Fire rune"));
    }

    @Test
    public void uimIgnoresNormalBankWhenCalculatingShortfall()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(2, Membership.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Arrays.asList(
                        item(561, "Nature rune", 100000),
                        item(554, "Fire rune", 100000)), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains(
                "normal bank never counts for UIM"));
        assertTrue(guidance.getSupplies().contains("Current-stage shortfall"));
        assertTrue(guidance.getSupplies().contains("resupply only"));
    }

    @Test
    public void uimHighAlchemyCountsLiveRunePouchRunesAsUsable()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.MAGIC, "high_level_alchemy", "High Level Alchemy", 55, 65));
        AccountSnapshot account = account(2, Membership.P2P,
                Skill.MAGIC, 55, Experience.getXpForLevel(55));
        Map<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.put(StorageKind.RUNE_POUCH, Capability.VERIFIED);
        Map<StorageKind, List<ItemState>> contents =
                new EnumMap<>(StorageKind.class);
        contents.put(StorageKind.RUNE_POUCH, Arrays.asList(
                item(561, "Nature rune", 2000),
                item(554, "Fire rune", 10000)));
        GameData data = GameData.builder(account)
                .inventory(new ItemsState(Collections.singletonList(
                        item(12791, "Rune pouch", 1))))
                .equipment(new ItemsState(Collections.emptyList()))
                .storage(new StorageSnapshot(states, contents))
                .build();

        Guidance guidance = service.build(
                data, Skill.MAGIC, 55, 60,
                plan("magic_high_alch", Skill.MAGIC), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Verified usable:"));
        assertTrue(guidance.getSupplies().contains("Nature rune"));
        assertTrue(guidance.getSupplies().contains("Fire rune"));
        assertTrue(guidance.getSupplies().contains(
                "You already have the modeled inputs"));
        assertFalse(guidance.getSupplies().contains("Acquire"));
    }

    @Test
    public void gimCountsObservedGroupStorageWhenEnabled()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.HERBLORE, "prayer_potion", "Prayer potion", 38, 87.5f));
        AccountSnapshot account = account(4, Membership.P2P,
                Skill.HERBLORE, 38, Experience.getXpForLevel(38));
        GameData data = GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .groupStorage(new ItemsState(true, Arrays.asList(
                        item(257, "Ranarr weed", 100),
                        item(231, "Snape grass", 100),
                        item(227, "Vial of water", 100))))
                .build();

        Guidance guidance = service.build(
                data, Skill.HERBLORE, 38, 40,
                plan("herblore_prayer_potions", Skill.HERBLORE), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Verified usable:"));
        assertTrue(guidance.getSupplies().contains("Ranarr weed"));
        assertTrue(guidance.getSupplies().contains("Snape grass"));
        assertTrue(guidance.getSupplies().contains("Vial of water"));
        assertTrue(guidance.getSupplies().contains("Group Storage"));
    }

    @Test
    public void mixedFlyFishingUsesAnOutfitAdjustedRangeNotSalmonPrecision()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.FISHING, "trout", "Trout", 20, 50),
                action(Skill.FISHING, "salmon", "Salmon", 30, 70));
        int currentXp = Experience.getXpForLevel(70);
        int targetXp = Experience.getXpForLevel(80);
        int xpNeeded = targetXp - currentXp;
        int lower = (int) Math.ceil(xpNeeded / (70.0 * 1.025));
        int upper = (int) Math.ceil(xpNeeded / (50.0 * 1.025));

        AccountSnapshot account = account(0, Membership.P2P,
                Skill.FISHING, 70, currentXp);
        ItemsState equipment = new ItemsState(Arrays.asList(
                item(1, "Angler hat", 1),
                item(2, "Angler top", 1),
                item(3, "Angler waders", 1),
                item(4, "Angler boots", 1)));
        GameData data = GameData.builder(account)
                .equipment(equipment)
                .build();

        Guidance guidance = service.build(
                data, Skill.FISHING, 70, 80,
                plan("fishing_f2p_fly", Skill.FISHING), true);

        assertNotNull(guidance);
        assertTrue(guidance.getProgress().contains(
                String.format(java.util.Locale.ROOT, "%,d–%,d", lower, upper)));
        assertTrue(guidance.getProgress().contains("Trout and Salmon"));
        assertFalse(guidance.getProgress().contains("with Salmon"));
        assertTrue(guidance.getNote().contains("full Angler/Spirit Angler outfit"));
    }

    @Test
    public void salamandersResolveExactTrapCountAndHabitat()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.HUNTER, "red_salamander",
                        "Red salamander", 59, 272));
        GameData data = GameData.builder(
                account(1, Membership.P2P, Skill.HUNTER, 60,
                        Experience.getXpForLevel(60)))
                .inventory(new ItemsState(Arrays.asList(
                        item(954, "Rope", 4),
                        item(303, "Small fishing net", 4))))
                .build();

        Guidance guidance = service.build(data, Skill.HUNTER,
                60, 61, plan("hunter_salamanders", Skill.HUNTER), true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains(
                "4 small fishing nets and 4 ropes"));
        assertEquals("Red salamander net-trap trees south of the Ourania Cave entrance.",
                guidance.getLocation());
    }

    @Test
    public void uimInventoryProcessingNeverRoutesThroughABank()
    {
        AdaptiveMilestoneGuidanceService service = serviceWith(
                action(Skill.CRAFTING, "sapphire", "Sapphire", 20, 50));
        GameData data = GameData.builder(
                account(2, Membership.P2P, Skill.CRAFTING, 20,
                        Experience.getXpForLevel(20)))
                .inventory(new ItemsState(Collections.singletonList(
                        item(1623, "Uncut sapphire", 20))))
                .build();

        Guidance guidance = service.build(data, Skill.CRAFTING,
                20, 21, plan("crafting_gems", Skill.CRAFTING), true);

        assertNotNull(guidance);
        assertTrue(guidance.getLocation().contains(
                "immediately usable carried materials"));
        assertFalse(guidance.getLocation().toLowerCase().contains("bank"));
    }

    private static AdaptiveMilestoneGuidanceService serviceWith(
            ActionDef... definitions)
    {
        final List<ActionDef> actions = Arrays.asList(definitions);
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override
            public List<ActionDef> actionsFor(Skill skill)
            {
                java.util.ArrayList<ActionDef> result =
                        new java.util.ArrayList<>();
                for (ActionDef action : actions)
                {
                    if (action.getSkill() == skill) result.add(action);
                }
                return result;
            }
        };
        return TestFixtures.adaptiveMilestoneGuidanceService(
                catalog,
                new MethodExecutionProfileCatalog(),
                new SkillingXpModifierService());
    }

    private static ActionDef action(
            Skill skill, String id, String name, int level, float xp)
    {
        return new ActionDef(
                skill,
                "runelite:" + skill.name().toLowerCase() + ":" + id,
                name,
                level,
                xp,
                null,
                Membership.P2P,
                -1);
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id, skill, 1, 99, id,
                "Use the modeled method.",
                10, 10, 10, AttentionLevel.MODERATE,
                10, 1, Collections.emptyList(),
                Confidence.VERIFIED);
        return new TrainingPlan(method, "test",
                Confidence.VERIFIED,
                Collections.emptyList());
    }

    private static AccountSnapshot account(
            int typeCode,
            Membership membership,
            Skill trainedSkill,
            int level,
            int trainedXp)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 99);
            xp.put(skill, Experience.getXpForLevel(99));
        }
        levels.put(trainedSkill, level);
        xp.put(trainedSkill, trainedXp);
        return new AccountSnapshot("Guidance Test", 0L, typeCode, typeCode == 0 ? "Main" : "Restricted", membership, 1, 2200, 0L, levels, xp);
    }

    private static ItemState item(int id, String name, int quantity)
    {
        return new ItemState(id, name, quantity);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VariableMethodGuidanceServiceTest
{
    private final VariableMethodGuidanceService service =
            new VariableMethodGuidanceService();

    @Test
    public void wintertodtGivesConcreteSetupWithoutFakeKillCount()
    {
        StrategyDataBundle data = data(Skill.FIREMAKING, 60,
                new ItemStackSnapshot(20704, "Bruma torch", 1));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.FIREMAKING,
                60,
                70,
                plan("firemaking_wintertodt", Skill.FIREMAKING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("500 personal points"));
        assertTrue(guidance.getSupplies().contains("four warm items"));
        assertTrue(guidance.getSupplies().contains("cakes"));
        assertTrue(guidance.getSupplies().contains("35% warmth"));
        assertTrue(guidance.getSupplies().contains("Bruma torch"));
        assertEquals("Wintertodt camp in northern Great Kourend.",
                guidance.getLocation());
        assertFalse(guidance.getAction().matches(".*about [0-9]+ (games|kills).*"));
        assertTrue(guidance.getNote().contains("without inventing a fixed kill count"));
    }

    @Test
    public void temporossUsesObservedHarpoonAndNoFakeGameCount()
    {
        StrategyDataBundle data = data(Skill.FISHING, 70,
                new ItemStackSnapshot(11920, "Dragon harpoon", 1));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.FISHING,
                70,
                80,
                plan("fishing_tempoross", Skill.FISHING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Dragon harpoon"));
        assertEquals("Tempoross island, entered from the Ruins of Unkah ferry.",
                guidance.getLocation());
        assertFalse(guidance.getAction().matches(".*about [0-9]+ games.*"));
    }

    @Test
    public void foundryExplainsTwentyEightBarCommissionWithoutFakeSwordCount()
    {
        StrategyDataBundle data = data(Skill.SMITHING, 70,
                new ItemStackSnapshot(2359, "Mithril bar", 500),
                new ItemStackSnapshot(2353, "Steel bar", 500));
        RecommendationGuidance guidance = service.build(
                data,
                Skill.SMITHING,
                70,
                80,
                plan("smithing_foundry", Skill.SMITHING),
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("28 bars"));
        assertTrue(guidance.getAction().contains("14 mithril bar"));
        assertTrue(guidance.getAction().contains("14 steel bar"));
        assertTrue(guidance.getAction().contains("highest green score"));
        assertEquals("Giants' Foundry beneath Kovac's workshop, east of Al Kharid.",
                guidance.getLocation());
        assertTrue(guidance.getSupplies().contains("Mithril bar"));
        assertFalse(guidance.getAction().matches(".*about [0-9]+ .*swords.*"));
    }

    @Test
    public void herbRunDoesNotDelegateSeedValueWhenNoneIsObserved()
    {
        StrategyDataBundle data = data(Skill.FARMING, 90,
                new ItemStackSnapshot(952, "Spade", 1));
        RecommendationGuidance guidance = service.build(
                data, Skill.FARMING, 90, 91,
                plan("farming_herbs_expanded", Skill.FARMING), true);

        assertNull(guidance);
    }

    @Test
    public void foundryDoesNotSurfaceWithoutOneCompleteCommissionOfMetal()
    {
        StrategyDataBundle data = data(Skill.SMITHING, 70,
                new ItemStackSnapshot(2359, "Mithril bar", 27));
        assertNull(service.build(data, Skill.SMITHING, 70, 80,
                plan("smithing_foundry", Skill.SMITHING), true));
    }

    @Test
    public void farmingContractTierIsResolvedFromLevel()
    {
        RecommendationGuidance guidance = service.build(
                data(Skill.FARMING, 72,
                        new ItemStackSnapshot(952, "Spade", 1)),
                Skill.FARMING, 72, 73,
                plan("farming_contracts", Skill.FARMING), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("medium contract"));
        assertTrue(guidance.getAction().contains("another medium contract"));
        assertTrue(guidance.getLocation().contains("Guildmaster Jane"));
    }

    @Test
    public void rumourMasterIsResolvedAndQuestGated()
    {
        RecommendationGuidance expert = service.build(
                data(Skill.HUNTER, 95,
                        new ItemStackSnapshot(1, "Basic quetzal whistle", 1)),
                Skill.HUNTER, 95, 96,
                plan("hunter_rumours", Skill.HUNTER), true);
        RecommendationGuidance master = service.build(
                dataWithQuest(Skill.HUNTER, 95, "At First Light"),
                Skill.HUNTER, 95, 96,
                plan("hunter_rumours", Skill.HUNTER), true);

        assertTrue(expert.getAction().contains("Expert rumour"));
        assertTrue(expert.getAction().contains("Guild Hunter Teco"));
        assertTrue(master.getAction().contains("Master rumour"));
        assertTrue(master.getAction().contains("Guild Hunter Wolf"));
    }

    @Test
    public void forestryResolvesOneTreeAndOneLocation()
    {
        RecommendationGuidance guidance = service.build(
                data(Skill.WOODCUTTING, 62,
                        new ItemStackSnapshot(1359, "Rune axe", 1)),
                Skill.WOODCUTTING, 62, 65,
                plan("woodcutting_forestry", Skill.WOODCUTTING), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("cut yew trees"));
        assertEquals("Yew trees beside Seers' Village church.",
                guidance.getLocation());
        assertFalse(guidance.getAction().contains("best reachable"));
        assertFalse(guidance.getLocation().contains("Choose"));
    }

    @Test
    public void mahoganyHomesUsesObservedSustainableTier()
    {
        RecommendationGuidance guidance = service.build(
                data(Skill.CONSTRUCTION, 75,
                        new ItemStackSnapshot(8780, "Teak plank", 100),
                        new ItemStackSnapshot(2353, "Steel bar", 10)),
                Skill.CONSTRUCTION, 75, 76,
                plan("construction_mahogany_homes", Skill.CONSTRUCTION), true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Adept contract"));
        assertFalse(guidance.getAction().contains("highest"));
        assertEquals("Amy at Mahogany Homes, immediately south of Falador Park.",
                guidance.getLocation());
    }

    @Test
    public void bundledStaticProfilesRenderWithoutLosingPlaceholders()
    {
        String[] ids = {
                "firemaking_wintertodt", "fishing_karambwan",
                "runecraft_zmi", "mining_mlm", "mining_volcanic",
                "mining_blast_mine", "herblore_mixology",
                "farming_falador_potatoes",
                "farming_falador_watermelons", "hunter_herbiboar",
                "thieving_pyramid", "thieving_varlamore"
        };
        Skill[] skills = {
                Skill.FIREMAKING, Skill.FISHING, Skill.RUNECRAFT,
                Skill.MINING, Skill.MINING, Skill.MINING, Skill.HERBLORE,
                Skill.FARMING, Skill.FARMING, Skill.HUNTER,
                Skill.THIEVING, Skill.THIEVING
        };
        for (int i = 0; i < ids.length; i++)
        {
            RecommendationGuidance guidance = service.build(
                    data(skills[i], 60,
                            new ItemStackSnapshot(1275, "Rune pickaxe", 1)),
                    skills[i], 60, 61, plan(ids[i], skills[i]), true);
            assertNotNull(ids[i], guidance);
            assertFalse(ids[i], guidance.getAction().contains("{xp}"));
            assertFalse(ids[i], guidance.getAction().contains("{target}"));
            assertFalse(ids[i], guidance.getSupplies().contains("{observed}"));
            assertFalse(ids[i], guidance.getSupplies().contains("{pickaxe}"));
        }
    }

    private static StrategyDataBundle data(
            Skill skill,
            int level,
            ItemStackSnapshot... observed)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill s : Skill.values())
        {
            levels.put(s, 60);
            xp.put(s, Experience.getXpForLevel(60));
        }
        levels.put(skill, level);
        xp.put(skill, Experience.getXpForLevel(level));
        AccountSnapshot account = new AccountSnapshot(
                "Variable Test",
                0,
                "Main",
                MembershipStatus.P2P,
                1,
                1500,
                0L,
                levels,
                xp);
        return StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Arrays.asList(observed), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .build();
    }

    private static StrategyDataBundle dataWithQuest(
            Skill skill, int level, String quest)
    {
        StrategyDataBundle base = data(skill, level);
        Map<String, QuestStatus> statuses = new HashMap<>();
        statuses.put(quest, QuestStatus.COMPLETE);
        return StrategyDataBundle.builder(base.getAccount())
                .bank(base.getBank())
                .inventory(base.getInventory())
                .quests(new QuestSnapshot(statuses))
                .build();
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new TrainingPlan(
                method,
                "test",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList());
    }
}

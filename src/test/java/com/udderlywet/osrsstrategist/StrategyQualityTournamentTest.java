package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Paired method tournaments: one account variable changes at a time. */
public class StrategyQualityTournamentTest
{
    private final TrainingMethodSelector selector = new TrainingMethodSelector(
            new TrainingMethodDatabase(),
            new RequirementEvidenceEngine(
                    new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                    new AgilityAccessEvaluator(new AgilityCourseCatalog())),
            new ExpandedTrainingMethodCatalog(),
            new F2pBaselineMethodCatalog(),
            new TrainingMethodPolicy());

    @Test
    public void cookingStyleAndOwnedSuppliesChangeThePracticalWinner()
    {
        StrategyDataBundle ready = data(0, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200),
                        item(ItemID.GRAPES, "Grapes", 200),
                        item(ItemID.JUG_WATER, "Jug of water", 200)),
                quests(), minigames(), true);

        assertWinner("cooking_wines", ready, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR);
        assertWinner("cooking_hosidius", ready, Skill.COOKING, 70,
                StrategyMode.RELAXED, SessionIntent.ONE_HOUR);
        assertWinner("cooking_hosidius", ready, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.AFK);

        StrategyDataBundle noWineSupplies = data(0, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                quests(), minigames(), true);
        assertWinner("cooking_hosidius", noWineSupplies, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN);
        assertWinner("cooking_wines", noWineSupplies, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);

        StrategyDataBundle iron = data(1, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                quests(), minigames(), true);
        assertWinner("cooking_hosidius", iron, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
    }

    @Test
    public void runecraftStyleChangesGotrVersusZmiAndEssenceTypesStayDistinct()
    {
        StrategyDataBundle ready = data(0, MembershipStatus.P2P,
                items(item(ItemID.BLANKRUNE_HIGH, "Pure essence", 2_000)),
                quests(), minigames("guardians-of-the-rift"), false);
        assertWinner("runecraft_gotr", ready, Skill.RUNECRAFT, 70,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertWinner("runecraft_zmi", ready, Skill.RUNECRAFT, 70,
                StrategyMode.RELAXED, SessionIntent.ONE_HOUR);

        StrategyDataBundle runeEssenceOnly = data(0, MembershipStatus.P2P,
                items(item(ItemID.BLANKRUNE, "Rune essence", 2_000),
                        item(ItemID.BODY_TALISMAN, "Body talisman", 1)),
                quests(), minigames(), false);
        TrainingPlan plan = winner(runeEssenceOnly, Skill.RUNECRAFT, 70,
                StrategyMode.RELAXED, SessionIntent.ONE_HOUR);
        assertEquals("runecraft_f2p_body", plan.getMethod().getId());
        assertFalse("Rune essence must not satisfy ZMI pure essence",
                plan.getMethod().getId().equals("runecraft_zmi"));
    }

    @Test
    public void fishingSessionIntentChangesTemporossVersusKarambwans()
    {
        Map<String, QuestStatus> completed = quests();
        completed.put("Tai Bwo Wannai Trio", QuestStatus.COMPLETE);
        StrategyDataBundle ready = data(1, MembershipStatus.P2P,
                items(item(ItemID.TBWT_KARAMBWAN_VESSEL,
                                "Karambwan vessel", 1),
                        item(ItemID.TBWT_RAW_KARAMBWANJI,
                                "Raw karambwanji", 2_000)),
                completed, minigames("tempoross"), false);

        assertWinner("fishing_tempoross", ready, Skill.FISHING, 70,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR);
        assertWinner("fishing_karambwan", ready, Skill.FISHING, 70,
                StrategyMode.RELAXED, SessionIntent.AFK);
    }

    @Test
    public void hunterStyleChangesFalconryVersusVerifiedHerbiboar()
    {
        Map<String, QuestStatus> completed = quests();
        completed.put("Bone Voyage", QuestStatus.COMPLETE);
        StrategyDataBundle ready = data(1, MembershipStatus.P2P,
                items(item(ItemID.COINS, "Coins", 500)), completed,
                minigames(), false);

        assertWinner("hunter_falconry", ready, Skill.HUNTER, 80,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR);
        assertWinner("hunter_herbiboar", ready, Skill.HUNTER, 80,
                StrategyMode.RELAXED, SessionIntent.ONE_HOUR);
    }

    @Test
    public void farmingStyleChangesContinuousTitheVersusRecurringHerbs()
    {
        StrategyDataBundle ready = tournamentData();
        assertWinner("farming_tithe", ready, Skill.FARMING, 80,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
        assertWinner("farming_herbs_expanded", ready, Skill.FARMING, 80,
                StrategyMode.RELAXED, SessionIntent.QUICK_20_MIN);
    }

    @Test
    public void f2pCannotWinHosidiusCookingRoute()
    {
        StrategyDataBundle f2p = data(0, MembershipStatus.F2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200),
                        item(ItemID.GRAPES, "Grapes", 200),
                        item(ItemID.JUG_WATER, "Jug of water", 200)),
                quests(), minigames(), true);
        TrainingPlan plan = winner(f2p, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
        assertFalse("F2P must not receive Hosidius",
                plan.getMethod().getId().equals("cooking_hosidius"));
    }

    @Test
    public void accountModeChangesWhetherLongSessionWineSetupIsWorthIt()
    {
        int[] accountTypes = {0, 1, 4, 2, 3};
        String[] expected = {"cooking_wines", "cooking_hosidius",
                "cooking_hosidius", "cooking_hosidius",
                "cooking_hosidius"};
        for (int i = 0; i < accountTypes.length; i++)
        {
            StrategyDataBundle account = data(accountTypes[i],
                    MembershipStatus.P2P,
                    items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                    quests(), minigames(), true);
            TrainingPlan plan = winner(account, Skill.COOKING, 70,
                    StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
            assertEquals(AccountMode.fromTypeCode(accountTypes[i]).name(),
                    expected[i], plan.getMethod().getId());
            System.out.println("ACCOUNT_MODE_SENSITIVITY "
                    + AccountMode.fromTypeCode(accountTypes[i])
                    + " winner=" + plan.getMethod().getId());
        }
    }

    @Test
    public void tournamentReportsWinnersAndRunnersUpWithoutUniversalDominance()
    {
        Map<String, Integer> wins = new LinkedHashMap<>();
        Set<String> cooking = new HashSet<>();
        Set<String> fishing = new HashSet<>();
        Set<String> runecraft = new HashSet<>();
        Set<String> farming = new HashSet<>();
        for (StrategyMode mode : StrategyMode.values())
        {
            for (SessionIntent session : SessionIntent.values())
            {
                StrategyDataBundle prepared = tournamentData();
                for (Skill skill : EnumSet.of(
                        Skill.COOKING, Skill.FISHING, Skill.RUNECRAFT,
                        Skill.FARMING, Skill.HUNTER, Skill.MAGIC,
                        Skill.SLAYER))
                {
                    List<TrainingPlan> ranked = selector.rankedCandidates(
                            prepared, skill, 80, mode, session, false, false);
                    assertTrue(skill.getName() + " has no legal tournament route",
                            !ranked.isEmpty());
                    String first = ranked.get(0).getMethod().getId();
                    String second = ranked.size() > 1
                            ? ranked.get(1).getMethod().getId() : "none";
                    wins.merge(first, 1, Integer::sum);
                    if (skill == Skill.COOKING) cooking.add(first);
                    if (skill == Skill.FISHING) fishing.add(first);
                    if (skill == Skill.RUNECRAFT) runecraft.add(first);
                    if (skill == Skill.FARMING) farming.add(first);
                    System.out.println("METHOD_TOURNAMENT " + skill.getName()
                            + " " + mode + " " + session + " winner="
                            + first + " runnerUp=" + second);
                }
            }
        }
        System.out.println("METHOD_DOMINANCE " + wins);
        assertTrue("Cooking remains universal", cooking.size() >= 2);
        assertTrue("Fishing remains universal", fishing.size() >= 2);
        assertTrue("Runecraft remains universal", runecraft.size() >= 2);
        assertTrue("Farming remains universal", farming.size() >= 2);
    }

    private StrategyDataBundle tournamentData()
    {
        Map<String, QuestStatus> completed = quests();
        completed.put("Bone Voyage", QuestStatus.COMPLETE);
        completed.put("Tai Bwo Wannai Trio", QuestStatus.COMPLETE);
        return data(1, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 500),
                        item(ItemID.GRAPES, "Grapes", 500),
                        item(ItemID.JUG_WATER, "Jug of water", 500),
                        item(ItemID.BLANKRUNE_HIGH, "Pure essence", 2_000),
                        item(ItemID.AIRRUNE, "Air rune", 10_000),
                        item(ItemID.FIRERUNE, "Fire rune", 10_000),
                        item(ItemID.DEATHRUNE, "Death rune", 10_000),
                        item(ItemID.COINS, "Coins", 500),
                        item(ItemID.TBWT_KARAMBWAN_VESSEL,
                                "Karambwan vessel", 1),
                        item(ItemID.TBWT_RAW_KARAMBWANJI,
                                "Raw karambwanji", 2_000),
                        item(ItemID.RANARR_SEED, "Ranarr seed", 20),
                        item(ItemID.RAKE, "Rake", 1),
                        item(ItemID.DIBBER, "Seed dibber", 1),
                        item(ItemID.SPADE, "Spade", 1),
                        item(ItemID.WATERING_CAN_8, "Watering can(8)", 8)),
                completed,
                minigames("tempoross", "guardians-of-the-rift",
                        "tithe-farm"), true);
    }

    private void assertWinner(String expected, StrategyDataBundle data,
            Skill skill, int level, StrategyMode mode, SessionIntent session)
    {
        assertEquals(expected,
                winner(data, skill, level, mode, session).getMethod().getId());
    }

    private TrainingPlan winner(StrategyDataBundle data, Skill skill, int level,
            StrategyMode mode, SessionIntent session)
    {
        TrainingPlan plan = selector.select(data, skill, level, mode, session,
                false, false);
        assertTrue("No winner for " + skill, plan != null);
        return plan;
    }

    private static StrategyDataBundle data(int accountType,
            MembershipStatus membership, List<ItemStackSnapshot> inventory,
            Map<String, QuestStatus> quests, MinigameSnapshot minigames,
            boolean easyKourendDiary)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 80 : 80;
            levels.put(skill, level);
            xp.put(skill, Experience.getXpForLevel(level));
            total += level;
            totalXp += xp.get(skill);
        }
        AccountSnapshot account = new AccountSnapshot("Tournament",
                90_000L + accountType, accountType,
                AccountMode.fromTypeCode(accountType).name(), membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                total, totalXp, levels, xp);
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();
        if (easyKourendDiary)
        {
            Map<DiaryTier, Boolean> kourend = new EnumMap<>(DiaryTier.class);
            kourend.put(DiaryTier.EASY, true);
            tiers.put("Kourend & Kebos", kourend);
        }
        Map<String, CapabilityState> tools = new HashMap<>();
        tools.put("rake", CapabilityState.VERIFIED);
        tools.put("dibber", CapabilityState.VERIFIED);
        tools.put("spade", CapabilityState.VERIFIED);
        return StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(inventory))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .quests(new QuestSnapshot(quests))
                .diaries(new DiarySnapshot(Collections.emptyMap(),
                        Collections.emptyMap(), tiers))
                .minigames(minigames)
                .transport(new TransportSnapshot(
                        Collections.singleton("fairy-rings")))
                .farming(new FarmingSnapshot(
                        Collections.singleton("falador"), tools,
                        Collections.emptyMap()))
                .build();
    }

    private static Map<String, QuestStatus> quests()
    {
        return new HashMap<>();
    }

    private static MinigameSnapshot minigames(String... ids)
    {
        Set<String> unlocked = new HashSet<>();
        Collections.addAll(unlocked, ids);
        return new MinigameSnapshot(unlocked, Collections.emptyMap());
    }

    private static List<ItemStackSnapshot> items(ItemStackSnapshot... values)
    {
        List<ItemStackSnapshot> items = new ArrayList<>();
        Collections.addAll(items, values);
        return items;
    }

    private static ItemStackSnapshot item(int id, String name, int quantity)
    {
        return new ItemStackSnapshot(id, name, quantity);
    }
}

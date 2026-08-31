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
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
        GameData ready = data(0, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200),
                        item(ItemID.GRAPES, "Grapes", 200),
                        item(ItemID.JUG_WATER, "Jug of water", 200)),
                quests(), minigames(), true);

        assertWinner("cooking_wines", ready, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.ONE_HOUR);
        assertLowAttentionCookingWinner(ready, StrategyMode.RELAXED,
                SessionIntent.ONE_HOUR);
        assertLowAttentionCookingWinner(ready, StrategyMode.EFFICIENT,
                SessionIntent.AFK);

        GameData noWineSupplies = data(0, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                quests(), minigames(), true);
        assertLowAttentionCookingWinner(noWineSupplies,
                StrategyMode.EFFICIENT, SessionIntent.QUICK_20_MIN);
        assertWinner("cooking_wines", noWineSupplies, Skill.COOKING, 70,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);

        GameData iron = data(1, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                quests(), minigames(), true);
        assertLowAttentionCookingWinner(iron, StrategyMode.EFFICIENT,
                SessionIntent.LONG_SESSION);
    }

    @Test
    public void runecraftStyleChangesGotrVersusZmiAndEssenceTypesStayDistinct()
    {
        Map<String, QuestStatus> completed = quests();
        completed.put("Temple of the Eye", QuestStatus.COMPLETE);
        GameData ready = data(0, MembershipStatus.P2P,
                items(item(ItemID.BLANKRUNE_HIGH, "Pure essence", 2_000),
                        item(ItemID.BRONZE_PICKAXE, "Bronze pickaxe", 1),
                        item(ItemID.CHISEL, "Chisel", 1)),
                completed, minigames(), false);
        assertWinner("runecraft_gotr", ready, Skill.RUNECRAFT, 70,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        assertWinner("runecraft_zmi", ready, Skill.RUNECRAFT, 70,
                StrategyMode.RELAXED, SessionIntent.ONE_HOUR);

        GameData runeEssenceOnly = data(0, MembershipStatus.P2P,
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
        completed.put("Fairytale II - Cure a Queen", QuestStatus.COMPLETE);
        GameData ready = data(1, MembershipStatus.P2P,
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
        GameData ready = data(1, MembershipStatus.P2P,
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
        GameData ready = tournamentData();
        assertWinner("farming_tithe", ready, Skill.FARMING, 80,
                StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
        assertWinner("farming_herbs_expanded", ready, Skill.FARMING, 80,
                StrategyMode.RELAXED, SessionIntent.QUICK_20_MIN);
    }

    @Test
    public void f2pCannotWinHosidiusCookingRoute()
    {
        GameData f2p = data(0, MembershipStatus.F2P,
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
    public void fishCookingRetainsItsRawFoodOpportunityCost()
    {
        Map<String, MethodCostTier> costs = new HashMap<>();
        for (CuratedTrainingMethod method :
                new ExpandedTrainingMethodCatalog().methodsFor(Skill.COOKING))
        {
            costs.put(method.getMethod().getId(),
                    method.getMetadata().getCostTier());
        }
        assertEquals(MethodCostTier.LOW, costs.get("cooking_f2p_fish"));
        assertEquals(MethodCostTier.LOW, costs.get("cooking_hosidius"));
    }

    @Test
    public void highAlchemyIsNotClassifiedAsAfkTraining()
    {
        TrainingMethod highAlchemy = null;
        for (CuratedTrainingMethod method :
                new ExpandedTrainingMethodCatalog().methodsFor(Skill.MAGIC))
            if ("magic_high_alch".equals(method.getMethod().getId()))
                highAlchemy = method.getMethod();

        assertNotNull(highAlchemy);
        assertEquals(AttentionLevel.ACTIVE,
                highAlchemy.getAttentionLevel());
    }

    @Test
    public void legacyChoiceDelegationIsTypedCatalogData()
    {
        TrainingMethodDatabase legacy = new TrainingMethodDatabase();
        TrainingMethod genericMagic = legacy.methodsFor(Skill.MAGIC).stream()
                .filter(method -> "magic_utility".equals(method.getId()))
                .findFirst().orElse(null);
        TrainingMethod concreteCombat = legacy.methodsFor(Skill.ATTACK).stream()
                .filter(method -> "attack_combat".equals(method.getId()))
                .findFirst().orElse(null);

        assertNotNull(genericMagic);
        assertTrue(genericMagic.delegatesMethodChoice());
        assertNotNull(concreteCombat);
        assertFalse(concreteCombat.delegatesMethodChoice());
    }

    @Test
    public void afkMagicWinnerIsExecutableAndUsesObservedSplashingSetup()
    {
        GameData ready = magicSplashingData(true);
        Recommendation magic = onlyMagicRecommendation(ready);

        assertEquals("magic_f2p_fire_strike_splash",
                magic.getTrainingPlan().getMethod().getId());
        assertEquals(Confidence.VERIFIED,
                magic.getConfidence());
        assertTrue("guidance=" + magic.getGuidance().getAction() + " | "
                        + magic.getGuidance().getSupplies() + " | "
                        + magic.getGuidance().getLocation(),
                new RecommendationQualityPolicy().isPresentable(magic));
        assertFalse("hard unresolved requirement",
                RequirementActionability.hasHardUnresolvedRequirement(
                        magic.getTrainingPlan()));
        assertTrue(Presentation.compactText(magic),
                new ActionabilityPolicy()
                .canLeadQueue(magic));
        assertTrue(magic.getGuidance().getSupplies().contains("autocast"));

        Recommendation withoutSetup = onlyMagicRecommendation(
                magicSplashingData(false));
        assertFalse("The final queue must reject an unobserved -64 setup",
                new ActionabilityPolicy()
                        .canLeadQueue(withoutSetup));
    }

    @Test
    public void accountModeChangesWhetherLongSessionWineSetupIsWorthIt()
    {
        int[] accountTypes = {0, 1, 4, 2, 3};
        for (int i = 0; i < accountTypes.length; i++)
        {
            GameData account = data(accountTypes[i],
                    MembershipStatus.P2P,
                    items(item(ItemID.RAW_SALMON, "Raw salmon", 200)),
                    quests(), minigames(), true);
            TrainingPlan plan = winner(account, Skill.COOKING, 70,
                    StrategyMode.EFFICIENT, SessionIntent.LONG_SESSION);
            if (accountTypes[i] == 0)
            {
                assertEquals(AccountMode.fromTypeCode(accountTypes[i]).name(),
                        "cooking_wines", plan.getMethod().getId());
            }
            else
            {
                assertEquals(AccountMode.fromTypeCode(accountTypes[i]).name(),
                        AttentionLevel.LOW,
                        plan.getMethod().getAttentionLevel());
                assertFalse(AccountMode.fromTypeCode(accountTypes[i]).name(),
                        "cooking_wines".equals(plan.getMethod().getId()));
            }
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
        Set<String> magic = new HashSet<>();
        for (StrategyMode mode : StrategyMode.values())
        {
            for (SessionIntent session : SessionIntent.values())
            {
                GameData prepared = tournamentData();
                for (Skill skill : EnumSet.of(
                        Skill.COOKING, Skill.FISHING, Skill.RUNECRAFT,
                        Skill.FARMING, Skill.HUNTER, Skill.MAGIC,
                        Skill.SLAYER))
                {
                    List<TrainingPlan> ranked = selector.rankedCandidates(
                            prepared, skill, 80, mode, session, false, false);
                    assertTrue(skill.getName() + " has no legal tournament route",
                            !ranked.isEmpty());
                    assertFalse(skill.getName()
                                    + " winner has unresolved access evidence: "
                                    + ranked.get(0).getMethod().getId(),
                            RequirementActionability
                                    .hasHardUnresolvedRequirement(ranked.get(0)));
                    String first = ranked.get(0).getMethod().getId();
                    String second = ranked.size() > 1
                            ? ranked.get(1).getMethod().getId() : "none";
                    wins.merge(first, 1, Integer::sum);
                    if (skill == Skill.COOKING) cooking.add(first);
                    if (skill == Skill.FISHING) fishing.add(first);
                    if (skill == Skill.RUNECRAFT) runecraft.add(first);
                    if (skill == Skill.FARMING) farming.add(first);
                    if (skill == Skill.MAGIC) magic.add(first);
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
        assertFalse("A generic Magic fallback must not dominate concrete bands",
                magic.contains("magic_f2p_baseline"));
    }

    private GameData tournamentData()
    {
        Map<String, QuestStatus> completed = quests();
        completed.put("Bone Voyage", QuestStatus.COMPLETE);
        completed.put("Tai Bwo Wannai Trio", QuestStatus.COMPLETE);
        completed.put("Fairytale II - Cure a Queen", QuestStatus.COMPLETE);
        completed.put("Temple of the Eye", QuestStatus.COMPLETE);
        return data(1, MembershipStatus.P2P,
                items(item(ItemID.RAW_SALMON, "Raw salmon", 500),
                        item(ItemID.GRAPES, "Grapes", 500),
                        item(ItemID.JUG_WATER, "Jug of water", 500),
                        item(ItemID.BLANKRUNE_HIGH, "Pure essence", 2_000),
                        item(ItemID.BRONZE_PICKAXE, "Bronze pickaxe", 1),
                        item(ItemID.CHISEL, "Chisel", 1),
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
                        "tithe-farm"), true,
                items(item(1, "Iron full helm", 1),
                        item(2, "Iron platebody", 1),
                        item(3, "Iron platelegs", 1),
                        item(4, "Iron kiteshield", 1),
                        item(5, "Fancy boots", 1),
                        item(6, "Cursed goblin staff", 1)),
                new CombatEvidenceSnapshot(0,
                        EnumSet.noneOf(Prayer.class), false, false, false));
    }

    private Recommendation onlyMagicRecommendation(GameData data)
    {
        RuneLiteSkillActionCatalog catalog = new RuneLiteSkillActionCatalog()
        {
            @Override
            public List<ActionDef> actionsFor(Skill skill)
            {
                if (skill != Skill.MAGIC) return Collections.emptyList();
                return Collections.singletonList(
                        new ActionDef(Skill.MAGIC,
                                "runelite:magic:fire_strike", "Fire Strike",
                                13, 11.5f, null, MembershipStatus.F2P));
            }
        };
        RecommendationGuidanceService guidance =
                new RecommendationGuidanceService(
                        new AdaptiveMilestoneGuidanceService(catalog,
                                new MethodExecutionProfileCatalog()));
        List<Recommendation> recommendations = new RecommendationEngine(
                selector, guidance)
                .recommendAll(data, StrategyMode.RELAXED, SessionIntent.AFK,
                        false, false, GoalType.AUTOMATIC,
                        new PreferenceProfile());
        assertEquals(1, recommendations.size());
        assertEquals(Skill.MAGIC, recommendations.get(0).getTrainingPlan()
                .getMethod().getSkill());
        return recommendations.get(0);
    }

    private static GameData magicSplashingData(boolean equipped)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.MAGIC ? 80 : 99;
            levels.put(skill, level);
            int skillXp = Experience.getXpForLevel(level);
            xp.put(skill, skillXp);
            total += level;
            totalXp += skillXp;
        }
        AccountSnapshot account = new AccountSnapshot("Magic tournament",
                90_099L, 0, AccountMode.MAIN.name(), MembershipStatus.P2P,
                1, total, totalXp, levels, xp);
        List<ItemState> equipment = equipped
                ? items(item(1, "Iron full helm", 1),
                        item(2, "Iron platebody", 1),
                        item(3, "Iron platelegs", 1),
                        item(4, "Iron kiteshield", 1),
                        item(5, "Fancy boots", 1),
                        item(6, "Cursed goblin staff", 1))
                : Collections.emptyList();
        return GameData.builder(account)
                .inventory(new ItemsState(items(
                        item(ItemID.AIRRUNE, "Air rune", 10_000),
                        item(ItemID.FIRERUNE, "Fire rune", 10_000),
                        item(ItemID.MINDRUNE, "Mind rune", 10_000))))
                .equipment(new ItemsState(equipment))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .combatEvidence(new CombatEvidenceSnapshot(0,
                        EnumSet.noneOf(Prayer.class), false, false, false))
                .build();
    }

    private void assertWinner(String expected, GameData data,
            Skill skill, int level, StrategyMode mode, SessionIntent session)
    {
        assertEquals(expected,
                winner(data, skill, level, mode, session).getMethod().getId());
    }

    private void assertLowAttentionCookingWinner(GameData data,
            StrategyMode mode, SessionIntent session)
    {
        TrainingPlan plan = winner(data, Skill.COOKING, 70, mode, session);
        assertEquals(AttentionLevel.LOW,
                plan.getMethod().getAttentionLevel());
        assertFalse("Unready wines must not be forced as diversity",
                "cooking_wines".equals(plan.getMethod().getId()));
    }

    private TrainingPlan winner(GameData data, Skill skill, int level,
            StrategyMode mode, SessionIntent session)
    {
        TrainingPlan plan = selector.select(data, skill, level, mode, session,
                false, false);
        assertTrue("No winner for " + skill, plan != null);
        return plan;
    }

    private static GameData data(int accountType,
            MembershipStatus membership, List<ItemState> inventory,
            Map<String, QuestStatus> quests, MinigameSnapshot minigames,
            boolean easyKourendDiary)
    {
        return data(accountType, membership, inventory, quests, minigames,
                easyKourendDiary, Collections.emptyList(), null);
    }

    private static GameData data(int accountType,
            MembershipStatus membership, List<ItemState> inventory,
            Map<String, QuestStatus> quests, MinigameSnapshot minigames,
            boolean easyKourendDiary, List<ItemState> equipment,
            CombatEvidenceSnapshot combatEvidence)
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
        return GameData.builder(account)
                .inventory(new ItemsState(inventory))
                .equipment(new ItemsState(equipment))
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .quests(new QuestSnapshot(quests))
                .diaries(new DiarySnapshot(Collections.emptyMap(),
                        Collections.emptyMap(), tiers))
                .minigames(minigames)
                .transport(new TransportSnapshot(
                        Collections.singleton("fairy-rings")))
                .farming(new FarmingSnapshot(
                        Collections.singleton("falador"), tools,
                        Collections.emptyMap()))
                .combatEvidence(combatEvidence)
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

    private static List<ItemState> items(ItemState... values)
    {
        List<ItemState> items = new ArrayList<>();
        Collections.addAll(items, values);
        return items;
    }

    private static ItemState item(int id, String name, int quantity)
    {
        return new ItemState(id, name, quantity);
    }
}

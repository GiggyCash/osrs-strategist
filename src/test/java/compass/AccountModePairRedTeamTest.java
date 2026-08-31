package compass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** One-variable account-mode comparisons over real strategy seams. */
public class AccountModePairRedTeamTest
{
    private static final List<Skill> HIGH_IMPACT_SKILLS = Arrays.asList(
            Skill.SMITHING, Skill.CRAFTING, Skill.HERBLORE,
            Skill.CONSTRUCTION, Skill.RUNECRAFT, Skill.PRAYER,
            Skill.FISHING, Skill.SLAYER);
    private final TrainingMethodSelector selector = new TrainingMethodSelector(
            new TrainingMethodDatabase(),
            new RequirementEvidenceEngine(
                    new FarmingAccessEvaluator(new FarmingAccessCatalog()),
                    new AgilityAccessEvaluator(new AgilityCourseCatalog())),
            new ExpandedTrainingMethodCatalog(),
            new F2pBaselineMethodCatalog(), new TrainingMethodPolicy());

    @Test
    public void mainIronAndUimGeneratePracticalSkillFamilies()
    {
        for (AccountMode mode : Arrays.asList(AccountMode.MAIN,
                AccountMode.IRONMAN, AccountMode.ULTIMATE_IRONMAN))
        {
            GameData data = data(mode, MembershipStatus.P2P,
                    carriedSetup(), null);
            for (Skill skill : HIGH_IMPACT_SKILLS)
            {
                List<TrainingPlan> plans = selector.rankedCandidates(data,
                        skill, 50, StrategyMode.BALANCED,
                        SessionIntent.ONE_HOUR, false, false);
                assertFalse(mode + " has no " + skill.getName()
                        + " strategy", plans.isEmpty());
                if (mode == AccountMode.ULTIMATE_IRONMAN)
                    for (TrainingPlan plan : plans)
                        assertNotEquals(skill + " leaked a bank loop",
                                MethodBankingBehavior.CONVENTIONAL_BANK_LOOP,
                                plan.getStrategyProfile().getBankingBehavior());
            }
        }
    }

    @Test
    public void mainVersusUimChangesSmithingAndRunecraftGeneration()
    {
        GameData main = data(AccountMode.MAIN,
                MembershipStatus.F2P, carriedSetup(), null);
        GameData uim = data(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.F2P, carriedSetup(), null);

        TrainingPlan mainSmithing = winner(main, Skill.SMITHING, 1);
        TrainingPlan uimSmithing = winner(uim, Skill.SMITHING, 1);
        assertEquals("smithing_f2p_uim_bronze",
                uimSmithing.getMethod().getId());
        assertNotEquals(mainSmithing.getMethod().getId(),
                uimSmithing.getMethod().getId());

        TrainingPlan mainRunecraft = winner(main, Skill.RUNECRAFT, 1);
        TrainingPlan uimRunecraft = winner(uim, Skill.RUNECRAFT, 1);
        assertEquals("runecraft_f2p_uim_local",
                uimRunecraft.getMethod().getId());
        assertNotEquals(mainRunecraft.getMethod().getId(),
                uimRunecraft.getMethod().getId());
    }

    @Test
    public void sharedFishingStaysSharedWhenThePracticalRouteIsShared()
    {
        TrainingMethod fly = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.FISHING).stream()
                .filter(value -> "fishing_f2p_fly".equals(
                        value.getMethod().getId()))
                .map(CuratedTrainingMethod::getMethod)
                .findFirst().orElseThrow(AssertionError::new);
        TrainingMethodMetadata metadata = new ExpandedTrainingMethodCatalog()
                .methodsFor(Skill.FISHING).stream()
                .filter(value -> "fishing_f2p_fly".equals(
                        value.getMethod().getId()))
                .map(CuratedTrainingMethod::getMetadata)
                .findFirst().orElseThrow(AssertionError::new);
        MethodStrategyKnowledgeCatalog catalog =
                new MethodStrategyKnowledgeCatalog();

        assertNotNull(catalog.profileFor(fly, metadata, AccountMode.MAIN));
        assertNotNull(catalog.profileFor(fly, metadata, AccountMode.IRONMAN));
        assertNotNull(catalog.profileFor(fly, metadata,
                AccountMode.ULTIMATE_IRONMAN));
    }

    @Test
    public void sharedActivitiesRemainAvailableBesideAccountSpecificVariants()
    {
        ExpandedTrainingMethodCatalog methods =
                new ExpandedTrainingMethodCatalog();
        MethodStrategyKnowledgeCatalog knowledge =
                new MethodStrategyKnowledgeCatalog();
        for (String id : Arrays.asList("smithing_giants_foundry",
                "construction_mahogany_homes",
                "prayer_bonecrusher_passive"))
        {
            CuratedTrainingMethod method = Arrays.stream(Skill.values())
                    .flatMap(skill -> methods.methodsFor(skill).stream())
                    .filter(value -> id.equals(value.getMethod().getId()))
                    .findFirst().orElseThrow(AssertionError::new);
            MethodStrategyProfile main = knowledge.profileFor(
                    method.getMethod(), method.getMetadata(), AccountMode.MAIN);
            MethodStrategyProfile iron = knowledge.profileFor(
                    method.getMethod(), method.getMetadata(),
                    AccountMode.IRONMAN);
            MethodStrategyProfile uim = knowledge.profileFor(
                    method.getMethod(), method.getMetadata(),
                    AccountMode.ULTIMATE_IRONMAN);
            assertNotNull(id + " Main", main);
            assertNotNull(id + " Iron", iron);
            assertNotNull(id + " UIM", uim);
            assertEquals(id + " Main tier", StrategyKnowledgeTier.VERIFIED_SHARED,
                    main.getTier());
            assertEquals(id + " UIM tier",
                    StrategyKnowledgeTier.VERIFIED_ACCOUNT_SPECIFIC,
                    uim.getTier());
        }
    }

    @Test
    public void mainIronGimAndUimResourceRoutesUseModeEvidence()
    {
        ResourceNeed need = new ResourceNeed(5000, "Steel bar", 10);
        ResourceAcquisitionPlanner planner = new ResourceAcquisitionPlanner();

        assertEquals(AcquisitionSource.GRAND_EXCHANGE,
                planner.plan(context(data(AccountMode.MAIN,
                        MembershipStatus.P2P, carriedSetup(), null), false),
                        need).getSource());
        assertEquals(AcquisitionSource.SELF_SOURCE,
                planner.plan(context(data(AccountMode.IRONMAN,
                        MembershipStatus.P2P, carriedSetup(), null), false),
                        need).getSource());

        ItemsState group = new ItemsState(true,
                Collections.singletonList(new ItemState(
                        5000, "Steel bar", 10)));
        for (AccountMode groupMode : Arrays.asList(AccountMode.GROUP_IRONMAN,
                AccountMode.UNRANKED_GROUP_IRONMAN))
            assertEquals(groupMode.name(), AcquisitionSource.GROUP_STORAGE,
                    planner.plan(context(data(groupMode,
                            MembershipStatus.P2P, carriedSetup(), group), true),
                            need).getSource());

        GameData uimBase = data(
                AccountMode.ULTIMATE_IRONMAN, MembershipStatus.P2P,
                carriedSetup(), null);
        GameData uimWithIllegalBank = GameData.builder(
                uimBase.account())
                .inventory(uimBase.inventory())
                .equipment(uimBase.equipment())
                .quests(uimBase.quests())
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(5000, "Steel bar", 10)), 1L))
                .build();
        assertEquals(AcquisitionSource.SELF_SOURCE,
                planner.plan(context(uimWithIllegalBank, false), need)
                        .getSource());
    }

    @Test
    public void ironVersusHardcoreRiskChangesEligibilityBeforeSelection()
    {
        TrainingMethod wilderness = new TrainingMethod("risky", Skill.PRAYER,
                1, 99, "Risky bones", "Use a Wilderness route.",
                1, 1, 1, AttentionLevel.MODERATE, 10, 2,
                Collections.emptyList(), Confidence.VERIFIED,
                false, true, false);
        TrainingMethodMetadata metadata = new TrainingMethodMetadata(
                TrainingIntensity.EFFICIENT, MethodCostTier.FREE,
                RiskLevel.HIGH, true, true, true, false,
                Collections.emptyList());
        TrainingMethodPolicy policy = new TrainingMethodPolicy();

        assertTrue(policy.isAllowed(data(AccountMode.IRONMAN,
                MembershipStatus.P2P, carriedSetup(), null), wilderness,
                metadata, true));
        assertFalse(policy.isAllowed(data(AccountMode.HARDCORE_IRONMAN,
                MembershipStatus.P2P, carriedSetup(), null), wilderness,
                metadata, true));
        assertFalse(policy.isAllowed(data(
                AccountMode.HARDCORE_GROUP_IRONMAN,
                MembershipStatus.P2P, carriedSetup(), null), wilderness,
                metadata, true));
    }

    @Test
    public void fullInventoryChangesUimQuestGearPvmAndInfrastructureOnlyWhenNeeded()
    {
        ActivityStrategyKnowledgeService knowledge =
                new ActivityStrategyKnowledgeService();
        StrategyContext uim = context(data(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P, fullInventory(), null), false);
        StrategyContext main = context(data(AccountMode.MAIN,
                MembershipStatus.P2P, fullInventory(), null), false);

        for (String id : Arrays.asList("quest:waterfall-quest", "clue:step",
                "minigame:tempoross", "upgrade:fighter-torso", "pvm:tztok_jad",
                "prepare:infrastructure:poh-costume-room"))
        {
            assertNotNull(id, knowledge.attach(candidate(id), main));
            assertEquals(id, null, knowledge.attach(candidate(id), uim));
        }
        assertNotNull(knowledge.attach(candidate("slayer:do-task"), uim));
        assertNotNull(knowledge.attach(candidate("pvm:the_gauntlet"), uim));
    }

    @Test
    public void fullInventoryRejectsEverySkillingFootprintThatDoesNotFit()
    {
        GameData normal = data(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P, carriedSetup(), null);
        GameData full = data(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P, fullInventory(), null);
        for (Skill skill : Arrays.asList(Skill.SMITHING, Skill.CRAFTING,
                Skill.HERBLORE, Skill.CONSTRUCTION, Skill.RUNECRAFT,
                Skill.FARMING, Skill.PRAYER, Skill.FISHING))
        {
            assertFalse(skill.getName() + " lacks a normal UIM strategy",
                    selector.rankedCandidates(normal, skill, 50,
                            StrategyMode.BALANCED, SessionIntent.ONE_HOUR,
                            false, false).isEmpty());
            for (TrainingPlan plan : selector.rankedCandidates(full, skill, 50,
                    StrategyMode.BALANCED, SessionIntent.ONE_HOUR,
                    false, false))
            {
                assertEquals(skill.getName() + " plan does not fit zero free slots",
                        0, plan.getStrategyProfile().getInventoryFootprint()
                                .getMinimumPracticalFreeSlots());
                assertNotEquals(MethodBankingBehavior.CONVENTIONAL_BANK_LOOP,
                        plan.getStrategyProfile().getBankingBehavior());
            }
        }
    }

    private TrainingPlan winner(GameData data, Skill skill, int level)
    {
        TrainingPlan plan = selector.select(data, skill, level,
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR, false, false);
        assertNotNull(skill.getName(), plan);
        return plan;
    }

    private static Recommendation candidate(String id)
    {
        return new Recommendation(id, "Candidate", "Reason", 10.0, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance("Do it.", "Observed setup",
                        "Verified location", "Note"),
                SafetyEvidence.harmless(false));
    }

    private static StrategyContext context(GameData data,
            boolean groupStorage)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, groupStorage, false, false,
                new PreferenceProfile());
    }

    private static GameData data(AccountMode mode,
            MembershipStatus membership, List<ItemState> inventory,
            ItemsState groupStorage)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 50 : 50;
            levels.put(skill, level);
            int value = Experience.getXpForLevel(level);
            xp.put(skill, value);
            total += level;
            totalXp += value;
        }
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Temple of the Eye", QuestStatus.COMPLETE);
        quests.put("The Knight's Sword", QuestStatus.COMPLETE);
        AccountSnapshot account = new AccountSnapshot(mode.name(),
                1000L + typeCode(mode), typeCode(mode), mode.name(),
                membership, membership == MembershipStatus.P2P ? 1 : 0,
                total, totalXp, levels, xp);
        GameData.Builder builder = GameData.builder(account)
                .inventory(new ItemsState(inventory, true))
                .equipment(new ItemsState(Collections.emptyList()))
                .quests(new QuestSnapshot(quests));
        if (mode != AccountMode.ULTIMATE_IRONMAN)
            builder.bank(new ItemsState(Collections.emptyList(), 1L));
        if (groupStorage != null) builder.groupStorage(groupStorage);
        return builder.build();
    }

    private static List<ItemState> carriedSetup()
    {
        return Arrays.asList(
                item(ItemID.BRONZE_PICKAXE, "Bronze pickaxe", 1, 0),
                item(ItemID.HAMMER, "Hammer", 1, 1),
                item(ItemID.CHISEL, "Chisel", 1, 2),
                item(ItemID.FLY_FISHING_ROD, "Fly fishing rod", 1, 3),
                item(ItemID.FEATHER, "Feather", 1000, 4),
                item(ItemID.COINS, "Coins", 100_000, 5));
    }

    private static List<ItemState> fullInventory()
    {
        List<ItemState> values = new ArrayList<>();
        for (int slot = 0; slot < 28; slot++)
            values.add(item(20_000 + slot, "Persistent item " + slot,
                    1, slot));
        return values;
    }

    private static ItemState item(int id, String name, int quantity,
            int slot)
    {
        return new ItemState(id, name, quantity, slot);
    }

    private static int typeCode(AccountMode mode)
    {
        switch (mode)
        {
            case MAIN: return 0;
            case IRONMAN: return 1;
            case ULTIMATE_IRONMAN: return 2;
            case HARDCORE_IRONMAN: return 3;
            case GROUP_IRONMAN: return 4;
            case HARDCORE_GROUP_IRONMAN: return 5;
            case UNRANKED_GROUP_IRONMAN: return 6;
            default: return -1;
        }
    }
}

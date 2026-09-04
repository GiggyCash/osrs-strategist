package compass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Adversarial matrix through the actual multi-domain provider registry. */
public class RealProviderStrategyTournamentTest
{
    private static final GoalType[] PUBLIC_GOALS = {
            GoalType.AUTOMATIC, GoalType.BARROWS_GLOVES,
            GoalType.FIRE_CAPE, GoalType.QUEST_CAPE, GoalType.PRIFDDINAS,
            GoalType.BOWFA, GoalType.INFERNAL_CAPE, GoalType.MAX
    };
    private static final Scenario[] ACCOUNTS = {
            new Scenario("F2P Main", Membership.F2P, 0),
            new Scenario("P2P Main", Membership.P2P, 0),
            new Scenario("Iron", Membership.P2P, 1),
            new Scenario("UIM", Membership.P2P, 2),
            new Scenario("HCIM", Membership.P2P, 3),
            new Scenario("GIM", Membership.P2P, 4),
            new Scenario("HCGIM", Membership.P2P, 5),
            new Scenario("Unranked GIM", Membership.P2P, 6),
            new Scenario("Unknown membership", Membership.UNKNOWN, 0)
    };

    @Test
    public void actualProvidersAlwaysReturnOneSafeSpecificLead()
    {
        StrategyEngine engine = engine();
        ActionabilityPolicy actionability =
                new ActionabilityPolicy();
        CandidateSafetyPolicy safety = new CandidateSafetyPolicy();
        Set<String> winningDomains = new HashSet<>();
        int scenarios = 0;
        for (Scenario account : ACCOUNTS)
        {
            for (int level : new int[]{5, 50, 85})
            {
                GameData data = data(account, level);
                // Goal breadth is the expensive dimension because each public
                // dependency graph is resolved through every real provider.
                // QUICK and AFK are opposing session properties; the focused
                // selector tournament covers all three strategy modes.
                for (StrategyMode mode : Collections.singletonList(
                        StrategyMode.BALANCED))
                {
                    for (SessionIntent session : Arrays.asList(
                            SessionIntent.QUICK_20_MIN,
                            SessionIntent.AFK))
                    {
                        for (GoalType goal : PUBLIC_GOALS)
                        {
                            StrategyResult result = engine.evaluate(data, mode,
                                    session, QuestTolerance.NORMAL, goal,
                                    true, false, false,
                                    new PreferenceProfile());
                            String label = account.name + " level=" + level
                                    + " mode=" + mode + " session=" + session
                                    + " goal=" + goal;
                            assertFalse(label, result.getRecommendations().isEmpty());
                            Recommendation lead = result.getRecommendations().get(0);
                            assertTrue(label + " => " + lead.getId(),
                                    actionability.canLeadQueue(lead));
                            StrategyContext context = new StrategyContext(data,
                                    mode, session, QuestTolerance.NORMAL, goal,
                                    true, false, false,
                                    new PreferenceProfile());
                            assertTrue(label + " unsafe => " + lead.getId(),
                                    safety.isAllowed(lead, context));
                            assertSpecific(label, lead);
                            winningDomains.add(
                                    StrategyEngine.alternativeDimension(lead));
                            if (account.membership != Membership.P2P)
                                assertTrue(label + " leaked members content",
                                        lead.getSafetyEvidence().getAccess()
                                                == Safety.Access.F2P_SAFE);
                            scenarios++;
                        }
                    }
                }
            }
        }
        assertTrue("Matrix must remain broad", scenarios >= 400);
        assertTrue("Real providers must compete across several domains: "
                        + winningDomains,
                winningDomains.size() >= 3);
    }

    private static void assertSpecific(String label, Recommendation lead)
    {
        Guidance guidance = lead.getGuidance();
        assertTrue(label + " missing guidance", guidance != null);
        assertFalse(label + " missing action",
                guidance.getAction() == null || guidance.getAction().trim().isEmpty());
        assertFalse(label + " missing location",
                guidance.getLocation() == null || guidance.getLocation().trim().isEmpty());
        String visible = Presentation.compactText(lead)
                .toLowerCase(Locale.ROOT);
        for (String slop : Arrays.asList(
                "strategist will verify", "choose the best",
                "best available", "use a nearby", "a training area",
                "get supplies", "whatever", "use any", "choose a suitable",
                "use a reachable", "an appropriate", "as needed",
                "use an altar", "use an anvil", "use a furnace",
                "current level band", "strategic value", "candidate",
                "evidence score", "typed requirement", "ranking",
                "provenance", "weighting", "resolver"))
            assertFalse(label + " contains '" + slop + "': " + visible,
                    visible.contains(slop));
        assertTrue(label + " lacks WHERE: " + visible,
                visible.contains("where"));
        assertTrue(label + " lacks DO: " + visible,
                visible.contains("do"));
    }

    @Test
    public void allStrategyAndSessionPropertiesReachActualProviderQueue()
    {
        StrategyEngine engine = engine();
        ActionabilityPolicy actionability =
                new ActionabilityPolicy();
        CandidateSafetyPolicy safety = new CandidateSafetyPolicy();
        int scenarios = 0;
        for (Scenario account : ACCOUNTS)
        {
            for (int level : new int[]{5, 50, 85})
            {
                GameData data = data(account, level);
                for (StrategyMode mode : StrategyMode.values())
                {
                    for (SessionIntent session : SessionIntent.values())
                    {
                        StrategyResult result = engine.evaluate(data, mode,
                                session, QuestTolerance.NORMAL,
                                GoalType.AUTOMATIC, true, false, false,
                                new PreferenceProfile());
                        String label = account.name + " level=" + level
                                + " mode=" + mode + " session=" + session;
                        assertFalse(label, result.getRecommendations().isEmpty());
                        Recommendation lead = result.getRecommendations().get(0);
                        assertTrue(label, actionability.canLeadQueue(lead));
                        assertTrue(label, safety.isAllowed(lead,
                                new StrategyContext(data, mode, session,
                                        QuestTolerance.NORMAL,
                                        GoalType.AUTOMATIC, true, false,
                                        false, new PreferenceProfile())));
                        assertSpecific(label, lead);
                        scenarios++;
                    }
                }
            }
        }
        assertTrue("Property matrix must remain broad", scenarios >= 300);
    }

    private static StrategyEngine engine()
    {
        TrainingMethodSelector selector = new TrainingMethodSelector(
                new TrainingMethodCatalog(),
                new RequirementEvidenceEngine(new FarmingAccessEvaluator(new FarmingAccessCatalog()), new AgilityAccessEvaluator(new AgilityCourseCatalog()), new FarmingSupplyCatalog(), new RunecraftSupplyCatalog()),
                new TrainingMethodPolicy(), new MethodStrategyKnowledgeCatalog(),
                new UimInventoryResolutionService());
        StrategyCandidateRegistry registry = new StrategyCandidateRegistry(
                Arrays.asList(
                        new ClueCandidateProvider(),
                        new PvmCandidateProvider(),
                        TestFixtures.questCandidateProvider(new QuestPriorityCatalog()),
                        new DiaryCandidateProvider(),
                        new CombatAchievementCandidateProvider(),
                        new InfrastructureCandidateProvider(
                                new InfrastructureMilestoneCatalog(),
                                new InfrastructureUnlockValueService()),
                        new ProgressionUpgradeCandidateProvider(),
                        new ResourceDetourCandidateProvider(),
                        new SlayerCandidateProvider(),
                        new GearCandidateProvider(new GearProgressionCatalog()),
                        new MoneyMakingCandidateProvider(new MoneyMakingCatalog()),
                        new MinigameCandidateProvider(new MinigameCatalog()),
                        new CollectionLogCandidateProvider()));
        return TestFixtures.strategyEngine(TestFixtures.recommendationEngine(selector), null,
                null, registry, new ActionabilityPolicy(),
                new RecommendationIntelligenceService(),
                new CandidateSafetyPolicy(),
                new GoalDependencyProvenanceService());
    }

    private static GameData data(Scenario scenario, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int value = skill == Skill.HITPOINTS ? Math.max(10, level) : level;
            levels.put(skill, value);
            int valueXp = value <= 1 ? 0 : Experience.getXpForLevel(value);
            xp.put(skill, valueXp);
            total += value;
            totalXp += valueXp;
        }
        AccountSnapshot account = new AccountSnapshot(scenario.name,
                50_000L + scenario.type, scenario.type,
                AccountMode.fromTypeCode(scenario.type).name(),
                scenario.membership,
                scenario.membership == Membership.P2P ? 1 : 0,
                total, totalXp, levels, xp);

        Map<String, QuestStatus> questStates = new HashMap<>();
        questStates.put("Cook's Assistant", QuestStatus.NOT_STARTED);
        questStates.put("The Restless Ghost", QuestStatus.NOT_STARTED);
        if (scenario.membership == Membership.P2P)
        {
            questStates.put("Waterfall Quest", QuestStatus.NOT_STARTED);
            questStates.put("Tree Gnome Village", QuestStatus.NOT_STARTED);
            questStates.put("Monkey Madness I", QuestStatus.NOT_STARTED);
        }

        GameData.Builder builder = GameData.builder(account)
                .inventory(new ItemsState(preparedItems()))
                .equipment(new ItemsState(Collections.emptyList()))
                .quests(new QuestSnapshot(questStates));
        if (scenario.type != 2)
            builder.bank(new ItemsState(Collections.emptyList(), 1L));
        if (scenario.membership == Membership.P2P)
        {
            builder.slayer(TestFixtures.slayerSnapshot(null, 0, null, 0,
                    Confidence.VERIFIED));
            builder.poh(observedEmptyPoh());
        }
        if (AccountMode.fromTypeCode(scenario.type).isGroupIronman())
            builder.groupStorage(new ItemsState(true,
                    Collections.emptyList()));
        return builder.build();
    }

    private static PohSnapshot observedEmptyPoh()
    {
        Map<String, Capability> furniture = new HashMap<>();
        for (InfrastructureMilestone definition
                : new InfrastructureMilestoneCatalog().all())
            if (definition.getEvidenceKind()
                    == InfrastructureEvidenceKind.POH_FURNITURE)
                furniture.put(definition.getEvidenceKey(),
                        Capability.BLOCKED);
        return new PohSnapshot(Capability.VERIFIED, furniture);
    }

    private static List<ItemState> preparedItems()
    {
        List<ItemState> items = new ArrayList<>();
        items.add(new ItemState(ItemID.BRONZE_PICKAXE,
                "Bronze pickaxe", 1));
        items.add(new ItemState(ItemID.BRONZE_AXE,
                "Bronze axe", 1));
        items.add(new ItemState(ItemID.TINDERBOX,
                "Tinderbox", 1));
        items.add(new ItemState(ItemID.NET,
                "Small fishing net", 1));
        items.add(new ItemState(ItemID.FLY_FISHING_ROD,
                "Fly fishing rod", 1));
        items.add(new ItemState(ItemID.FEATHER, "Feather", 5_000));
        items.add(new ItemState(ItemID.HAMMER, "Hammer", 1));
        items.add(new ItemState(ItemID.KNIFE, "Knife", 1));
        return items;
    }

    private static final class Scenario
    {
        private final String name;
        private final Membership membership;
        private final int type;

        private Scenario(String name, Membership membership, int type)
        {
            this.name = name;
            this.membership = membership;
            this.type = type;
        }
    }
}

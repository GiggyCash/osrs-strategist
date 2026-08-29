package com.udderlywet.osrsstrategist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

public class InfrastructureUnlockValueServiceTest
{
    private final InfrastructureMilestoneCatalog catalog =
            new InfrastructureMilestoneCatalog();
    private final AccountStrategicPriorityService priorities =
            new AccountStrategicPriorityService();
    private final InfrastructureUnlockValueService service =
            new InfrastructureUnlockValueService(catalog, priorities);

    @Test
    public void catalogContainsOnlyAuditedConcreteMilestones()
    {
        assertEquals(12, catalog.all().size());
        assertEquals(42,
                catalog.get("poh-costume-room").getRequiredLevel());
        assertEquals(46,
                catalog.get("poh-armour-case").getRequiredLevel());
        assertEquals(50,
                catalog.get("poh-portal-chamber").getRequiredLevel());
        assertTrue(catalog.get("poh-costume-room").getAction()
                .contains("50,000"));
        assertTrue(catalog.get("poh-portal-chamber").getAction()
                .contains("100,000"));
        assertTrue(InfrastructureMilestoneCatalog.PROVENANCE_URLS.stream()
                .allMatch(url -> url.startsWith(
                        "https://oldschool.runescape.wiki/w/")));
    }

    @Test
    public void sameCostumeRoomPropertiesAreMoreValuableForUimThanMain()
    {
        StrategyDataBundle mainData = costumeData(AccountMode.MAIN,
                MembershipStatus.P2P, CapabilityState.BLOCKED);
        StrategyDataBundle uimData = costumeData(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P, CapabilityState.BLOCKED);

        InfrastructureValueAssessment main = service.assess(
                catalog.get("poh-costume-room"),
                priorities.assess(AccountMode.MAIN, mainData, false), mainData);
        InfrastructureValueAssessment uim = service.assess(
                catalog.get("poh-costume-room"),
                priorities.assess(AccountMode.ULTIMATE_IRONMAN, uimData, false),
                uimData);

        assertEquals(InfrastructureMilestoneState.ACTIONABLE, main.getState());
        assertEquals(InfrastructureMilestoneState.ACTIONABLE, uim.getState());
        assertEquals(StrategicPriority.MODERATE, main.getStrategicValue());
        assertEquals(StrategicPriority.HIGH, uim.getStrategicValue());
        assertTrue(uim.canRecommendAcquisition());
        assertTrue(uim.getContributions().stream().anyMatch(value ->
                value.getDimension()
                        == AccountStrategicDimension.POH_VALUE
                        && value.getEffectivePriority()
                        == StrategicPriority.HIGH));
    }

    @Test
    public void unknownCompletionCannotMasqueradeAsActionable()
    {
        StrategyDataBundle data = costumeData(AccountMode.ULTIMATE_IRONMAN,
                MembershipStatus.P2P, CapabilityState.UNKNOWN);
        InfrastructureValueAssessment assessment = service.assess(
                catalog.get("poh-costume-room"), priorities.assess(
                        AccountMode.ULTIMATE_IRONMAN, data, false), data);

        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                assessment.getState());
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                assessment.getConfidence());
        assertFalse(assessment.canRecommendAcquisition());
    }

    @Test
    public void f2pCannotReceiveMembersInfrastructureAction()
    {
        StrategyDataBundle data = costumeData(AccountMode.MAIN,
                MembershipStatus.F2P, CapabilityState.BLOCKED);
        InfrastructureValueAssessment assessment = service.assess(
                catalog.get("poh-costume-room"),
                priorities.assess(AccountMode.MAIN, data, false), data);

        assertEquals(InfrastructureMilestoneState.NOT_APPLICABLE,
                assessment.getState());
        assertEquals(RecommendationConfidence.BLOCKED,
                assessment.getConfidence());
        assertFalse(assessment.canRecommendAcquisition());
    }

    @Test
    public void questTransportRequiresProvenQuestAndRouteEvidence()
    {
        StrategyDataBundle missing = transportData(QuestStatus.NOT_STARTED,
                Collections.emptySet());
        InfrastructureValueAssessment missingAssessment = service.assess(
                catalog.get("fairy-ring-network"),
                priorities.assess(AccountMode.IRONMAN, missing, false), missing);
        assertEquals(InfrastructureMilestoneState.REQUIREMENTS_MISSING,
                missingAssessment.getState());

        StrategyDataBundle approaching = transportData(QuestStatus.IN_PROGRESS,
                Collections.emptySet());
        InfrastructureValueAssessment approachingAssessment = service.assess(
                catalog.get("fairy-ring-network"), priorities.assess(
                        AccountMode.IRONMAN, approaching, false), approaching);
        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                approachingAssessment.getState());

        StrategyDataBundle observed = transportData(QuestStatus.IN_PROGRESS,
                Collections.singleton("fairy-rings"));
        InfrastructureValueAssessment complete = service.assess(
                catalog.get("fairy-ring-network"),
                priorities.assess(AccountMode.IRONMAN, observed, false), observed);
        assertEquals(InfrastructureMilestoneState.COMPLETE,
                complete.getState());
        assertEquals(RecommendationConfidence.VERIFIED,
                complete.getConfidence());
    }

    @Test
    public void roomLevelDoesNotProveRoomExists()
    {
        StrategyDataBundle data = portalData();
        InfrastructureValueAssessment assessment = service.assess(
                catalog.get("poh-portal-chamber"),
                priorities.assess(AccountMode.IRONMAN, data, false), data);

        assertEquals(70,
                data.getAccount().getSkillLevel(Skill.CONSTRUCTION));
        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                assessment.getState());
        assertFalse(assessment.canRecommendAcquisition());
    }

    private static StrategyDataBundle costumeData(AccountMode mode,
            MembershipStatus membership, CapabilityState costume)
    {
        Map<String, CapabilityState> furniture = new HashMap<>();
        furniture.put(LivePohStateReader.COSTUME_ROOM, costume);
        return StrategyDataBundle.builder(account(mode, membership, 70))
                .poh(new PohSnapshot(CapabilityState.VERIFIED,
                        furniture))
                .build();
    }

    private static StrategyDataBundle portalData()
    {
        return StrategyDataBundle.builder(account(AccountMode.IRONMAN,
                        MembershipStatus.P2P, 70))
                .poh(new PohSnapshot(CapabilityState.VERIFIED,
                        Collections.emptyMap()))
                .transport(new TransportSnapshot(Collections.emptySet()))
                .build();
    }

    private static StrategyDataBundle transportData(QuestStatus quest,
            java.util.Set<String> routes)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Fairytale II - Cure a Queen", quest);
        return StrategyDataBundle.builder(account(AccountMode.IRONMAN,
                        MembershipStatus.P2P, 70))
                .quests(new QuestSnapshot(quests))
                .transport(new TransportSnapshot(new HashSet<>(routes)))
                .build();
    }

    private static AccountSnapshot account(AccountMode mode,
            MembershipStatus membership, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Infrastructure", 2L, mode.ordinal(),
                mode.name(), membership, membership == MembershipStatus.P2P
                        ? 1 : 0, level * Skill.values().length, 0L, levels, xp);
    }
}

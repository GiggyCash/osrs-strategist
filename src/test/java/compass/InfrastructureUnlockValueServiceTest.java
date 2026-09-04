package compass;

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
    private final InfrastructureUnlockValueService service =
            new InfrastructureUnlockValueService(catalog);

    @Test
    public void catalogContainsOnlyAuditedConcreteMilestones()
    {
        assertEquals(12, catalog.all().size());
        assertEquals(42, constructionLevel("poh-costume-room"));
        assertEquals(46, constructionLevel("poh-armour-case"));
        assertEquals(50, constructionLevel("poh-portal-chamber"));
        assertTrue(catalog.get("poh-costume-room").getAction()
                .contains("50,000"));
        assertTrue(catalog.get("poh-portal-chamber").getAction()
                .contains("100,000"));
        assertTrue(InfrastructureMilestoneCatalog.PROVENANCE_URLS.stream()
                .allMatch(url -> url.startsWith(
                        "https://oldschool.runescape.wiki/w/")));
    }

    private int constructionLevel(String id)
    {
        return catalog.get(id).getRequiredSkills()
                .get(net.runelite.api.Skill.CONSTRUCTION);
    }

    @Test
    public void sameCostumeRoomPropertiesAreMoreValuableForUimThanMain()
    {
        GameData mainData = costumeData(AccountMode.MAIN,
                Membership.P2P, Capability.BLOCKED);
        GameData uimData = costumeData(AccountMode.ULTIMATE_IRONMAN,
                Membership.P2P, Capability.BLOCKED);

        InfraAssessment main = service.assess("poh-costume-room",
                context(mainData));
        InfraAssessment uim = service.assess("poh-costume-room",
                context(uimData));

        assertEquals(InfrastructureMilestoneState.ACTIONABLE, main.getState());
        assertEquals(InfrastructureMilestoneState.ACTIONABLE, uim.getState());
        assertEquals(Priority.MODERATE, main.getStrategicValue());
        assertEquals(Priority.HIGH, uim.getStrategicValue());
        assertTrue(uim.canRecommendAcquisition());
    }

    @Test
    public void unknownCompletionCannotMasqueradeAsActionable()
    {
        GameData data = costumeData(AccountMode.ULTIMATE_IRONMAN,
                Membership.P2P, Capability.UNKNOWN);
        InfraAssessment assessment = service.assess("poh-costume-room",
                context(data));

        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                assessment.getState());
        assertEquals(Confidence.CHECK_NEEDED,
                assessment.getConfidence());
        assertFalse(assessment.canRecommendAcquisition());
    }

    @Test
    public void f2pCannotReceiveMembersInfrastructureAction()
    {
        GameData data = costumeData(AccountMode.MAIN,
                Membership.F2P, Capability.BLOCKED);
        InfraAssessment assessment = service.assess("poh-costume-room",
                context(data));

        assertEquals(InfrastructureMilestoneState.NOT_APPLICABLE,
                assessment.getState());
        assertEquals(Confidence.BLOCKED,
                assessment.getConfidence());
        assertFalse(assessment.canRecommendAcquisition());
    }

    @Test
    public void questTransportRequiresProvenQuestAndRouteEvidence()
    {
        GameData missing = transportData(QuestStatus.NOT_STARTED,
                Collections.emptySet());
        InfraAssessment missingAssessment = service.assess(
                "fairy-ring-network", context(missing));
        assertEquals(InfrastructureMilestoneState.REQUIREMENTS_MISSING,
                missingAssessment.getState());

        GameData approaching = transportData(QuestStatus.IN_PROGRESS,
                Collections.emptySet());
        InfraAssessment approachingAssessment = service.assess(
                "fairy-ring-network", context(approaching));
        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                approachingAssessment.getState());

        GameData observed = transportData(QuestStatus.IN_PROGRESS,
                Collections.singleton("fairy-rings"));
        InfraAssessment complete = service.assess(
                "fairy-ring-network", context(observed));
        assertEquals(InfrastructureMilestoneState.COMPLETE,
                complete.getState());
        assertEquals(Confidence.VERIFIED,
                complete.getConfidence());
    }

    @Test
    public void roomLevelDoesNotProveRoomExists()
    {
        GameData data = portalData();
        InfraAssessment assessment = service.assess(
                "poh-portal-chamber", context(data));

        assertEquals(70,
                data.account().getSkillLevel(Skill.CONSTRUCTION));
        assertEquals(InfrastructureMilestoneState.CHECK_NEEDED,
                assessment.getState());
        assertFalse(assessment.canRecommendAcquisition());
    }

    private static GameData costumeData(AccountMode mode,
            Membership membership, Capability costume)
    {
        Map<String, Capability> furniture = new HashMap<>();
        furniture.put(LivePohStateReader.COSTUME_ROOM, costume);
        return GameData.builder(account(mode, membership, 70))
                .poh(new PohSnapshot(Capability.VERIFIED,
                        furniture))
                .build();
    }

    private static GameData portalData()
    {
        return GameData.builder(account(AccountMode.IRONMAN,
                        Membership.P2P, 70))
                .poh(new PohSnapshot(Capability.VERIFIED,
                        Collections.emptyMap()))
                .transport(new TransportSnapshot(Collections.emptySet()))
                .build();
    }

    private static GameData transportData(QuestStatus quest,
            java.util.Set<String> routes)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Fairytale II - Cure a Queen", quest);
        return GameData.builder(account(AccountMode.IRONMAN,
                        Membership.P2P, 70))
                .quests(new QuestSnapshot(quests))
                .transport(new TransportSnapshot(new HashSet<>(routes)))
                .build();
    }

    private static AccountSnapshot account(AccountMode mode,
            Membership membership, int level)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        return new AccountSnapshot("Infrastructure", 2L, mode.ordinal(),
                mode.name(), membership, membership == Membership.P2P
                        ? 1 : 0, level * Skill.values().length, 0L, levels, xp);
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false, new PreferenceProfile());
    }
}

package compass;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DoNextInvariantTest
{
    @Test
    public void normalAndEveryNegativeFeedbackActionHaveImmediateDoNext()
    {
        Recommendation first = ready("skill:mining", 100);
        Recommendation second = ready("skill:fishing", 90);
        StrategyEngine engine = engine(first, second);
        GameData data = observedData();

        assertEquals(first.getId(), evaluate(engine, data,
                new PreferenceProfile()).getId());
        for (FeedbackAction action : Arrays.asList(FeedbackAction.LATER,
                FeedbackAction.NOT_TODAY, FeedbackAction.DISLIKE))
        {
            PreferenceProfile preferences = new PreferenceProfile();
            preferences.apply(first.getId(), action);
            assertEquals(second.getId(), evaluate(engine, data, preferences).getId());
        }
    }

    @Test
    public void multipleSuppressedCandidatesStillProduceFallback()
    {
        Recommendation first = ready("skill:mining", 100);
        Recommendation second = ready("skill:fishing", 90);
        PreferenceProfile preferences = new PreferenceProfile();
        preferences.apply(first.getId(), FeedbackAction.NOT_TODAY);
        preferences.apply(second.getId(), FeedbackAction.LATER);

        Recommendation result = evaluate(engine(first, second), observedData(),
                preferences);
        assertTrue(FallbackRecommendationFactory.isFallback(result));
        assertFalse(Presentation.compactText(result).trim().isEmpty());
    }

    @Test
    public void usefulPreparationLeadsBeforeGenericFallback()
    {
        Recommendation preparation = new Recommendation(
                "prepare:bank", "Verify banked supplies", "Bank state is missing.",
                50, null, Confidence.CHECK_NEEDED, 0, 0,
                new Guidance(
                        "Open your bank and leave it open for one game tick.",
                        "No supplies required.",
                        "Grand Exchange bank booths, northwest of Varrock.",
                        "This records an observed snapshot."),
                Safety.harmless(true));

        assertEquals(preparation.getId(), evaluate(engine(preparation),
                observedData(), new PreferenceProfile()).getId());
    }

    @Test
    public void exhaustedAndUnavailablePoolsProduceHonestFallbacks()
    {
        Recommendation exhausted = evaluate(engine(), observedData(),
                new PreferenceProfile());
        assertEquals("fallback:starter-pickaxe", exhausted.getId());

        StrategyResult unavailable = engine().evaluate(null,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                new PreferenceProfile());
        assertEquals("fallback:login",
                unavailable.getRecommendations().get(0).getId());
    }

    @Test
    public void rejectedPoolUsesConcreteRecoveryWithoutInternalLanguage()
    {
        Recommendation vague = new Recommendation(
                "skill:mining", "Train Mining", "Candidate was filtered.",
                999, null, Confidence.CHECK_NEEDED, 1, 10,
                new Guidance("Choose the best available method.",
                        "Get supplies.", "A nearby mine.",
                        "Quality gate diagnostic."),
                Safety.harmless(true));

        Recommendation result = evaluate(engine(vague), observedData(),
                new PreferenceProfile());
        String playerText = (Presentation.compactText(result)
                + " " + result.getReason() + " "
                + result.getGuidance().getNote()).toLowerCase();
        assertEquals("fallback:starter-pickaxe", result.getId());
        for (String forbidden : Arrays.asList(
                "filtered", "quality gate", "developer", "debug",
                "higher-value route", "passed the current"))
        {
            assertFalse(forbidden, playerText.contains(forbidden));
        }
        assertTrue(playerText.contains("east lumbridge swamp mine"));
    }

    @Test
    public void sidebarNeverRendersBlankWhenGivenEmptyOrNullQueue()
    {
        OsrsStrategistPanel panel = new OsrsStrategistPanel((id, action) -> { }, null);
        panel.updateRecommendations(Collections.emptyList());
        assertTrue(allText(panel).contains("Log in to continue"));
        panel.updateRecommendations(null);
        String text = allText(panel);
        assertTrue(text.contains("Log in to continue"));
        assertFalse(text.trim().isEmpty());
    }

    @Test
    public void largeMixedFamilyPoolSurvivesRepeatedFeedbackUntilFallback()
    {
        String[] ids = {"skill:mining", "quest:holy-grail", "gear:salve",
                "pvm:obor", "resource:molten-glass", "opportunity:herb-run",
                "skill:fishing", "quest:cabin-fever", "gear:ibans-staff",
                "pvm:scurrius", "resource:bow-string", "opportunity:birdhouse"};
        Recommendation[] pool = new Recommendation[ids.length];
        for (int i = 0; i < ids.length; i++) pool[i] = ready(ids[i], 200 - i);
        StrategyEngine engine = engine(pool);
        PreferenceProfile preferences = new PreferenceProfile();
        FeedbackAction[] actions = {FeedbackAction.LATER,
                FeedbackAction.NOT_TODAY, FeedbackAction.DISLIKE};

        for (int i = 0; i < ids.length; i++)
        {
            Recommendation before = evaluate(engine, observedData(), preferences);
            assertFalse(FallbackRecommendationFactory.isFallback(before));
            preferences.apply(before.getId(), actions[i % actions.length]);
            Recommendation after = evaluate(engine, observedData(), preferences);
            assertFalse(Presentation.compactText(after).trim().isEmpty());
            assertFalse(before.getId().equals(after.getId()));
        }
        assertTrue(FallbackRecommendationFactory.isFallback(
                evaluate(engine, observedData(), preferences)));
    }

    @Test
    public void missingStateFallbacksAreSpecificAndUimNeverRequestsBank()
    {
        GameData accountOnly = GameData.builder(
                account(0, Membership.P2P)).build();
        assertEquals("fallback:inventory", evaluate(engine(), accountOnly,
                new PreferenceProfile()).getId());

        GameData inventoryOnly = GameData.builder(
                        account(0, Membership.P2P))
                .inventory(new ItemsState(Collections.emptyList())).build();
        assertEquals("fallback:equipment", evaluate(engine(), inventoryOnly,
                new PreferenceProfile()).getId());

        GameData noBank = GameData.builder(
                        account(0, Membership.P2P))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList())).build();
        assertEquals("fallback:bank", evaluate(engine(), noBank,
                new PreferenceProfile()).getId());

        GameData uim = GameData.builder(
                        account(2, Membership.P2P))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList())).build();
        Recommendation uimFallback = evaluate(engine(), uim,
                new PreferenceProfile());
        assertEquals("fallback:starter-pickaxe", uimFallback.getId());
        assertFalse(Presentation.compactText(uimFallback)
                .toLowerCase().contains("open your bank"));
    }

    @Test
    public void unsafePoolFallsBackForUnknownRestrictedAndHardcoreStates()
    {
        Recommendation membersOnly = new Recommendation("pvm:unsafe", "Unsafe",
                "Unsafe test", 500, null, Confidence.VERIFIED,
                0, 0, new Guidance("Enter the encounter.",
                "Bring gear.", "Members area.", "Unsafe."),
                Safety.potentiallyIrreversible(false));
        for (GameData data : Arrays.asList(
                data(account(0, Membership.UNKNOWN)),
                data(oneDefenceAccount()),
                data(account(3, Membership.P2P))))
        {
            Recommendation result = evaluate(engine(membersOnly), data,
                    new PreferenceProfile());
            assertTrue(FallbackRecommendationFactory.isFallback(result));
            assertFalse(Presentation.compactText(result).trim().isEmpty());
        }
    }

    private static StrategyEngine engine(Recommendation... candidates)
    {
        RecommendationEngine recommendations = new RecommendationEngine((TrainingMethodSelector) null,
                TestFixtures.recommendationGuidanceService(),
                null, null, null, null, null)
        {
            @Override
            public List<Recommendation> recommendAll(GameData data,
                    StrategyMode mode, SessionIntent intent, boolean groupStorage,
                    boolean wilderness, PreferenceProfile preferences)
            {
                List<Recommendation> visible = new ArrayList<>();
                for (Recommendation candidate : candidates)
                    if (!preferences.isOnCooldown(candidate.getId())) visible.add(candidate);
                return visible;
            }
        };
        return TestFixtures.strategyEngine(recommendations, null, null, null,
                new ActionabilityPolicy(),
                new RecommendationIntelligenceService());
    }

    private static Recommendation evaluate(StrategyEngine engine,
            GameData data, PreferenceProfile preferences)
    {
        List<Recommendation> recommendations = engine.evaluate(data,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                preferences).getRecommendations();
        assertFalse(recommendations.isEmpty());
        return recommendations.get(0);
    }

    private static Recommendation ready(String id, double score)
    {
        Skill skill = id.contains("mining") ? Skill.MINING : Skill.FISHING;
        String title = skill == Skill.MINING
                ? "Mine copper to reach 2 Mining"
                : "Catch shrimp to reach 2 Fishing";
        String location = skill == Skill.MINING
                ? "Southeast Varrock mine"
                : "Lumbridge Swamp fishing spots";
        return new Recommendation(id, title, "Safe training.",
                score, null, Confidence.VERIFIED, 1, 2,
                new Guidance(
                        skill == Skill.MINING
                                ? "Mine copper rocks, drop the ore, and repeat."
                                : "Catch shrimp, drop them, and repeat.",
                        skill == Skill.MINING
                                ? "Bronze pickaxe."
                                : "Small fishing net.",
                        location, "This is a legal training action."),
                Safety.skill(true, skill));
    }

    private static GameData observedData()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 20); xp.put(skill, 0); }
        AccountSnapshot account = new AccountSnapshot("Fallback", 0L, 0, "Main", Membership.F2P, 0, 460, 0, levels, xp);
        return GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
    }

    private static GameData data(AccountSnapshot account)
    {
        return GameData.builder(account)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .bank(new ItemsState(Collections.emptyList(), 1L)).build();
    }

    private static AccountSnapshot account(int type, Membership membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 70); xp.put(skill, 0); }
        return new AccountSnapshot("Invariant", 0L, type, AccountMode.fromTypeCode(type).name(), membership, membership == Membership.P2P ? 1 : 0, 1500, 0, levels, xp);
    }

    private static AccountSnapshot oneDefenceAccount()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, 70); xp.put(skill, 0); }
        levels.put(Skill.DEFENCE, 1);
        return new AccountSnapshot("Pure", 0L, 0, "Main", Membership.P2P, 1, 1400, 0, levels, xp);
    }

    private static String allText(Component component)
    {
        StringBuilder text = new StringBuilder();
        if (component instanceof JTextArea)
            text.append(((JTextArea) component).getText()).append('\n');
        if (component instanceof Container)
            for (Component child : ((Container) component).getComponents())
                text.append(allText(child));
        return text.toString();
    }
}

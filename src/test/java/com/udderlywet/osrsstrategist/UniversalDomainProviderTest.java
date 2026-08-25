package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniversalDomainProviderTest
{
    private final UniversalDependencyPlanner planner =
            new UniversalDependencyPlanner();

    @Test
    public void miniquestsAndAbilityPrerequisitesUseTypedDomains()
    {
        UniversalDependencyResolution miniquest = planner.resolveQuest(
                "Barbarian Training", context(0, levels(40), null, null,
                        null, false));
        assertKinds(miniquest, GoalNodeKind.MINIQUEST);

        Map<Skill, Integer> pure = levels(70);
        pure.put(Skill.DEFENCE, 1);
        pure.put(Skill.PRAYER, 60);
        UniversalDependencyResolution piety = planner.resolveAbility("piety",
                context(0, pure, null, null, null, false));
        assertKinds(piety, GoalNodeKind.PRAYER, GoalNodeKind.QUEST,
                GoalNodeKind.SKILL_LEVEL, GoalNodeKind.ACCESS);
        assertTrue(piety.getNodes().stream().anyMatch(node ->
                node.getAction().contains("Do not train protected Defence")));
        assertFalse(piety.getNodes().stream().anyMatch(node ->
                node.getKind() == GoalNodeKind.TRAINING_METHOD
                        && node.getAction().contains("Defence")));

        UniversalDependencyResolution spellbook = planner.resolveAbility(
                "ancient-magicks", context(0, levels(99), null, null,
                        null, false));
        assertKinds(spellbook, GoalNodeKind.SPELLBOOK, GoalNodeKind.QUEST,
                GoalNodeKind.ACCESS);
    }

    @Test
    public void minigameAlternativesRemainAlternativesAndCurrencyUsesObservation()
    {
        Set<String> unlocked = Collections.singleton("tempoross");
        Map<String, Integer> currencies = new HashMap<>();
        currencies.put("reward-permits", 12);
        MinigameSnapshot snapshot = new MinigameSnapshot(unlocked, currencies);
        StrategyContext context = context(0, levels(99), snapshot, null,
                null, false);

        UniversalDependencyResolution setup = planner.resolveMinigame(
                "tempoross", context);
        assertKinds(setup, GoalNodeKind.MINIGAME,
                GoalNodeKind.PREPARATION_ACTION,
                GoalNodeKind.TRANSPORTATION);
        assertFalse("A tool class must not become a fake exact item",
                setup.getNodes().stream().anyMatch(node ->
                        node.getKind() == GoalNodeKind.ITEM));
        assertTrue(setup.getNodes().stream().anyMatch(node ->
                node.getAction().contains("item alternative")));

        UniversalDependencyResolution currency =
                planner.resolveMinigameCurrency("tempoross",
                        "reward-permits", "reward permits", 20, context);
        assertKinds(currency, GoalNodeKind.CURRENCY, GoalNodeKind.MINIGAME);
        assertEquals(8, currency.getNodes().stream().filter(node ->
                node.getKind() == GoalNodeKind.CURRENCY).findFirst()
                .orElseThrow(AssertionError::new).getQuantity());

        UniversalDependencyResolution unknown =
                planner.resolveMinigameCurrency("tempoross", "spirit-flakes",
                        "spirit flakes", 20, context);
        assertTrue(unknown.getNodes().get(0).getAction()
                .contains("Observe the current"));
    }

    @Test
    public void slayerRecurringAndCombatAchievementsFailClosedOnLiveState()
    {
        UniversalDependencyResolution wilderness = planner.resolveSlayerTask(
                "Revenants", context(3, levels(80), null, null, null, false));
        assertKinds(wilderness, GoalNodeKind.SLAYER, GoalNodeKind.ACCESS);
        assertTrue(wilderness.nextAction().getAction().contains("Hardcore"));

        Map<String, Long> cooldowns = new HashMap<>();
        cooldowns.put("birdhouse-run", 1_000L);
        RecurringOpportunitySnapshot recurring =
                new RecurringOpportunitySnapshot(cooldowns);
        StrategyContext readyContext = context(0, levels(80), null,
                recurring, new CombatAchievementSnapshot(10, 20), false);
        UniversalDependencyResolution ready = planner.resolveRecurring(
                "birdhouse-run", "Birdhouse run", 2_000L, readyContext);
        assertKinds(ready, GoalNodeKind.RECURRING_OPPORTUNITY,
                GoalNodeKind.PREPARATION_ACTION);
        assertEquals(RecommendationConfidence.VERIFIED,
                ready.getNodes().get(0).getConfidence());

        UniversalDependencyResolution combat =
                planner.resolveCombatAchievement(CombatAchievementTier.EASY,
                        readyContext);
        assertKinds(combat, GoalNodeKind.COMBAT_ACHIEVEMENT,
                GoalNodeKind.CURRENCY, GoalNodeKind.PREPARATION_ACTION);
        assertTrue(combat.getNodes().stream().anyMatch(node ->
                node.getAction().contains("21 more")));

        UniversalDependencyResolution unknownMembership =
                planner.resolveCombatAchievement(CombatAchievementTier.EASY,
                        context(0, levels(80), null, null, null, true));
        assertKinds(unknownMembership, GoalNodeKind.ACCESS);
    }

    private static StrategyContext context(int accountType,
            Map<Skill, Integer> levels, MinigameSnapshot minigames,
            RecurringOpportunitySnapshot recurring,
            CombatAchievementSnapshot achievements,
            boolean unknownMembership)
    {
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) xp.put(skill, 0);
        MembershipStatus membership = unknownMembership
                ? MembershipStatus.UNKNOWN : MembershipStatus.P2P;
        AccountSnapshot account = new AccountSnapshot("Universal", 921L,
                accountType, "TEST", membership,
                membership == MembershipStatus.P2P ? 1 : 0,
                1_500, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .minigames(minigames)
                .recurringOpportunities(recurring)
                .combatAchievements(achievements)
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL, GoalType.MAX,
                false, false, false, new PreferenceProfile());
    }

    private static Map<Skill, Integer> levels(int level)
    {
        Map<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) result.put(skill, level);
        return result;
    }

    private static void assertKinds(UniversalDependencyResolution resolution,
            GoalNodeKind... expected)
    {
        Set<GoalNodeKind> actual = new HashSet<>();
        for (UniversalDependencyNode node : resolution.getNodes())
            actual.add(node.getKind());
        for (GoalNodeKind kind : expected)
            assertTrue("Missing " + kind + " from " + actual,
                    actual.contains(kind));
    }
}

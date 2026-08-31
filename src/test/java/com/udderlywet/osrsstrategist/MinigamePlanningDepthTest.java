package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MinigamePlanningDepthTest
{
    @Test
    public void activityWithoutExactSetupProducesSpecificVerificationAction()
    {
        Recommendation candidate = find(candidates(0, 60,
                Collections.singleton("shooting-stars"),
                Collections.emptyList(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR), "shooting-stars");

        assertEquals(Confidence.CHECK_NEEDED,
                candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction()
                .contains("Shooting Stars setup"));
        assertFalse(candidate.getGuidance().getAction().trim().isEmpty());
    }

    private final MinigameCandidateProvider provider =
            new MinigameCandidateProvider(new MinigameCatalog());

    @Test
    public void observedTemporossSetupTransitionsFromPreparationToReady()
    {
        Recommendation missing = find(candidates(0, 60,
                Collections.singleton("tempoross"), Collections.emptyList(),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(Confidence.CHECK_NEEDED,
                missing.getConfidence());
        assertTrue(missing.getGuidance().getAction().contains("Harpoon"));

        Recommendation ready = find(candidates(0, 60,
                Collections.singleton("tempoross"),
                Collections.singletonList(item("Dragon harpoon")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(Confidence.VERIFIED, ready.getConfidence());
        assertTrue(ready.getGuidance().getAction()
                .contains("Fish harpoonfish"));
        assertTrue(ready.getGuidance().getSupplies()
                .contains("observed harpoon"));
    }

    @Test
    public void exactMajorMinigameSetupsUseObservedMaterials()
    {
        Recommendation foundry = find(candidates(0, 80,
                Collections.singleton("giants-foundry"),
                Collections.singletonList(item("Steel bar", 28)),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR),
                "giants-foundry");
        assertEquals(Confidence.VERIFIED,
                foundry.getConfidence());
        assertTrue(foundry.getGuidance().getAction()
                .contains("exactly 28 bars' worth"));

        Recommendation tithe = find(candidates(0, 80,
                Collections.singleton("tithe-farm"), Arrays.asList(
                        item("Spade"), item("Seed dibber"),
                        item("Gricoller's can")), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR), "tithe-farm");
        assertEquals(Confidence.VERIFIED,
                tithe.getConfidence());
        assertTrue(tithe.getGuidance().getAction()
                .contains("seed for the observed Farming level"));
    }

    @Test
    public void variableSafetyAndContractEvidenceRemainPreparation()
    {
        Recommendation wintertodt = find(candidates(0, 80,
                Collections.singleton("wintertodt"), Arrays.asList(
                        item("Rune axe"), item("Tinderbox")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR),
                "wintertodt");
        assertEquals(Confidence.CHECK_NEEDED,
                wintertodt.getConfidence());
        assertTrue(wintertodt.getGuidance().getAction()
                .contains("four verified warm-clothing pieces"));

        Recommendation homes = find(candidates(0, 80,
                Collections.singleton("mahogany-homes"), Arrays.asList(
                        item("Hammer"), item("Saw")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR),
                "mahogany-homes");
        assertEquals(Confidence.CHECK_NEEDED,
                homes.getConfidence());
        assertTrue(homes.getGuidance().getAction()
                .contains("live Mahogany Homes contract"));
    }

    @Test
    public void uimConventionalBankCannotProveMinigameSetup()
    {
        Recommendation candidate = find(candidates(2, 60,
                Collections.singleton("tempoross"),
                Collections.singletonList(item("Dragon harpoon")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(Confidence.CHECK_NEEDED,
                candidate.getConfidence());
    }

    @Test
    public void combatMinigameFailsClosedForVerifiedSkiller()
    {
        StrategyContext context = context(0, 1,
                Collections.singleton("pest-control"), Collections.emptyList(),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        Recommendation candidate = find(provider.candidates(context),
                "pest-control");
        assertFalse(new CandidateSafetyPolicy().isAllowed(
                candidate.getSafetyEvidence(), context));
    }

    @Test
    public void relaxedModePrefersLowAttentionVerifiedActivity()
    {
        List<ItemState> items = Arrays.asList(item("Rune pickaxe"),
                item("Chisel"));
        StrategyContext context = context(0, 60,
                new HashSet<>(Arrays.asList("motherlode-mine",
                        "guardians-of-the-rift")), items,
                StrategyMode.RELAXED, SessionIntent.AFK);
        List<Recommendation> candidates = provider.candidates(context);
        assertEquals("minigame:motherlode-mine", candidates.get(0).getId());
        assertEquals(Confidence.VERIFIED,
                candidates.get(0).getConfidence());
    }

    @Test
    public void forestryUsesLevelAppropriateNamedLocation()
    {
        Recommendation oak = find(candidates(0, 20,
                Collections.singleton("forestry"),
                Collections.singletonList(item("Rune axe")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "forestry");
        assertTrue(oak.getGuidance().getLocation().contains("east of Draynor"));

        Recommendation maple = find(candidates(0, 50,
                Collections.singleton("forestry"),
                Collections.singletonList(item("Rune axe")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "forestry");
        assertTrue(maple.getGuidance().getLocation().contains("Seers' Village"));
        assertTrue(maple.getGuidance().getAction().contains("maple trees"));
    }

    private List<Recommendation> candidates(int type, int level,
            java.util.Set<String> unlocked, List<ItemState> bank,
            StrategyMode mode, SessionIntent intent)
    {
        return provider.candidates(context(type, level, unlocked, bank, mode, intent));
    }

    private StrategyContext context(int type, int level,
            java.util.Set<String> unlocked, List<ItemState> bank,
            StrategyMode mode, SessionIntent intent)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) { levels.put(skill, level); xp.put(skill, 0); }
        if (level == 1)
        {
            for (Skill skill : Skill.values())
                if (skill != Skill.ATTACK && skill != Skill.STRENGTH
                        && skill != Skill.DEFENCE && skill != Skill.RANGED
                        && skill != Skill.MAGIC && skill != Skill.PRAYER
                        && skill != Skill.HITPOINTS && skill != Skill.SLAYER)
                    levels.put(skill, 50);
            levels.put(Skill.HITPOINTS, 10);
        }
        AccountSnapshot account = new AccountSnapshot("Player", type,
                AccountMode.fromTypeCode(type).name(), MembershipStatus.P2P,
                1, level * Skill.values().length, 0, levels, xp);
        GameData data = GameData.builder(account)
                .bank(new ItemsState(bank, 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .minigames(new MinigameSnapshot(unlocked, Collections.emptyMap()))
                .build();
        return new StrategyContext(data, mode, intent, QuestTolerance.NORMAL,
                GoalType.MAX, false, false, false, new PreferenceProfile());
    }

    private static Recommendation find(List<Recommendation> values,
            String id)
    {
        Recommendation result = values.stream()
                .filter(value -> value.getId().contains(id)).findFirst().orElse(null);
        assertNotNull(result);
        return result;
    }

    private static ItemState item(String name)
    {
        return item(name, 1);
    }

    private static ItemState item(String name, int quantity)
    {
        return new ItemState(name.hashCode(), name, quantity);
    }
}

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
        StrategyCandidate candidate = find(candidates(0, 60,
                Collections.singleton("shooting-stars"),
                Collections.emptyList(), StrategyMode.BALANCED,
                SessionIntent.ONE_HOUR), "shooting-stars");

        assertEquals(RecommendationConfidence.CHECK_NEEDED,
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
        StrategyCandidate missing = find(candidates(0, 60,
                Collections.singleton("tempoross"), Collections.emptyList(),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                missing.getConfidence());
        assertTrue(missing.getGuidance().getAction().contains("Harpoon"));

        StrategyCandidate ready = find(candidates(0, 60,
                Collections.singleton("tempoross"),
                Collections.singletonList(item("Dragon harpoon")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(RecommendationConfidence.VERIFIED, ready.getConfidence());
        assertTrue(ready.getGuidance().getAction().contains("Start Tempoross"));
    }

    @Test
    public void uimConventionalBankCannotProveMinigameSetup()
    {
        StrategyCandidate candidate = find(candidates(2, 60,
                Collections.singleton("tempoross"),
                Collections.singletonList(item("Dragon harpoon")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "tempoross");
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                candidate.getConfidence());
    }

    @Test
    public void combatMinigameFailsClosedForVerifiedSkiller()
    {
        StrategyContext context = context(0, 1,
                Collections.singleton("pest-control"), Collections.emptyList(),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR);
        StrategyCandidate candidate = find(provider.candidates(context),
                "pest-control");
        assertFalse(new CandidateSafetyPolicy().isAllowed(
                candidate.getSafetyEvidence(), context));
    }

    @Test
    public void relaxedModePrefersLowAttentionVerifiedActivity()
    {
        List<ItemStackSnapshot> items = Arrays.asList(item("Rune pickaxe"),
                item("Chisel"));
        StrategyContext context = context(0, 60,
                new HashSet<>(Arrays.asList("motherlode-mine",
                        "guardians-of-the-rift")), items,
                StrategyMode.RELAXED, SessionIntent.AFK);
        List<StrategyCandidate> candidates = provider.candidates(context);
        assertEquals("minigame:motherlode-mine", candidates.get(0).getId());
        assertEquals(RecommendationConfidence.VERIFIED,
                candidates.get(0).getConfidence());
    }

    @Test
    public void forestryUsesLevelAppropriateNamedLocation()
    {
        StrategyCandidate oak = find(candidates(0, 20,
                Collections.singleton("forestry"),
                Collections.singletonList(item("Rune axe")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "forestry");
        assertTrue(oak.getGuidance().getLocation().contains("east of Draynor"));

        StrategyCandidate maple = find(candidates(0, 50,
                Collections.singleton("forestry"),
                Collections.singletonList(item("Rune axe")),
                StrategyMode.BALANCED, SessionIntent.ONE_HOUR), "forestry");
        assertTrue(maple.getGuidance().getLocation().contains("Seers' Village"));
        assertTrue(maple.getGuidance().getAction().contains("maple trees"));
    }

    private List<StrategyCandidate> candidates(int type, int level,
            java.util.Set<String> unlocked, List<ItemStackSnapshot> bank,
            StrategyMode mode, SessionIntent intent)
    {
        return provider.candidates(context(type, level, unlocked, bank, mode, intent));
    }

    private StrategyContext context(int type, int level,
            java.util.Set<String> unlocked, List<ItemStackSnapshot> bank,
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
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(bank, 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .minigames(new MinigameSnapshot(unlocked, Collections.emptyMap()))
                .build();
        return new StrategyContext(data, mode, intent, QuestTolerance.NORMAL,
                GoalType.MAX, false, false, false, new PreferenceProfile());
    }

    private static StrategyCandidate find(List<StrategyCandidate> values,
            String id)
    {
        StrategyCandidate result = values.stream()
                .filter(value -> value.getId().contains(id)).findFirst().orElse(null);
        assertNotNull(result);
        return result;
    }

    private static ItemStackSnapshot item(String name)
    {
        return new ItemStackSnapshot(name.hashCode(), name, 1);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CombatGuidanceServiceTest
{
    private final CombatGuidanceService service = new CombatGuidanceService();

    @Test
    public void defencePureUsesObservedNoAttackWeaponAndDefensiveStyle()
    {
        AccountSnapshot account = defencePure();
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(24219, "Swift blade", 1)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.DEFENCE,
                75,
                80,
                plan("defence_crabs", Skill.DEFENCE),
                SessionIntent.PICK_FOR_ME,
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Defensive / Defence XP"));
        assertTrue(guidance.getAction().contains("Swift blade"));
        assertTrue(guidance.getAction().contains("successful damage dealt"));
        assertTrue(guidance.getLocation().contains("sand crabs"));
        assertTrue(guidance.getNote().contains("Defence pure"));
    }

    @Test
    public void strengthPlannerDoesNotRecommendWhipForDedicatedStrengthXp()
    {
        AccountSnapshot account = standard(70);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(4151, "Abyssal whip", 1)), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.STRENGTH,
                70,
                80,
                plan("strength_crabs", Skill.STRENGTH),
                SessionIntent.PICK_FOR_ME,
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getAction().contains("Aggressive / Strength XP"));
        assertFalse(guidance.getAction().contains("Abyssal whip"));
    }

    @Test
    public void afkGemstoneCrabUsesItsReducedXpPerDamage()
    {
        AccountSnapshot account = standard(70);
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Children of the Sun", QuestStatus.COMPLETE);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(4151, "Abyssal whip", 1)), 1L))
                .quests(new QuestSnapshot(quests))
                .build();

        RecommendationGuidance guidance = service.build(
                data,
                Skill.ATTACK,
                70,
                80,
                plan("attack_crabs", Skill.ATTACK),
                SessionIntent.AFK,
                true);

        assertNotNull(guidance);
        assertTrue(guidance.getLocation().contains("Gemstone Crab"));
        assertTrue(guidance.getAction().contains("3.5 XP per damage"));
    }

    private static TrainingPlan plan(String id, Skill skill)
    {
        TrainingMethod method = new TrainingMethod(
                id, skill, 1, 99, id, "test",
                10, 10, 10, AttentionLevel.LOW,
                10, 1, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new TrainingPlan(method, "test",
                RecommendationConfidence.VERIFIED,
                Collections.emptyList());
    }

    private static AccountSnapshot defencePure()
    {
        Map<Skill, Integer> levels = baseLevels(60);
        Map<Skill, Integer> xp = baseXp(60);
        levels.put(Skill.ATTACK, 1);
        levels.put(Skill.STRENGTH, 1);
        levels.put(Skill.DEFENCE, 75);
        levels.put(Skill.RANGED, 1);
        levels.put(Skill.MAGIC, 1);
        levels.put(Skill.PRAYER, 43);
        levels.put(Skill.HITPOINTS, 63);
        xp.put(Skill.DEFENCE, Experience.getXpForLevel(75));
        return new AccountSnapshot(
                "Def pure", 0, "Main", MembershipStatus.P2P, 1,
                1000, 0L, levels, xp);
    }

    private static AccountSnapshot standard(int combatLevel)
    {
        Map<Skill, Integer> levels = baseLevels(60);
        Map<Skill, Integer> xp = baseXp(60);
        levels.put(Skill.ATTACK, combatLevel);
        levels.put(Skill.STRENGTH, combatLevel);
        levels.put(Skill.DEFENCE, combatLevel);
        levels.put(Skill.RANGED, combatLevel);
        levels.put(Skill.MAGIC, combatLevel);
        levels.put(Skill.PRAYER, 70);
        levels.put(Skill.HITPOINTS, 80);
        xp.put(Skill.ATTACK, Experience.getXpForLevel(combatLevel));
        xp.put(Skill.STRENGTH, Experience.getXpForLevel(combatLevel));
        xp.put(Skill.DEFENCE, Experience.getXpForLevel(combatLevel));
        return new AccountSnapshot(
                "Main", 0, "Main", MembershipStatus.P2P, 1,
                1600, 0L, levels, xp);
    }

    private static Map<Skill, Integer> baseLevels(int value)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, value);
        return levels;
    }

    private static Map<Skill, Integer> baseXp(int level)
    {
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
            xp.put(skill, Experience.getXpForLevel(level));
        return xp;
    }
}

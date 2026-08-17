package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProgressionUpgradeCandidateProviderTest
{
    private final ProgressionUpgradeCandidateProvider provider =
            new ProgressionUpgradeCandidateProvider();

    @Test
    public void oneDefencePureDoesNotGetDefenceLockedMeleeUpgrades()
    {
        AccountSnapshot account = account(0, 60, 70, 1, 80, 52, 80, 70, 60, 60);
        List<StrategyCandidate> candidates = provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false));

        assertEquals(RestrictedBuildType.ONE_DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(account));
        assertNull(find(candidates, "upgrade:fighter-torso"));
        assertNull(find(candidates, "upgrade:dragon-defender"));
    }

    @Test
    public void earlyMidIronGetsFighterTorsoWhenMissing()
    {
        AccountSnapshot account = account(1, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        StrategyCandidate torso = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:fighter-torso");

        assertNotNull(torso);
        assertTrue(torso.getReason().contains("375 honour points in each role"));
        assertTrue(torso.getReason().contains("Penance Queen"));
    }

    @Test
    public void ironWith85SlayerGetsWhipSelfSourceRoute()
    {
        AccountSnapshot account = account(1, 70, 75, 70, 70, 70, 70, 80, 85, 70);
        StrategyCandidate whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(RecommendationConfidence.VERIFIED,
                whip.getConfidence());
        assertTrue(whip.getReason().contains("85 Slayer"));
        assertTrue(whip.getReason().contains("abyssal demons"));
        assertFalse(whip.getReason().contains("Grand Exchange"));
    }

    @Test
    public void mainWhipPurchaseWaitsForLiveBudgetValidation()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 70);
        StrategyCandidate whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                whip.getConfidence());
        assertTrue(whip.getReason().contains("verified cash budget"));
    }

    @Test
    public void fishing82MakesMissingAnglerSetWorthConsidering()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 82);
        StrategyCandidate angler = find(provider.candidates(
                context(data(account), GoalType.MAX, false)),
                "upgrade:angler-outfit");

        assertNotNull(angler);
        assertTrue(angler.getTitle().contains("0/4"));
        assertTrue(angler.getReason().contains("minnow access"));
    }

    private static StrategyDataBundle data(AccountSnapshot account)
    {
        return StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
    }

    private static StrategyContext context(
            StrategyDataBundle data,
            GoalType goal,
            boolean collectionist)
    {
        return new StrategyContext(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL,
                goal,
                true,
                collectionist,
                false,
                new PreferenceProfile());
    }

    private static StrategyCandidate find(
            List<StrategyCandidate> candidates,
            String id)
    {
        for (StrategyCandidate candidate : candidates)
        {
            if (id.equals(candidate.getId())) return candidate;
        }
        return null;
    }

    private static AccountSnapshot account(
            int typeCode,
            int attack,
            int strength,
            int defence,
            int ranged,
            int prayer,
            int magic,
            int hp,
            int slayer,
            int fishing)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp);
        levels.put(Skill.SLAYER, slayer);
        levels.put(Skill.FISHING, fishing);
        xp.put(Skill.FISHING, Experience.getXpForLevel(fishing));

        return new AccountSnapshot(
                "Upgrade Test",
                typeCode,
                typeCode == 0 ? "Main" : "Ironman",
                MembershipStatus.P2P,
                1,
                1600,
                0L,
                levels,
                xp);
    }
}

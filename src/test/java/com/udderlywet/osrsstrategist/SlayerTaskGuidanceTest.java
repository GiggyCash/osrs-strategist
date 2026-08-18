package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SlayerTaskGuidanceTest
{
    @Test
    public void expandedCorpusCoversProtectionAccessAndIronObjectives()
    {
        SlayerTaskProfileCatalog catalog = new SlayerTaskProfileCatalog();
        assertTrue(catalog.all().size() >= 25);
        assertTrue(catalog.profileFor("Cave horrors").getRequiredProtection()
                .contains("Witchwood icon"));
        assertTrue(catalog.profileFor("Cave horrors").getIronObjectives()
                .contains("Black mask"));
        assertTrue(catalog.profileFor("Skeletal wyverns").getMechanicsNote()
                .contains("does not replace"));
        assertTrue(catalog.profileFor("Drakes").getPreferredLocation()
                .contains("Mount Karuulm"));
    }
    private final SlayerGuidanceService service = new SlayerGuidanceService();

    @Test
    public void dustDevilsUseVerifiedProtectionAndSafeDefaultLocation()
    {
        StrategyDataBundle data = data(
                account(0),
                new SlayerSnapshot("Dust devils", 143, "Duradel", 500,
                        RecommendationConfidence.VERIFIED),
                Arrays.asList(new ItemStackSnapshot(4164, "Facemask", 1)));

        RecommendationGuidance guidance = service.build(data, 80, 90, true);

        assertNotNull(guidance);
        assertTrue(guidance.getSupplies().contains("Verified"));
        assertTrue(guidance.getSupplies().contains("Facemask"));
        assertTrue(guidance.getAction().contains("bursting/barraging"));
        assertTrue(guidance.getLocation().contains("Catacombs of Kourend"));
        assertTrue(guidance.getNote().contains("must be worn"));
        assertTrue(guidance.getNote().contains("Multitarget Magic is supported"));
        assertTrue(guidance.getNote().contains("Cannon use is not confirmed"));
        assertTrue(guidance.getNote().contains("Wilderness variant"));
    }

    @Test
    public void liveKonarLocationOverridesCatalogDefault()
    {
        StrategyDataBundle data = data(
                account(0),
                new SlayerSnapshot("Dust devils", 90, "Konar quo Maten",
                        "Smoke Dungeon", 200,
                        RecommendationConfidence.VERIFIED),
                Arrays.asList(new ItemStackSnapshot(4164, "Facemask", 1)));

        RecommendationGuidance guidance = service.build(data, 75, 80, true);
        assertTrue(guidance.getLocation().contains("Smoke Dungeon"));
        assertFalse(guidance.getLocation().contains("Catacombs of Kourend"));
    }

    @Test
    public void ironKuraskWithoutLegalWeaponGetsSelfSourceInstruction()
    {
        StrategyDataBundle data = data(
                account(1),
                new SlayerSnapshot("Kurasks", 120, "Nieve", 200,
                        RecommendationConfidence.VERIFIED),
                Collections.emptyList());

        RecommendationGuidance guidance = service.build(data, 75, 80, true);
        assertTrue(guidance.getSupplies().contains("Self-source"));
        assertTrue(guidance.getSupplies().contains("Leaf-bladed"));
        assertTrue(guidance.getAction().contains("Ordinary weapons cannot damage"));
    }

    @Test
    public void uimDoesNotCountNormalBankedTaskProtection()
    {
        StrategyDataBundle data = data(
                account(2),
                new SlayerSnapshot("Aberrant spectres", 80, "Nieve", 200,
                        RecommendationConfidence.VERIFIED),
                Arrays.asList(new ItemStackSnapshot(4168, "Nose peg", 1)));

        RecommendationGuidance guidance = service.build(data, 70, 80, true);
        assertTrue(guidance.getSupplies().contains("Normal bank state is ignored for UIM"));
        assertFalse(guidance.getSupplies().startsWith("Verified:"));
    }

    @Test
    public void unknownTaskKeepsConservativeFallback()
    {
        StrategyDataBundle data = data(
                account(0),
                new SlayerSnapshot("Future monster", 50, "Nieve", 0,
                        RecommendationConfidence.VERIFIED),
                Collections.emptyList());

        RecommendationGuidance guidance = service.build(data, 75, 80, true);
        assertTrue(guidance.getSupplies().contains("No catalogued mandatory Slayer item"));
        assertTrue(guidance.getNote().contains("no fixed kills-to-level"));
    }

    @Test
    public void unknownMembershipCannotReceiveSlayerGuidanceDirectly()
    {
        AccountSnapshot p2p = account(0);
        AccountSnapshot unknown = new AccountSnapshot(p2p.getPlayerName(),
                p2p.getAccountTypeCode(), p2p.getAccountTypeName(),
                MembershipStatus.UNKNOWN, 0, p2p.getTotalLevel(),
                p2p.getTotalExperience(), p2p.getSkillLevels(),
                p2p.getSkillExperience());
        assertTrue(service.build(data(unknown,
                new SlayerSnapshot("Future monster", 10, "Unknown", 0,
                        RecommendationConfidence.VERIFIED),
                Collections.emptyList()), 80, 81, true) == null);
    }

    @Test
    public void corpusCoversEarlyMidAndLateTasksWithoutDemonAliasCollision()
    {
        SlayerTaskProfileCatalog catalog = new SlayerTaskProfileCatalog();
        assertTrue(catalog.all().size() >= 40);
        assertEquals("cave-crawlers", catalog.profileFor("Cave crawlers").getId());
        assertEquals("abyssal-demons", catalog.profileFor("Abyssal demons").getId());
        assertEquals("greater-demons", catalog.profileFor("Greater demons").getId());
        assertEquals("black-demons", catalog.profileFor("Black demons").getId());
        assertTrue(catalog.profileFor("Harpie bug swarms").getRequiredProtection()
                .contains("Lit bug lantern"));
    }

    private static StrategyDataBundle data(
            AccountSnapshot account,
            SlayerSnapshot slayer,
            java.util.List<ItemStackSnapshot> bankItems)
    {
        return StrategyDataBundle.builder(account)
                .slayer(slayer)
                .bank(new BankSnapshot(bankItems, 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
    }

    private static AccountSnapshot account(int typeCode)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = skill == Skill.HITPOINTS ? 85 : 80;
            levels.put(skill, level);
            int skillXp = Experience.getXpForLevel(level);
            xp.put(skill, skillXp);
            total += level;
            totalXp += skillXp;
        }
        return new AccountSnapshot(
                "Slayer Test",
                typeCode,
                AccountMode.fromTypeCode(typeCode).name(),
                MembershipStatus.P2P,
                1,
                total,
                totalXp,
                levels,
                xp);
    }
}

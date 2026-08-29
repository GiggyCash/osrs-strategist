package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Test
    public void reviewedStrategicCorpusCoversEveryDetailedTaskFamily()
    {
        SlayerTaskProfileCatalog mechanics = new SlayerTaskProfileCatalog();
        SlayerTaskStrategicCatalog catalog = new SlayerTaskStrategicCatalog();
        assertEquals(mechanics.all().size(), catalog.size());
        for (SlayerTaskProfile profile : mechanics.all())
            assertNotNull(profile.getId(), catalog.profileFor(
                    profile.getAliases().get(0)));
        assertNotNull(catalog.profileFor("Aberrant spectres"));
        assertNotNull(catalog.profileFor("Cave krakens"));
        assertNotNull(catalog.profileFor("Dagannoths"));
        assertNotNull(catalog.profileFor("Dark beasts"));
        assertNotNull(catalog.profileFor("Hydras"));
        assertNotNull(catalog.profileFor("Fossil Island Wyverns"));
        assertNotNull(catalog.profileFor("Molanisks"));
        assertEquals(RiskLevel.HIGH,
                catalog.profileFor("Revenants").getInherentRisk());
        assertTrue(catalog.profileFor("Vorkath").isDirectEncounter());
        assertNotNull(catalog.profileFor("Venators"));
    }

    @Test
    public void reviewedHighLevelMasterWeightsMatchCurrentAssignmentTables()
    {
        SlayerTaskStrategicCatalog catalog = new SlayerTaskStrategicCatalog();
        assertEquals(Integer.valueOf(12),
                catalog.profileFor("Abyssal demons").weightFor("duradel"));
        assertEquals(Integer.valueOf(9),
                catalog.profileFor("Abyssal demons").weightFor("nieve"));
        assertEquals(Integer.valueOf(11),
                catalog.profileFor("Dark beasts").weightFor("duradel"));
        assertEquals(Integer.valueOf(5),
                catalog.profileFor("Dark beasts").weightFor("nieve"));
        assertEquals(Integer.valueOf(9),
                catalog.profileFor("Cave krakens").weightFor("duradel"));
        assertEquals(Integer.valueOf(6),
                catalog.profileFor("Cave krakens").weightFor("nieve"));
        assertEquals(Integer.valueOf(2),
                catalog.profileFor("Waterfiends").weightFor("duradel"));
        assertEquals(Integer.valueOf(10),
                catalog.profileFor("Hydras").weightFor("konar"));
        assertEquals(Integer.valueOf(10),
                catalog.profileFor("Wyrms").weightFor("konar"));
        assertEquals(Integer.valueOf(12),
                catalog.profileFor("Cave krakens").weightFor("chaeldar"));
        assertEquals(Integer.valueOf(10),
                catalog.profileFor("Araxytes").weightFor("duradel"));
        assertEquals(Integer.valueOf(11),
                catalog.profileFor("Custodian Stalkers").weightFor("chaeldar"));
        assertEquals(Integer.valueOf(7),
                catalog.profileFor("Gryphons").weightFor("nieve"));
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
    public void currentTaskCannotLeadUntilItsLoadoutIsConcrete()
    {
        StrategyDataBundle assigned = data(
                account(0),
                new SlayerSnapshot("Dust devils", 143, "Duradel", 500,
                        RecommendationConfidence.VERIFIED),
                Collections.singletonList(
                        new ItemStackSnapshot(4164, "Facemask", 1)));
        Recommendation assignedRecommendation = recommendation(
                service.build(assigned, 80, 81, true));

        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(assignedRecommendation));

        StrategyDataBundle unassigned = data(account(0), null,
                Collections.emptyList());
        Recommendation getTask = recommendation(
                service.build(unassigned, 80, 81, true));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(getTask));
    }

    @Test
    public void corpusCoversEarlyMidAndLateTasksWithoutDemonAliasCollision()
    {
        SlayerTaskProfileCatalog catalog = new SlayerTaskProfileCatalog();
        assertTrue(catalog.all().size() >= 52);
        assertEquals("cave-crawlers", catalog.profileFor("Cave crawlers").getId());
        assertEquals("abyssal-demons", catalog.profileFor("Abyssal demons").getId());
        assertEquals("greater-demons", catalog.profileFor("Greater demons").getId());
        assertEquals("black-demons", catalog.profileFor("Black demons").getId());
        assertTrue(catalog.profileFor("Harpie bug swarms").getRequiredProtection()
                .contains("Lit bug lantern"));
    }

    @Test
    public void everyCanonicalRuneLiteAssignmentHasSpecificReviewableGuidance()
    {
        SlayerTaskIdentityCatalog identities = new SlayerTaskIdentityCatalog();
        SlayerTaskProfileCatalog profiles = new SlayerTaskProfileCatalog();
        List<String> missing = new ArrayList<>();
        Set<String> enumIds = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (SlayerTaskIdentity identity : identities.all())
        {
            assertTrue("Duplicate RuneLite enum identity "
                    + identity.getEnumIdentity(),
                    enumIds.add(identity.getEnumIdentity()));
            assertTrue("Duplicate canonical assignment "
                    + identity.getAssignment(),
                    names.add(identity.getAssignment().toLowerCase()));
            SlayerTaskProfile profile = profiles.profileFor(
                    identity.getAssignment());
            if (profile == null)
            {
                missing.add(identity.getAssignment());
                continue;
            }
            assertFalse(identity.getAssignment(),
                    profile.getPreferredLocation().trim().isEmpty());
            assertFalse(identity.getAssignment(),
                    profile.getStyleGuidance().trim().isEmpty());
            assertFalse(identity.getAssignment(),
                    profile.getMechanicsNote().trim().isEmpty());
            assertFalse(identity.getAssignment(),
                    profile.getTaskDecisionGuidance().trim().isEmpty());
        }
        assertEquals(missing.toString(), Collections.emptyList(), missing);
        assertEquals(151, identities.all().size());
    }

    @Test
    public void reviewedTaskLocationsNameAConcreteDestination()
    {
        SlayerTaskProfileCatalog profiles = new SlayerTaskProfileCatalog();
        for (SlayerTaskProfile profile : profiles.all())
        {
            String location = profile.getPreferredLocation().toLowerCase();
            assertFalse(profile.getId(), location.contains("reachable location"));
            assertFalse(profile.getId(), location.contains("suitable location"));
            assertFalse(profile.getId(), location.contains("task-valid"));
            assertFalse(profile.getId(), location.contains("nearby bank"));
            assertFalse(profile.getId(), location.contains("best available location"));
            assertFalse(profile.getId(), location.contains("safest non-wilderness"));
        }
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

    private static Recommendation recommendation(
            RecommendationGuidance guidance)
    {
        TrainingMethod method = new TrainingMethod(
                "slayer_task", Skill.SLAYER, 1, 99,
                "Complete a Slayer assignment", "Use live task state.",
                10, 10, 10, AttentionLevel.MODERATE, 20, 2,
                Collections.emptyList(), RecommendationConfidence.VERIFIED);
        return new Recommendation("skill:slayer", "Train Slayer to 81",
                "Advance Slayer.", 10,
                new TrainingPlan(method, "Live task",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, 80, 81, guidance,
                CandidateSafetyEvidence.skill(false, Skill.SLAYER));
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

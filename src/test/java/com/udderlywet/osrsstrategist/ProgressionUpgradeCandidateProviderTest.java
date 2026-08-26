package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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
    public void earlyMidIronTorsoIsActuallyActionable()
    {
        AccountSnapshot account = account(1, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        StrategyCandidate torso = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:fighter-torso");

        assertNotNull(torso);
        assertEquals(RecommendationConfidence.VERIFIED, torso.getConfidence());
        assertNotNull(torso.getGuidance());
        assertTrue(torso.getGuidance().getAction().contains("375 honour points"));
        assertTrue(torso.getGuidance().getAction().contains("Penance Queen"));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(torso.toRecommendation()));
    }

    @Test
    public void ironWith85SlayerStillNeedsProvenWhipLocationAndLoadout()
    {
        AccountSnapshot account = account(1, 70, 75, 70, 70, 70, 70, 80, 85, 70);
        StrategyCandidate whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                whip.getConfidence());
        assertNotNull(whip.getGuidance());
        assertTrue(whip.getGuidance().getAction().contains("Slayer Tower"));
        assertFalse(whip.getGuidance().getAction().contains("Grand Exchange"));
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(whip.toRecommendation()));
    }

    @Test
    public void mainWhipPurchaseWaitsForLiveBudgetValidation()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 70);
        StrategyCandidate whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(RecommendationConfidence.CHECK_NEEDED, whip.getConfidence());
        assertTrue(whip.getGuidance().getSupplies().contains("Live price"));
        assertFalse(new RecommendationActionabilityPolicy()
                .canLeadQueue(whip.toRecommendation()));
    }

    @Test
    public void fishing82MakesMissingAnglerSetActionable()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 82);
        StrategyCandidate angler = find(provider.candidates(
                context(data(account), GoalType.MAX, false)),
                "upgrade:angler-outfit");

        assertNotNull(angler);
        assertTrue(angler.getTitle().contains("0/4"));
        assertTrue(angler.getGuidance().getNote().contains("minnow access"));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(angler.toRecommendation()));
    }

    @Test
    public void completedRfdAndVerifiedCashMakesBarrowsGlovesReady()
    {
        AccountSnapshot account = account(0, 75, 75, 70, 75, 70, 75, 80, 70, 70);
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("Recipe for Disaster"))
                .economy(new AccountEconomySnapshot(
                        200_000L, 10_000_000L,
                        RecommendationConfidence.VERIFIED))
                .build();

        StrategyCandidate gloves = find(provider.candidates(
                context(data, GoalType.BARROWS_GLOVES, false)),
                "upgrade:barrows-gloves");

        assertNotNull(gloves);
        assertEquals(RecommendationConfidence.VERIFIED, gloves.getConfidence());
        assertTrue(gloves.getGuidance().getSupplies().contains("130,000"));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(gloves.toRecommendation()));
    }

    @Test
    public void completedMonkeyMadnessCreatesExactDragonScimitarShopRoute()
    {
        AccountSnapshot account = account(1, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("Monkey Madness I"))
                .economy(new AccountEconomySnapshot(100_000L, 100_000L,
                        RecommendationConfidence.VERIFIED)).build();
        StrategyCandidate candidate = find(provider.candidates(
                context(data, GoalType.GEAR_TARGET, false)),
                "upgrade:dragon-scimitar");
        assertNotNull(candidate);
        assertEquals(RecommendationConfidence.VERIFIED, candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction().contains("100,000 coins"));
        assertFalse(candidate.getGuidance().getAction().contains("Grand Exchange"));
    }

    @Test
    public void ownedAvaDeviceSuppressesDuplicateAcquisition()
    {
        AccountSnapshot account = account(0, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        StrategyDataBundle missing = builder(account)
                .quests(questsComplete("Animal Magnetism")).build();
        assertNotNull(find(provider.candidates(
                context(missing, GoalType.GEAR_TARGET, false)), "upgrade:ava-device"));

        StrategyDataBundle owned = builder(account)
                .quests(questsComplete("Animal Magnetism"))
                .bank(new BankSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(1, "Ava's accumulator", 1)), 1L)).build();
        assertNull(find(provider.candidates(
                context(owned, GoalType.GEAR_TARGET, false)), "upgrade:ava-device"));
    }

    @Test
    public void eliteLumbridgeDiscountUsesReducedBarrowsGlovesPrice()
    {
        AccountSnapshot account = account(0, 75, 75, 70, 75, 70, 75, 80, 70, 70);
        Map<DiaryTier, Boolean> tiers = new EnumMap<>(DiaryTier.class);
        tiers.put(DiaryTier.ELITE, true);
        Map<String, Map<DiaryTier, Boolean>> completed = new HashMap<>();
        completed.put("Lumbridge & Draynor", tiers);
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("Recipe for Disaster"))
                .diaries(new DiarySnapshot(
                        Collections.emptyMap(), Collections.emptyMap(), completed))
                .economy(new AccountEconomySnapshot(
                        110_000L, 10_000_000L,
                        RecommendationConfidence.VERIFIED))
                .build();

        StrategyCandidate gloves = find(provider.candidates(
                context(data, GoalType.BARROWS_GLOVES, false)),
                "upgrade:barrows-gloves");
        assertNotNull(gloves);
        assertEquals(RecommendationConfidence.VERIFIED, gloves.getConfidence());
        assertTrue(gloves.getGuidance().getSupplies().contains("104,000"));
    }

    @Test
    public void ironWithEnhancedSeedAndShardsCanCreateBowfaNow()
    {
        AccountSnapshot account = accountWithSkillOverrides(
                1, 80, 85, 80, 85, 70, 85, 90, 85, 80,
                Skill.SMITHING, 82, Skill.CRAFTING, 82);
        List<ItemStackSnapshot> bank = new ArrayList<>();
        bank.add(new ItemStackSnapshot(1, "Enhanced crystal weapon seed", 1));
        bank.add(new ItemStackSnapshot(2, "Crystal shard", 100));
        StrategyDataBundle data = builder(account)
                .bank(new BankSnapshot(bank, 1L))
                .quests(questsComplete("Song of the Elves"))
                .build();

        StrategyCandidate bowfa = find(provider.candidates(
                context(data, GoalType.BOWFA, false)),
                "upgrade:bowfa");

        assertNotNull(bowfa);
        assertEquals(RecommendationConfidence.VERIFIED, bowfa.getConfidence());
        assertTrue(bowfa.getGuidance().getAction().contains("100 Crystal shards"));
        assertTrue(new RecommendationActionabilityPolicy()
                .canLeadQueue(bowfa.toRecommendation()));
    }

    @Test
    public void hardcoreIronBowfaSeedHuntIsDeliberateRiskOnly()
    {
        AccountSnapshot account = account(3, 80, 85, 80, 85, 70, 85, 90, 85, 80);
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("Song of the Elves"))
                .build();

        StrategyCandidate bowfa = find(provider.candidates(
                context(data, GoalType.BOWFA, false)),
                "upgrade:bowfa");

        assertNotNull(bowfa);
        assertEquals(RecommendationConfidence.CHECK_NEEDED, bowfa.getConfidence());
        assertTrue(bowfa.getGuidance().getNote().contains("Hardcore"));
    }

    @Test
    public void uimRetrievalStoredTorsoSuppressesDuplicateTorsoGrind()
    {
        AccountSnapshot account = account(2, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(
                        new ItemStackSnapshot(10551, "Fighter torso", 1)));
        StorageSnapshot storage = new StorageSnapshot(states, contents);
        StrategyDataBundle data = builder(account)
                .storage(storage)
                .build();

        assertNull(find(provider.candidates(
                context(data, GoalType.GEAR_TARGET, false)),
                "upgrade:fighter-torso"));
    }

    @Test
    public void hauntedMineCreatesExactSalveReacquisitionAndOwnershipSuppressesIt()
    {
        AccountSnapshot account = account(1, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        StrategyDataBundle missing = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .inventory(new InventorySnapshot(Collections.singletonList(
                        new ItemStackSnapshot(1, "Chisel", 1))))
                .build();
        StrategyCandidate salve = find(provider.candidates(
                context(missing, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet");
        assertNotNull(salve);
        assertEquals(RecommendationConfidence.VERIFIED, salve.getConfidence());
        assertTrue(salve.getGuidance().getAction().contains("crystal outcrop"));

        StrategyDataBundle owned = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .equipment(new EquipmentSnapshot(Collections.singletonList(
                        new ItemStackSnapshot(2, "Salve amulet", 1))))
                .build();
        assertNull(find(provider.candidates(
                context(owned, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet"));
    }

    @Test
    public void questRewardReplacementRoutesRemainPreparationUntilCostObserved()
    {
        AccountSnapshot account = account(2, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("The Fremennik Isles", "Underground Pass"))
                .build();
        List<StrategyCandidate> candidates = provider.candidates(
                context(data, GoalType.GEAR_TARGET, false));
        StrategyCandidate helm = find(candidates, "upgrade:helm-of-neitiznot");
        StrategyCandidate staff = find(candidates, "upgrade:ibans-staff");
        assertNotNull(helm);
        assertNotNull(staff);
        assertEquals(RecommendationConfidence.CHECK_NEEDED, helm.getConfidence());
        assertEquals(RecommendationConfidence.CHECK_NEEDED, staff.getConfidence());
        assertTrue(helm.getGuidance().getSupplies().contains("inventory space"));
        assertFalse(helm.getGuidance().getAction().contains("Grand Exchange"));
    }

    @Test
    public void uimStoredSalvePromotesRetrievalInsteadOfDuplicateAcquisition()
    {
        AccountSnapshot account = account(2, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemStackSnapshot>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(new ItemStackSnapshot(
                        1, "Salve amulet", 1)));
        StrategyDataBundle data = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .storage(new StorageSnapshot(states, contents)).build();

        StrategyCandidate salve = find(provider.candidates(
                context(data, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet");
        assertNotNull(salve);
        assertEquals(RecommendationConfidence.CHECK_NEEDED,
                salve.getConfidence());
        assertTrue(salve.getTitle().startsWith("Retrieve"));
        assertTrue(salve.getGuidance().getAction().contains("retrieve"));
    }

    private static StrategyDataBundle data(AccountSnapshot account)
    {
        return builder(account).build();
    }

    private static StrategyDataBundle.Builder builder(AccountSnapshot account)
    {
        return StrategyDataBundle.builder(account)
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()));
    }

    private static QuestSnapshot questsComplete(String... names)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        for (String name : names) quests.put(name, QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
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
        return accountWithSkillOverrides(
                typeCode, attack, strength, defence, ranged, prayer,
                magic, hp, slayer, fishing);
    }

    private static AccountSnapshot accountWithSkillOverrides(
            int typeCode,
            int attack,
            int strength,
            int defence,
            int ranged,
            int prayer,
            int magic,
            int hp,
            int slayer,
            int fishing,
            Object... skillLevelPairs)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) levels.put(skill, 60);
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.MAGIC, magic);
        levels.put(Skill.HITPOINTS, hp);
        levels.put(Skill.SLAYER, slayer);
        levels.put(Skill.FISHING, fishing);
        for (int i = 0; i + 1 < skillLevelPairs.length; i += 2)
        {
            levels.put((Skill) skillLevelPairs[i], (Integer) skillLevelPairs[i + 1]);
        }

        int total = 0;
        long totalXp = 0L;
        for (Skill skill : Skill.values())
        {
            int level = levels.get(skill);
            int skillXp = level <= 1 ? 0 : Experience.getXpForLevel(level);
            xp.put(skill, skillXp);
            total += level;
            totalXp += skillXp;
        }

        return new AccountSnapshot(
                "Upgrade Test",
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

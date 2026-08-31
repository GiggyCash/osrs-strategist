package compass;

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
        List<Recommendation> candidates = provider.candidates(
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
        Recommendation torso = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:fighter-torso");

        assertNotNull(torso);
        assertEquals(Confidence.VERIFIED, torso.getConfidence());
        assertNotNull(torso.getGuidance());
        assertTrue(torso.getGuidance().getAction().contains("375 honour points"));
        assertTrue(torso.getGuidance().getAction().contains("Penance Queen"));
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(torso));
    }

    @Test
    public void ironWith85SlayerStillNeedsProvenWhipLocationAndLoadout()
    {
        AccountSnapshot account = account(1, 70, 75, 70, 70, 70, 70, 80, 85, 70);
        Recommendation whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(Confidence.CHECK_NEEDED,
                whip.getConfidence());
        assertNotNull(whip.getGuidance());
        assertTrue(whip.getGuidance().getAction().contains("Slayer Tower"));
        assertFalse(whip.getGuidance().getAction().contains("Grand Exchange"));
        assertFalse(new ActionabilityPolicy()
                .canLeadQueue(whip));
    }

    @Test
    public void mainWhipPurchaseWaitsForLiveBudgetValidation()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 70);
        Recommendation whip = find(provider.candidates(
                context(data(account), GoalType.GEAR_TARGET, false)),
                "upgrade:abyssal-whip");

        assertNotNull(whip);
        assertEquals(Confidence.CHECK_NEEDED, whip.getConfidence());
        assertTrue(whip.getGuidance().getSupplies().contains("Live price"));
        assertFalse(new ActionabilityPolicy()
                .canLeadQueue(whip));
    }

    @Test
    public void fishing82MakesMissingAnglerSetActionable()
    {
        AccountSnapshot account = account(0, 70, 75, 70, 70, 70, 70, 80, 70, 82);
        Recommendation angler = find(provider.candidates(
                context(data(account), GoalType.MAX, false)),
                "upgrade:angler-outfit");

        assertNotNull(angler);
        assertTrue(angler.getTitle().contains("0/4"));
        assertTrue(angler.getGuidance().getNote().contains("minnow access"));
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(angler));
    }

    @Test
    public void completedRfdAndVerifiedCashMakesBarrowsGlovesReady()
    {
        AccountSnapshot account = account(0, 75, 75, 70, 75, 70, 75, 80, 70, 70);
        GameData data = builder(account)
                .quests(questsComplete("Recipe for Disaster"))
                .economy(new AccountEconomySnapshot(
                        200_000L, 10_000_000L,
                        Confidence.VERIFIED))
                .build();

        Recommendation gloves = find(provider.candidates(
                context(data, GoalType.BARROWS_GLOVES, false)),
                "upgrade:barrows-gloves");

        assertNotNull(gloves);
        assertEquals(Confidence.VERIFIED, gloves.getConfidence());
        assertTrue(gloves.getGuidance().getSupplies().contains("130,000"));
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(gloves));
    }

    @Test
    public void completedMonkeyMadnessCreatesExactDragonScimitarShopRoute()
    {
        AccountSnapshot account = account(1, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        GameData data = builder(account)
                .quests(questsComplete("Monkey Madness I"))
                .economy(new AccountEconomySnapshot(100_000L, 100_000L,
                        Confidence.VERIFIED)).build();
        Recommendation candidate = find(provider.candidates(
                context(data, GoalType.GEAR_TARGET, false)),
                "upgrade:dragon-scimitar");
        assertNotNull(candidate);
        assertEquals(Confidence.VERIFIED, candidate.getConfidence());
        assertTrue(candidate.getGuidance().getAction().contains("100,000 coins"));
        assertFalse(candidate.getGuidance().getAction().contains("Grand Exchange"));
    }

    @Test
    public void ownedAvaDeviceSuppressesDuplicateAcquisition()
    {
        AccountSnapshot account = account(0, 60, 70, 45, 60, 43, 60, 70, 50, 60);
        GameData missing = builder(account)
                .quests(questsComplete("Animal Magnetism")).build();
        assertNotNull(find(provider.candidates(
                context(missing, GoalType.GEAR_TARGET, false)), "upgrade:ava-device"));

        GameData owned = builder(account)
                .quests(questsComplete("Animal Magnetism"))
                .bank(new ItemsState(Collections.singletonList(
                        new ItemState(1, "Ava's accumulator", 1)), 1L)).build();
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
        GameData data = builder(account)
                .quests(questsComplete("Recipe for Disaster"))
                .diaries(new DiarySnapshot(
                        Collections.emptyMap(), Collections.emptyMap(), completed))
                .economy(new AccountEconomySnapshot(
                        110_000L, 10_000_000L,
                        Confidence.VERIFIED))
                .build();

        Recommendation gloves = find(provider.candidates(
                context(data, GoalType.BARROWS_GLOVES, false)),
                "upgrade:barrows-gloves");
        assertNotNull(gloves);
        assertEquals(Confidence.VERIFIED, gloves.getConfidence());
        assertTrue(gloves.getGuidance().getSupplies().contains("104,000"));
    }

    @Test
    public void ironWithEnhancedSeedAndShardsCanCreateBowfaNow()
    {
        AccountSnapshot account = accountWithSkillOverrides(
                1, 80, 85, 80, 85, 70, 85, 90, 85, 80,
                Skill.SMITHING, 82, Skill.CRAFTING, 82);
        List<ItemState> bank = new ArrayList<>();
        bank.add(new ItemState(1, "Enhanced crystal weapon seed", 1));
        bank.add(new ItemState(2, "Crystal shard", 100));
        GameData data = builder(account)
                .bank(new ItemsState(bank, 1L))
                .quests(questsComplete("Song of the Elves"))
                .build();

        Recommendation bowfa = find(provider.candidates(
                context(data, GoalType.BOWFA, false)),
                "upgrade:bowfa");

        assertNotNull(bowfa);
        assertEquals(Confidence.VERIFIED, bowfa.getConfidence());
        assertTrue(bowfa.getGuidance().getAction().contains("100 Crystal shards"));
        assertTrue(new ActionabilityPolicy()
                .canLeadQueue(bowfa));
    }

    @Test
    public void hardcoreIronBowfaSeedHuntIsDeliberateRiskOnly()
    {
        AccountSnapshot account = account(3, 80, 85, 80, 85, 70, 85, 90, 85, 80);
        GameData data = builder(account)
                .quests(questsComplete("Song of the Elves"))
                .build();

        Recommendation bowfa = find(provider.candidates(
                context(data, GoalType.BOWFA, false)),
                "upgrade:bowfa");

        assertNotNull(bowfa);
        assertEquals(Confidence.CHECK_NEEDED, bowfa.getConfidence());
        assertTrue(bowfa.getGuidance().getNote().contains("Hardcore"));
    }

    @Test
    public void uimRetrievalStoredTorsoSuppressesDuplicateTorsoGrind()
    {
        AccountSnapshot account = account(2, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        Map<StorageCapability, CapabilityState> states =
                new EnumMap<>(StorageCapability.class);
        states.put(StorageCapability.DEATH_STORAGE, CapabilityState.VERIFIED);
        Map<StorageCapability, List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(
                        new ItemState(10551, "Fighter torso", 1)));
        StorageSnapshot storage = new StorageSnapshot(states, contents);
        GameData data = builder(account)
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
        GameData missing = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .inventory(new ItemsState(Collections.singletonList(
                        new ItemState(1, "Chisel", 1))))
                .build();
        Recommendation salve = find(provider.candidates(
                context(missing, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet");
        assertNotNull(salve);
        assertEquals(Confidence.VERIFIED, salve.getConfidence());
        assertTrue(salve.getGuidance().getAction().contains("crystal outcrop"));

        GameData owned = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .equipment(new ItemsState(Collections.singletonList(
                        new ItemState(2, "Salve amulet", 1))))
                .build();
        assertNull(find(provider.candidates(
                context(owned, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet"));
    }

    @Test
    public void questRewardReplacementRoutesRemainPreparationUntilCostObserved()
    {
        AccountSnapshot account = account(2, 70, 70, 70, 70, 60, 70, 80, 70, 70);
        GameData data = builder(account)
                .quests(questsComplete("The Fremennik Isles", "Underground Pass"))
                .build();
        List<Recommendation> candidates = provider.candidates(
                context(data, GoalType.GEAR_TARGET, false));
        Recommendation helm = find(candidates, "upgrade:helm-of-neitiznot");
        Recommendation staff = find(candidates, "upgrade:ibans-staff");
        assertNotNull(helm);
        assertNotNull(staff);
        assertEquals(Confidence.CHECK_NEEDED, helm.getConfidence());
        assertEquals(Confidence.CHECK_NEEDED, staff.getConfidence());
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
        Map<StorageCapability, List<ItemState>> contents =
                new EnumMap<>(StorageCapability.class);
        contents.put(StorageCapability.DEATH_STORAGE,
                Collections.singletonList(new ItemState(
                        1, "Salve amulet", 1)));
        GameData data = builder(account)
                .quests(questsComplete("Haunted Mine"))
                .storage(new StorageSnapshot(states, contents)).build();

        Recommendation salve = find(provider.candidates(
                context(data, GoalType.GEAR_TARGET, false)),
                "upgrade:salve-amulet");
        assertNotNull(salve);
        assertEquals(Confidence.CHECK_NEEDED,
                salve.getConfidence());
        assertTrue(salve.getTitle().startsWith("Retrieve"));
        assertTrue(salve.getGuidance().getAction().contains("retrieve"));
    }

    private static GameData data(AccountSnapshot account)
    {
        return builder(account).build();
    }

    private static GameData.Builder builder(AccountSnapshot account)
    {
        return GameData.builder(account)
                .bank(new ItemsState(Collections.emptyList(), 1L))
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()));
    }

    private static QuestSnapshot questsComplete(String... names)
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        for (String name : names) quests.put(name, QuestStatus.COMPLETE);
        return new QuestSnapshot(quests);
    }

    private static StrategyContext context(
            GameData data,
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

    private static Recommendation find(
            List<Recommendation> candidates,
            String id)
    {
        for (Recommendation candidate : candidates)
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

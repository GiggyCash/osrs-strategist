package compass;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Cross-system regression tests for failures that are unacceptable in beta.
 *
 * <p>These deliberately test boundaries rather than one implementation detail:
 * membership isolation, primary-queue actionability, restricted builds, and
 * account-mode storage semantics.</p>
 */
public class BetaSafetyRegressionTest
{
    @Test
    public void unknownMembershipFailsClosedToF2pContent()
    {
        assertTrue(ContentAccessRules.isSkillAvailable(
                Skill.DEFENCE, Membership.UNKNOWN));
        assertFalse(ContentAccessRules.isSkillAvailable(
                Skill.SLAYER, Membership.UNKNOWN));
        assertFalse(ContentAccessRules.isSkillAvailable(
                Skill.SAILING, Membership.UNKNOWN));

        TrainingMethod f2p = method(
                "mining_f2p_iron", Skill.MINING, false,
                Confidence.VERIFIED);
        TrainingMethod members = method(
                "mining_mlm", Skill.MINING, true,
                Confidence.VERIFIED);

        assertTrue(ContentAccessRules.isMethodAvailable(
                f2p, Membership.UNKNOWN));
        assertFalse(ContentAccessRules.isMethodAvailable(
                members, Membership.UNKNOWN));
    }

    @Test
    public void f2pNeverGetsCombatAchievementRewardTierCandidate()
    {
        AccountSnapshot account = account(
                Membership.F2P, 0, 60, 1, 1, 1, 1);
        GameData data = GameData.builder(account)
                .combatAchievements(new CombatAchievementSnapshot(18, 27,
                        Collections.emptySet()))
                .build();
        StrategyContext context = new StrategyContext(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL,
                GoalType.MAX,
                false,
                false,
                false,
                new PreferenceProfile());

        assertTrue(new CombatAchievementCandidateProvider()
                .candidates(context).isEmpty());
    }

    @Test
    public void needsInfoCannotOutrankReadyDoNext()
    {
        Recommendation ready = new Recommendation(
                "skill:defence",
                "Train Defence to 80",
                "Protected Defence-pure progression.",
                40.0,
                null,
                Confidence.VERIFIED,
                75,
                80,
                new Guidance(
                        "Train Defence with the best legal defensive style.",
                        "Use verified food and gear.",
                        "Use a safe F2P combat target.",
                        "Defence is the protected offensive-free combat path for this build."));
        Recommendation unresolvedQuest = new Recommendation(
                "quest:pandemonium",
                "Quest: Pandemonium",
                "Unresolved quest candidate.",
                999.0,
                null,
                Confidence.CHECK_NEEDED,
                0,
                0,
                null);

        ActionabilityPolicy policy =
                new ActionabilityPolicy();
        assertTrue(policy.canLeadQueue(ready));
        assertFalse(policy.canLeadQueue(unresolvedQuest));

        List<Recommendation> queue = queue(
                Arrays.asList(unresolvedQuest, ready), policy);

        assertEquals(1, queue.size());
        assertEquals("skill:defence", queue.get(0).getId());
    }

    @Test
    public void queueReturnsNothingRatherThanInventingPrimaryAction()
    {
        Recommendation unresolved = new Recommendation(
                "quest:unknown",
                "Quest: Unknown",
                "Requirements are not verified.",
                500.0, Confidence.CHECK_NEEDED, null, Safety.unknown());
        List<Recommendation> queue = queue(
                Collections.singletonList(unresolved),
                new ActionabilityPolicy());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void defencePureAllowsDefenceAndPrayerButBlocksOffence()
    {
        AccountSnapshot account = account(
                Membership.F2P, 0, 75, 1, 1, 1, 43);

        assertEquals(BuildType.DEFENCE_PURE,
                AccountBuildPolicy.effectiveBuild(account));
        assertTrue(AccountBuildPolicy.allowsSkill(account, Skill.DEFENCE));
        assertTrue(AccountBuildPolicy.allowsSkill(account, Skill.PRAYER));
        assertFalse(AccountBuildPolicy.allowsSkill(account, Skill.ATTACK));
        assertFalse(AccountBuildPolicy.allowsSkill(account, Skill.STRENGTH));
        assertFalse(AccountBuildPolicy.allowsSkill(account, Skill.RANGED));
        assertFalse(AccountBuildPolicy.allowsSkill(account, Skill.MAGIC));
        assertFalse(AccountBuildPolicy.allowsSkill(account, Skill.SLAYER));
    }

    @Test
    public void uimNeverCountsNormalBankAsImmediatelyUsable()
    {
        AccountSnapshot uim = account(
                Membership.P2P, 2, 70, 1, 1, 1, 43);
        ItemsState bank = new ItemsState(
                Collections.singletonList(
                        new ItemState(383, "Raw shark", 1000)),
                1L);
        GameData data = GameData.builder(uim)
                .bank(bank)
                .inventory(new ItemsState(Collections.emptyList()))
                .equipment(new ItemsState(Collections.emptyList()))
                .build();

        ItemIndex items = new ItemIndex(data, false);
        assertEquals(0, items.quantity("Raw shark"));
        assertFalse(items.bankObserved());
        assertTrue(items.primaryOwnershipObserved());
    }

    @Test
    public void uimModeAloneNeverProvesAnUnobservedInventoryEmpty()
    {
        GameData data = GameData.builder(account(
                Membership.P2P, 2, 70, 1, 1, 1, 43)).build();

        ItemIndex items = new ItemIndex(data, false);
        assertFalse(items.bankObserved());
        assertFalse(items.primaryOwnershipObserved());
        assertFalse(items.usableOwnershipObserved());
    }

    @Test
    public void ordinaryAccountCountsObservedBank()
    {
        AccountSnapshot main = account(
                Membership.P2P, 0, 70, 70, 70, 70, 70);
        ItemsState bank = new ItemsState(
                Collections.singletonList(
                        new ItemState(383, "Raw shark", 1000)),
                1L);
        GameData data = GameData.builder(main)
                .bank(bank)
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        assertEquals(1000,
                new ItemIndex(data, false).quantity("Raw shark"));
    }

    @Test
    public void groupStorageOnlyCountsWhenEnabledAndObserved()
    {
        AccountSnapshot gim = account(
                Membership.P2P, 4, 70, 70, 70, 70, 70);
        ItemsState group = new ItemsState(
                true,
                Collections.singletonList(
                        new ItemState(1515, "Yew logs", 500)));
        GameData data = GameData.builder(gim)
                .groupStorage(group)
                .inventory(new ItemsState(Collections.emptyList()))
                .build();

        assertEquals(0,
                new ItemIndex(data, false).quantity("Yew logs"));
        assertEquals(500,
                new ItemIndex(data, true).quantity("Yew logs"));
    }

    private static List<Recommendation> queue(
            List<Recommendation> pool,
            ActionabilityPolicy policy)
    {
        StrategyEngine engine = TestFixtures.strategyEngine(
                null, null, null, null, policy);
        return engine.buildPlayerQueue(pool, null);
    }

    private static TrainingMethod method(
            String id,
            Skill skill,
            boolean membersOnly,
            Confidence confidence)
    {
        return new TrainingMethod(
                id,
                skill,
                1,
                99,
                id,
                "test",
                10,
                10,
                10,
                AttentionLevel.MODERATE,
                10,
                1,
                Collections.emptyList(),
                confidence,
                membersOnly,
                false,
                false);
    }

    private static AccountSnapshot account(
            Membership membership,
            int typeCode,
            int defence,
            int attack,
            int strength,
            int ranged,
            int prayer)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 30);
            xp.put(skill, Experience.getXpForLevel(30));
        }
        levels.put(Skill.ATTACK, attack);
        levels.put(Skill.STRENGTH, strength);
        levels.put(Skill.DEFENCE, defence);
        levels.put(Skill.RANGED, ranged);
        levels.put(Skill.MAGIC, 1);
        levels.put(Skill.PRAYER, prayer);
        levels.put(Skill.HITPOINTS, Math.max(10, defence >= 20 ? 63 : 10));
        xp.put(Skill.ATTACK, Experience.getXpForLevel(Math.max(1, attack)));
        xp.put(Skill.STRENGTH, Experience.getXpForLevel(Math.max(1, strength)));
        xp.put(Skill.DEFENCE, Experience.getXpForLevel(Math.max(1, defence)));
        xp.put(Skill.RANGED, Experience.getXpForLevel(Math.max(1, ranged)));
        xp.put(Skill.MAGIC, 0);
        xp.put(Skill.PRAYER, Experience.getXpForLevel(Math.max(1, prayer)));
        xp.put(Skill.HITPOINTS, Experience.getXpForLevel(
                Math.max(10, defence >= 20 ? 63 : 10)));

        return new AccountSnapshot("Beta Safety", 0L, typeCode, AccountMode.fromTypeCode(typeCode).name(), membership, 1, 1200, 0L, levels, xp);
    }
}

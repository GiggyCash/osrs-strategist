package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Fifty defensible winner assertions across the shared final decision layer. */
public class SensibleWinnerScenarioMatrixTest
{
    @Test
    public void everyPublicGoalReshapesOneIdenticalCandidatePool()
    {
        List<Recommendation> candidates = Arrays.asList(
                ready("opportunity:automatic", 31, "Ready account opportunity."),
                skill("skill:mining", Skill.MINING, 30,
                        AttentionLevel.LOW, 2, 20, 70, 71),
                ready("quest:recipe-for-disaster", 30,
                        "Advance Recipe for Disaster."),
                ready("pvm:tztok_jad", 30,
                        "Observed Fight Caves preparation."),
                ready("quest:remaining-prerequisite", 30,
                        "Complete a quest-cape prerequisite."),
                ready("quest:song-of-the-elves", 30,
                        "Unlock Prifddinas."),
                ready("upgrade:bowfa", 30,
                        "Advance crystal weapon acquisition."),
                ready("pvm:inferno", 30,
                        "Complete verified Inferno preparation."));
        Map<GoalType, String> expected = new LinkedHashMap<>();
        expected.put(GoalType.AUTOMATIC, "opportunity:automatic");
        expected.put(GoalType.BARROWS_GLOVES, "quest:recipe-for-disaster");
        expected.put(GoalType.FIRE_CAPE, "pvm:tztok_jad");
        expected.put(GoalType.QUEST_CAPE, "quest:");
        expected.put(GoalType.PRIFDDINAS, "quest:song-of-the-elves");
        expected.put(GoalType.BOWFA, "upgrade:bowfa");
        expected.put(GoalType.INFERNAL_CAPE, "pvm:inferno");
        expected.put(GoalType.MAX, "skill:mining");

        StrategyEngine engine = new StrategyEngine(null, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
        for (Map.Entry<GoalType, String> entry : expected.entrySet())
        {
            StrategyContext context = context(0, MembershipStatus.P2P,
                    StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                    entry.getKey(), standard(70), false);
            String winner = engine.buildPlayerQueue(candidates, context)
                    .get(0).getId();
            assertTrue(entry.getKey() + " did not reshape the pool: " + winner,
                    winner.startsWith(entry.getValue()));
            System.out.println("GOAL_SENSITIVITY " + entry.getKey()
                    + " winner=" + winner);
        }
    }

    @Test
    public void fiftyRealisticAccountSituationsChooseTheExpectedActionFamily()
    {
        List<Scenario> cases = scenarios();
        assertTrue(cases.size() >= 50);
        StrategyEngine engine = new StrategyEngine(null, null, null, null,
                new RecommendationActionabilityPolicy(),
                new RecommendationIntelligenceService());
        StringBuilder failures = new StringBuilder();
        for (Scenario scenario : cases)
        {
            List<Recommendation> queue = engine.buildPlayerQueue(
                    scenario.candidates, scenario.context);
            if (queue.isEmpty())
                failures.append("\nNo winner for ").append(scenario.name);
            else if (!queue.get(0).getId().startsWith(scenario.expectedPrefix))
                failures.append('\n').append(scenario.name).append(" expected ")
                        .append(scenario.expectedPrefix).append(" but got ")
                        .append(queue.get(0).getId());
        }
        assertEquals(failures.toString(), 0, failures.length());
    }

    private static List<Scenario> scenarios()
    {
        List<Scenario> values = new ArrayList<>();
        StrategyContext max = context(0, MembershipStatus.P2P,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                GoalType.MAX, standard(70), false);

        // Goal-directed winners: every major goal competes with plausible work.
        values.add(goal("01 max values skill progress", GoalType.MAX, "skill:",
                skill("skill:mining", Skill.MINING, 42, AttentionLevel.ACTIVE, 2, 20, 70, 71),
                ready("money:generic", 44, "Earn spendable GP.")));
        values.add(goal("02 quest cape values prerequisite quest", GoalType.QUEST_CAPE, "quest:",
                ready("quest:prerequisite", 32, "Completes a required quest."),
                skill("skill:fishing", Skill.FISHING, 43, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("03 barrows gloves values RFD", GoalType.BARROWS_GLOVES, "quest:",
                ready("quest:recipe-for-disaster", 28, "Advance Recipe for Disaster."),
                ready("upgrade:generic", 47, "General upgrade.")));
        values.add(goal("04 Prifddinas values Song of the Elves", GoalType.PRIFDDINAS, "quest:",
                ready("quest:song-of-the-elves", 25, "Unlock Prifddinas."),
                skill("skill:mining", Skill.MINING, 48, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("05 Bowfa values crystal acquisition", GoalType.BOWFA, "upgrade:",
                ready("upgrade:bowfa", 24, "Crystal weapon progression."),
                skill("skill:woodcutting", Skill.WOODCUTTING, 48, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("06 Infernal cape values Inferno preparation", GoalType.INFERNAL_CAPE, "pvm:",
                ready("pvm:inferno", 20, "Verified Inferno preparation."),
                ready("money:generic", 52, "Earn GP.")));
        values.add(goal("06b Fire cape values observed Jad readiness", GoalType.FIRE_CAPE, "pvm:tztok_jad",
                ready("pvm:tztok_jad", 24, "Observed Fight Cave readiness."),
                skill("skill:mining", Skill.MINING, 48, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("07 diary cape values diary task", GoalType.DIARY_CAPE, "diary:",
                ready("diary:kandarin-elite", 28, "Finish an actionable diary prerequisite."),
                skill("skill:cooking", Skill.COOKING, 45, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("08 elite CA values combat achievement", GoalType.ELITE_COMBAT_ACHIEVEMENTS, "combat-achievement:",
                ready("combat-achievement:task", 27, "Complete a verified CA task."),
                skill("skill:agility", Skill.AGILITY, 44, AttentionLevel.ACTIVE, 2, 20, 70, 71)));
        values.add(goal("09 raid ready values encounter preparation", GoalType.RAID_READY, "pvm:",
                ready("pvm:raid-prep", 31, "Verified raid preparation."),
                skill("skill:firemaking", Skill.FIREMAKING, 46, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(goal("10 total 2000 values a skill level", GoalType.TOTAL_2000, "skill:",
                skill("skill:crafting", Skill.CRAFTING, 31, AttentionLevel.ACTIVE, 2, 20, 70, 71),
                ready("clue:pending", 45, "Complete a clue.")));
        values.add(goal("11 Slayer 85 values Slayer", GoalType.SLAYER_85, "skill:slayer",
                skill("skill:slayer", Skill.SLAYER, 18, AttentionLevel.ACTIVE, 3, 30, 70, 85),
                ready("upgrade:generic", 53, "General upgrade.")));
        values.add(goal("12 base 70s values sub-70 skill", GoalType.BASE_70S, "skill:",
                skill("skill:runecraft", Skill.RUNECRAFT, 29, AttentionLevel.ACTIVE, 2, 20, 62, 70),
                ready("money:generic", 46, "Earn GP.")));
        values.add(goal("13 gear target values practical upgrade", GoalType.GEAR_TARGET, "upgrade:",
                ready("upgrade:practical", 24, "Best practical target-specific upgrade."),
                skill("skill:mining", Skill.MINING, 50, AttentionLevel.LOW, 2, 20, 70, 71)));

        // Session fit affects the global queue, including non-training work.
        values.add(scenario("14 short session chooses low setup", ctx(max, SessionIntent.QUICK_20_MIN), "skill:quick",
                skill("skill:quick", Skill.FISHING, 40, AttentionLevel.LOW, 1, 15, 70, 71),
                skill("skill:long", Skill.MINING, 48, AttentionLevel.ACTIVE, 12, 60, 70, 71)));
        values.add(scenario("15 AFK session chooses AFK method", ctx(max, SessionIntent.AFK), "skill:afk",
                skill("skill:afk", Skill.WOODCUTTING, 38, AttentionLevel.AFK, 2, 30, 70, 71),
                skill("skill:active", Skill.MINING, 48, AttentionLevel.ACTIVE, 2, 30, 70, 71)));
        values.add(scenario("16 long session chooses substantial activity", ctx(max, SessionIntent.LONG_SESSION), "pvm:",
                ready("pvm:verified-boss", 42, "Verified long encounter preparation."),
                ready("money:quick", 44, "Quick GP task.")));
        values.add(scenario("17 one hour fits quest block", ctx(max, SessionIntent.ONE_HOUR), "quest:",
                ready("quest:one-hour", 44, "Actionable quest segment."),
                ready("money:generic", 43, "Earn GP.")));
        values.add(scenario("18 short session values ready recurring", ctx(max, SessionIntent.QUICK_20_MIN), "opportunity:",
                ready("opportunity:herb-run", 38, "Observed ready recurring work."),
                ready("quest:long", 47, "Long quest.")));
        values.add(scenario("19 AFK avoids active PvM", ctx(max, SessionIntent.AFK), "skill:",
                skill("skill:afk-fishing", Skill.FISHING, 39, AttentionLevel.AFK, 1, 30, 70, 71),
                ready("pvm:active", 52, "Active encounter.")));

        // Strategy modes affect non-training families and method attention.
        values.add(scenario("20 efficient chooses shared unlock",
                context(0, MembershipStatus.P2P, StrategyMode.EFFICIENT,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, standard(70), false), "quest:",
                ready("quest:shared-unlock", 42, "Shared prerequisite saves future time."),
                ready("money:generic", 44, "Earn GP.")));
        values.add(scenario("21 balanced chooses multi-goal progress", max, "detour:",
                ready("detour:shared", 42, "Also advances multiple goals."),
                ready("money:generic", 44, "Earn GP.")));
        values.add(scenario("22 relaxed chooses low-fatigue method",
                context(0, MembershipStatus.P2P, StrategyMode.RELAXED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, standard(70), false), "skill:relaxed",
                skill("skill:relaxed", Skill.FISHING, 39, AttentionLevel.AFK, 2, 30, 70, 71),
                skill("skill:sweaty", Skill.MINING, 46, AttentionLevel.ACTIVE, 2, 30, 70, 71)));
        values.add(scenario("23 relaxed avoids encounter grind",
                context(0, MembershipStatus.P2P, StrategyMode.RELAXED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, standard(70), false), "skill:",
                skill("skill:woodcutting", Skill.WOODCUTTING, 40, AttentionLevel.LOW, 2, 30, 70, 71),
                ready("pvm:grind", 49, "Repeated active encounter.")));

        // Account-mode economics and setup safety.
        values.add(scenario("24 main prefers direct money tool",
                accountContext(0), "money:", ready("money:direct", 42, "Earn GP for the target."),
                ready("detour:self-source", 44, "Self-source a side route.")));
        values.add(scenario("25 Iron values self-source detour",
                accountContext(1), "detour:", ready("detour:self-source", 37, "Self-source supplies while training."),
                ready("money:generic", 45, "Grand Exchange value.")));
        values.add(scenario("26 UIM values low setup",
                accountContext(2), "skill:low-setup",
                skill("skill:low-setup", Skill.FISHING, 36, AttentionLevel.LOW, 1, 20, 70, 71),
                skill("skill:bank-heavy", Skill.CRAFTING, 47, AttentionLevel.LOW, 15, 30, 70, 71)));
        values.add(scenario("27 HCIM refuses Wilderness winner",
                accountContext(3), "skill:", skill("skill:safe", Skill.FISHING, 25, AttentionLevel.LOW, 2, 20, 70, 71),
                ready("pvm:wilderness-boss", 500, "Dangerous Wilderness encounter.")));
        values.add(scenario("28 GIM values useful self-source upgrade",
                accountContext(4), "upgrade:", ready("upgrade:group-useful", 39, "Self-source supplies useful to the group."),
                ready("money:generic", 45, "GE value only.")));
        values.add(scenario("29 HCGIM refuses risky encounter",
                accountContext(5), "skill:", skill("skill:safe", Skill.COOKING, 28, AttentionLevel.LOW, 2, 20, 70, 71),
                irreversible("pvm:risky", 200, "High risk encounter.")));
        values.add(scenario("30 UGIM values shared self-source detour",
                accountContext(6), "detour:", ready("detour:shared-resource", 37, "Self-source supplies while advancing another goal."),
                ready("money:generic", 46, "GE value only.")));
        values.add(scenario("31 UIM rejects bank-heavy reason",
                accountContext(2), "skill:", skill("skill:safe", Skill.AGILITY, 35, AttentionLevel.LOW, 2, 20, 70, 71),
                ready("upgrade:banked", 49, "Open the bank and reorganise stored gear.")));

        // Membership and restricted-build final safety boundaries.
        values.add(scenario("32 unknown membership fails closed to F2P-safe action",
                context(0, MembershipStatus.UNKNOWN, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, standard(45), false), "skill:f2p",
                skillF2p("skill:f2p", Skill.MINING, 35), membersReady("quest:members", 200, "Members quest.")));
        values.add(scenario("33 F2P blocks members upgrade",
                context(0, MembershipStatus.F2P, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, GoalType.GEAR_TARGET, standard(45), false), "skill:f2p",
                skillF2p("skill:f2p", Skill.FISHING, 35), membersReady("upgrade:members", 200, "Members upgrade.")));
        values.add(scenario("34 one Defence pure blocks Defence XP",
                context(0, MembershipStatus.P2P, StrategyMode.EFFICIENT,
                        SessionIntent.LONG_SESSION, GoalType.MAX, oneDefence(), false), "skill:agility",
                skill("skill:defence", Skill.DEFENCE, 200, AttentionLevel.ACTIVE, 2, 30, 1, 2),
                skill("skill:agility", Skill.AGILITY, 30, AttentionLevel.ACTIVE, 2, 30, 70, 71)));
        values.add(scenario("35 level three skiller blocks combat XP",
                context(0, MembershipStatus.P2P, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, skiller(), false), "skill:mining",
                skill("skill:attack", Skill.ATTACK, 200, AttentionLevel.ACTIVE, 2, 30, 1, 2),
                skill("skill:mining", Skill.MINING, 30, AttentionLevel.LOW, 2, 30, 50, 51)));
        values.add(scenario("36 Defence pure keeps Defence route",
                context(0, MembershipStatus.P2P, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, defencePure(), false), "skill:defence",
                skill("skill:attack", Skill.ATTACK, 200, AttentionLevel.ACTIVE, 2, 30, 1, 2),
                skill("skill:defence", Skill.DEFENCE, 30, AttentionLevel.LOW, 2, 30, 75, 76)));
        values.add(scenario("37 ten HP build chooses noncombat",
                context(0, MembershipStatus.P2P, StrategyMode.BALANCED,
                        SessionIntent.PICK_FOR_ME, GoalType.MAX, tenHp(), false), "skill:crafting",
                skill("skill:ranged", Skill.RANGED, 200, AttentionLevel.ACTIVE, 2, 30, 60, 61),
                skill("skill:crafting", Skill.CRAFTING, 30, AttentionLevel.LOW, 2, 30, 60, 61)));

        // Actionability, clue/STASH, transport, money and feedback behaviour.
        values.add(scenario("38 active clue can beat generic training", max, "clue:",
                ready("clue:pending", 53, "Clear the pending clue slot."),
                skill("skill:mining", Skill.MINING, 40, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("39 unknown STASH check cannot lead", max, "skill:",
                check("stash:unknown", 500, "Check the exact built state."),
                skill("skill:fishing", Skill.FISHING, 28, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("40 transport fanout beats isolated detour", max, "detour:transport",
                ready("detour:transport", 42, "Also advances multiple goals through a shared transport prerequisite and saves future time."),
                ready("money:generic", 45, "Earn GP.")));
        values.add(scenario("41 money is a tool when target needs it", max, "money:",
                ready("money:target-shortfall", 54, "Earn only the verified target shortfall."),
                skill("skill:firemaking", Skill.FIREMAKING, 39, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("42 money does not override valuable unlock", max, "quest:",
                ready("money:generic", 41, "Generic GP."),
                ready("quest:shared-unlock", 44, "Shared prerequisite saves future time.")));
        values.add(scenario("43 quest check loses to ready action", max, "skill:",
                check("quest:unknown", 500, "Unknown access."),
                skill("skill:mining", Skill.MINING, 25, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("44 PvM prepare check loses to ready action", max, "skill:",
                check("pvm:prepare", 500, "Verify mandatory protection."),
                skill("skill:fishing", Skill.FISHING, 25, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("45 unresolved gear price cannot lead", max, "skill:",
                check("upgrade:price-unknown", 500, "Verify price and GP."),
                skill("skill:agility", Skill.AGILITY, 25, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("46 ready recurring can rotate ordinary work", max, "opportunity:",
                ready("opportunity:birdhouse", 55, "Observed ready and time-sensitive."),
                skill("skill:mining", Skill.MINING, 39, AttentionLevel.LOW, 2, 20, 70, 71)));

        PreferenceProfile later = new PreferenceProfile();
        later.addTemporaryScoreAdjustment("skill:mining", -30, 60_000);
        values.add(scenario("47 Later rotates the exact action immediately",
                withPreferences(max, later), "skill:fishing",
                skill("skill:mining", Skill.MINING, 50 + later.timedScoreAdjustmentFor("skill:mining"), AttentionLevel.LOW, 2, 20, 70, 71),
                skill("skill:fishing", Skill.FISHING, 35, AttentionLevel.LOW, 2, 20, 70, 71)));
        PreferenceProfile dislike = new PreferenceProfile();
        dislike.apply("skill:mining", FeedbackAction.DISLIKE);
        values.add(scenario("48 Dislike rotates a durable negative preference",
                withPreferences(max, dislike), "skill:fishing",
                skill("skill:mining", Skill.MINING, 35 + dislike.weightFor("skill:mining") * 10, AttentionLevel.LOW, 2, 20, 70, 71),
                skill("skill:fishing", Skill.FISHING, 35, AttentionLevel.LOW, 2, 20, 70, 71)));
        values.add(scenario("50 unobserved Group Storage cannot make gear ready",
                accountContext(4), "skill:", check("upgrade:group-storage", 500, "Observe Group Storage first."),
                skill("skill:slayer", Skill.SLAYER, 28, AttentionLevel.ACTIVE, 2, 30, 70, 71)));
        values.add(scenario("51 irrelevant low candidate cannot perturb winner", max, "skill:mining",
                skill("skill:mining", Skill.MINING, 45, AttentionLevel.LOW, 2, 20, 70, 71),
                skill("skill:fishing", Skill.FISHING, 40, AttentionLevel.LOW, 2, 20, 70, 71),
                ready("money:irrelevant", -500, "Irrelevant state.")));
        return values;
    }

    private static Scenario goal(String name, GoalType goal, String expected,
            Recommendation... candidates)
    {
        return scenario(name, context(0, MembershipStatus.P2P,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME, goal,
                standard(70), false), expected, candidates);
    }

    private static Scenario scenario(String name, StrategyContext context,
            String expected, Recommendation... candidates)
    {
        return new Scenario(name, context, expected, Arrays.asList(candidates));
    }

    private static StrategyContext accountContext(int accountType)
    {
        return context(accountType, MembershipStatus.P2P,
                StrategyMode.BALANCED, SessionIntent.PICK_FOR_ME,
                GoalType.MAX, standard(70), false);
    }

    private static StrategyContext ctx(StrategyContext source, SessionIntent session)
    {
        return context(source.getData().getAccount().getAccountTypeCode(),
                source.getData().getAccount().getMembershipStatus(),
                source.getStrategyMode(), session, source.getActiveGoal(),
                source.getData().getAccount().getSkillLevels(), false);
    }

    private static StrategyContext withPreferences(StrategyContext source,
            PreferenceProfile preferences)
    {
        return new StrategyContext(source.getData(), source.getStrategyMode(),
                source.getSessionIntent(), QuestTolerance.NORMAL,
                source.getActiveGoal(), false, false, false, preferences);
    }

    private static StrategyContext context(int type, MembershipStatus membership,
            StrategyMode strategy, SessionIntent session, GoalType goal,
            Map<Skill, Integer> levels, boolean wilderness)
    {
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        int total = 0;
        long totalXp = 0;
        for (Skill skill : Skill.values())
        {
            int level = levels.getOrDefault(skill, 1);
            xp.put(skill, level <= 1 ? 0 : Experience.getXpForLevel(level));
            total += level;
            totalXp += xp.get(skill);
        }
        AccountSnapshot account = new AccountSnapshot("Scenario", 100L + type,
                type, AccountMode.fromTypeCode(type).name(), membership,
                membership == MembershipStatus.P2P ? 1 : 0, total, totalXp,
                levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .build();
        return new StrategyContext(data, strategy, session,
                QuestTolerance.NORMAL, goal, false, false, wilderness,
                new PreferenceProfile());
    }

    private static Recommendation skillF2p(String id, Skill skill, double score)
    {
        return skill(id, skill, score, AttentionLevel.LOW, 2, 20, 45, 46, true);
    }

    private static Recommendation skill(String id, Skill skill, double score,
            AttentionLevel attention, int setup, int minimum,
            int current, int target)
    {
        return skill(id, skill, score, attention, setup, minimum,
                current, target, false);
    }

    private static Recommendation skill(String id, Skill skill, double score,
            AttentionLevel attention, int setup, int minimum,
            int current, int target, boolean freeToPlay)
    {
        TrainingMethod method = new TrainingMethod(id + ":method", skill, 1,
                99, "Train " + skill.getName(), "Use the verified method.",
                1, 1, 1, attention, minimum, setup, Collections.emptyList(),
                RecommendationConfidence.VERIFIED);
        return new Recommendation(id, "Train " + skill.getName() + " to " + target,
                "Useful account progress.", score,
                new TrainingPlan(method, "Scenario method",
                        RecommendationConfidence.VERIFIED),
                RecommendationConfidence.VERIFIED, current, target, guidance(),
                CandidateSafetyEvidence.skill(freeToPlay, skill));
    }

    private static Recommendation ready(String id, double score, String reason)
    {
        return new Recommendation(id, title(id), reason, score, null,
                RecommendationConfidence.VERIFIED, 0, 0, guidance(),
                CandidateSafetyEvidence.harmless(true));
    }

    private static Recommendation membersReady(String id, double score,
            String reason)
    {
        return new Recommendation(id, title(id), reason, score, null,
                RecommendationConfidence.VERIFIED, 0, 0, guidance(),
                CandidateSafetyEvidence.harmless(false));
    }

    private static Recommendation irreversible(String id, double score,
            String reason)
    {
        return new Recommendation(id, title(id), reason, score, null,
                RecommendationConfidence.VERIFIED, 0, 0, guidance(),
                CandidateSafetyEvidence.potentiallyIrreversible(false));
    }

    private static Recommendation check(String id, double score, String reason)
    {
        return new Recommendation(id, title(id), reason, score, null,
                RecommendationConfidence.CHECK_NEEDED, 0, 0, guidance(),
                CandidateSafetyEvidence.harmless(true));
    }

    private static String title(String id)
    {
        return id.replace(':', ' ').replace('-', ' ');
    }

    private static RecommendationGuidance guidance()
    {
        return new RecommendationGuidance(
                "Follow the named route until the displayed target is complete.",
                "Verified: setup is available.", "Named scenario location.",
                "This advances the stated account goal.");
    }

    private static Map<Skill, Integer> standard(int level)
    {
        Map<Skill, Integer> values = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) values.put(skill, level);
        values.put(Skill.HITPOINTS, Math.max(10, level));
        return values;
    }

    private static Map<Skill, Integer> oneDefence()
    {
        Map<Skill, Integer> values = standard(70);
        values.put(Skill.DEFENCE, 1);
        return values;
    }

    private static Map<Skill, Integer> skiller()
    {
        Map<Skill, Integer> values = standard(50);
        for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH,
                Skill.DEFENCE, Skill.RANGED, Skill.PRAYER, Skill.MAGIC,
                Skill.SLAYER}) values.put(skill, 1);
        values.put(Skill.HITPOINTS, 10);
        return values;
    }

    private static Map<Skill, Integer> defencePure()
    {
        Map<Skill, Integer> values = standard(50);
        values.put(Skill.ATTACK, 1);
        values.put(Skill.STRENGTH, 1);
        values.put(Skill.RANGED, 1);
        values.put(Skill.MAGIC, 1);
        values.put(Skill.SLAYER, 1);
        values.put(Skill.DEFENCE, 75);
        values.put(Skill.PRAYER, 43);
        return values;
    }

    private static Map<Skill, Integer> tenHp()
    {
        Map<Skill, Integer> values = standard(60);
        values.put(Skill.HITPOINTS, 10);
        values.put(Skill.SLAYER, 1);
        return values;
    }

    private static final class Scenario
    {
        private final String name;
        private final StrategyContext context;
        private final String expectedPrefix;
        private final List<Recommendation> candidates;

        private Scenario(String name, StrategyContext context,
                String expectedPrefix, List<Recommendation> candidates)
        {
            this.name = name;
            this.context = context;
            this.expectedPrefix = expectedPrefix;
            this.candidates = candidates;
        }
    }
}

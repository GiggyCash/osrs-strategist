package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * High-value account upgrades that can interrupt raw skill XP when the route is
 * verified, build-safe, and valuable enough for the current account.
 *
 * <p>Every VERIFIED non-skill candidate in this provider includes concrete
 * guidance. This is important because ActionabilityPolicy will
 * not allow an attractive-sounding upgrade with no executable action to steal
 * the primary DO NEXT slot.</p>
 */
@Singleton
public class ProgressionUpgradeCandidateProvider
        implements CandidateProvider
{
    @Override
    public String getId()
    {
        return "progression-upgrades";
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return result;
        }

        GameData data = context.data();
        AccountSnapshot account = data.account();
        ItemIndex items = new ItemIndex(
                data, context.isUseGroupStorage());

        fighterTorso(context, account, items, result);
        abyssalWhip(context, account, items, result);
        dragonDefender(context, account, items, result);
        dragonScimitar(context, account, items, result);
        avaDevice(context, account, items, result);
        barrowsGloves(context, account, items, result);
        bowfaRoute(context, account, items, result);
        anglerOutfit(context, account, items, result);
        questRewardGear(context, account, items, result);

        result.sort(Comparator.comparingDouble(
                Recommendation::getScore).reversed());
        return result;
    }

    private static void questRewardGear(StrategyContext context,
            AccountSnapshot account, ItemIndex items,
            List<Recommendation> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || !ownershipCanBeJudged(account, items)
                || context.data().quests() == null) return;

        addQuestRewardGear(context, items, result,
                "salve-amulet", "Salve amulet", "Haunted Mine",
                Text.get(453),
                Text.get(464),
                Text.get(475),
                items.has("Chisel"));
        addQuestRewardGear(context, items, result,
                "helm-of-neitiznot", "Helm of neitiznot", Text.get(1394),
                Text.get(486),
                Text.get(497),
                Text.get(508), false);
        addQuestRewardGear(context, items, result,
                "ibans-staff", "Iban's staff", "Underground Pass",
                Text.get(519),
                Text.get(530),
                Text.get(541), false);
    }

    private static void addQuestRewardGear(StrategyContext context,
            ItemIndex items, List<Recommendation> result,
            String suffix, String item, String quest, String action,
            String supplies, String note, boolean ready)
    {
        if (context.data().quests().statusOf(quest) != QuestStatus.COMPLETE
                || items.has(item)) return;
        String id = "upgrade:" + suffix;
        if (context.preferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.accountMode();
        boolean retrievalOnly = mode == AccountMode.ULTIMATE_IRONMAN
                && items.restrictedQuantity(item) > 0;
        String uim = mode == AccountMode.ULTIMATE_IRONMAN
                ? Text.get(454)
                : "";
        String nextAction = retrievalOnly
                ? Text.get(455)
                        + item + Text.get(456)
                : action;
        result.add(new Recommendation(id,
                (retrievalOnly ? "Retrieve " : "Recover ") + item,
                quest + Text.get(457),
                34.0 + preference(context, id), ready && !retrievalOnly
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED,
                new Guidance(nextAction, supplies + uim,
                        Text.get(458), note),
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void dragonScimitar(StrategyContext context,
            AccountSnapshot account, ItemIndex items,
            List<Recommendation> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || account.getSkillLevel(Skill.ATTACK) < 60
                || !AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                || !ownershipCanBeJudged(account, items)
                || ownsObserved(account, items, "Dragon scimitar",
                        "Abyssal whip", "Blade of saeldor", Text.get(1395))) return;
        QuestSnapshot quests = context.data().quests();
        if (quests == null || quests.statusOf("Monkey Madness I")
                != QuestStatus.COMPLETE) return;
        String id = "upgrade:dragon-scimitar";
        if (context.preferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.accountMode();
        String setup = mode == AccountMode.ULTIMATE_IRONMAN
                ? Text.get(459)
                : Text.get(460);
        boolean cashReady = verifiedCoins(context.data(), 100_000L);
        result.add(new Recommendation(id, Text.get(1396),
                Text.get(461),
                42.0 + preference(context, id), cashReady
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED,
                new Guidance(
                        cashReady
                                ? Text.get(462)
                                : Text.get(463),
                        setup + (cashReady ? Text.get(1397) : Text.get(465))
                                + Text.get(466),
                        Text.get(467),
                        Text.get(468)),
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void avaDevice(StrategyContext context,
            AccountSnapshot account, ItemIndex items,
            List<Recommendation> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || account.getSkillLevel(Skill.RANGED) < 30
                || !AccountBuildPolicy.allowsSkill(account, Skill.RANGED)
                || !ownershipCanBeJudged(account, items)
                || ownsObserved(account, items, "Ava's attractor", "Ava's accumulator",
                        "Ava's assembler", "Masori assembler", "Dizana's quiver")) return;
        QuestSnapshot quests = context.data().quests();
        if (quests == null || quests.statusOf("Animal Magnetism")
                != QuestStatus.COMPLETE) return;
        String id = "upgrade:ava-device";
        if (context.preferenceProfile().isOnCooldown(id)) return;
        String device = account.getSkillLevel(Skill.RANGED) >= 50
                ? "Ava's accumulator" : "Ava's attractor";
        String replacement = account.getSkillLevel(Skill.RANGED) >= 50
                ? Text.get(469)
                : Text.get(470);
        boolean arrowsReady = account.getSkillLevel(Skill.RANGED) < 50
                || items.quantity("Steel arrow") >= 75;
        boolean replacementReady = verifiedCoins(context.data(), 999L)
                && arrowsReady;
        result.add(new Recommendation(id, "Get " + device,
                Text.get(471),
                40.0 + preference(context, id), replacementReady
                        ? Confidence.VERIFIED
                        : Confidence.CHECK_NEEDED,
                new Guidance(
                        replacementReady
                                ? Text.get(472) + device + Text.get(473)
                                : Text.get(474) + device + ".",
                        replacement + (replacementReady
                                ? Text.get(476)
                                : Text.get(477)),
                        Text.get(478),
                        Text.get(479)),
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void fighterTorso(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        if (account.getSkillLevel(Skill.DEFENCE) < 40) return;

        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        boolean defencePure = build == RestrictedBuildType.DEFENCE_PURE;
        if (!defencePure && Math.max(account.getSkillLevel(Skill.ATTACK),
                account.getSkillLevel(Skill.STRENGTH)) < 40)
        {
            return;
        }

        if (build == RestrictedBuildType.SKILLER
                || build == RestrictedBuildType.F2P_SKILLER
                || build == RestrictedBuildType.PRAYER_SKILLER
                || build == RestrictedBuildType.TEN_HITPOINTS)
        {
            return;
        }

        if (!ownershipCanBeJudged(account, items)) return;
        if (ownsObserved(account, items,
                "Fighter torso", "Fighter torso (l)",
                "Bandos chestplate", Text.get(1398),
                "Torva platebody", Text.get(1399)))
        {
            return;
        }

        String id = "upgrade:fighter-torso";
        if (context.preferenceProfile().isOnCooldown(id)) return;

        AccountMode mode = context.accountMode();
        double score = mode.isIronLike() ? 48.0 : 37.0;
        if (defencePure) score += 8.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 10.0;
        }
        score += preference(context, id);

        String buildNote = defencePure
                ? Text.get(480)
                : Text.get(481);
        Guidance guidance = new Guidance(
                Text.get(482),
                Text.get(483) + buildNote,
                Text.get(484),
                Text.get(485)
        );

        result.add(new Recommendation(
                id,
                Text.get(1400),
                Text.get(487),
                score,
                Confidence.VERIFIED,
                guidance,
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void abyssalWhip(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        if (account.getSkillLevel(Skill.ATTACK) < 70) return;
        if (!AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)) return;
        if (!ownershipCanBeJudged(account, items)) return;

        if (ownsObserved(account, items,
                "Abyssal whip", "Abyssal whip (or)", "Abyssal tentacle",
                "Blade of saeldor", Text.get(1395),
                "Ghrazi rapier", "Osmumten's fang",
                "Soulreaper axe", "Scythe of vitur"))
        {
            return;
        }

        String id = "upgrade:abyssal-whip";
        if (context.preferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.accountMode();
        int slayer = account.getSkillLevel(Skill.SLAYER);
        double score;
        String title;
        String reason;
        Confidence confidence;
        Guidance guidance;

        if (mode.usesGrandExchange())
        {
            score = 41.0;
            title = Text.get(1401);
            reason = Text.get(488);
            confidence = Confidence.CHECK_NEEDED;
            guidance = new Guidance(
                    Text.get(489),
                    Text.get(490),
                    "Grand Exchange.",
                    Text.get(491)
            );
        }
        else if (slayer >= 85)
        {
            score = 49.0;
            title = Text.get(1401);
            reason = Text.get(492);
            confidence = Confidence.CHECK_NEEDED;
            guidance = new Guidance(
                    Text.get(493),
                    Text.get(494),
                    Text.get(495),
                    Text.get(496)
            );
        }
        else
        {
            if (context.getActiveGoal() != GoalType.MAX
                    && context.getActiveGoal() != GoalType.SLAYER_85
                    && context.getActiveGoal() != GoalType.GEAR_TARGET
                    && context.getActiveGoal() != GoalType.RAID_READY)
            {
                return;
            }
            int remaining = 85 - slayer;
            score = Math.max(24.0, 42.0 - remaining * 0.8);
            title = Text.get(1402);
            reason = Text.get(498);
            confidence = Confidence.VERIFIED;
            guidance = new Guidance(
                    Text.get(1403) + slayer + Text.get(499),
                    Text.get(500),
                    Text.get(501),
                    Text.get(502)
            );
        }

        score += preference(context, id);
        result.add(new Recommendation(
                id, title, reason, score, confidence, guidance,
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void dragonDefender(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        if (!ownershipCanBeJudged(account, items)) return;

        if (account.getSkillLevel(Skill.DEFENCE) < 60) return;
        if (account.getSkillLevel(Skill.ATTACK) < 60) return;
        if (!AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                || !AccountBuildPolicy.allowsSkill(account, Skill.STRENGTH))
        {
            return;
        }

        int attack = account.getSkillLevel(Skill.ATTACK);
        int strength = account.getSkillLevel(Skill.STRENGTH);
        if (attack < 99 && strength < 99 && attack + strength < 130) return;

        if (ownsObserved(account, items,
                "Dragon defender", Text.get(1404),
                "Avernic defender", Text.get(1405)))
        {
            return;
        }

        String id = "upgrade:dragon-defender";
        if (context.preferenceProfile().isOnCooldown(id)) return;
        double score = 45.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 8.0;
        }
        score += preference(context, id);

        Guidance guidance = new Guidance(
                Text.get(503),
                Text.get(504),
                Text.get(505),
                Text.get(506)
        );
        result.add(new Recommendation(
                id,
                Text.get(1406),
                Text.get(507),
                score,
                Confidence.VERIFIED,
                guidance,
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void barrowsGloves(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P) return;
        QuestSnapshot quests = context.data().quests();
        if (quests == null
                || quests.statusOf(Text.get(1198)) != QuestStatus.COMPLETE)
        {
            return;
        }
        if (!ownershipCanBeJudged(account, items)) return;
        if (ownsObserved(account, items,
                "Barrows gloves", "Ferocious gloves", "Zaryte vambraces"))
        {
            return;
        }

        String id = "upgrade:barrows-gloves";
        if (context.preferenceProfile().isOnCooldown(id)) return;

        DiarySnapshot diaries = context.data().diaries();
        boolean eliteLumbridge = diaries != null
                && diaries.isTierComplete(Text.get(1152), DiaryTier.ELITE);
        long price = eliteLumbridge ? 104_000L : 130_000L;
        AccountEconomySnapshot economy = context.data().economy();
        boolean cashVerified = economy != null
                && economy.getConfidence() == Confidence.VERIFIED;
        boolean affordable = cashVerified && economy.getCoins() >= price;

        double score = 48.0;
        if (context.getActiveGoal() == GoalType.BARROWS_GLOVES) score += 35.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY) score += 10.0;
        score += preference(context, id);

        Confidence confidence = affordable
                ? Confidence.VERIFIED
                : Confidence.CHECK_NEEDED;
        String supplies;
        if (!cashVerified)
        {
            supplies = Text.get(509) + format(price) + " coins.";
        }
        else if (!affordable)
        {
            supplies = "You have " + format(economy.getCoins())
                    + Text.get(1407) + format(price)
                    + ". You are " + format(price - economy.getCoins())
                    + " coins short.";
        }
        else
        {
            supplies = Text.get(1408) + format(price)
                    + " coin shop price.";
        }

        Guidance guidance = new Guidance(
                Text.get(510),
                supplies,
                Text.get(511),
                eliteLumbridge
                        ? Text.get(512)
                        : Text.get(513)
        );
        result.add(new Recommendation(
                id,
                Text.get(1409),
                Text.get(514),
                score,
                confidence,
                guidance,
                SafetyEvidence.verifiedSafe(false)));
    }

    private static void bowfaRoute(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P) return;
        if (context.getActiveGoal() != GoalType.BOWFA
                && context.getActiveGoal() != GoalType.GEAR_TARGET
                && context.getActiveGoal() != GoalType.RAID_READY)
        {
            return;
        }
        QuestSnapshot quests = context.data().quests();
        if (quests == null
                || quests.statusOf("Song of the Elves") != QuestStatus.COMPLETE)
        {
            return;
        }
        if (!ownershipCanBeJudged(account, items)) return;
        if (ownsObserved(account, items,
                "Bow of faerdhinen", Text.get(1345)))
        {
            return;
        }

        String id = "upgrade:bowfa";
        if (context.preferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.accountMode();
        boolean seedOwned = ownsObserved(account, items,
                Text.get(1410));
        int shards = items.quantity("Crystal shard");
        int smithing = account.getSkillLevel(Skill.SMITHING);
        int crafting = account.getSkillLevel(Skill.CRAFTING);
        double score = context.getActiveGoal() == GoalType.BOWFA ? 78.0 : 54.0;
        Confidence confidence;
        Guidance guidance;
        String title;
        String reason;

        if (seedOwned)
        {
            boolean canSelfSing = smithing >= 82 && crafting >= 82;
            int neededShards = canSelfSing ? 100 : 150;
            int shortfall = Math.max(0, neededShards - shards);
            confidence = shortfall == 0
                    ? Confidence.VERIFIED
                    : Confidence.CHECK_NEEDED;
            title = Text.get(1411);
            reason = Text.get(515);
            String action = canSelfSing
                    ? Text.get(516) + neededShards
                            + Text.get(517)
                    : Text.get(518)
                            + neededShards + Text.get(520);
            String supplies = shortfall == 0
                    ? Text.get(521)
                            + neededShards + Text.get(1412)
                    : Text.get(1413) + shortfall
                            + Text.get(1414) + (shortfall == 1 ? "" : "s")
                            + Text.get(1415);
            guidance = new Guidance(
                    action,
                    supplies,
                    Text.get(522),
                    Text.get(523)
            );
        }
        else if (mode.usesGrandExchange())
        {
            confidence = Confidence.CHECK_NEEDED;
            title = Text.get(1416);
            reason = Text.get(524);
            guidance = new Guidance(
                    Text.get(525),
                    Text.get(526),
                    Text.get(527),
                    Text.get(528)
            );
        }
        else
        {
            AccountMode accountMode = context.accountMode();
            boolean hardcore = accountMode == AccountMode.HARDCORE_IRONMAN
                    || accountMode == AccountMode.HARDCORE_GROUP_IRONMAN;
            boolean uimDeathStorage = accountMode == AccountMode.ULTIMATE_IRONMAN
                    && hasDeathStorage(context.data().storage());
            confidence = hardcore || uimDeathStorage
                    ? Confidence.CHECK_NEEDED
                    : Confidence.VERIFIED;
            title = Text.get(1417);
            reason = Text.get(529);
            guidance = new Guidance(
                    Text.get(531),
                    Text.get(532),
                    Text.get(1418),
                    hardcore
                            ? Text.get(533)
                            : uimDeathStorage
                            ? Text.get(534)
                            : Text.get(535)
            );
        }

        score += preference(context, id);
        result.add(new Recommendation(
                id, title, reason, score, confidence, guidance,
                SafetyEvidence.potentiallyIrreversible(false)));
    }

    private static void anglerOutfit(
            StrategyContext context,
            AccountSnapshot account,
            ItemIndex items,
            List<Recommendation> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        int fishing = account.getSkillLevel(Skill.FISHING);
        if (fishing < 15) return;
        if (!ownershipCanBeJudged(account, items)) return;

        int pieces = anglerPieces(account, items);
        if (pieces >= 4) return;

        String id = "upgrade:angler-outfit";
        if (context.preferenceProfile().isOnCooldown(id)) return;

        int currentXp = account.getSkillExperience(Skill.FISHING);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(fishing);
        int remainingXp = Math.max(0,
                Experience.getXpForLevel(99) - currentXp);

        double score = 16.0;
        if (context.isCollectionistMode()) score += 30.0;
        if (fishing >= 82) score += 17.0;
        if (context.getActiveGoal() == GoalType.MAX && remainingXp >= 5_000_000)
            score += 12.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET) score += 5.0;
        score += pieces * 2.0;
        score += preference(context, id);

        if (score < 25.0) return;
        Guidance guidance = new Guidance(
                Text.get(536),
                Text.get(537),
                Text.get(538),
                Text.get(1419) + pieces + Text.get(539)
        );
        result.add(new Recommendation(
                id,
                Text.get(1420) + pieces + "/4)",
                Text.get(540),
                score,
                Confidence.VERIFIED,
                guidance,
                SafetyEvidence.skill(false, Skill.FISHING)));
    }

    private static int anglerPieces(
            AccountSnapshot account,
            ItemIndex items)
    {
        int pieces = 0;
        if (ownsObserved(account, items, "Angler hat", Text.get(1214))) pieces++;
        if (ownsObserved(account, items, "Angler top", "Spirit angler top")) pieces++;
        if (ownsObserved(account, items, "Angler waders", Text.get(1215))) pieces++;
        if (ownsObserved(account, items, "Angler boots", Text.get(1216))) pieces++;
        return pieces;
    }

    private static boolean ownershipCanBeJudged(
            AccountSnapshot account,
            ItemIndex items)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        return items.usableOwnershipObserved();
    }

    /** UIM retrieval-only storage still proves ownership even when not usable now. */
    private static boolean ownsObserved(
            AccountSnapshot account,
            ItemIndex items,
            String... names)
    {
        if (items.has(names)) return true;
        if (AccountMode.fromTypeCode(account.getAccountTypeCode())
                != AccountMode.ULTIMATE_IRONMAN)
        {
            return false;
        }
        for (String name : names)
        {
            if (items.restrictedQuantity(name) > 0) return true;
        }
        return false;
    }

    private static boolean hasDeathStorage(StorageSnapshot storage)
    {
        return storage != null
                && storage.getDeathStorageItems() != null
                && !storage.getDeathStorageItems().isEmpty();
    }

    private static double preference(StrategyContext context, String id)
    {
        return context.preferenceProfile().weightFor(id) * 10.0;
    }

    private static boolean verifiedCoins(GameData data, long needed)
    {
        AccountEconomySnapshot economy = data == null ? null : data.economy();
        return economy != null
                && economy.getConfidence() == Confidence.VERIFIED
                && economy.getCoins() >= needed;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}

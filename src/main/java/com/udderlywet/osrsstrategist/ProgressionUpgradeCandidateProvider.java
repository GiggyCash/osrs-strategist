package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Build-safe, account-aware progression upgrades that can interrupt raw XP. */
@Singleton
public class ProgressionUpgradeCandidateProvider implements CandidateProvider
{
    @Override
    public String getId()
    {
        return "progression-upgrades";
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null)
        {
            return Collections.emptyList();
        }
        return new UpgradeScan(context).run();
    }

    /** Shared account evidence for every upgrade rule. */
    private static final class UpgradeScan
    {
        private final StrategyContext context;
        private final GameData data;
        private final AccountSnapshot account;
        private final AccountMode mode;
        private final ItemIndex items;
        private final List<Recommendation> result = new ArrayList<>();

        private UpgradeScan(StrategyContext context)
        {
            this.context = context;
            data = context.data();
            account = data.account();
            mode = context.accountMode();
            items = new ItemIndex(data, context.isUseGroupStorage());
        }

        private List<Recommendation> run()
        {
            fighterTorso();
            abyssalWhip();
            dragonDefender();
            dragonScimitar();
            avaDevice();
            barrowsGloves();
            bowfa();
            anglerOutfit();
            questRewardGear();
            result.sort(Comparator.comparingDouble(
                    Recommendation::getScore).reversed());
            return result;
        }

        private boolean members()
        {
            return ContentAccessRules.hasVerifiedMembership(
                    account.getMembershipStatus());
        }

        private boolean owns(String... names)
        {
            if (items.has(names)) return true;
            if (mode != AccountMode.ULTIMATE_IRONMAN) return false;
            for (String name : names)
            {
                if (items.restrictedQuantity(name) > 0) return true;
            }
            return false;
        }

        private boolean eligible(String id)
        {
            return items.usableOwnershipObserved()
                    && !context.preferenceProfile().isOnCooldown(id);
        }

        private void add(String id, String title, String reason, double score,
                Confidence confidence, Guidance guidance,
                SafetyEvidence safety)
        {
            result.add(new Recommendation(id, title, reason,
                    score + context.preferenceProfile().weightFor(id) * 10.0,
                    confidence, guidance, safety));
        }

        private boolean questComplete(String quest)
        {
            return data.quests() != null
                    && data.quests().statusOf(quest) == QuestStatus.COMPLETE;
        }

        private void questRewardGear()
        {
            if (!members() || data.quests() == null) return;
            questReward("salve-amulet", "Salve amulet", "Haunted Mine",
                    get(453), get(464), get(475),
                    items.has("Chisel"));
            questReward("helm-of-neitiznot", "Helm of neitiznot",
                    get(1394), get(486), get(497),
                    get(508), false);
            questReward("ibans-staff", "Iban's staff", "Underground Pass",
                    get(519), get(530), get(541), false);
        }

        private void questReward(String suffix, String item, String quest,
                String action, String supplies, String note, boolean ready)
        {
            String id = "upgrade:" + suffix;
            // Retrieval-only UIM storage proves the item exists but does not
            // make it usable now; emit the retrieval action in that case.
            if (!eligible(id) || !questComplete(quest) || items.has(item)) return;
            boolean retrieval = mode == AccountMode.ULTIMATE_IRONMAN
                    && items.restrictedQuantity(item) > 0;
            add(id, (retrieval ? "Retrieve " : "Recover ") + item,
                    quest + get(457), 34.0,
                    ready && !retrieval ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    new Guidance(retrieval
                            ? get(455) + item + get(456) : action,
                            supplies + (mode == AccountMode.ULTIMATE_IRONMAN
                                    ? get(454) : ""),
                            get(458), note),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void dragonScimitar()
        {
            String id = "upgrade:dragon-scimitar";
            if (!members() || account.getSkillLevel(Skill.ATTACK) < 60
                    || !AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                    || !eligible(id) || !questComplete("Monkey Madness I")
                    || owns("Dragon scimitar", "Abyssal whip",
                            "Blade of saeldor", get(1395))) return;
            boolean cash = verifiedCoins(100_000L);
            String setup = mode == AccountMode.ULTIMATE_IRONMAN
                    ? get(459) : get(460);
            add(id, get(1396), get(461), 42.0,
                    cash ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(cash ? get(462) : get(463),
                            setup + (cash ? get(1397) : get(465))
                                    + get(466),
                            get(467), get(468)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void avaDevice()
        {
            String id = "upgrade:ava-device";
            int ranged = account.getSkillLevel(Skill.RANGED);
            if (!members() || ranged < 30
                    || !AccountBuildPolicy.allowsSkill(account, Skill.RANGED)
                    || !eligible(id) || !questComplete("Animal Magnetism")
                    || owns("Ava's attractor", "Ava's accumulator",
                            "Ava's assembler", "Masori assembler",
                            "Dizana's quiver")) return;
            String device = ranged >= 50
                    ? "Ava's accumulator" : "Ava's attractor";
            String replacement = ranged >= 50 ? get(469) : get(470);
            boolean ready = verifiedCoins(999L)
                    && (ranged < 50 || items.quantity("Steel arrow") >= 75);
            add(id, "Get " + device, get(471), 40.0,
                    ready ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(ready
                            ? get(472) + device + get(473)
                            : get(474) + device + ".",
                            replacement + (ready ? get(476)
                                    : get(477)),
                            get(478), get(479)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void fighterTorso()
        {
            String id = "upgrade:fighter-torso";
            RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
            boolean defencePure = build == RestrictedBuildType.DEFENCE_PURE;
            boolean protectedBuild = build == RestrictedBuildType.SKILLER
                    || build == RestrictedBuildType.F2P_SKILLER
                    || build == RestrictedBuildType.PRAYER_SKILLER
                    || build == RestrictedBuildType.TEN_HITPOINTS;
            if (!members() || account.getSkillLevel(Skill.DEFENCE) < 40
                    || protectedBuild
                    || (!defencePure && Math.max(
                            account.getSkillLevel(Skill.ATTACK),
                            account.getSkillLevel(Skill.STRENGTH)) < 40)
                    || !eligible(id)
                    || owns("Fighter torso", "Fighter torso (l)",
                            "Bandos chestplate", get(1398),
                            "Torva platebody", get(1399))) return;
            double score = mode.isIronLike() ? 48.0 : 37.0;
            if (defencePure) score += 8.0;
            if (goalIs(GoalType.GEAR_TARGET, GoalType.RAID_READY)) score += 10.0;
            add(id, get(1400), get(487), score,
                    Confidence.VERIFIED,
                    new Guidance(get(482), get(483)
                            + (defencePure ? get(480) : get(481)),
                            get(484), get(485)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void abyssalWhip()
        {
            String id = "upgrade:abyssal-whip";
            int attack = account.getSkillLevel(Skill.ATTACK);
            if (!members() || attack < 70
                    || !AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                    || !eligible(id)
                    || owns("Abyssal whip", "Abyssal whip (or)",
                            "Abyssal tentacle", "Blade of saeldor",
                            get(1395), "Ghrazi rapier", "Osmumten's fang",
                            "Soulreaper axe", "Scythe of vitur")) return;
            int slayer = account.getSkillLevel(Skill.SLAYER);
            if (mode.usesGrandExchange())
            {
                add(id, get(1401), get(488), 41.0,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(489), get(490),
                                "Grand Exchange.", get(491)),
                        SafetyEvidence.verifiedSafe(false));
                return;
            }
            if (slayer >= 85)
            {
                add(id, get(1401), get(492), 49.0,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(493), get(494),
                                get(495), get(496)),
                        SafetyEvidence.verifiedSafe(false));
                return;
            }
            if (!goalIs(GoalType.MAX, GoalType.SLAYER_85,
                    GoalType.GEAR_TARGET, GoalType.RAID_READY)) return;
            add(id, get(1402), get(498),
                    Math.max(24.0, 42.0 - (85 - slayer) * 0.8),
                    Confidence.VERIFIED,
                    new Guidance(get(1403) + slayer + get(499),
                            get(500), get(501), get(502)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void dragonDefender()
        {
            String id = "upgrade:dragon-defender";
            int attack = account.getSkillLevel(Skill.ATTACK);
            int strength = account.getSkillLevel(Skill.STRENGTH);
            if (!members() || !eligible(id)
                    || account.getSkillLevel(Skill.DEFENCE) < 60 || attack < 60
                    || !AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                    || !AccountBuildPolicy.allowsSkill(account, Skill.STRENGTH)
                    || (attack < 99 && strength < 99 && attack + strength < 130)
                    || owns("Dragon defender", get(1404),
                            "Avernic defender", get(1405))) return;
            double score = 45.0 + (goalIs(GoalType.GEAR_TARGET,
                    GoalType.RAID_READY) ? 8.0 : 0.0);
            add(id, get(1406), get(507), score,
                    Confidence.VERIFIED,
                    new Guidance(get(503), get(504),
                            get(505), get(506)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void barrowsGloves()
        {
            String id = "upgrade:barrows-gloves";
            if (!members() || !eligible(id) || !questComplete(get(1198))
                    || owns("Barrows gloves", "Ferocious gloves",
                            "Zaryte vambraces")) return;
            boolean elite = data.diaries() != null
                    && data.diaries().isTierComplete(get(1152),
                            DiaryTier.ELITE);
            long price = elite ? 104_000L : 130_000L;
            AccountEconomySnapshot economy = data.economy();
            boolean known = economy != null
                    && economy.getConfidence() == Confidence.VERIFIED;
            boolean affordable = known && economy.getCoins() >= price;
            String supplies = !known
                    ? get(509) + format(price) + " coins."
                    : !affordable
                    ? "You have " + format(economy.getCoins())
                            + get(1407) + format(price) + ". You are "
                            + format(price - economy.getCoins()) + " coins short."
                    : get(1408) + format(price) + " coin shop price.";
            double score = 48.0;
            if (goalIs(GoalType.BARROWS_GLOVES)) score += 35.0;
            if (goalIs(GoalType.GEAR_TARGET, GoalType.RAID_READY)) score += 10.0;
            add(id, get(1409), get(514), score,
                    affordable ? Confidence.VERIFIED : Confidence.CHECK_NEEDED,
                    new Guidance(get(510), supplies, get(511),
                            elite ? get(512) : get(513)),
                    SafetyEvidence.verifiedSafe(false));
        }

        private void bowfa()
        {
            String id = "upgrade:bowfa";
            if (!members() || !goalIs(GoalType.BOWFA, GoalType.GEAR_TARGET,
                    GoalType.RAID_READY) || !questComplete("Song of the Elves")
                    || !eligible(id)
                    || owns("Bow of faerdhinen", get(1345))) return;
            boolean seed = owns(get(1410));
            int shards = items.quantity("Crystal shard");
            double score = goalIs(GoalType.BOWFA) ? 78.0 : 54.0;
            if (seed)
            {
                boolean selfSing = account.getSkillLevel(Skill.SMITHING) >= 82
                        && account.getSkillLevel(Skill.CRAFTING) >= 82;
                int needed = selfSing ? 100 : 150;
                int shortfall = Math.max(0, needed - shards);
                add(id, get(1411), get(515), score,
                        shortfall == 0 ? Confidence.VERIFIED
                                : Confidence.CHECK_NEEDED,
                        new Guidance(selfSing
                                ? get(516) + needed + get(517)
                                : get(518) + needed + get(520),
                                shortfall == 0
                                ? get(521) + needed + get(1412)
                                : get(1413) + shortfall + get(1414)
                                        + (shortfall == 1 ? "" : "s")
                                        + get(1415),
                                get(522), get(523)),
                        SafetyEvidence.potentiallyIrreversible(false));
                return;
            }
            if (mode.usesGrandExchange())
            {
                add(id, get(1416), get(524), score,
                        Confidence.CHECK_NEEDED,
                        new Guidance(get(525), get(526),
                                get(527), get(528)),
                        SafetyEvidence.potentiallyIrreversible(false));
                return;
            }
            boolean hardcore = mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN;
            boolean deathStorage = mode == AccountMode.ULTIMATE_IRONMAN
                    && data.storage() != null
                    && !data.storage().getDeathStorageItems().isEmpty();
            add(id, get(1417), get(529), score,
                    hardcore || deathStorage ? Confidence.CHECK_NEEDED
                            : Confidence.VERIFIED,
                    new Guidance(get(531), get(532), get(1418),
                            hardcore ? get(533)
                                    : deathStorage ? get(534)
                                    : get(535)),
                    SafetyEvidence.potentiallyIrreversible(false));
        }

        private void anglerOutfit()
        {
            String id = "upgrade:angler-outfit";
            int fishing = account.getSkillLevel(Skill.FISHING);
            if (!members() || fishing < 15 || !eligible(id)) return;
            int pieces = (owns("Angler hat", get(1214)) ? 1 : 0)
                    + (owns("Angler top", "Spirit angler top") ? 1 : 0)
                    + (owns("Angler waders", get(1215)) ? 1 : 0)
                    + (owns("Angler boots", get(1216)) ? 1 : 0);
            if (pieces >= 4) return;
            int xp = account.getSkillExperience(Skill.FISHING);
            if (xp <= 0) xp = Experience.getXpForLevel(fishing);
            int remaining = Math.max(0, Experience.getXpForLevel(99) - xp);
            double score = 16.0 + (context.isCollectionistMode() ? 30.0 : 0.0)
                    + (fishing >= 82 ? 17.0 : 0.0)
                    + (goalIs(GoalType.MAX) && remaining >= 5_000_000
                            ? 12.0 : 0.0)
                    + (goalIs(GoalType.GEAR_TARGET) ? 5.0 : 0.0)
                    + pieces * 2.0;
            if (score + context.preferenceProfile().weightFor(id) * 10.0
                    < 25.0) return;
            add(id, get(1420) + pieces + "/4)", get(540), score,
                    Confidence.VERIFIED,
                    new Guidance(get(536), get(537), get(538),
                            get(1419) + pieces + get(539)),
                    SafetyEvidence.skill(false, Skill.FISHING));
        }

        private boolean goalIs(GoalType... goals)
        {
            for (GoalType goal : goals)
            {
                if (context.getActiveGoal() == goal) return true;
            }
            return false;
        }

        private boolean verifiedCoins(long needed)
        {
            AccountEconomySnapshot economy = data.economy();
            return economy != null
                    && economy.getConfidence() == Confidence.VERIFIED
                    && economy.getCoins() >= needed;
        }
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}

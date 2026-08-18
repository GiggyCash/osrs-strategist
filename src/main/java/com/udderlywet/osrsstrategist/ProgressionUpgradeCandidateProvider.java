package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * High-value account upgrades that can interrupt raw skill XP when the route is
 * verified, build-safe, and valuable enough for the current account.
 *
 * <p>Every VERIFIED non-skill candidate in this provider includes concrete
 * guidance. This is important because RecommendationActionabilityPolicy will
 * not allow an attractive-sounding upgrade with no executable action to steal
 * the primary DO NEXT slot.</p>
 */
@Singleton
public class ProgressionUpgradeCandidateProvider
        implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "progression-upgrades";
    }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null)
        {
            return result;
        }

        StrategyDataBundle data = context.getData();
        AccountSnapshot account = data.getAccount();
        ObservedItemIndex items = new ObservedItemIndex(
                data, context.isUseGroupStorage());

        fighterTorso(context, account, items, result);
        abyssalWhip(context, account, items, result);
        dragonDefender(context, account, items, result);
        dragonScimitar(context, account, items, result);
        avaDevice(context, account, items, result);
        barrowsGloves(context, account, items, result);
        fireCape(context, account, items, result);
        bowfaRoute(context, account, items, result);
        anglerOutfit(context, account, items, result);
        questRewardGear(context, account, items, result);

        result.sort(Comparator.comparingDouble(
                StrategyCandidate::getScore).reversed());
        return result;
    }

    private static void questRewardGear(StrategyContext context,
            AccountSnapshot account, ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || !ownershipCanBeJudged(account, items)
                || context.getData().getQuests() == null) return;

        addQuestRewardGear(context, items, result,
                "salve-amulet", "Salve amulet", "Haunted Mine",
                "Take a chisel to the crystal outcrop at the bottom of the Abandoned Mine and cut a salve shard, then use the chisel on the shard.",
                "Bring a chisel and preserve enough inventory space for the shard and amulet.",
                "The amulet is useful against applicable undead targets; upgrades and encounter mechanics still need their own checks.",
                items.has("Chisel"));
        addQuestRewardGear(context, items, result,
                "helm-of-neitiznot", "Helm of neitiznot", "The Fremennik Isles",
                "Return to Mawnis Burowgar on Neitiznot and verify the current replacement requirement before paying or changing setup.",
                "The quest is complete, but replacement cost and spendable cash must be observed before calling the acquisition ready.",
                "This is a broadly useful melee helm, not a universal best choice.", false);
        addQuestRewardGear(context, items, result,
                "ibans-staff", "Iban's staff", "Underground Pass",
                "Talk to the Dark Mage in West Ardougne and verify the current replacement requirement before paying.",
                "The quest is complete, but replacement cost, spellbook, runes and UIM setup must be checked separately.",
                "Iban Blast can be useful progression Magic where the target and rune cost support it.", false);
    }

    private static void addQuestRewardGear(StrategyContext context,
            ObservedItemIndex items, List<StrategyCandidate> result,
            String suffix, String item, String quest, String action,
            String supplies, String note, boolean ready)
    {
        if (context.getData().getQuests().statusOf(quest) != QuestStatus.COMPLETE
                || items.has(item)) return;
        String id = "upgrade:" + suffix;
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        boolean retrievalOnly = mode == AccountMode.ULTIMATE_IRONMAN
                && items.restrictedQuantity(item) > 0;
        String uim = mode == AccountMode.ULTIMATE_IRONMAN
                ? " Check inventory space and retrieval/storage consequences before travelling."
                : "";
        String nextAction = retrievalOnly
                ? "Verify the current death or looting-bag state, then retrieve "
                        + item + " without destroying the active UIM setup."
                : action;
        result.add(new StrategyCandidate(id,
                (retrievalOnly ? "Retrieve " : "Recover ") + item,
                quest + " is complete and the item is not present in observed usable ownership.",
                34.0 + preference(context, id), ready && !retrievalOnly
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(nextAction, supplies + uim,
                        "Use the quest's established replacement or reacquisition location.", note),
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void dragonScimitar(StrategyContext context,
            AccountSnapshot account, ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || account.getSkillLevel(Skill.ATTACK) < 60
                || !AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)
                || !ownershipCanBeJudged(account, items)
                || ownsObserved(account, items, "Dragon scimitar",
                        "Abyssal whip", "Blade of saeldor", "Blade of saeldor (c)")) return;
        QuestSnapshot quests = context.getData().getQuests();
        if (quests == null || quests.statusOf("Monkey Madness I")
                != QuestStatus.COMPLETE) return;
        String id = "upgrade:dragon-scimitar";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        String setup = mode == AccountMode.ULTIMATE_IRONMAN
                ? "Keep one inventory space free and preserve any retrieval-heavy setup before travelling."
                : "Withdraw 100,000 coins only when ready to make the purchase.";
        boolean cashReady = verifiedCoins(context.getData(), 100_000L);
        result.add(new StrategyCandidate(id, "Buy a Dragon scimitar",
                "Monkey Madness I and 60 Attack are observed, making this a concrete slash progression purchase rather than a generic gear tier.",
                42.0 + preference(context, id), cashReady
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        cashReady
                                ? "Travel to Ape Atoll and buy a Dragon scimitar from Daga's Scimitar Smithy for 100,000 coins."
                                : "Verify or obtain 100,000 spendable coins, then travel to Daga's Scimitar Smithy on Ape Atoll.",
                        setup + (cashReady ? " Coin affordability is observed." : " Coin affordability is not yet observed.")
                                + " This is the same shop route for Main and Iron accounts; no Grand Exchange assumption is needed.",
                        "Daga's Scimitar Smithy on Ape Atoll after Monkey Madness I.",
                        "Use it where a one-handed slash weapon is appropriate; it is not a universal encounter weapon."),
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void avaDevice(StrategyContext context,
            AccountSnapshot account, ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P
                || account.getSkillLevel(Skill.RANGED) < 30
                || !AccountBuildPolicy.allowsSkill(account, Skill.RANGED)
                || !ownershipCanBeJudged(account, items)
                || ownsObserved(account, items, "Ava's attractor", "Ava's accumulator",
                        "Ava's assembler", "Masori assembler", "Dizana's quiver")) return;
        QuestSnapshot quests = context.getData().getQuests();
        if (quests == null || quests.statusOf("Animal Magnetism")
                != QuestStatus.COMPLETE) return;
        String id = "upgrade:ava-device";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        String device = account.getSkillLevel(Skill.RANGED) >= 50
                ? "Ava's accumulator" : "Ava's attractor";
        String replacement = account.getSkillLevel(Skill.RANGED) >= 50
                ? "Bring 999 coins and 75 steel arrows for a replacement accumulator."
                : "Bring 999 coins for a replacement attractor.";
        boolean arrowsReady = account.getSkillLevel(Skill.RANGED) < 50
                || items.quantity("Steel arrow") >= 75;
        boolean replacementReady = verifiedCoins(context.getData(), 999L)
                && arrowsReady;
        result.add(new StrategyCandidate(id, "Get " + device,
                "Animal Magnetism is complete and no Ava device is observed; this is a reusable Ranged cape-slot and ammunition-recovery upgrade.",
                40.0 + preference(context, id), replacementReady
                        ? RecommendationConfidence.VERIFIED
                        : RecommendationConfidence.CHECK_NEEDED,
                new RecommendationGuidance(
                        replacementReady
                                ? "Talk to Ava in Draynor Manor and obtain " + device + ". Equip it for compatible Ranged setups."
                                : "Obtain the replacement materials, then talk to Ava in Draynor Manor for " + device + ".",
                        replacement + (replacementReady
                                ? " The required coins and arrows are observed."
                                : " The complete replacement cost is not yet observed."),
                        "Ava's room in the west wing of Draynor Manor.",
                        "Weapon and ammunition compatibility still matter; owning the device does not prove a complete encounter loadout."),
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void fighterTorso(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
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
                "Bandos chestplate", "Blood moon chestplate",
                "Torva platebody", "Torva platebody (damaged)"))
        {
            return;
        }

        String id = "upgrade:fighter-torso";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;

        AccountMode mode = context.getAccountMode();
        double score = mode.isIronLike() ? 48.0 : 37.0;
        if (defencePure) score += 8.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 10.0;
        }
        score += preference(context, id);

        String buildNote = defencePure
                ? "Defence-pure safety: use only a Defence-training legal attacker style and do not intentionally gain Attack, Strength, Ranged, or Magic XP."
                : "Use a build-legal combat setup for the Attacker role.";
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Play Barbarian Assault until you have 375 honour points in Attacker, Defender, Collector, and Healer, and have defeated the Penance Queen once. Then buy the Fighter torso from Commander Connad.",
                "No GP purchase is required for the torso. Bring only gear that is legal for this account build. " + buildNote,
                "Barbarian Assault beneath the Barbarian Outpost. Purchase the torso from Commander Connad after the point and Queen requirements are complete.",
                "The torso requires 40 Defence to equip. The route is allowed to lead DO NEXT only because the account/build checks above are already satisfied."
        );

        result.add(new StrategyCandidate(
                id,
                "Get a Fighter torso",
                "A strong reusable melee-body upgrade that does not require a tradeable drop or GP purchase.",
                score,
                RecommendationConfidence.VERIFIED,
                guidance,
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void abyssalWhip(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        if (account.getSkillLevel(Skill.ATTACK) < 70) return;
        if (!AccountBuildPolicy.allowsSkill(account, Skill.ATTACK)) return;
        if (!ownershipCanBeJudged(account, items)) return;

        if (ownsObserved(account, items,
                "Abyssal whip", "Abyssal whip (or)", "Abyssal tentacle",
                "Blade of saeldor", "Blade of saeldor (c)",
                "Ghrazi rapier", "Osmumten's fang",
                "Soulreaper axe", "Scythe of vitur"))
        {
            return;
        }

        String id = "upgrade:abyssal-whip";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        int slayer = account.getSkillLevel(Skill.SLAYER);
        double score;
        String title;
        String reason;
        RecommendationConfidence confidence;
        RecommendationGuidance guidance;

        if (mode.usesGrandExchange())
        {
            score = 41.0;
            title = "Get an Abyssal whip";
            reason = "You have 70 Attack and no observed whip-or-better general melee weapon.";
            confidence = RecommendationConfidence.CHECK_NEEDED;
            guidance = new RecommendationGuidance(
                    "Buy an Abyssal whip once Strategist has a verified live purchase price and confirms the account can afford it without violating the configured spending logic.",
                    "Live price and cash affordability still need to be resolved before this purchase can lead DO NEXT.",
                    "Grand Exchange.",
                    "Keep this as a secondary option until live price and affordability are observed."
            );
        }
        else if (slayer >= 85)
        {
            score = 49.0;
            title = "Get an Abyssal whip";
            reason = "This Iron-style account has reached the Slayer unlock needed for the self-source route.";
            confidence = RecommendationConfidence.VERIFIED;
            guidance = new RecommendationGuidance(
                    "Kill abyssal demons on a safe, reachable account-appropriate route until an Abyssal whip drops. This is RNG, so no fixed kill count is shown.",
                    "Use your strongest build-legal sustainable Slayer setup. Bring the supplies required by the chosen abyssal-demon location; do not route into the Wilderness unless Wilderness methods are explicitly enabled.",
                    "Use a verified non-Wilderness abyssal-demon location by default.",
                    "The acquisition is probabilistic. The useful exact fact here is that the self-source route is unlocked, not the number of kills remaining."
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
            title = "Work toward 85 Slayer for a whip";
            reason = "This Iron-style account cannot buy the upgrade, so Slayer progression is the acquisition chain.";
            confidence = RecommendationConfidence.VERIFIED;
            guidance = new RecommendationGuidance(
                    "Train Slayer from " + slayer + " to 85, then self-source an Abyssal whip from abyssal demons.",
                    "Use banked/observed combat supplies and task gear where possible. A live actionable Slayer task takes priority over this long-term acquisition step.",
                    "Use the highest safe standard Slayer master and the live task location selected by the Slayer planner.",
                    "This is an acquisition-chain recommendation, not a claim that every Slayer level should be trained using the same task or setup."
            );
        }

        score += preference(context, id);
        result.add(new StrategyCandidate(
                id, title, reason, score, confidence, guidance,
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void dragonDefender(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
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
                "Dragon defender", "Dragon defender (t)",
                "Avernic defender", "Avernic defender (l)"))
        {
            return;
        }

        String id = "upgrade:dragon-defender";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        double score = 45.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY)
        {
            score += 8.0;
        }
        score += preference(context, id);

        RecommendationGuidance guidance = new RecommendationGuidance(
                "Enter the Warriors' Guild, obtain defenders in order from the upstairs Cyclopes through Rune, show the Rune defender for basement access, then kill the basement Cyclopes until a Dragon defender drops.",
                "Bring armour, food, a build-legal melee weapon, and enough Warriors' Guild tokens for the Cyclops rooms. Token consumption and defender drops are variable, so no fake exact token/kill count is shown.",
                "Warriors' Guild in Burthorpe. Bronze through Rune defenders are obtained upstairs; the Dragon defender comes from the stronger basement Cyclopes after basement access is unlocked.",
                "The account has the guild-entry stats and the 60 Attack/Defence equip requirements. Defender drops are RNG."
        );
        result.add(new StrategyCandidate(
                id,
                "Get a Dragon defender",
                "A major melee off-hand progression step is available and no Dragon/Avernic defender is observed.",
                score,
                RecommendationConfidence.VERIFIED,
                guidance,
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void barrowsGloves(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P) return;
        QuestSnapshot quests = context.getData().getQuests();
        if (quests == null
                || quests.statusOf("Recipe for Disaster") != QuestStatus.COMPLETE)
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
        if (context.getPreferenceProfile().isOnCooldown(id)) return;

        DiarySnapshot diaries = context.getData().getDiaries();
        boolean eliteLumbridge = diaries != null
                && diaries.isTierComplete("Lumbridge & Draynor", DiaryTier.ELITE);
        long price = eliteLumbridge ? 104_000L : 130_000L;
        AccountEconomySnapshot economy = context.getData().getEconomy();
        boolean cashVerified = economy != null
                && economy.getConfidence() == RecommendationConfidence.VERIFIED;
        boolean affordable = cashVerified && economy.getCoins() >= price;

        double score = 48.0;
        if (context.getActiveGoal() == GoalType.BARROWS_GLOVES) score += 35.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY) score += 10.0;
        score += preference(context, id);

        RecommendationConfidence confidence = affordable
                ? RecommendationConfidence.VERIFIED
                : RecommendationConfidence.CHECK_NEEDED;
        String supplies;
        if (!cashVerified)
        {
            supplies = "Strategist needs verified current cash before treating the shop purchase as immediately ready. Required shop price: " + format(price) + " coins.";
        }
        else if (!affordable)
        {
            supplies = "You have " + format(economy.getCoins())
                    + " verified coins and need " + format(price)
                    + ". You are " + format(price - economy.getCoins())
                    + " coins short.";
        }
        else
        {
            supplies = "Verified cash covers the " + format(price)
                    + " coin shop price.";
        }

        RecommendationGuidance guidance = new RecommendationGuidance(
                "Purchase Barrows gloves from the fully unlocked Culinaromancer's Chest.",
                supplies,
                "Culinaromancer's Chest in the Lumbridge Castle cellar.",
                eliteLumbridge
                        ? "The Elite Lumbridge & Draynor Diary discount is observed, so the reduced shop price is used."
                        : "The full Recipe for Disaster completion is observed. The standard shop price is used unless the Elite Lumbridge & Draynor discount is verified."
        );
        result.add(new StrategyCandidate(
                id,
                "Buy Barrows gloves",
                "Recipe for Disaster is complete and this reusable glove upgrade is not observed.",
                score,
                confidence,
                guidance,
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void fireCape(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P) return;
        if (!ownershipCanBeJudged(account, items)) return;
        if (ownsObserved(account, items,
                "Fire cape", "Fire cape (l)", "Infernal cape", "Infernal cape (l)"))
        {
            return;
        }
        if (!AccountBuildPolicy.allowsSkill(account, Skill.RANGED)
                || account.getSkillLevel(Skill.RANGED) < 70
                || account.getSkillLevel(Skill.PRAYER) < 43
                || account.getSkillLevel(Skill.HITPOINTS) < 50)
        {
            return;
        }

        RestrictedBuildType build = AccountBuildPolicy.effectiveBuild(account);
        if (build == RestrictedBuildType.SKILLER
                || build == RestrictedBuildType.F2P_SKILLER
                || build == RestrictedBuildType.PRAYER_SKILLER
                || build == RestrictedBuildType.DEFENCE_PURE
                || build == RestrictedBuildType.TEN_HITPOINTS)
        {
            return;
        }

        String id = "upgrade:fire-cape";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        boolean hardcoreGroup = mode == AccountMode.HARDCORE_GROUP_IRONMAN;
        double score = 36.0;
        if (context.getActiveGoal() == GoalType.GEAR_TARGET
                || context.getActiveGoal() == GoalType.RAID_READY) score += 12.0;
        if (context.getActiveGoal() == GoalType.INFERNAL_CAPE) score += 20.0;
        if (mode == AccountMode.ULTIMATE_IRONMAN) score -= 5.0;
        if (hardcoreGroup) score -= 25.0;
        score += preference(context, id);

        RecommendationConfidence confidence = hardcoreGroup
                ? RecommendationConfidence.CHECK_NEEDED
                : RecommendationConfidence.VERIFIED;
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Complete all 63 waves of the TzHaar Fight Cave and defeat TzTok-Jad to obtain the Fire cape. Use Ranged as the default route unless a deliberately specialized build plan says otherwise.",
                "Bring the best observed build-legal Ranged setup plus sustainable prayer restoration, healing, and ammunition. Exact quantities depend on weapon, Prayer level, Defence, execution, and run duration, so no universal inventory is shown.",
                "TzHaar Fight Cave in Mor Ul Rek beneath Karamja volcano.",
                hardcoreGroup
                        ? "Important: Fight Cave death is dangerous for Hardcore Group Ironman lives. This stays out of automatic DO NEXT until the player deliberately accepts that risk."
                        : "The planner's 70 Ranged, 43 Prayer, and 50 Hitpoints gate is a conservative readiness heuristic, not a formal game requirement."
        );
        result.add(new StrategyCandidate(
                id,
                "Get a Fire cape",
                "The account has reached a conservative Fight Cave readiness band and no Fire/Infernal cape is observed.",
                score,
                confidence,
                guidance,
                CandidateSafetyEvidence.verifiedSafe(false)));
    }

    private static void bowfaRoute(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (account.getMembershipStatus() != MembershipStatus.P2P) return;
        if (context.getActiveGoal() != GoalType.BOWFA
                && context.getActiveGoal() != GoalType.GEAR_TARGET
                && context.getActiveGoal() != GoalType.RAID_READY)
        {
            return;
        }
        QuestSnapshot quests = context.getData().getQuests();
        if (quests == null
                || quests.statusOf("Song of the Elves") != QuestStatus.COMPLETE)
        {
            return;
        }
        if (!ownershipCanBeJudged(account, items)) return;
        if (ownsObserved(account, items,
                "Bow of faerdhinen", "Bow of faerdhinen (c)"))
        {
            return;
        }

        String id = "upgrade:bowfa";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;
        AccountMode mode = context.getAccountMode();
        boolean seedOwned = ownsObserved(account, items,
                "Enhanced crystal weapon seed");
        int shards = items.quantity("Crystal shard");
        int smithing = account.getSkillLevel(Skill.SMITHING);
        int crafting = account.getSkillLevel(Skill.CRAFTING);
        double score = context.getActiveGoal() == GoalType.BOWFA ? 78.0 : 54.0;
        RecommendationConfidence confidence;
        RecommendationGuidance guidance;
        String title;
        String reason;

        if (seedOwned)
        {
            boolean canSelfSing = smithing >= 82 && crafting >= 82;
            int neededShards = canSelfSing ? 100 : 150;
            int shortfall = Math.max(0, neededShards - shards);
            confidence = shortfall == 0
                    ? RecommendationConfidence.VERIFIED
                    : RecommendationConfidence.CHECK_NEEDED;
            title = "Create your Bow of faerdhinen";
            reason = "An Enhanced crystal weapon seed is observed, so the RNG acquisition step is already complete.";
            String action = canSelfSing
                    ? "Use the Enhanced crystal weapon seed with " + neededShards
                            + " Crystal shards at a singing bowl to create the Bow of faerdhinen."
                    : "Have Conwenna or Reese sing the Enhanced crystal weapon seed into a Bow of faerdhinen using "
                            + neededShards + " Crystal shards because the account does not currently have both 82 Smithing and 82 Crafting.";
            String supplies = shortfall == 0
                    ? "Verified: the Enhanced crystal weapon seed and at least "
                            + neededShards + " Crystal shards are observed."
                    : "The seed is observed, but you need " + shortfall
                            + " more Crystal shard" + (shortfall == 1 ? "" : "s")
                            + " before this creation route is ready.";
            guidance = new RecommendationGuidance(
                    action,
                    supplies,
                    "Use a singing bowl in Prifddinas; the NPC-assisted route is also available there when skill requirements are not met.",
                    "Corrupting the bow is a separate 2,000-shard decision and should not be silently bundled into the initial 100/150-shard creation cost."
            );
        }
        else if (mode.usesGrandExchange())
        {
            confidence = RecommendationConfidence.CHECK_NEEDED;
            title = "Get a Bow of faerdhinen";
            reason = "Song of the Elves is complete, but no Bowfa or Enhanced crystal weapon seed is observed.";
            guidance = new RecommendationGuidance(
                    "Buy the Bow of faerdhinen or its seed route only after Strategist verifies a live price and confirms the purchase fits the account's current cash budget.",
                    "Check the live purchase price before choosing this over a ready action.",
                    "Grand Exchange for a Main; Prifddinas singing bowl if buying/using an Enhanced crystal weapon seed instead.",
                    "The planner deliberately avoids hard-coding a market price."
            );
        }
        else
        {
            AccountMode accountMode = context.getAccountMode();
            boolean hardcore = accountMode == AccountMode.HARDCORE_IRONMAN
                    || accountMode == AccountMode.HARDCORE_GROUP_IRONMAN;
            boolean uimDeathStorage = accountMode == AccountMode.ULTIMATE_IRONMAN
                    && hasDeathStorage(context.getData().getStorage());
            confidence = hardcore || uimDeathStorage
                    ? RecommendationConfidence.CHECK_NEEDED
                    : RecommendationConfidence.VERIFIED;
            title = "Hunt the Enhanced crystal weapon seed";
            reason = "This Iron-style account must self-source the Bowfa seed after Song of the Elves.";
            guidance = new RecommendationGuidance(
                    "Run the Corrupted Gauntlet for an Enhanced crystal weapon seed. The seed is an RNG reward, so progress is tracked without a fixed completion count.",
                    "The Gauntlet supplies its own temporary equipment inside the activity, so do not plan normal bank gear as a required input. After the seed drops, the Bowfa creation step needs Crystal shards.",
                    "The Corrupted Gauntlet in Prifddinas.",
                    hardcore
                            ? "The Gauntlet is a dangerous activity for Hardcore status. Keep this as a deliberate risk decision rather than an automatic primary recommendation."
                            : uimDeathStorage
                            ? "UIM safety: retrieval-service items are currently observed. A dangerous death can threaten that storage state, so this route stays secondary until the death-storage risk is cleared."
                            : "The Enhanced crystal weapon seed is probabilistic. Corrupted Gauntlet has the substantially better seed chance than normal Gauntlet, but no number of completions is guaranteed."
            );
        }

        score += preference(context, id);
        result.add(new StrategyCandidate(
                id, title, reason, score, confidence, guidance,
                CandidateSafetyEvidence.potentiallyIrreversible(false)));
    }

    private static void anglerOutfit(
            StrategyContext context,
            AccountSnapshot account,
            ObservedItemIndex items,
            List<StrategyCandidate> result)
    {
        if (!ContentAccessRules.hasVerifiedMembership(account.getMembershipStatus())) return;
        int fishing = account.getSkillLevel(Skill.FISHING);
        if (fishing < 15) return;
        if (!ownershipCanBeJudged(account, items)) return;

        int pieces = anglerPieces(account, items);
        if (pieces >= 4) return;

        String id = "upgrade:angler-outfit";
        if (context.getPreferenceProfile().isOnCooldown(id)) return;

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
        RecommendationGuidance guidance = new RecommendationGuidance(
                "Play Fishing Trawler and keep contribution at or above the reward threshold each game until the missing Angler pieces are obtained.",
                "Bring the normal Fishing Trawler repair/activity supplies appropriate to the chosen contribution method. Outfit pieces are RNG rewards, so no fake exact game count is shown.",
                "Fishing Trawler at Port Khazard. Inspect the trawler net after qualifying games for rewards.",
                "You currently have " + pieces + "/4 observed Angler/Spirit Angler pieces. The full set gives the Fishing XP set bonus and also unlocks minnow access, but Strategist only detours here when the remaining account value justifies it."
        );
        result.add(new StrategyCandidate(
                id,
                "Finish the Angler outfit (" + pieces + "/4)",
                "The remaining Fishing grind or collection goal is large enough for this reusable skilling unlock to compete with direct XP.",
                score,
                RecommendationConfidence.VERIFIED,
                guidance,
                CandidateSafetyEvidence.skill(false, Skill.FISHING)));
    }

    private static int anglerPieces(
            AccountSnapshot account,
            ObservedItemIndex items)
    {
        int pieces = 0;
        if (ownsObserved(account, items, "Angler hat", "Spirit angler headband")) pieces++;
        if (ownsObserved(account, items, "Angler top", "Spirit angler top")) pieces++;
        if (ownsObserved(account, items, "Angler waders", "Spirit angler waders")) pieces++;
        if (ownsObserved(account, items, "Angler boots", "Spirit angler boots")) pieces++;
        return pieces;
    }

    private static boolean ownershipCanBeJudged(
            AccountSnapshot account,
            ObservedItemIndex items)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        return mode == AccountMode.ULTIMATE_IRONMAN || items.bankObserved();
    }

    /** UIM retrieval-only storage still proves ownership even when not usable now. */
    private static boolean ownsObserved(
            AccountSnapshot account,
            ObservedItemIndex items,
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
        return context.getPreferenceProfile().weightFor(id) * 10.0;
    }

    private static boolean verifiedCoins(StrategyDataBundle data, long needed)
    {
        AccountEconomySnapshot economy = data == null ? null : data.getEconomy();
        return economy != null
                && economy.getConfidence() == RecommendationConfidence.VERIFIED
                && economy.getCoins() >= needed;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }
}

package compass;

import java.util.*;
import java.util.Set;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Skill;
import static compass.Text.get;

/**
 * Hard guardrails for player-imposed account builds.
 *
 * <p>This layer answers a different question from Ironman/Main/UIM policy. A
 * Hardcore Ironman can also be a 1 Defence pure, a Main can be a level-3
 * skiller, and a GIM can be a Defence pure. Both policy layers must agree before
 * Compass recommends an action.</p>
 */
final class AccountBuildPolicy
{
    private AccountBuildPolicy() {}

    public static RestrictedBuildSuggestion detect(AccountSnapshot account)
    {
        if (account == null)
            return build(RestrictedBuildType.STANDARD,
                    Confidence.CHECK_NEEDED, get(1206));

        var attack = account.level(Skill.ATTACK);
        var strength = account.level(Skill.STRENGTH);
        var defence = account.level(Skill.DEFENCE);
        var ranged = account.level(Skill.RANGED);
        var prayer = account.level(Skill.PRAYER);
        var magic = account.level(Skill.MAGIC);
        var hp = account.level(Skill.HITPOINTS);
        var highNonCombat = nonCombatExtreme(account, true);
        var lowNonCombat = nonCombatExtreme(account, false);
        int offence = Math.max(Math.max(attack, strength),
                Math.max(ranged, magic));
        var combat = Math.max(Math.max(offence, defence), prayer);
        boolean baselineOffence = attack <= 1 && strength <= 1
                && ranged <= 1 && magic <= 1;

        if (baselineOffence && defence <= 1 && prayer <= 1 && hp <= 10
                && highNonCombat >= 20)
            return verified(account.membership() != MembershipStatus.P2P
                    ? RestrictedBuildType.F2P_SKILLER
                    : RestrictedBuildType.SKILLER, get(583));
        if (baselineOffence && defence <= 1 && hp <= 10 && prayer >= 15
                && highNonCombat >= 20)
            return verified(RestrictedBuildType.PRAYER_SKILLER, get(590));
        if (lowNonCombat <= 1 && highNonCombat <= 1 && combat >= 40)
            return verified(RestrictedBuildType.COMBAT_ONLY, get(591));
        if (baselineOffence && defence >= 20)
            return verified(RestrictedBuildType.DEFENCE_PURE, get(592));
        if (hp <= 10 && (ranged >= 20 || magic >= 20 || prayer >= 20
                || highNonCombat >= 50))
            return verified(RestrictedBuildType.TEN_HITPOINTS, get(593));
        if (attack <= 1 && defence <= 1 && strength >= 50)
            return verified(RestrictedBuildType.OBSIDIAN_MAULER, get(594));
        if (defence <= 1 && offence >= 40)
            return verified(RestrictedBuildType.ONE_DEFENCE_PURE, get(595));

        if (offence >= 50)
        {
            if (defence >= 2 && defence <= 13)
                return verified(RestrictedBuildType.LOW_DEFENCE_PURE,
                        get(596));
            if (defence <= 20)
                return verified(RestrictedBuildType.INITIATE_PURE, get(597));
            if (defence >= 39 && defence <= 40)
                return verified(RestrictedBuildType.RUNE_PURE, get(584));
            if (defence <= 42 && defence >= 41)
                return verified(RestrictedBuildType.VOID_PURE, get(585));
            if (defence >= 43 && defence <= 45 && attack >= 50 && strength >= 50)
                return verified(RestrictedBuildType.ZERKER, get(586));
        }
        if (defence >= 70 && ranged >= 80 && magic >= 70
                && attack <= 60 && strength <= 70)
            return build(RestrictedBuildType.RANGE_TANK,
                    Confidence.CHECK_NEEDED, get(587));
        if (lowNonCombat <= 1 && highNonCombat <= 5 && combat >= 30)
            return build(RestrictedBuildType.COMBAT_ONLY,
                    Confidence.CHECK_NEEDED, get(588));
        return verified(RestrictedBuildType.STANDARD, get(589));
    }

    private static RestrictedBuildSuggestion verified(
            RestrictedBuildType type, String evidence)
    {
        return build(type, Confidence.VERIFIED, evidence);
    }

    private static RestrictedBuildSuggestion build(RestrictedBuildType type,
            Confidence confidence, String evidence)
    {
        return new RestrictedBuildSuggestion(type, confidence, evidence);
    }

    private static int nonCombatExtreme(AccountSnapshot account,
            boolean highest)
    {
        var value = highest ? 1 : Integer.MAX_VALUE;
        for (Skill skill : Skill.values())
            if (!isCombatProgressionSkill(skill))
                value = highest ? Math.max(value, account.level(skill))
                        : Math.min(value, account.level(skill));
        return value == Integer.MAX_VALUE ? 1 : value;
    }

    /**
     * Returns the build that is safe to enforce automatically. Ambiguous
     * CHECK_NEEDED suggestions remain STANDARD until the player confirms them in
     * a future explicit build setting.
     */
    public static RestrictedBuildType effectiveBuild(AccountSnapshot account)
    {
        var suggestion = detect(account);
        if (suggestion.getConfidence() != Confidence.VERIFIED)
        {
            return RestrictedBuildType.STANDARD;
        }
        return suggestion.getType();
    }

    public static boolean allowsSkill(AccountSnapshot account, Skill skill)
    {
        if (account == null || skill == null) return true;
        var build = effectiveBuild(account);

        switch (build)
        {
            case SKILLER:
            case F2P_SKILLER:
                return !isCombatProgressionSkill(skill);

            case PRAYER_SKILLER:
                return skill == Skill.PRAYER || !isCombatProgressionSkill(skill);

            case ONE_DEFENCE_PURE:
                return skill != Skill.DEFENCE;

            case LOW_DEFENCE_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 13;

            case INITIATE_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 20;

            case RUNE_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 40;

            case VOID_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 42;

            case ZERKER:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 45;

            case OBSIDIAN_MAULER:
                return skill != Skill.ATTACK && skill != Skill.DEFENCE;

            case DEFENCE_PURE:
                // Defence-pure combat progression is deliberately limited to
                // Defence and Prayer. Hitpoints rises incidentally from legal
                // Defence training and is never a direct recommendation anyway.
                return skill != Skill.ATTACK
                        && skill != Skill.STRENGTH
                        && skill != Skill.RANGED
                        && skill != Skill.MAGIC
                        && skill != Skill.SLAYER;

            case TEN_HITPOINTS:
                // Attack/Strength/Defence/Ranged training normally creates HP
                // experience. Magic can remain legal through splashing,
                // alching, teleports, curses, enchanting, etc., so method-level
                // policy decides which Magic methods are safe.
                return skill != Skill.ATTACK
                        && skill != Skill.STRENGTH
                        && skill != Skill.DEFENCE
                        && skill != Skill.RANGED
                        && skill != Skill.HITPOINTS
                        && skill != Skill.SLAYER;

            case COMBAT_ONLY:
                return isCombatProgressionSkill(skill);

            case RANGE_TANK:
            case MED_BUILD:
            case STANDARD:
            default:
                return true;
        }
    }

    public static boolean allowsMethod(
            AccountSnapshot account,
            TrainingMethod method)
    {
        if (account == null || method == null) return true;
        if (!allowsSkill(account, method.getSkill())) return false;

        var build = effectiveBuild(account);
        var id = method.id == null ? "" : method.id.toLowerCase();

        if (build == RestrictedBuildType.TEN_HITPOINTS)
        {
            if (method.getSkill() == Skill.MAGIC)
            {
                // Only known non-damaging/zero-HP Magic routes are allowed by
                // default. Combat, Slayer, bursting, and barraging are blocked.
                return id.contains("alch")
                        || id.contains("curse")
                        || id.contains("teleport")
                        || id.contains("enchant")
                        || id.contains("utility");
            }
            return !isHpGeneratingCombatSkill(method.getSkill());
        }

        if ((build == RestrictedBuildType.SKILLER
                || build == RestrictedBuildType.F2P_SKILLER
                || build == RestrictedBuildType.PRAYER_SKILLER)
                && isHpGeneratingCombatSkill(method.getSkill()))
        {
            return false;
        }

        return true;
    }

    /**
     * Prevents broad gear ladders from suggesting armour that violates a known
     * Defence cap. Exact item-level requirement data can later replace these
     * conservative tier checks.
     */
    public static boolean allowsGearEntry(
            AccountSnapshot account,
            GearProgressionEntry entry)
    {
        if (account == null || entry == null) return true;
        var build = effectiveBuild(account);
        var id = entry.id == null ? "" : entry.id.toLowerCase();

        switch (build)
        {
            case SKILLER:
            case F2P_SKILLER:
            case PRAYER_SKILLER:
            case TEN_HITPOINTS:
            case COMBAT_ONLY:
                // Combat-only can use gear normally. The fall-through is split
                // below to keep the restricted non-aggressor builds explicit.
                return build == RestrictedBuildType.COMBAT_ONLY;

            case ONE_DEFENCE_PURE:
            case OBSIDIAN_MAULER:
                return id.contains("1def") || id.contains("pure");

            case LOW_DEFENCE_PURE:
                return id.contains("low-def") || id.contains("pure");

            case INITIATE_PURE:
                return id.contains("20def") || id.contains("pure");

            case RUNE_PURE:
                return id.contains("40def") || id.contains("rune-pure")
                        || id.contains("pure");

            case VOID_PURE:
                return id.contains("42def") || id.contains("void-pure")
                        || id.contains("pure");

            case ZERKER:
                return id.contains("45def") || id.contains("zerker")
                        || id.contains("pure");

            case DEFENCE_PURE:
                return id.contains("defence-pure") || id.contains("tank");

            case RANGE_TANK:
                return entry.getStyle() == CombatStyle.RANGED
                        || entry.getStyle() == CombatStyle.MAGIC
                        || id.contains("tank");

            case MED_BUILD:
            case STANDARD:
            default:
                return true;
        }
    }

    public static String label(AccountSnapshot account)
    {
        return pretty(effectiveBuild(account));
    }

    private static boolean isCombatProgressionSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case PRAYER:
            case MAGIC:
            case SLAYER:
                return true;
            default:
                return false;
        }
    }

    private static boolean isHpGeneratingCombatSkill(Skill skill)
    {
        return skill == Skill.ATTACK
                || skill == Skill.STRENGTH
                || skill == Skill.DEFENCE
                || skill == Skill.RANGED
                || skill == Skill.MAGIC
                || skill == Skill.SLAYER;
    }

    private static String pretty(RestrictedBuildType type)
    {
        var text = type.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

/**
 * Central account-mode rules used by planners before they recommend a route.
 *
 * <p>Keeping these rules in one place prevents accidental GE suggestions on
 * irons, inappropriate group-storage assumptions, or UIM storage advice that
 * ignores the account's restrictions.</p>
 */
final class AccountModePolicy
{
    private AccountModePolicy()
    {
    }

    public static boolean mayUseGrandExchange(AccountMode mode)
    {
        return mode == AccountMode.MAIN;
    }

    public static boolean mayUseGroupStorage(
            AccountMode mode,
            boolean userEnabled)
    {
        return userEnabled
                && mode != null
                && mode.isGroupIronman();
    }

    public static boolean requiresSelfSourcing(AccountMode mode)
    {
        return mode != null && mode.isIronLike();
    }

    public static boolean requiresCapabilityCheckedStorage(AccountMode mode)
    {
        return mode == AccountMode.ULTIMATE_IRONMAN;
    }

    public static boolean isRiskSensitive(AccountMode mode)
    {
        return mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN
                || mode == AccountMode.ULTIMATE_IRONMAN;
    }
}

/**
 * Hard gate for the primary DO NEXT slot and quality gate for alternatives.
 *
 * <p>The primary card must always contain an executable next action. A fully
 * verified route can lead immediately. A route with only ordinary preparation
 * outstanding may also lead when its guidance explains that preparation. True
 * unknown access, quest, build, or unlock requirements can never lead.</p>
 */
@Singleton
class ActionabilityPolicy
{
    private final RecommendationQualityPolicy qualityPolicy =
            new RecommendationQualityPolicy();

    public boolean canLeadQueue(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() == Confidence.BLOCKED)
        {
            return false;
        }

        var plan = recommendation.plan();
        var guidance = recommendation.getGuidance();
        if (!qualityPolicy.isPresentable(recommendation)) return false;

        if (plan == null)
        {
            // Non-skill candidates must be fully verified and structured before
            // they can displace a concrete training action. The one exception
            // is an explicitly typed preparation/verification action whose
            // remaining work is fully described by the quality contract.
            return (recommendation.getConfidence()
                        == Confidence.VERIFIED
                    || (recommendation.getConfidence()
                        == Confidence.CHECK_NEEDED
                        && isExplicitPreparation(recommendation)))
                    && guidance != null && hasText(guidance.getAction());
        }

        if (plan.method() == null || guidance == null
                || !hasText(guidance.getAction()))
        {
            return false;
        }

        if (recommendation.getConfidence() == Confidence.VERIFIED)
        {
            return !RequirementActionability.hasHardUnresolvedRequirement(plan);
        }

        // CHECK_NEEDED is allowed only when every unresolved check is ordinary
        // preparation and the guidance explicitly covers supplies/setup.
        return RequirementActionability.isActionablePreparation(plan, guidance);
    }

    public boolean mayAppearAsAlternative(Recommendation recommendation)
    {
        if (recommendation == null
                || recommendation.getConfidence() == Confidence.BLOCKED)
        {
            return false;
        }
        if (canLeadQueue(recommendation)) return true;

        if (!qualityPolicy.isPresentable(recommendation)) return false;

        var guidance = recommendation.getGuidance();
        var plan = recommendation.plan();

        // A secondary card still needs to tell the player something useful.
        // Bare "Needs Info" quest/upgrade placeholders are hidden until their
        // provider can produce a concrete next verification/preparation step.
        if (plan == null)
        {
            return guidance != null && hasText(guidance.getAction())
                    && (recommendation.getConfidence()
                            == Confidence.VERIFIED
                        || isExplicitPreparation(recommendation));
        }

        return plan.method() != null
                && guidance != null
                && hasText(guidance.getAction())
                && !RequirementActionability.hasHardUnresolvedRequirement(plan);
    }

    public int queuePriority(Recommendation recommendation)
    {
        if (canLeadQueue(recommendation)) return 2;
        if (mayAppearAsAlternative(recommendation)) return 1;
        return 0;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isExplicitPreparation(Recommendation recommendation)
    {
        String id = recommendation == null || recommendation.id == null
                ? "" : recommendation.id.toLowerCase(
                        java.util.Locale.ROOT);
        return id.startsWith("prepare:")
                || id.startsWith("preparation:")
                || id.startsWith("verify:");
    }
}

/** Final access/build boundary shared by every recommendation family. */
@Singleton
class CandidateSafetyPolicy
{
    public boolean isAllowed(Recommendation recommendation, StrategyContext context)
    {
        if (recommendation == null || context == null || context.data() == null
                || context.data().account() == null)
        {
            return recommendation != null;
        }

        var account = context.data().account();
        if (AccountMode.fromTypeCode(account.modeCode())
                    == AccountMode.ULTIMATE_IRONMAN
                && (recommendation.getSafetyEvidence()
                        .isConventionalBankRequired()
                    || recommendation.getSafetyEvidence()
                        .hasUnverifiedDangerousStorage()
                    || recommendation.getGuidance() != null
                        && recommendation.getGuidance().getBankingBehavior()
                            == MethodBankingBehavior.CONVENTIONAL_BANK_LOOP))
            return false;
        if (recommendation.getSafetyEvidence().hasInvalidCurrentExecution())
            return false;
        return isAllowed(recommendation.getSafetyEvidence(), account);
    }

    public boolean isAllowed(SafetyEvidence evidence,
            StrategyContext context)
    {
        if (evidence == null || context == null || context.data() == null
                || context.data().account() == null) return false;
        return isAllowed(evidence, context.data().account());
    }

    private static boolean isAllowed(SafetyEvidence evidence,
            AccountSnapshot account)
    {

        if (evidence.hasInvalidCurrentExecution()) return false;

        // Unannotated content is never assumed F2P-safe. This is the final
        // protection against a new provider forgetting its early access filter.
        if (account.membership() != MembershipStatus.P2P
                && evidence.getAccess() != SafetyEvidence.Access.F2P_SAFE)
        {
            return false;
        }

        var mode = AccountMode.fromTypeCode(account.modeCode());
        if ((mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                && (evidence.getBuildEffect()
                == SafetyEvidence.BuildEffect.POTENTIALLY_IRREVERSIBLE
                || evidence.getBuildEffect()
                == SafetyEvidence.BuildEffect.UNKNOWN))
        {
            // When the candidate cannot prove its risk/build effects, preserving
            // a Hardcore life takes precedence over provider score.
            return false;
        }

        var suggestion = AccountBuildPolicy.detect(account);
        if (suggestion.getConfidence() == Confidence.VERIFIED
                && suggestion.getType() == RestrictedBuildType.STANDARD)
        {
            return true;
        }

        switch (evidence.getBuildEffect())
        {
            case HARMLESS:
            case VERIFIED_SAFE:
                return true;
            case SKILL_XP:
                return evidence.getAffectedSkill() != null
                        && AccountBuildPolicy.allowsSkill(account,
                        evidence.getAffectedSkill());
            case POTENTIALLY_IRREVERSIBLE:
            case UNKNOWN:
            default:
                // Ambiguous and verified restricted signatures both fail closed
                // unless the provider supplied a harmless or verified-safe proof.
                return false;
        }
    }

}

/**
 * Hard membership boundary for quest recommendations.
 *
 * <p>RuneLite exposes the complete quest list even while the character is on a
 * free-to-play account/world. The Compass must therefore filter by content
 * entitlement before scoring quests. This list tracks the current F2P quest set
 * and intentionally fails closed for unknown names on F2P.</p>
 */
final class QuestMembershipPolicy
{
    private static final Set<String> FREE_TO_PLAY_QUESTS =
            PolicyLists.normalizedSet(PolicyLists.DATA.free_to_play_quests);

    private QuestMembershipPolicy()
    {
    }

    public static boolean isAvailable(String questName, MembershipStatus membership)
    {
        if (questName == null || questName.trim().isEmpty()) return false;
        if (membership == MembershipStatus.P2P) return true;
        return FREE_TO_PLAY_QUESTS.contains(normalize(questName));
    }

    public static boolean isFreeToPlayQuest(String questName)
    {
        return questName != null && FREE_TO_PLAY_QUESTS.contains(normalize(questName));
    }

    public static Set<String> freeToPlayQuestNames()
    {
        return FREE_TO_PLAY_QUESTS;
    }

    private static String normalize(String value)
    {
        return PolicyLists.normalize(value);
    }
}

/** Rejects known placeholder-shaped copy before it can reach player cards. */
final class RecommendationQualityPolicy
{
    private static final List<String> GENERIC_TITLES =
            PolicyLists.list(PolicyLists.DATA.generic_titles);
    private static final List<String> GENERIC_ACTIONS =
            PolicyLists.list(PolicyLists.DATA.generic_actions);
    private static final List<String> GENERIC_LOCATIONS =
            PolicyLists.list(PolicyLists.DATA.generic_locations);
    private static final List<String> UNRESOLVED_SUPPLIES =
            PolicyLists.list(PolicyLists.DATA.unresolved_supplies);

    boolean isPresentable(Recommendation recommendation)
    {
        if (recommendation == null || !hasText(recommendation.getTitle())) return false;
        if (isGenericTitle(recommendation.getTitle()))
            return false;
        var guidance = recommendation.getGuidance();
        if (guidance == null || !hasText(guidance.getAction())) return false;
        if (containsAny(guidance.getAction(), GENERIC_ACTIONS)) return false;
        if (containsAny(guidance.getLocation(), GENERIC_ACTIONS)) return false;
        if (containsAny(guidance.getLocation(), GENERIC_LOCATIONS)) return false;
        if (containsAny(guidance.getSupplies(), UNRESOLVED_SUPPLIES)) return false;

        var plan = recommendation.plan();
        if (plan != null)
        {
            if (plan.method() == null
                    || !hasText(plan.method().getName())
                    || !hasText(guidance.getLocation())) return false;
            if (containsAny(plan.method().getName(), GENERIC_ACTIONS))
            {
                return false;
            }
        }
        return coherentRuneRoute(plan, guidance);
    }

    private static boolean coherentRuneRoute(
            TrainingPlan plan, Guidance guidance)
    {
        if (plan == null || plan.method() == null
                || plan.method().getSkill() != net.runelite.api.Skill.RUNECRAFT)
        {
            return true;
        }
        var method = Names.text(plan.method().getName());
        var action = Names.text(guidance.getAction());
        String[] runes = {"air", "mind", "water", "earth", "fire", "body"};
        for (String rune : runes)
        {
            if (method.contains(rune + " rune"))
                return action.contains(rune + " rune");
        }
        return true;
    }

    private static boolean containsAny(String value, List<String> needles)
    {
        var normalized = Names.text(value);
        if (normalized.isEmpty()) return false;
        for (String needle : needles)
            if (normalized.contains(needle)) return true;
        return false;
    }

    private static boolean isGenericTitle(String value)
    {
        var normalized = Names.text(value);
        if (GENERIC_TITLES.contains(normalized)) return true;
        for (Skill skill : Skill.values())
        {
            if (normalized.equals("train " + Names.text(skill.getName())))
                return true;
        }
        return false;
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.trim().isEmpty();
    }

}

/** Fail-closed quest safety for irreversible restricted-build XP. */
final class RestrictedQuestPolicy
{
    private static final Set<String> ONE_DEFENCE = PolicyLists.normalizedSet(
            PolicyLists.DATA.one_defence_safe);
    private static final Set<String> LEVEL_THREE = PolicyLists.normalizedSet(
            PolicyLists.DATA.level_three_safe);
    private static final Set<String> PRAYER_EXTRA = PolicyLists.normalizedSet(
            PolicyLists.DATA.prayer_skiller_extra);

    private RestrictedQuestPolicy() {}

    public static boolean isSafe(AccountSnapshot account, String questName)
    {
        if (account == null || questName == null) return false;
        var build = AccountBuildPolicy.effectiveBuild(account);
        switch (build)
        {
            case STANDARD:
            case RANGE_TANK:
            case MED_BUILD:
            case COMBAT_ONLY:
                return true;
            case SKILLER:
            case F2P_SKILLER:
                return LEVEL_THREE.contains(PolicyLists.normalize(questName));
            case PRAYER_SKILLER:
            case DEFENCE_PURE:
            case TEN_HITPOINTS:
                return safeForPrayerOnly(questName);
            case ONE_DEFENCE_PURE:
            case LOW_DEFENCE_PURE:
            case INITIATE_PURE:
            case RUNE_PURE:
            case VOID_PURE:
            case ZERKER:
            case OBSIDIAN_MAULER:
                return ONE_DEFENCE.contains(PolicyLists.normalize(questName));
            default:
                return false;
        }
    }

    private static boolean safeForPrayerOnly(String quest)
    {
        var key = PolicyLists.normalize(quest);
        return LEVEL_THREE.contains(key) || PRAYER_EXTRA.contains(key);
    }
}

/** Single access boundary for optional hosted capabilities; local safety is unconditional. */
@Singleton
class StrategistFeatureAccessPolicy
{
    public boolean canUse(Feature feature, Snapshot entitlement)
    {
        return feature != null && (feature.isCoreLocal()
                || entitlement != null && entitlement.has(feature));
    }

    public void requireHosted(Feature feature, Snapshot entitlement)
    {
        if (feature == null || feature.isCoreLocal())
            throw new IllegalArgumentException(Text.get(730));
        if (!canUse(feature, entitlement))
            throw new HostedFeatureUnavailableException(feature);
    }

    public enum Feature
    {
        CORE_PLANNER(false), LOCAL_PROFILE_MEMORY(false),
        LOCAL_METHOD_GUIDANCE(false), LOCAL_BUILD_SAFETY(false),
        LOCAL_RESOURCE_PLANNING(false), LOCAL_RECOMMENDATION_HISTORY(false),
        CLOUD_PROFILE_SYNC(true), CROSS_DEVICE_HISTORY(true),
        GIM_TEAM_PLANNING(true), REMOTE_REMINDERS(true), WEB_DASHBOARD(true),
        ONLINE_REASONING(true), ADVANCED_CLOUD_ANALYTICS(true);

        @Getter private final boolean hostedPremium;
        Feature(boolean hosted) { hostedPremium = hosted; }
        public boolean isCoreLocal() { return !hostedPremium; }
    }

    @Getter
    public static final class Snapshot
    {
        private final Set<Feature> hostedFeatures;
        private final Confidence confidence;
        private final String source;

        public Snapshot(Set<Feature> features, Confidence confidence, String source)
        {
            var copy = EnumSet.noneOf(Feature.class);
            if (features != null) for (Feature feature : features)
                if (feature != null && feature.isHostedPremium()) copy.add(feature);
            hostedFeatures = Collections.unmodifiableSet(copy);
            this.confidence = confidence == null ? Confidence.CHECK_NEEDED : confidence;
            this.source = source == null ? "unknown" : source;
        }

        public static Snapshot none()
        {
            return new Snapshot(Collections.emptySet(), Confidence.CHECK_NEEDED,
                    "not-connected");
        }

        boolean has(Feature feature)
        {
            return confidence == Confidence.VERIFIED
                    && feature.isHostedPremium() && hostedFeatures.contains(feature);
        }
    }

    public static final class HostedFeatureUnavailableException
            extends IllegalStateException
    {
        @Getter private final Feature feature;
        HostedFeatureUnavailableException(Feature feature)
        {
            super(Text.get(731) + feature);
            this.feature = feature;
        }
    }
}

/** Account-mode, restricted-build, and play-style guardrails for training methods. */
@Singleton
class TrainingMethodPolicy
{
    public boolean isAllowed(
            GameData data,
            TrainingMethod method,
            TrainingMethodMetadata metadata,
            boolean allowWildernessMethods)
    {
        if (method == null || metadata == null) return false;
        var account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.modeCode());
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.membership();

        if (!AccountBuildPolicy.allowsMethod(account, method)) return false;

        // UNKNOWN membership is intentionally treated like F2P here. The route
        // can widen as soon as membership is verified, but it can never leak a
        // members-only method into an F2P account during a transient read.
        if (membership != MembershipStatus.P2P
                && !metadata.isFreeToPlayAllowed())
        {
            return false;
        }

        if (method.isWilderness() && !allowWildernessMethods) return false;

        if (mode == AccountMode.HARDCORE_IRONMAN
                || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
        {
            if (method.isWilderness()
                    || !metadata.isHardcoreSafe()
                    || metadata.getRiskLevel() == RiskLevel.HIGH
                    || metadata.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            {
                return false;
            }
        }

        if (mode == AccountMode.ULTIMATE_IRONMAN
                && !metadata.isUimFriendly())
        {
            return false;
        }

        if (AccountModePolicy.isRiskSensitive(mode)
                && metadata.getRiskLevel() == RiskLevel.IRREVERSIBLE)
        {
            return false;
        }

        return true;
    }

    public double scoreAdjustment(
            GameData data,
            TrainingMethodMetadata metadata,
            StrategyMode strategyMode,
            SessionIntent sessionIntent)
    {
        if (metadata == null) return 0.0;
        var account = data == null ? null : data.account();
        AccountMode mode = account == null
                ? AccountMode.UNKNOWN
                : AccountMode.fromTypeCode(account.modeCode());
        double score = intensityAdjustment(
                metadata.getIntensity(), strategyMode, sessionIntent);

        // Account-specific method value now comes from sourced strategy
        // profiles and live readiness in TrainingMethodSelector. Keep this
        // policy focused on play-style fit and real risk instead of layering
        // arbitrary Iron/UIM/method-cost bonuses over the knowledge model.
        if (AccountModePolicy.isRiskSensitive(mode))
        {
            if (metadata.getRiskLevel() == RiskLevel.MEDIUM) score -= 5.0;
            if (metadata.getRiskLevel() == RiskLevel.HIGH) score -= 10.0;
        }
        return score;
    }

    private static double intensityAdjustment(
            TrainingIntensity intensity,
            StrategyMode mode,
            SessionIntent intent)
    {
        var score = 0.0;
        var safeMode = mode == null ? StrategyMode.BALANCED : mode;
        var safeIntent = intent == null ? SessionIntent.PICK_FOR_ME : intent;

        switch (safeMode)
        {
            case EFFICIENT:
                if (intensity == TrainingIntensity.SWEATY) score += 5.0;
                if (intensity == TrainingIntensity.EFFICIENT) score += 7.0;
                if (intensity == TrainingIntensity.RELAXED) score -= 2.0;
                if (intensity == TrainingIntensity.AFK) score -= 3.0;
                break;
            case RELAXED:
                if (intensity == TrainingIntensity.AFK) score += 8.0;
                if (intensity == TrainingIntensity.RELAXED) score += 7.0;
                if (intensity == TrainingIntensity.BALANCED) score += 2.0;
                if (intensity == TrainingIntensity.SWEATY) score -= 9.0;
                break;
            case BALANCED:
            default:
                if (intensity == TrainingIntensity.BALANCED) score += 6.0;
                if (intensity == TrainingIntensity.EFFICIENT) score += 3.0;
                if (intensity == TrainingIntensity.RELAXED) score += 3.0;
                if (intensity == TrainingIntensity.SWEATY) score -= 1.0;
                break;
        }

        if (safeIntent == SessionIntent.AFK)
        {
            if (intensity == TrainingIntensity.AFK) score += 9.0;
            if (intensity == TrainingIntensity.RELAXED) score += 4.0;
            if (intensity == TrainingIntensity.BALANCED) score -= 2.0;
            if (intensity == TrainingIntensity.EFFICIENT) score -= 5.0;
            if (intensity == TrainingIntensity.SWEATY) score -= 12.0;
        }
        return score;
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Converts verified minigame unlocks into useful progression candidates. */
@Singleton
public class MinigameCandidateProvider implements StrategyCandidateProvider
{
    private final MinigameCatalog catalog;
    private final MinigameSetupCatalog setupCatalog;
    private final ItemRequirementEvaluator itemEvaluator;

    public MinigameCandidateProvider(MinigameCatalog catalog)
    {
        this(catalog, new MinigameSetupCatalog(), new ItemRequirementEvaluator());
    }

    @Inject
    public MinigameCandidateProvider(MinigameCatalog catalog,
            MinigameSetupCatalog setupCatalog,
            ItemRequirementEvaluator itemEvaluator)
    {
        this.catalog = catalog;
        this.setupCatalog = setupCatalog;
        this.itemEvaluator = itemEvaluator;
    }

    @Override
    public String getId() { return "minigame-candidates"; }

    @Override
    public List<StrategyCandidate> candidates(StrategyContext context)
    {
        List<StrategyCandidate> result = new ArrayList<>();
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null
                || context.getData().getMinigames() == null) return result;

        AccountSnapshot account = context.getData().getAccount();
        AccountMode mode = context.getAccountMode();
        MinigameSnapshot snapshot = context.getData().getMinigames();

        for (MinigameDefinition definition : catalog.all())
        {
            if (!snapshot.isUnlocked(definition.getId())) continue;
            if (!definition.supports(mode)) continue;
            if (!ContentAccessRules.isContentAvailable(
                    account.getMembershipStatus(), definition.isFreeToPlay())) continue;
            if (definition.getPrimarySkill() != null
                    && account.getSkillLevel(definition.getPrimarySkill())
                    < definition.getMinimumLevel()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && definition.getRiskLevel() == RiskLevel.HIGH) continue;

            String id = "minigame:" + definition.getId();
            if (context.getPreferenceProfile().isOnCooldown(id)) continue;
            double score = 28.0;
            if (definition.getRiskLevel() == RiskLevel.NONE) score += 4.0;
            if (context.getStrategyMode() == StrategyMode.RELAXED
                    && (definition.getAttention() == AttentionLevel.LOW
                    || definition.getAttention() == AttentionLevel.AFK)) score += 6.0;
            if (context.getSessionIntent() == SessionIntent.LONG_SESSION) score += 2.0;
            if (context.isCollectionistMode()) score += 4.0;
            score += context.getPreferenceProfile().weightFor(id) * 10.0;

            MinigameSetupProfile setup = setupCatalog.forActivity(
                    definition.getId());
            ItemRequirementResult itemResult = setup == null ? null
                    : itemEvaluator.evaluate(setup.getItems(), context.getData(),
                            context.isUseGroupStorage());
            boolean verified = setup != null && itemResult.isSatisfied();
            RecommendationGuidance guidance = setup == null
                    ? verificationGuidance(definition)
                    : "forestry".equals(definition.getId())
                            ? forestryGuidance(account, verified, itemResult)
                            : new RecommendationGuidance(
                            verified ? setup.getInstructions()
                                    : itemResult.getAction() + " before " + definition.getName() + ".",
                            verified ? setup.getSupplies() : itemResult.getAction(),
                            setup.getLocation(), definition.getRewardFocus() + ".");

            result.add(new StrategyCandidate(
                    id,
                    definition.getName(),
                    definition.getRewardFocus()
                            + ". Unlock is verified, but loadout, consumables, currency and account-mode constraints must also pass before the activity is Ready.",
                    score,
                    verified ? RecommendationConfidence.VERIFIED
                            : RecommendationConfidence.CHECK_NEEDED,
                    guidance,
                    safetyFor(definition)
            ));
        }

        result.sort(Comparator.comparingDouble(StrategyCandidate::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static RecommendationGuidance verificationGuidance(
            MinigameDefinition definition)
    {
        String activity = definition.getName();
        return new RecommendationGuidance(
                "Open your inventory and equipment tab with your " + activity
                        + " setup equipped.",
                "Carry the supplies you intend to use. Compass will keep this as preparation until the required setup can be proven.",
                "Use the verified in-game unlock for " + activity + ".",
                definition.getRewardFocus() + ".");
    }

    private static RecommendationGuidance forestryGuidance(
            AccountSnapshot account, boolean verified,
            ItemRequirementResult itemResult)
    {
        int level = account.getSkillLevel(net.runelite.api.Skill.WOODCUTTING);
        boolean f2p = account.getMembershipStatus() != MembershipStatus.P2P;
        String tree;
        String location;
        if (level < 30)
        {
            tree = "oak trees";
            location = "the oak trees immediately east of Draynor Village bank";
        }
        else if (f2p || level < 45)
        {
            tree = "willow trees";
            location = "the willow trees south of Draynor Village bank";
        }
        else if (level < 60)
        {
            tree = "maple trees";
            location = "the maple trees north of Seers' Village bank";
        }
        else
        {
            tree = "yew trees";
            location = "the yew trees at Seers' Village church";
        }
        boolean uim = AccountMode.fromTypeCode(account.getAccountTypeCode())
                == AccountMode.ULTIMATE_IRONMAN;
        String loop = uim
                ? "Switch to an official Forestry world, cut " + tree
                        + ", drop only the freshly cut logs when the inventory fills, and complete every event that spawns. Never drop carried setup items."
                : "Switch to an official Forestry world, cut " + tree
                        + ", bank each inventory, and complete every event that spawns.";
        return new RecommendationGuidance(
                verified
                        ? loop
                        : itemResult.getAction() + " before starting Forestry.",
                verified ? "Bring the observed axe and a Forestry kit when owned."
                        : itemResult.getAction(),
                location + ".",
                "The tree and location match the current Woodcutting level; event frequency varies, so no event count is invented."
                        + (uim ? " Only renewable logs produced by this method are discarded." : ""));
    }

    private static CandidateSafetyEvidence safetyFor(MinigameDefinition definition)
    {
        if (definition.getRiskLevel() == RiskLevel.HIGH
                || definition.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            return CandidateSafetyEvidence.potentiallyIrreversible(
                    definition.isFreeToPlay());
        if (definition.isCombatActivity())
            return CandidateSafetyEvidence.potentiallyIrreversible(
                    definition.isFreeToPlay());
        if (definition.getPrimarySkill() != null)
            return CandidateSafetyEvidence.skill(definition.isFreeToPlay(),
                    definition.getPrimarySkill());
        return CandidateSafetyEvidence.harmless(definition.isFreeToPlay());
    }
}

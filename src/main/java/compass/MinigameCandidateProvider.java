package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Converts verified minigame unlocks into useful progression candidates. */
@Singleton
public class MinigameCandidateProvider implements CandidateProvider
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
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().account() == null
                || context.data().minigames() == null) return result;

        var account = context.data().account();
        var mode = context.accountMode();
        var snapshot = context.data().minigames();

        for (MinigameDefinition definition : catalog.all())
        {
            if (!snapshot.isUnlocked(definition.getId())) continue;
            if (!definition.supports(mode)) continue;
            if (!ContentAccessRules.isContentAvailable(
                    account.membership(), definition.isFreeToPlay())) continue;
            if (definition.getPrimarySkill() != null
                    && account.level(definition.getPrimarySkill())
                    < definition.getMinimumLevel()) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && definition.getRiskLevel() == RiskLevel.HIGH) continue;

            var id = "minigame:" + definition.getId();
            if (context.preferenceProfile().isOnCooldown(id)) continue;
            var score = 28.0;
            if (definition.getRiskLevel() == RiskLevel.NONE) score += 4.0;
            if (context.mode() == StrategyMode.RELAXED
                    && (definition.getAttention() == AttentionLevel.LOW
                    || definition.getAttention() == AttentionLevel.AFK)) score += 6.0;
            if (context.intent() == SessionIntent.LONG_SESSION) score += 2.0;
            if (context.collectionist()) score += 4.0;
            score += context.preferenceProfile().weightFor(id) * 10.0;

            MinigameSetupProfile setup = setupCatalog.forActivity(
                    definition.getId());
            ItemRequirementResult itemResult = setup == null ? null
                    : itemEvaluator.evaluate(setup.getItems(), context.data(),
                            context.usesGroupStorage());
            var verified = setup != null && itemResult.isSatisfied();
            Guidance guidance = setup == null
                    ? verificationGuidance(definition)
                    : "forestry".equals(definition.getId())
                            ? forestryGuidance(account, verified, itemResult)
                            : new Guidance(
                            verified ? setup.getInstructions()
                                    : itemResult.getAction() + " before " + definition.getName() + ".",
                            verified ? setup.getSupplies() : itemResult.getAction(),
                            setup.getLocation(), definition.getRewardFocus() + ".");

            result.add(new Recommendation(
                    id,
                    definition.getName(),
                    definition.getRewardFocus()
                            + get(344),
                    score,
                    verified ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    guidance,
                    safetyFor(definition)
            ));
        }

        result.sort(Comparator.comparingDouble(Recommendation::getScore).reversed());
        if (result.size() > 4) return new ArrayList<>(result.subList(0, 4));
        return result;
    }

    private static Guidance verificationGuidance(
            MinigameDefinition definition)
    {
        var activity = definition.getName();
        return new Guidance(
                get(350) + activity
                        + " setup equipped.",
                get(351),
                get(1492) + activity + ".",
                definition.getRewardFocus() + ".");
    }

    private static Guidance forestryGuidance(
            AccountSnapshot account, boolean verified,
            ItemRequirementResult itemResult)
    {
        var level = account.level(net.runelite.api.Skill.WOODCUTTING);
        var f2p = account.membership() != MembershipStatus.P2P;
        String tree;
        String location;
        if (level < 30)
        {
            tree = "oak trees";
            location = get(352);
        }
        else if (f2p || level < 45)
        {
            tree = "willow trees";
            location = get(353);
        }
        else if (level < 60)
        {
            tree = "maple trees";
            location = get(354);
        }
        else
        {
            tree = "yew trees";
            location = get(355);
        }
        boolean uim = AccountMode.fromTypeCode(account.modeCode())
                == AccountMode.ULTIMATE_IRONMAN;
        String loop = uim
                ? get(356) + tree
                        + get(357)
                : get(345) + tree
                        + get(346);
        return new Guidance(
                verified
                        ? loop
                        : itemResult.getAction() + get(1493),
                verified ? get(347)
                        : itemResult.getAction(),
                location + ".",
                get(348)
                        + (uim ? get(349) : ""));
    }

    private static SafetyEvidence safetyFor(MinigameDefinition definition)
    {
        if (definition.getRiskLevel() == RiskLevel.HIGH
                || definition.getRiskLevel() == RiskLevel.IRREVERSIBLE)
            return SafetyEvidence.potentiallyIrreversible(
                    definition.isFreeToPlay());
        if (definition.isCombatActivity())
            return SafetyEvidence.potentiallyIrreversible(
                    definition.isFreeToPlay());
        if (definition.getPrimarySkill() != null)
            return SafetyEvidence.skill(definition.isFreeToPlay(),
                    definition.getPrimarySkill());
        return SafetyEvidence.harmless(definition.isFreeToPlay());
    }
}

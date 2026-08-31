package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Lets an observed clue become actual DO NEXT work without making it spammy. */
@Singleton
public class ClueCandidateProvider implements StrategyCandidateProvider
{
    @Override
    public String getId()
    {
        return "clue-candidates";
    }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.getData() == null) return result;
        ClueSnapshot clue = context.getData().getClue();
        if (clue == null || !clue.isCluePresent()) return result;

        ClueTier tier = ClueTier.fromText(clue.getClueType());
        AccountSnapshot account = context.getData().getAccount();
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.getMembershipStatus();
        if (!tier.isAvailableFor(membership)) return result;

        // Keep one stable activity id across clue tiers so learned preference,
        // snoozes, and older profiles continue to work after this richer model.
        String id = "clue:pending";
        PreferenceProfile preferences = context.getPreferenceProfile();
        if (preferences.isOnCooldown(id)) return result;

        long age = Math.max(0L,
                System.currentTimeMillis() - clue.getFirstSeenAtMillis());
        double ageHours = age / 3_600_000.0;
        double score = 39.0
                + tier.getPriorityBonus()
                + Math.min(15.0, ageHours * 0.5)
                + preferences.weightFor(id) * 10.0;

        if (context.isCollectionistMode()) score += 6.0;
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN) score -= 6.0;
        if (context.getSessionIntent() == SessionIntent.QUICK_20_MIN) score += 4.0;
        if (context.getSessionIntent() == SessionIntent.AFK) score -= 8.0;
        if (context.getStrategyMode() == StrategyMode.EFFICIENT
                && (tier == ClueTier.BEGINNER || tier == ClueTier.EASY))
            score -= 7.0;

        String type = tier == ClueTier.UNKNOWN
                ? "clue"
                : tier.name().toLowerCase() + " clue";
        ClueStepSnapshot step = clue.getCurrentStep();
        StringBuilder reason = new StringBuilder();
        reason.append("Clears the pending ").append(type)
                .append(Text.get(132));
        if (context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            reason.append(Text.get(134));
        }
        if (step == null)
        {
            reason.append(Text.get(135));
        }
        else
            reason.append(" RuneLite identified the current ")
                    .append(step.getKind()).append(" and its concrete setup.");

        boolean hardcore = context.getAccountMode() == AccountMode.HARDCORE_IRONMAN
                || context.getAccountMode() == AccountMode.HARDCORE_GROUP_IRONMAN;
        boolean wildernessHold = step != null && step.isWilderness()
                && (!context.isAllowWildernessMethods() || hardcore);
        if (wildernessHold)
        {
            score -= hardcore ? 30.0 : 18.0;
            reason.append(hardcore
                    ? Text.get(136)
                    : Text.get(137));
        }

        String title;
        String candidateId;
        RecommendationGuidance guidance;
        RecommendationConfidence confidence;
        if (step == null)
        {
            title = "Inspect " + type;
            candidateId = "verify:clue-current-step";
            guidance = new RecommendationGuidance(
                    Text.get(138),
                    null, "Inventory", Text.get(139));
            confidence = RecommendationConfidence.CHECK_NEEDED;
        }
        else if (wildernessHold)
        {
            title = "Hold " + type + " — Wilderness step";
            candidateId = "prepare:clue-wilderness-hold";
            guidance = new RecommendationGuidance(
                    context.getAccountMode() == AccountMode.ULTIMATE_IRONMAN
                            ? Text.get(140)
                            : Text.get(141),
                    supplies(step), step.getLocation(),
                    Text.get(133));
            confidence = RecommendationConfidence.CHECK_NEEDED;
        }
        else
        {
            title = (step.requiresPreparation() ? "Prepare " : "Do ")
                    + type + ": " + step.getKind();
            candidateId = step.requiresPreparation()
                    ? "prepare:clue-current-step" : id;
            guidance = new RecommendationGuidance(step.getAction(),
                    supplies(step), step.getLocation(), note(step));
            // RuneLite proves the step, not every quest/access requirement.
            // Beginner steps are the only F2P-safe tier and can lead when no
            // additional setup, combat, light or Wilderness evidence remains.
            confidence = tier == ClueTier.BEGINNER
                    && !step.requiresPreparation()
                    ? RecommendationConfidence.VERIFIED
                    : RecommendationConfidence.CHECK_NEEDED;
        }

        result.add(new Recommendation(
                candidateId,
                title,
                reason.toString(),
                score,
                confidence,
                guidance,
                step != null && step.hasEnemy()
                        ? CandidateSafetyEvidence.potentiallyIrreversible(
                                tier == ClueTier.BEGINNER)
                        : CandidateSafetyEvidence.harmless(
                                tier == ClueTier.BEGINNER),
                RecommendationStrategicValue.builder()
                        .accountModeFit(context.getAccountMode()
                                == AccountMode.ULTIMATE_IRONMAN ? -0.35 : 0.0)
                        .riskBurden(step != null && (step.isWilderness()
                                || step.hasEnemy()) ? 0.8 : 0.0)
                        .opportunityCost(context.getSessionIntent()
                                == SessionIntent.AFK ? 0.7 : 0.15)
                        .evidence(step == null
                                ? "runelite:clue-tier"
                                : "runelite:clue-current-step")
                        .build()
        ));
        return result;
    }

    private static String supplies(ClueStepSnapshot step)
    {
        if (step == null) return null;
        List<String> values = new ArrayList<>(step.getItemRequirements());
        if (step.isRequiresSpade()) values.add("Spade");
        if (step.isRequiresLight()) values.add("Light source");
        if (step.hasEnemy()) values.add("Food and a legal setup for " + step.getEnemy());
        if (values.isEmpty()) return null;
        return String.join(", ", values);
    }

    private static String note(ClueStepSnapshot step)
    {
        if (step == null) return null;
        List<String> values = new ArrayList<>();
        if (step.hasStashUnit())
            values.add("STASH: " + display(step.getStashUnit())
                    + "; contents count only when observed");
        if (step.isWilderness()) values.add("Wilderness step");
        return values.isEmpty() ? null : String.join(". ", values) + ".";
    }

    private static String display(String value)
    {
        if (value == null || value.isEmpty()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

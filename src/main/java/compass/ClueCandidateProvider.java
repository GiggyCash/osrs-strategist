package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Singleton;

/** Lets an observed clue become actual DO NEXT work without making it spammy. */
@Singleton
public class ClueCandidateProvider implements CandidateProvider
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
        if (context == null || context.data() == null) return result;
        var clue = context.data().clue();
        if (clue == null || !clue.isCluePresent()) return result;

        var tier = ClueTier.fromText(clue.getClueType());
        var account = context.data().account();
        MembershipStatus membership = account == null
                ? MembershipStatus.UNKNOWN
                : account.membership();
        if (!tier.isAvailableFor(membership)) return result;

        // Keep one stable activity id across clue tiers so learned preference,
        // snoozes, and older profiles continue to work after this richer model.
        var id = "clue:pending";
        var preferences = context.preferenceProfile();
        if (preferences.isOnCooldown(id)) return result;

        long age = Math.max(0L,
                System.currentTimeMillis() - clue.getFirstSeenAtMillis());
        var ageHours = age / 3_600_000.0;
        double score = 39.0
                + tier.getPriorityBonus()
                + Math.min(15.0, ageHours * 0.5)
                + preferences.weightFor(id) * 10.0;

        if (context.collectionist()) score += 6.0;
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN) score -= 6.0;
        if (context.intent() == SessionIntent.QUICK_20_MIN) score += 4.0;
        if (context.intent() == SessionIntent.AFK) score -= 8.0;
        if (context.mode() == StrategyMode.EFFICIENT
                && (tier == ClueTier.BEGINNER || tier == ClueTier.EASY))
            score -= 7.0;

        String type = tier == ClueTier.UNKNOWN
                ? "clue"
                : tier.name().toLowerCase() + " clue";
        var step = clue.getCurrentStep();
        var reason = new StringBuilder();
        reason.append(get(1361)).append(type)
                .append(get(132));
        if (context.accountMode() == AccountMode.ULTIMATE_IRONMAN)
        {
            reason.append(get(134));
        }
        if (step == null)
        {
            reason.append(get(135));
        }
        else
            reason.append(get(1362))
                    .append(step.getKind()).append(get(1363));

        boolean hardcore = context.accountMode() == AccountMode.HARDCORE_IRONMAN
                || context.accountMode() == AccountMode.HARDCORE_GROUP_IRONMAN;
        boolean wildernessHold = step != null && step.isWilderness()
                && (!context.allowsWilderness() || hardcore);
        if (wildernessHold)
        {
            score -= hardcore ? 30.0 : 18.0;
            reason.append(hardcore
                    ? get(136)
                    : get(137));
        }

        String title;
        String candidateId;
        Guidance guidance;
        Confidence confidence;
        if (step == null)
        {
            title = "Inspect " + type;
            candidateId = "verify:clue-current-step";
            guidance = new Guidance(
                    get(138),
                    null, "Inventory", get(139));
            confidence = Confidence.CHECK_NEEDED;
        }
        else if (wildernessHold)
        {
            title = "Hold " + type + get(1364);
            candidateId = "prepare:clue-wilderness-hold";
            guidance = new Guidance(
                    context.accountMode() == AccountMode.ULTIMATE_IRONMAN
                            ? get(140)
                            : get(141),
                    supplies(step), step.getLocation(),
                    get(133));
            confidence = Confidence.CHECK_NEEDED;
        }
        else
        {
            title = (step.requiresPreparation() ? "Prepare " : "Do ")
                    + type + ": " + step.getKind();
            candidateId = step.requiresPreparation()
                    ? "prepare:clue-current-step" : id;
            guidance = new Guidance(step.getAction(),
                    supplies(step), step.getLocation(), note(step));
            // RuneLite proves the step, not every quest/access requirement.
            // Beginner steps are the only F2P-safe tier and can lead when no
            // additional setup, combat, light or Wilderness evidence remains.
            confidence = tier == ClueTier.BEGINNER
                    && !step.requiresPreparation()
                    ? Confidence.VERIFIED
                    : Confidence.CHECK_NEEDED;
        }

        result.add(new Recommendation(
                candidateId,
                title,
                reason.toString(),
                score,
                confidence,
                guidance,
                step != null && step.hasEnemy()
                        ? SafetyEvidence.potentiallyIrreversible(
                                tier == ClueTier.BEGINNER)
                        : SafetyEvidence.harmless(
                                tier == ClueTier.BEGINNER),
                StrategicValue.builder()
                        .accountModeFit(context.accountMode()
                                == AccountMode.ULTIMATE_IRONMAN ? -0.35 : 0.0)
                        .riskBurden(step != null && (step.isWilderness()
                                || step.hasEnemy()) ? 0.8 : 0.0)
                        .opportunityCost(context.intent()
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
        if (step.hasEnemy()) values.add(get(1365) + step.getEnemy());
        if (values.isEmpty()) return null;
        return String.join(", ", values);
    }

    private static String note(ClueStepSnapshot step)
    {
        if (step == null) return null;
        List<String> values = new ArrayList<>();
        if (step.hasStashUnit())
            values.add("STASH: " + display(step.getStashUnit())
                    + get(1366));
        if (step.isWilderness()) values.add("Wilderness step");
        return values.isEmpty() ? null : String.join(". ", values) + ".";
    }

    private static String display(String value)
    {
        if (value == null || value.isEmpty()) return "";
        var lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}

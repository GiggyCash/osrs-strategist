package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stable semantic collapse for equivalent actions emitted by several domains. */
public final class RecommendationDeduplicator
{
    private static final Pattern TRAIN_TO = Pattern.compile(
            "(?i)^train\\s+(.+?)(?:\\s+from\\s+\\d+)?\\s+to\\s+(\\d+)(?:\\D.*)?$");

    public List<Recommendation> deduplicate(List<Recommendation> candidates)
    {
        Map<String, Recommendation> merged = new LinkedHashMap<>();
        if (candidates == null) return new ArrayList<>();
        for (Recommendation candidate : candidates)
        {
            if (candidate == null) continue;
            // Do not allow weaker evidence to borrow VERIFIED status from an
            // equivalent-looking action emitted by another provider.
            String key = semanticKey(candidate) + "|" + candidate.getConfidence();
            Recommendation previous = merged.get(key);
            merged.put(key, previous == null ? candidate : merge(previous, candidate));
        }
        return new ArrayList<>(merged.values());
    }

    String semanticKey(Recommendation candidate)
    {
        if (candidate == null) return "";
        TrainingPlan plan = candidate.getTrainingPlan();
        String activity = canonicalActivity(candidate, plan);
        if (activity != null) return "activity:" + activity;
        if (plan != null && plan.getMethod() != null
                && candidate.getTargetLevel() > 0)
            return "skill-level:" + normalize(plan.getMethod().getSkill().getName())
                    + ":" + candidate.getTargetLevel();

        Matcher training = TRAIN_TO.matcher(safe(candidate.getTitle()).trim());
        if (training.matches())
            return "skill-level:" + normalize(training.group(1))
                    + ":" + training.group(2);

        String id = safe(candidate.getId()).toLowerCase(Locale.ROOT);
        if (id.startsWith("quest:") || id.startsWith("diary:")
                || id.startsWith("clue:") || id.startsWith("stash:")
                || id.startsWith("gear:") || id.startsWith("upgrade:")
                || id.startsWith("pvm:") || id.startsWith("slayer:"))
            return family(id) + ":" + normalize(candidate.getTitle());
        return normalize(candidate.getTitle());
    }

    private static Recommendation merge(Recommendation first,
            Recommendation second)
    {
        Recommendation primary = better(first, second);
        Recommendation other = primary == first ? second : first;
        String reason = mergeText(primary.getReason(), other.getReason());
        double sharedBenefitBonus = sameText(first.getReason(), second.getReason())
                ? 0.0 : 3.0;
        return new Recommendation(primary.getId(), primary.getTitle(), reason,
                Math.max(first.getScore(), second.getScore()) + sharedBenefitBonus,
                primary.getTrainingPlan(), primary.getConfidence(),
                primary.getCurrentLevel(), primary.getTargetLevel(),
                primary.getGuidance(), primary.getSafetyEvidence())
                .withStrategicValue(first.getStrategicValue().merge(
                        second.getStrategicValue()));
    }

    private static String canonicalActivity(Recommendation candidate,
            TrainingPlan plan)
    {
        String id = safe(candidate.getId()).toLowerCase(Locale.ROOT);
        if (id.startsWith("minigame:")) return id.substring("minigame:".length());
        if (plan == null || plan.getMethod() == null) return null;
        String method = safe(plan.getMethod().getId()).toLowerCase(Locale.ROOT);
        switch (method)
        {
            case "firemaking_wintertodt": return "wintertodt";
            case "fishing_tempoross": return "tempoross";
            case "runecraft_gotr": return "guardians-of-the-rift";
            case "smithing_giants_foundry": return "giants-foundry";
            case "construction_mahogany_homes": return "mahogany-homes";
            case "farming_tithe": return "tithe-farm";
            case "mining_mlm": return "motherlode-mine";
            case "mining_volcanic": return "volcanic-mine";
            case "mining_blast_mine": return "blast-mine";
            case "thieving_pyramid": return "pyramid-plunder";
            case "fishing_aerial": return "aerial-fishing";
            case "fishing_drift_net": return "drift-net-fishing";
            case "thieving_artefacts": return "stealing-artefacts";
            case "woodcutting_forestry": return "forestry";
            case "mining_stars": return "shooting-stars";
            case "smithing_blast_furnace_gold":
            case "smithing_blast_furnace_bars": return "blast-furnace";
            default: return null;
        }
    }

    private static Recommendation better(Recommendation first,
            Recommendation second)
    {
        if (second.getScore() > first.getScore()) return second;
        if (second.getScore() < first.getScore()) return first;
        return safe(second.getId()).compareTo(safe(first.getId())) < 0
                ? second : first;
    }

    private static String mergeText(String first, String second)
    {
        String left = safe(first).trim();
        String right = safe(second).trim();
        if (right.isEmpty() || sameText(left, right)) return left;
        if (left.isEmpty()) return right;
        return left + " Also advances: " + right;
    }

    private static boolean sameText(String first, String second)
    {
        return normalize(first).equals(normalize(second));
    }

    private static String family(String id)
    {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(0, colon);
    }

    private static String normalize(String value)
    {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String safe(String value)
    {
        return value == null ? "" : value;
    }
}

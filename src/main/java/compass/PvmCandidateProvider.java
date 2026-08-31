package compass;
import static compass.Text.get;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Makes explicitly verified/realistic PvM assessments eligible for DO NEXT. */
@Singleton
public class PvmCandidateProvider implements CandidateProvider
{
    private final PvmActivityCatalog catalog;

    @Inject
    public PvmCandidateProvider(PvmActivityCatalog catalog)
    {
        this.catalog = catalog;
    }

    public PvmCandidateProvider()
    {
        this(new PvmActivityCatalog());
    }

    @Override
    public String getId() { return "pvm-candidates"; }

    @Override
    public List<Recommendation> candidates(StrategyContext context)
    {
        List<Recommendation> result = new ArrayList<>();
        if (context == null || context.data() == null
                || context.data().pvm() == null) return result;

        var mode = context.accountMode();
        var account = context.data().account();
        var preferences = context.preferenceProfile();
        for (Map.Entry<String, PvmReadiness> entry
                : context.data().pvm().getReadinessByActivity().entrySet())
        {
            var readiness = entry.getValue();
            if (readiness == null) continue;
            if (readiness.getConfidence() == Confidence.BLOCKED) continue;

            var definition = catalog.match(entry.getKey());
            if (definition != null)
            {
                if (account == null || !ContentAccessRules.isContentAvailable(
                        account.membership(), definition.isFreeToPlay())) continue;
                if (definition.isWilderness() && !context.allowsWilderness()) continue;
                if ((mode == AccountMode.HARDCORE_IRONMAN
                        || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                        && !definition.isHardcoreSafeByDefault()) continue;
            }
            else continue; // Unknown/future metadata cannot prove beta readiness.

            String normalizedKey = entry.getKey().startsWith("pvm:")
                    ? entry.getKey().substring(4) : entry.getKey();
            var id = "pvm:" + normalizedKey;
            if (preferences.isOnCooldown(id)) continue;
            var ready = readiness.isReadyForRecommendation();
            var relevant = progressionRelevant(definition, context);
            // A generic hiscore identity is not a reason to boss. Preparation
            // competes globally only when a goal/task makes the encounter
            // relevant and a curated readiness floor can name concrete work.
            if (!ready && (!catalog.hasCuratedReadinessProfile(definition.id)
                    || !relevant)) continue;

            double score = (ready ? 48.0 : 32.0)
                    + preferences.weightFor(id) * 10.0;
            if (relevant) score += 14.0;
            if (definition != null)
            {
                if (definition.isRaid()) score += 4.0;
                if (definition.getRiskLevel() == RiskLevel.HIGH
                        && AccountModePolicy.isRiskSensitive(mode)) score -= 8.0;
            }

            var title = definition == null ? entry.getKey() : definition.getName();
            String missing = readiness.getMissingRequirements().isEmpty()
                    ? "" : String.join("; ", readiness.getMissingRequirements());
            if (!ready && missing.trim().isEmpty()) continue;
            Guidance guidance = ready
                    ? readyGuidance(definition, title)
                    : new Guidance(
                            get(417) + title + ": " + missing + ".",
                            missing,
                            get(428),
                            get(433));
            result.add(new Recommendation(
                    id,
                    "Do " + title,
                    ready
                            ? get(434)
                            : get(435),
                    score,
                    ready ? Confidence.VERIFIED
                            : Confidence.CHECK_NEEDED,
                    guidance,
                    SafetyEvidence.potentiallyIrreversible(
                            definition.isFreeToPlay())
            ));
        }
        return result;
    }

    private static Guidance readyGuidance(
            PvmActivityDefinition definition, String title)
    {
        if (definition != null && "pvm:tztok_jad".equals(definition.id))
        {
            return new Guidance(
                    get(436),
                    get(437),
                    get(438),
                    get(439));
        }
        if (definition != null && "pvm:obor".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(418),
                    get(419),
                    get(420));
        if (definition != null && "pvm:bryophyta".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(421),
                    get(422),
                    get(423));
        if (definition != null && "pvm:scurrius".equals(definition.id))
            return simpleReadyGuidance(title,
                    get(424),
                    get(1338),
                    get(425));
        return new Guidance(
                "Attempt " + title + get(426),
                get(427),
                get(429),
                get(430));
    }

    private static Guidance simpleReadyGuidance(String title,
            String action, String location, String supplies)
    {
        return new Guidance(action, supplies, location,
                get(431)
                        + title + get(432));
    }

    private static boolean progressionRelevant(PvmActivityDefinition definition,
            StrategyContext context)
    {
        if (definition == null || context == null) return false;
        var id = definition.id;
        var goal = context.goal();
        if (goal == GoalType.BOWFA)
            return id.contains("gauntlet");
        if (goal == GoalType.FIRE_CAPE)
            return id.endsWith("tztok_jad");
        if (goal == GoalType.INFERNAL_CAPE)
            return id.endsWith("tztok_jad") || id.endsWith("tzkal_zuk");
        if (goal == GoalType.RAID_READY)
            return definition.isRaid();
        if (goal == GoalType.ELITE_COMBAT_ACHIEVEMENTS)
            return catalogChallengeEncounter(id);

        SlayerSnapshot slayer = context.data() == null
                ? null : context.data().slayer();
        if (slayer == null || !slayer.hasTask()) return false;
        var task = Names.words(slayer.getTaskName());
        var boss = Names.words(definition.getName());
        return boss.contains(task) || task.contains(boss)
                || (id.endsWith("kraken") && task.contains("kraken"))
                || (id.endsWith("cerberus") && task.contains("hellhound"))
                || (id.endsWith(get(1809)) && task.contains("hydra"))
                || (id.endsWith("araxxor") && task.contains("araxyte"));
    }

    private static boolean catalogChallengeEncounter(String id)
    {
        return id != null && (id.contains("gauntlet") || id.contains("raid")
                || id.contains(get(1810)) || id.contains(get(1811))
                || id.contains(get(1812)) || id.endsWith("tzkal_zuk")
                || id.endsWith("sol_heredit") || id.endsWith("nex"));
    }

}

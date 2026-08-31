package compass;

import java.util.Map;
import net.runelite.api.Skill;

/** Development-time machine-readable census entry point. */
public final class ContentCensusReporter
{
    private ContentCensusReporter() { }

    public static void main(String[] args)
    {
        QuestRequirementCensus quests = new QuestRequirementCensus();
        TrainingMethodCensus training = new TrainingMethodCensus();
        StashUnitCensus stash = new StashUnitCensus();
        DiaryTaskCatalog diaries = new DiaryTaskCatalog();
        TransportCatalog transports = new TransportCatalog();
        StringBuilder output = new StringBuilder();
        boolean includeUnresolved = args.length == 0
                || !"--summary".equals(args[0]);
        output.append("{\n  \"quests\": {")
                .append("\n    \"total\": ").append(quests.getTotalQuests())
                .append(",\n    \"withItemRequirements\": ").append(quests.getQuestsWithItemRequirements())
                .append(",\n    \"fullyExecutableOrVerifiedNone\": ").append(quests.getFullyExecutable())
                .append(",\n    \"partiallyExecutable\": ").append(quests.getPartiallyExecutable())
                .append(",\n    \"rawOnly\": ").append(quests.getRawOnly())
                .append(",\n    \"unsupportedExpressions\": ").append(quests.getUnsupportedExpressions())
                .append(",\n    \"explicitCheckExpressions\": ").append(quests.getExplicitCheckExpressions())
                .append(",\n    \"parseFailures\": ").append(quests.getParseFailures())
                .append(",\n    \"sourceMissingFields\": ").append(quests.getSourceMissingFields());
        if (!quests.getRawOnlyQuests().isEmpty())
        {
            output.append(",\n    \"rawOnlyQuests\": [");
            for (int index = 0; index < quests.getRawOnlyQuests().size(); index++)
            {
                if (index > 0) output.append(',');
                output.append('\"').append(json(quests.getRawOnlyQuests().get(index)))
                        .append('\"');
            }
            output.append(']');
        }
        if (!includeUnresolved)
        {
            output.append("\n  },");
            appendTraining(output, training);
            output.append(',');
            appendStash(output, stash);
            output.append(',');
            appendDiaries(output, diaries);
            output.append(',');
            appendTransports(output, transports);
            output.append(',');
            appendCombatAndAcquisition(output);
            output.append("\n}");
            System.out.println(output);
            return;
        }
        output.append(",\n    \"unresolved\": [");
        boolean first = true;
        for (QuestRequirementCensus.Unresolved entry : quests.getUnresolved())
        {
            if (!first) output.append(',');
            first = false;
            output.append("\n      {\"quest\":\"").append(json(entry.getQuest()))
                    .append("\",\"field\":\"").append(json(entry.getField()))
                    .append("\",\"rawEvidence\":\"").append(json(entry.getRawEvidence()))
                    .append("\",\"reason\":\"").append(json(entry.getReason()))
                    .append("\"}");
        }
        if (!first) output.append('\n');
        output.append("    ]\n  },");
        appendTraining(output, training);
        output.append(',');
        appendStash(output, stash);
        output.append(',');
        appendDiaries(output, diaries);
        output.append(',');
        appendTransports(output, transports);
        output.append(',');
        appendCombatAndAcquisition(output);
        output.append("\n}");
        System.out.println(output);
    }

    private static void appendDiaries(StringBuilder output,
            DiaryTaskCatalog diaries)
    {
        int requirements = 0;
        int alternativeChecks = 0;
        int transportTasks = 0;
        for (DiaryTaskDefinition task : diaries.all())
        {
            requirements += task.getRequirements().size();
            if (task.isTransportRelevant()) transportTasks++;
            for (DiaryTaskRequirement requirement : task.getRequirements())
                if (requirement.getKind()
                        == DiaryTaskRequirement.Kind.ALTERNATIVE_CHECK)
                    alternativeChecks++;
        }
        output.append("\n  \"diaryTasks\": {")
                .append("\n    \"regions\": ").append(diaries.census().size())
                .append(",\n    \"tiers\": 48")
                .append(",\n    \"tasks\": ").append(diaries.all().size())
                .append(",\n    \"structuredRequirements\": ")
                .append(requirements - alternativeChecks)
                .append(",\n    \"alternativeChecks\": ")
                .append(alternativeChecks)
                .append(",\n    \"transportRelevantTasks\": ")
                .append(transportTasks)
                .append("\n  }");
    }

    private static void appendTransports(StringBuilder output,
            TransportCatalog transports)
    {
        output.append("\n  \"transportSystems\": {")
                .append("\n    \"systems\": ").append(transports.all().size())
                .append(",\n    \"categories\": ")
                .append(TransportCategory.values().length)
                .append(",\n    \"reusableFanOutUses\": ")
                .append(transports.all().stream()
                        .mapToInt(TransportDefinition::getFanOut).sum())
                .append("\n  }");
    }

    private static void appendCombatAndAcquisition(StringBuilder output)
    {
        SlayerTaskIdentityCatalog slayerIdentities =
                new SlayerTaskIdentityCatalog();
        SlayerTaskProfileCatalog slayerProfiles =
                new SlayerTaskProfileCatalog();
        int mapped = 0;
        for (SlayerTaskIdentity identity : slayerIdentities.all())
            if (slayerProfiles.profileFor(identity.getAssignment()) != null)
                mapped++;
        int aliases = slayerProfiles.all().stream()
                .mapToInt(profile -> profile.getAliases().size()).sum();
        PvmActivityCatalog activities = new PvmActivityCatalog();
        PvmPreparationProfileCatalog preparation =
                new PvmPreparationProfileCatalog();
        output.append("\n  \"combatAndAcquisition\": {")
                .append("\n    \"pvmIdentities\": ")
                .append(activities.all().size())
                .append(",\n    \"pvmPreparationProfiles\": ")
                .append(preparation.all().size())
                .append(",\n    \"pvmLocallyVerifiedProfiles\": ")
                .append(new PvmEvidenceProfileCatalog().size())
                .append(",\n    \"slayerCanonicalIdentities\": ")
                .append(slayerIdentities.all().size())
                .append(",\n    \"slayerMappedIdentities\": ")
                .append(mapped)
                .append(",\n    \"slayerProfiles\": ")
                .append(slayerProfiles.all().size())
                .append(",\n    \"slayerAliases\": ").append(aliases)
                .append(",\n    \"gearAcquisitionTargets\": ")
                .append(new GearAcquisitionCatalog().all().size())
                .append(",\n    \"gearContextLadders\": ")
                .append(new GearProgressionCatalog().all().size())
                .append(",\n    \"gearDecisionKinds\": ")
                .append(GearDecisionKind.values().length)
                .append(",\n    \"deterministicResourceDefinitions\": ")
                .append(new ResourceDependencyCatalog().size())
                .append(",\n    \"accountAwareResourceSourceFamilies\": ")
                .append(new ResourceSourceCatalog().all().size())
                .append("\n  }");
    }

    private static void appendStash(StringBuilder output,
            StashUnitCensus stash)
    {
        output.append("\n  \"stashUnits\": {")
                .append("\n    \"total\": ").append(stash.getTotal())
                .append(",\n    \"missingEvidence\": ")
                .append(stash.getMissingEvidence())
                .append(",\n    \"wilderness\": ")
                .append(stash.getWildernessUnits())
                .append(",\n    \"byTier\": {");
        boolean first = true;
        for (Map.Entry<ClueTier, Integer> entry : stash.getByTier().entrySet())
        {
            if (!first) output.append(',');
            first = false;
            output.append("\n      \"")
                    .append(entry.getKey().name().toLowerCase())
                    .append("\": ").append(entry.getValue());
        }
        if (!first) output.append('\n');
        output.append("    }\n  }");
    }

    private static void appendTraining(StringBuilder output,
            TrainingMethodCensus training)
    {
        output.append("\n  \"trainingMethods\": {")
                .append("\n    \"skills\": ").append(training.getSkillCount())
                .append(",\n    \"curatedMethods\": ")
                .append(training.getCuratedMethodCount())
                .append(",\n    \"runeLiteActions\": ")
                .append(training.getRuneLiteActionCount())
                .append(",\n    \"skillsWithRuneLiteActions\": ")
                .append(training.getSkillsWithRuneLiteActions())
                .append(",\n    \"duplicateIds\": ")
                .append(training.getDuplicateIds())
                .append(",\n    \"invalidMethods\": ")
                .append(training.getInvalidMethods().size())
                .append(",\n    \"perSkill\": {");
        boolean first = true;
        for (Map.Entry<Skill, TrainingMethodCensus.SkillCoverage> entry
                : training.getBySkill().entrySet())
        {
            if (!first) output.append(',');
            first = false;
            TrainingMethodCensus.SkillCoverage value = entry.getValue();
            output.append("\n      \"").append(entry.getKey().getName())
                    .append("\": {\"curated\":")
                    .append(value.getCuratedMethods())
                    .append(",\"runeLiteActions\":")
                    .append(value.getRuneLiteActions()).append('}');
        }
        if (!first) output.append('\n');
        output.append("    }\n  }");
    }

    private static String json(String value)
    {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

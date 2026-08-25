package com.udderlywet.osrsstrategist;

/** Development-time machine-readable census entry point. */
public final class ContentCensusReporter
{
    private ContentCensusReporter() { }

    public static void main(String[] args)
    {
        QuestRequirementCensus quests = new QuestRequirementCensus();
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
            output.append("\n  }\n}");
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
        output.append("    ]\n  }\n}");
        System.out.println(output);
    }

    private static String json(String value)
    {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

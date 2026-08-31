package compass;

import java.util.*;

/**
 * Executable subset of the pinned authoritative quest item evidence.
 * Complex prose remains visible as explicit verification rather than guessed.
 */
public final class ImportedQuestItemRequirementCatalog
{
    private static final String RESOURCE = Text.get(1725);
    private final Map<String, Result> requirements;

    public ImportedQuestItemRequirementCatalog()
    {
        Map<String, Result> result = new LinkedHashMap<>();
        for (Entry entry : BundledCatalogLoader.array(RESOURCE, Entry[].class))
        {
            if (entry.quest == null || entry.result == null)
                throw new IllegalStateException(Text.get(1137) + RESOURCE);
            entry.result.freeze();
            if (result.put(Names.words(entry.quest), entry.result) != null)
                throw new IllegalStateException(Text.get(1138) + entry.quest);
        }
        requirements = Collections.unmodifiableMap(result);
    }

    public Result resultFor(String questName)
    {
        return requirements.get(Names.words(questName));
    }

    public int questCount() { return requirements.size(); }

    public long fullyExecutableCount()
    {
        return requirements.values().stream()
                .filter(Result::isFullyExecutable).count();
    }

    public long partiallyExecutableCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() != null
                        && !result.isFullyExecutable()).count();
    }

    public long rawOnlyCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() == null
                        && !result.getUnresolved().isEmpty()).count();
    }


    private static final class Entry
    {
        private String quest;
        private Result result;
    }

    /** Immutable executable evidence generated from the pinned source snapshot. */
    public static final class Result
    {
        private ItemRequirementExpression expression;
        private List<String> unresolved;
        private int parsedLineCount;

        private void freeze()
        {
            expression = expression == null ? null : expression.freeze();
            unresolved = unresolved == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(unresolved));
        }

        public ItemRequirementExpression getExpression() { return expression; }
        public List<String> getUnresolved() { return unresolved; }
        public int getParsedLineCount() { return parsedLineCount; }
        public boolean isFullyExecutable() { return unresolved.isEmpty(); }
        public boolean isDeterministicallyExecutable()
        {
            return unresolved.isEmpty() && countChecks(expression) == 0;
        }
        public int getCheckNeededExpressionCount() { return countChecks(expression); }
        private static int countChecks(ItemRequirementExpression value)
        {
            if (value == null) return 0;
            var count = value.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED ? 1 : 0;
            for (ItemRequirementExpression child : value.getChildren())
                count += countChecks(child);
            return count;
        }
    }
}

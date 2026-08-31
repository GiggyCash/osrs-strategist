package compass;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LiveDiaryStateReaderTest
{
    private final DiaryTaskCatalog catalog = new DiaryTaskCatalog();

    @Test
    public void readsOnlyExactRowsAndPreservesStrikeState()
    {
        DiaryTaskDefinition teleport = catalog.forTier(
                "Ardougne", DiaryTier.EASY).get(0);
        DiaryTaskDefinition cake = catalog.forTier(
                "Ardougne", DiaryTier.EASY).get(1);

        Map<String, Boolean> observed = LiveDiaryStateReader
                .observedTasksFromRows("Ardougne", Arrays.asList(
                        "Ardougne Area Tasks",
                        "<str>" + teleport.getTask() + "</str>",
                        cake.getTask()), catalog);

        assertEquals(Boolean.TRUE, observed.get(teleport.getId()));
        assertEquals(Boolean.FALSE, observed.get(cake.getId()));
        assertEquals(2, observed.size());
    }

    @Test
    public void rejoinsWrappedTaskRowsWithoutInferringAbsentTasks()
    {
        DiaryTaskDefinition task = catalog.forTier(
                "Ardougne", DiaryTier.MEDIUM).get(0);
        String instruction = task.getTask();
        int split = instruction.indexOf("Ardougne zoo");

        Map<String, Boolean> observed = LiveDiaryStateReader
                .observedTasksFromRows("Ardougne", Arrays.asList(
                        "<str>" + instruction.substring(0, split).trim(),
                        instruction.substring(split) + "</str>"), catalog);

        assertEquals(Boolean.TRUE, observed.get(task.getId()));
        assertEquals(1, observed.size());
    }

    @Test
    public void acceptsRuneLiteRequirementTextAppendedToTaskRow()
    {
        DiaryTaskDefinition task = catalog.forTier(
                "Ardougne", DiaryTier.EASY).get(1);

        Map<String, Boolean> observed = LiveDiaryStateReader
                .observedTasksFromRows("Ardougne", Arrays.asList(
                        task.getTask() + " (Thieving 5)"), catalog);

        assertEquals(Boolean.FALSE, observed.get(task.getId()));
    }
}

package compass;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContentCoverageManifestTest
{
    @Test
    public void everyRuneLiteQuestIdentityHasAnExplicitDisposition()
    {
        QuestKnowledgeCatalog knowledge = new QuestKnowledgeCatalog();
        assertEquals(Quest.values().length, knowledge.all().size());
        assertEquals(211, knowledge.all().size());
        assertEquals(0, knowledge.all().values().stream()
                .filter(QuestDefinition::hasFieldUncertainty).count());
        assertNotNull(knowledge.definitionFor("Watchtower"));
        assertNotNull(knowledge.definitionFor("Enter the Abyss"));
    }

    @Test
    public void minigameCensusHasNoSilentOrDuplicateEntries()
    {
        MinigameCoverageManifest manifest = new MinigameCoverageManifest();
        assertEquals(43, manifest.all().size());
        assertUniqueAndExplained(manifest.all());
        assertTrue(manifest.all().stream().anyMatch(entry ->
                entry.getName().equals("Burthorpe Games Room")
                        && entry.getState() == ContentCoverageState.NOT_PROGRESSION_RELEVANT));
        assertEquals(42, count(manifest.all(), ContentCoverageState.STRUCTURED));
        assertEquals(0, count(manifest.all(), ContentCoverageState.CONSERVATIVE_FAIL_CLOSED));
        assertEquals(1, count(manifest.all(), ContentCoverageState.NOT_PROGRESSION_RELEVANT));
    }

    @Test
    public void everyRuneLiteHiscoreBossHasAnExplicitCoverageState()
    {
        PvmCoverageManifest manifest = new PvmCoverageManifest();
        assertEquals(new PvmActivityCatalog().all().size(), manifest.all().size());
        assertEquals(71, manifest.all().size());
        assertUniqueAndExplained(manifest.all());
        assertEquals(4, count(manifest.all(), ContentCoverageState.STRUCTURED));
        assertEquals(67, count(manifest.all(), ContentCoverageState.PARTIAL_PREPARATION));
        assertEquals(0, count(manifest.all(), ContentCoverageState.CONSERVATIVE_FAIL_CLOSED));
        assertEquals(71, new PvmPreparationProfileCatalog().all().size());
    }

    @Test
    public void diaryAndTransportCensusesHaveNoSilentCoverageGaps()
    {
        DiaryTaskCatalog diaries = new DiaryTaskCatalog();
        assertEquals(378, diaries.all().size());
        assertEquals(12, diaries.census().size());
        assertEquals(48, diaries.census().values().stream()
                .mapToInt(Map::size).sum());

        TransportCatalog transports = new TransportCatalog();
        assertEquals(41, transports.all().size());
        assertEquals(TransportCategory.values().length,
                transports.all().stream().map(TransportDefinition::getCategory)
                        .collect(java.util.stream.Collectors.toSet()).size());
        assertEquals(41, ids(transports.all().stream()
                .map(TransportDefinition::getId).toArray(String[]::new)).size());
    }

    @Test
    public void catalogIdentitiesAreUnique()
    {
        Set<String> questNames = new HashSet<>();
        for (Quest quest : Quest.values()) questNames.add(quest.getName());
        for (QuestDefinition definition : new QuestKnowledgeCatalog().all().values())
            assertTrue("quest definition is not a RuneLite identity: "
                    + definition.getName(), questNames.contains(definition.getName()));

        Set<String> minigameCensusIds = new HashSet<>();
        for (ContentCoverageEntry entry : new MinigameCoverageManifest().all())
            minigameCensusIds.add(entry.getId());
        for (MinigameDefinition definition : new MinigameCatalog().all())
            assertTrue("minigame definition is absent from census: "
                    + definition.getId(), minigameCensusIds.contains(definition.getId()));

        assertEquals(new MinigameCatalog().all().size(),
                ids(new MinigameCatalog().all().stream()
                        .map(MinigameDefinition::getId).toArray(String[]::new)).size());
        assertEquals(new SlayerTaskProfileCatalog().all().size(),
                ids(new SlayerTaskProfileCatalog().all().stream()
                        .map(SlayerTaskProfile::getId).toArray(String[]::new)).size());
        // RuneLite's DB-backed Slayer task rows can lead its older plugin Task
        // enum when newly released assignments arrive (currently Venators).
        assertEquals(147, new SlayerTaskProfileCatalog().all().size());

        SlayerTaskIdentityCatalog canonical = new SlayerTaskIdentityCatalog();
        assertEquals(151, canonical.all().size());
        for (SlayerTaskIdentity identity : canonical.all())
            assertNotNull(identity.getAssignment(),
                    new SlayerTaskProfileCatalog().profileFor(
                            identity.getAssignment()));

        Set<String> aliases = new HashSet<>();
        for (SlayerTaskProfile profile : new SlayerTaskProfileCatalog().all())
            for (String alias : profile.getAliases())
                assertTrue("duplicate Slayer alias: " + alias,
                        aliases.add(alias.toLowerCase(java.util.Locale.ROOT).trim()));
        assertEquals(230, aliases.size());
    }

    private static void assertUniqueAndExplained(List<ContentCoverageEntry> entries)
    {
        Set<String> ids = new HashSet<>();
        for (ContentCoverageEntry entry : entries)
        {
            assertTrue(ids.add(entry.getId()));
            assertNotNull(entry.getState());
            assertFalse(entry.getReason().trim().isEmpty());
            assertFalse(entry.getProvenance().trim().isEmpty());
        }
    }

    private static long count(List<ContentCoverageEntry> entries,
            ContentCoverageState state)
    {
        return entries.stream().filter(entry -> entry.getState() == state).count();
    }

    private static Set<String> ids(String... values)
    {
        Set<String> result = new HashSet<>();
        java.util.Collections.addAll(result, values);
        return result;
    }
}

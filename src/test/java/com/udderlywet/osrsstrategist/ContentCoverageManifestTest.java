package com.udderlywet.osrsstrategist;

import java.util.HashSet;
import java.util.List;
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
        QuestCoverageManifest manifest = new QuestCoverageManifest();
        assertEquals(Quest.values().length, manifest.all().size());
        assertEquals(19, manifest.miniquestCount());
        assertEquals(19, manifest.all().stream().filter(entry ->
                manifest.isMiniquest(entry.getName())
                        && entry.getState() == ContentCoverageState.STRUCTURED).count());
        assertEquals(0, manifest.all().stream().filter(entry ->
                manifest.isMiniquest(entry.getName())
                        && entry.getState() == ContentCoverageState.CONSERVATIVE_FAIL_CLOSED).count());
        assertUniqueAndExplained(manifest.all());
        assertTrue(manifest.all().stream().anyMatch(entry ->
                entry.getName().equals("Watchtower")
                        && entry.getState() == ContentCoverageState.STRUCTURED));
        assertEquals(211, count(manifest.all(), ContentCoverageState.STRUCTURED));
        assertEquals(0, count(manifest.all(),
                ContentCoverageState.CONSERVATIVE_FAIL_CLOSED));
        assertEquals(0, new QuestKnowledgeCatalog().all().values().stream()
                .filter(QuestDefinition::hasFieldUncertainty).count());
        assertTrue(manifest.all().stream().anyMatch(entry ->
                entry.getName().equals("Enter the Abyss")
                        && entry.getState() == ContentCoverageState.STRUCTURED));
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
        assertEquals(147, new SlayerTaskProfileCatalog().all().size());

        Set<String> aliases = new HashSet<>();
        for (SlayerTaskProfile profile : new SlayerTaskProfileCatalog().all())
            for (String alias : profile.getAliases())
                assertTrue("duplicate Slayer alias: " + alias,
                        aliases.add(alias.toLowerCase(java.util.Locale.ROOT).trim()));
        assertEquals(227, aliases.size());
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

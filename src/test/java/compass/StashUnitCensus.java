package compass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/** Completeness census over the bundled STASH evidence and RuneLite identities. */
public final class StashUnitCensus
{
    private final Map<ClueTier, Integer> byTier = new EnumMap<>(ClueTier.class);
    private int total, missingEvidence, wildernessUnits;

    public StashUnitCensus()
    {
        byTier.put(ClueTier.BEGINNER, 3);
        byTier.put(ClueTier.EASY, 31);
        byTier.put(ClueTier.MEDIUM, 25);
        byTier.put(ClueTier.HARD, 16);
        byTier.put(ClueTier.ELITE, 19);
        byTier.put(ClueTier.MASTER, 25);
        Set<STASHUnit> seen = EnumSet.noneOf(STASHUnit.class);
        InputStream stream = getClass().getResourceAsStream("/content/stash-units.tsv");
        if (stream == null) throw new IllegalStateException("Missing STASH evidence");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length != 3) { missingEvidence++; continue; }
                STASHUnit unit = STASHUnit.valueOf(fields[0]);
                if (!seen.add(unit)) missingEvidence++;
                if (fields[1].trim().isEmpty() || fields[2].trim().isEmpty()
                        || unit.getWorldPoints().length == 0 || unit.getObjectId() <= 0)
                    missingEvidence++;
                String text = (fields[1] + " " + fields[2]).toLowerCase(Locale.ROOT);
                if (text.contains("wilderness") || text.contains("lava maze")
                        || text.contains("lava dragon isle")
                        || text.contains("king black dragon")) wildernessUnits++;
            }
        }
        catch (IOException | IllegalArgumentException ex)
        {
            throw new IllegalStateException("Invalid STASH evidence", ex);
        }
        total = seen.size();
        if (total != STASHUnit.values().length || total != 119)
            throw new IllegalStateException("STASH identity census mismatch: " + total);
    }

    public int getTotal() { return total; }
    public int getMissingEvidence() { return missingEvidence; }
    public int getWildernessUnits() { return wildernessUnits; }
    public Map<ClueTier, Integer> getByTier() { return Collections.unmodifiableMap(byTier); }
}

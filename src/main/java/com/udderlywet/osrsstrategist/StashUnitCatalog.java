package com.udderlywet.osrsstrategist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/** Complete current RuneLite STASH catalogue, consumed locally at runtime. */
@Singleton
public final class StashUnitCatalog
{
    public static final int EXPECTED_CURRENT_COUNT = 119;
    public static final String PROVENANCE = "Generated from RuneLite 1.12.35 STASHUnit and EmoteClue sources";
    public static final String AUDITED_THROUGH = "2026-08-25";

    private final List<StashUnitDefinition> units;
    private final Map<STASHUnit, StashUnitDefinition> byUnit;

    public StashUnitCatalog()
    {
        Map<STASHUnit, Evidence> clues = loadEvidence();

        List<StashUnitDefinition> result = new ArrayList<>();
        Map<STASHUnit, StashUnitDefinition> index = new LinkedHashMap<>();
        for (STASHUnit unit : STASHUnit.values())
        {
            Evidence clue = clues.get(unit);
            if (clue == null)
                throw new IllegalStateException("Missing emote clue evidence for " + unit);
            StashUnitDefinition definition = new StashUnitDefinition(
                    unit, tierFor(unit), clue.clueText, clue.location);
            result.add(definition);
            index.put(unit, definition);
        }
        units = Collections.unmodifiableList(result);
        byUnit = Collections.unmodifiableMap(index);
    }

    private static Map<STASHUnit, Evidence> loadEvidence()
    {
        InputStream stream = StashUnitCatalog.class.getResourceAsStream(
                "/content/stash-units.tsv");
        if (stream == null)
            throw new IllegalStateException("Missing /content/stash-units.tsv");
        Map<STASHUnit, Evidence> result = new EnumMap<>(STASHUnit.class);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] fields = line.split("\\t", 3);
                if (fields.length != 3)
                    throw new IllegalStateException("Invalid STASH evidence line "
                            + lineNumber);
                STASHUnit unit;
                try
                {
                    unit = STASHUnit.valueOf(fields[0]);
                }
                catch (IllegalArgumentException ex)
                {
                    throw new IllegalStateException("Unknown STASH identity on line "
                            + lineNumber + ": " + fields[0], ex);
                }
                if (result.put(unit, new Evidence(fields[1], fields[2])) != null)
                    throw new IllegalStateException("Duplicate STASH evidence: "
                            + fields[0]);
            }
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to read STASH evidence", ex);
        }
        if (result.size() != EXPECTED_CURRENT_COUNT)
            throw new IllegalStateException("Expected " + EXPECTED_CURRENT_COUNT
                    + " STASH evidence rows, found " + result.size());
        return result;
    }

    public List<StashUnitDefinition> all() { return units; }
    public StashUnitDefinition get(STASHUnit unit) { return byUnit.get(unit); }

    public List<StashUnitDefinition> forTier(ClueTier tier)
    {
        List<StashUnitDefinition> result = new ArrayList<>();
        for (StashUnitDefinition unit : units)
            if (unit.getTier().getClueTier() == tier) result.add(unit);
        return Collections.unmodifiableList(result);
    }

    /**
     * RuneLite groups the original units by tier and appends new live units.
     * New upstream identities deliberately fail here until their tier is audited.
     */
    private static StashTierDefinition tierFor(STASHUnit unit)
    {
        int ordinal = unit.ordinal();
        if (ordinal <= 28) return StashTierDefinition.EASY;
        if (ordinal <= 50) return StashTierDefinition.MEDIUM;
        if (ordinal <= 65) return StashTierDefinition.HARD;
        if (ordinal <= 81) return StashTierDefinition.ELITE;
        if (ordinal <= 102) return StashTierDefinition.MASTER;
        switch (unit)
        {
            case NORTH_OF_MOUNT_KARUULM:
            case TWILIGHT_TEMPLE_MINE:
            case ORTUS_MEETS_PROUDSPIRE:
                return StashTierDefinition.MEDIUM;
            case GYPSY_TENT_ENTRANCE:
            case FINE_CLOTHES_ENTRANCE:
            case BOB_AXES_ENTRANCE:
                return StashTierDefinition.BEGINNER;
            case CRYSTALLINE_MAPLE_TREES:
            case CAM_TORUM_ENTRANCE:
            case WESTERN_SALVAGER_OVERLOOK:
            case BRITTLE_ISLE:
                return StashTierDefinition.MASTER;
            case CHARCOAL_BURNERS:
            case TEMPLE_SOUTHEAST_OF_THE_BAZAAR:
            case WINTUMBER_ISLAND:
                return StashTierDefinition.ELITE;
            case FORTIS_GRAND_MUSEUM:
            case PANDEMONIUM_BAR:
                return StashTierDefinition.EASY;
            case OUTSIDE_TWILIGHT_TEMPLE:
                return StashTierDefinition.HARD;
            default:
                throw new IllegalStateException("Unaudited STASH tier: " + unit);
        }
    }

    private static final class Evidence
    {
        private final String location;
        private final String clueText;

        private Evidence(String location, String clueText)
        {
            this.location = location;
            this.clueText = clueText;
        }
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/** Maintained census for minigames and major minigame-style progression loops. */
@Singleton
public class MinigameCoverageManifest
{
    public static final String PROVENANCE = "OSRS activity census; audited against current Wiki categories 2026-08-18";
    private static final String[][] CENSUS = {
            {"wintertodt", "Wintertodt"}, {"tempoross", "Tempoross"},
            {"guardians-of-the-rift", "Guardians of the Rift"},
            {"giants-foundry", "Giants' Foundry"}, {"mahogany-homes", "Mahogany Homes"},
            {"tithe-farm", "Tithe Farm"}, {"pest-control", "Pest Control"},
            {"barbarian-assault", "Barbarian Assault"}, {"fishing-trawler", "Fishing Trawler"},
            {"rogues-den", "Rogues' Den"}, {"mage-training-arena", "Mage Training Arena"},
            {"volcanic-mine", "Volcanic Mine"}, {"blast-mine", "Blast Mine"},
            {"hallowed-sepulchre", "Hallowed Sepulchre"}, {"pyramid-plunder", "Pyramid Plunder"},
            {"soul-wars", "Soul Wars"}, {"last-man-standing", "Last Man Standing"},
            {"castle-wars", "Castle Wars"}, {"trouble-brewing", "Trouble Brewing"},
            {"shades-of-mortton", "Shades of Mort'ton"},
            {"brimhaven-agility-arena", "Brimhaven Agility Arena"},
            {"gnome-restaurant", "Gnome Restaurant"}, {"temple-trekking", "Temple Trekking"},
            {"warriors-guild", "Warriors' Guild cyclopes"},
            {"motherlode-mine", "Motherlode Mine"}, {"barracuda-trials", "Barracuda Trials"},
            {"deep-sea-trawling", "Deep Sea Trawling"},
            {"nightmare-zone", "Nightmare Zone"}, {"blast-furnace", "Blast Furnace"},
            {"sorceresss-garden", "Sorceress's Garden"},
            {"tai-bwo-wannai-cleanup", "Tai Bwo Wannai Cleanup"},
            {"aerial-fishing", "Aerial Fishing"}, {"drift-net-fishing", "Drift Net Fishing"},
            {"champions-challenge", "Champions' Challenge"}, {"chompy-hunting", "Chompy bird hunting"},
            {"gnome-ball", "Gnome Ball"}, {"burthorpe-games-room", "Burthorpe Games Room"},
            {"rat-pits", "Rat Pits"}, {"stealing-artefacts", "Stealing artefacts"},
            {"underwater-agility-thieving", "Underwater Agility and Thieving"},
            {"shooting-stars", "Shooting Stars"}, {"forestry", "Forestry"},
            {"vale-totems", "Vale Totems"}
    };

    private final List<ContentCoverageEntry> entries;

    public MinigameCoverageManifest()
    {
        MinigameCatalog catalog = new MinigameCatalog();
        Map<String, MinigameDefinition> represented = new LinkedHashMap<>();
        for (MinigameDefinition definition : catalog.all())
            represented.put(definition.getId(), definition);
        List<ContentCoverageEntry> values = new ArrayList<>();
        for (String[] row : CENSUS)
        {
            boolean structured = represented.containsKey(row[0]);
            boolean notProgressionRelevant = "burthorpe-games-room".equals(row[0]);
            values.add(new ContentCoverageEntry(row[0], row[1],
                    structured ? ContentCoverageState.STRUCTURED
                            : notProgressionRelevant
                            ? ContentCoverageState.NOT_PROGRESSION_RELEVANT
                            : ContentCoverageState.CONSERVATIVE_FAIL_CLOSED,
                    structured
                            ? "The local catalog models membership, primary level, risk, attention and progression rewards; live unlock evidence is still required."
                            : notProgressionRelevant
                            ? "Verified social board-game activity with rankings but no XP, item, currency or account-progression reward; it is intentionally excluded from DO NEXT."
                            : "The activity is census-tracked, but its access and reward requirements are not fully modeled; it cannot enter the recommendation pool.",
                    PROVENANCE));
        }
        entries = Collections.unmodifiableList(values);
    }

    public List<ContentCoverageEntry> all() { return entries; }
}

package com.udderlywet.osrsstrategist;

import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/** One authoritative STASH identity and its exact RuneLite clue equipment evidence. */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
public final class StashUnitDefinition
{
    private final STASHUnit runeLiteUnit;
    private final StashTierDefinition tier;
    private final String clueText;
    private final String location;


    public String getId() { return "stash:" + runeLiteUnit.name().toLowerCase(); }
    public int getObjectId() { return runeLiteUnit.getObjectId(); }
    public WorldPoint[] getWorldPoints() { return runeLiteUnit.getWorldPoints().clone(); }
    /**
     * The maintained clue wording is retained verbatim because phrases such as
     * "any full barrows set" and "any piece" are semantic alternatives, not a
     * licence to invent one exact item.
     */
    public String getStoredEquipmentEvidence()
    {
        int equip = indexOfIgnoreCase(clueText, "equip ");
        int wear = indexOfIgnoreCase(clueText, "wear ");
        int start = equip >= 0 ? equip + 6 : (wear >= 0 ? wear + 5 : -1);
        return start < 0 ? clueText : clueText.substring(start).trim();
    }

    public boolean isWilderness()
    {
        String value = (location + " " + clueText).toLowerCase();
        return value.contains("wilderness") || value.contains("lava maze")
                || value.contains("lava dragon isle")
                || value.contains("king black dragon");
    }

    private static String readable(String value)
    {
        String normalized = value.startsWith("_") ? value.substring(1) : value;
        String[] words = normalized.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words)
        {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(word);
        }
        return result.toString();
    }

    private static int indexOfIgnoreCase(String value, String needle)
    {
        return value.toLowerCase().indexOf(needle.toLowerCase());
    }
}

package com.udderlywet.osrsstrategist;

import java.util.Locale;
import javax.inject.Singleton;

/**
 * Small item-family catalog for deterministic method inputs. Unknown items do
 * not receive a guessed GE or scarcity classification.
 */
@Singleton
public final class ResourcePipelinePolicyCatalog
{
    public ResourcePipelinePolicy forInput(String itemName)
    {
        String value = normalize(itemName);
        if (value.equals("spirit seed") || value.equals("crystal acorn"))
            return consumed(ResourceScarcity.SCARCE, false);
        if (containsAny(value, "rune", "essence", "bar", "plank", "nail",
                "log", "raw ", "grape", "jug of water", "feather",
                "arrowhead", "headless arrow", "dart tip", "unfinished bolt",
                "uncut ", "herb", "weed", "snape grass", "crushed nest",
                "red spiders eggs", "sapling", "seed"))
            return consumed(ResourceScarcity.ORDINARY, true);
        return null;
    }

    private static ResourcePipelinePolicy consumed(
            ResourceScarcity scarcity, boolean tradeable)
    {
        return new ResourcePipelinePolicy(
                ResourceUseKind.ONE_OFF_CONSUMABLE, scarcity, tradeable);
    }

    private static boolean containsAny(String value, String... terms)
    {
        for (String term : terms)
            if (value.contains(term)) return true;
        return false;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}

package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;

/** Common progression resource routes loaded from the bundled catalog. */
@Singleton
public class ResourceSourceCatalog
{
    private static final String RESOURCE = Text.get(708);
    private final List<ResourceSourceDefinition> sources;

    public ResourceSourceCatalog()
    {
        sources = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, ResourceSourceDefinition[].class)));
        for (ResourceSourceDefinition source : sources)
            if (source.getId() == null || source.getNameTokens() == null)
                throw new IllegalStateException("Incomplete resource source in " + RESOURCE);
    }

    public List<ResourceSourceDefinition> all() { return sources; }

    public List<ResourceSourceDefinition> match(String itemName)
    {
        String normalized = normalize(itemName);
        if (normalized.isEmpty()) return Collections.emptyList();
        List<ResourceSourceDefinition> result = new ArrayList<>();
        for (ResourceSourceDefinition source : sources)
        {
            if ("raw-fish".equals(source.getId()) && !normalized.startsWith("raw ")) continue;
            if ("cooked-food".equals(source.getId()) && normalized.startsWith("raw ")) continue;
            for (String token : source.getNameTokens())
                if (containsPhrase(normalized, normalize(token)))
                {
                    result.add(source);
                    break;
                }
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> suggestions(String itemName, AccountMode mode, boolean allowWilderness)
    {
        return suggestions(itemName, mode, MembershipStatus.P2P, allowWilderness);
    }

    public List<String> suggestions(String itemName, AccountMode mode,
            MembershipStatus membership, boolean allowWilderness)
    {
        List<String> result = new ArrayList<>();
        for (ResourceSourceDefinition source : match(itemName))
        {
            if (source.isWilderness() && !allowWilderness) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && source.getRiskLevel() == RiskLevel.HIGH) continue;
            String route = membership == MembershipStatus.P2P
                    ? memberRoute(source, mode) : freeToPlayRoute(source, itemName, mode);
            if (route != null && !route.trim().isEmpty() && !result.contains(route))
                result.add(route);
            if (result.size() >= 4) break;
        }
        return Collections.unmodifiableList(result);
    }

    private static String memberRoute(ResourceSourceDefinition source, AccountMode mode)
    {
        if (mode == AccountMode.ULTIMATE_IRONMAN) return source.getUimRoute();
        if (mode != null && mode.isIronLike()) return source.getIronRoute();
        return source.getMainRoute();
    }

    private static String freeToPlayRoute(ResourceSourceDefinition source,
            String itemName, AccountMode mode)
    {
        String normalized = normalize(itemName);
        boolean explicitlySafe = false;
        for (String safeName : source.getFreeToPlayItemNames())
            if (normalized.equals(normalize(safeName)))
            {
                explicitlySafe = true;
                break;
            }
        if (!explicitlySafe) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN) return source.getFreeToPlayUimRoute();
        if (mode != null && mode.isIronLike()) return source.getFreeToPlayIronRoute();
        return source.getFreeToPlayMainRoute();
    }

    private static boolean containsPhrase(String value, String phrase)
    {
        if (phrase.isEmpty()) return false;
        return (" " + value + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9']+", " ").trim();
    }
}

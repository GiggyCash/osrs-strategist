package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

/** Exact current-step evidence supplied by RuneLite's Clue Scroll plugin. */
public final class ClueStepSnapshot
{
    @Getter
    private final String kind;
    @Getter
    private final String action;
    @Getter
    private final String location;
    @Getter
    private final List<String> itemRequirements;
    @Getter
    private final boolean requiresSpade;
    @Getter
    private final boolean requiresLight;
    @Getter
    private final String enemy;
    @Getter
    private final boolean wilderness;
    @Getter
    private final String stashUnit;

    public ClueStepSnapshot(String kind, String action, String location,
            List<String> itemRequirements, boolean requiresSpade,
            boolean requiresLight, String enemy, boolean wilderness,
            String stashUnit)
    {
        this.kind = clean(kind);
        this.action = clean(action);
        this.location = clean(location);
        this.itemRequirements = Collections.unmodifiableList(new ArrayList<>(
                itemRequirements == null
                        ? Collections.emptyList() : itemRequirements));
        this.requiresSpade = requiresSpade;
        this.requiresLight = requiresLight;
        this.enemy = clean(enemy);
        this.wilderness = wilderness;
        this.stashUnit = clean(stashUnit);
    }

    public boolean hasEnemy() { return enemy != null; }
    public boolean hasStashUnit() { return stashUnit != null; }

    /** Ordinary preparation that must be resolved before claiming DO NOW. */
    public boolean requiresPreparation()
    {
        return !itemRequirements.isEmpty() || requiresSpade || requiresLight
                || hasEnemy() || wilderness;
    }

    private static String clean(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

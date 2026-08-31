package compass;

import java.util.*;

import lombok.Getter;

/** Exact current-step evidence supplied by RuneLite's Clue Scroll plugin. */
@Getter
public final class ClueStepSnapshot
{
    private final String kind;
    private final String action;
    private final String location;
    private final List<String> itemRequirements;
    private final boolean requiresSpade;
    private final boolean requiresLight;
    private final String enemy;
    private final boolean wilderness;
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

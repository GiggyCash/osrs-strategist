package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Conservative task-specific Slayer knowledge without fake DPS precision. */
public final class SlayerTaskProfile
{
    private final String id;
    private final List<String> aliases;
    private final List<String> requiredProtection;
    private final String preferredLocation;
    private final String styleGuidance;
    private final String mechanicsNote;

    public SlayerTaskProfile(
            String id,
            List<String> aliases,
            List<String> requiredProtection,
            String preferredLocation,
            String styleGuidance,
            String mechanicsNote)
    {
        this.id = id;
        this.aliases = immutable(aliases);
        this.requiredProtection = immutable(requiredProtection);
        this.preferredLocation = preferredLocation;
        this.styleGuidance = styleGuidance;
        this.mechanicsNote = mechanicsNote;
    }

    public String getId() { return id; }
    public List<String> getAliases() { return aliases; }
    public List<String> getRequiredProtection() { return requiredProtection; }
    public String getPreferredLocation() { return preferredLocation; }
    public String getStyleGuidance() { return styleGuidance; }
    public String getMechanicsNote() { return mechanicsNote; }

    private static List<String> immutable(List<String> values)
    {
        return values == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One canonical RuneLite Slayer assignment identity and its target aliases. */
public final class SlayerTaskIdentity
{
    private final String enumIdentity;
    private final String assignment;
    private final List<String> targetAliases;

    SlayerTaskIdentity(String enumIdentity, String assignment,
            List<String> targetAliases)
    {
        this.enumIdentity = enumIdentity;
        this.assignment = assignment;
        this.targetAliases = Collections.unmodifiableList(
                new ArrayList<>(targetAliases));
    }

    public String getEnumIdentity() { return enumIdentity; }
    public String getAssignment() { return assignment; }
    public List<String> getTargetAliases() { return targetAliases; }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** One canonical RuneLite Slayer assignment identity and its target aliases. */
public final class SlayerTaskIdentity
{
    @Getter
    private final String enumIdentity;
    @Getter
    private final String assignment;
    @Getter
    private final List<String> targetAliases;

    SlayerTaskIdentity(String enumIdentity, String assignment,
            List<String> targetAliases)
    {
        this.enumIdentity = enumIdentity;
        this.assignment = assignment;
        this.targetAliases = Collections.unmodifiableList(
                new ArrayList<>(targetAliases));
    }

}

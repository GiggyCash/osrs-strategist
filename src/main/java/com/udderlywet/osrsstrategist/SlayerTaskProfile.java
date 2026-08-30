package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/** Conservative task-specific Slayer knowledge without fake DPS precision. */
public final class SlayerTaskProfile
{
    @Getter
    private final String id;
    @Getter
    private final List<String> aliases;
    @Getter
    private final List<String> requiredProtection;
    @Getter
    private final String preferredLocation;
    @Getter
    private final String styleGuidance;
    @Getter
    private final String mechanicsNote;
    @Getter
    private final CapabilityState cannonEligibility;
    @Getter
    private final CapabilityState multiTargetMagicEligibility;
    @Getter
    private final boolean wildernessVariantKnown;
    @Getter
    private final List<String> ironObjectives;
    @Getter
    private final String taskDecisionGuidance;

    public SlayerTaskProfile(
            String id,
            List<String> aliases,
            List<String> requiredProtection,
            String preferredLocation,
            String styleGuidance,
            String mechanicsNote)
    {
        this(id, aliases, requiredProtection, preferredLocation, styleGuidance,
                mechanicsNote, CapabilityState.UNKNOWN, CapabilityState.UNKNOWN,
                false, Collections.emptyList(), null);
    }

    public SlayerTaskProfile(String id, List<String> aliases,
            List<String> requiredProtection, String preferredLocation,
            String styleGuidance, String mechanicsNote,
            CapabilityState cannonEligibility,
            CapabilityState multiTargetMagicEligibility,
            boolean wildernessVariantKnown, List<String> ironObjectives,
            String taskDecisionGuidance)
    {
        this.id = id;
        this.aliases = immutable(aliases);
        this.requiredProtection = immutable(requiredProtection);
        this.preferredLocation = preferredLocation;
        this.styleGuidance = styleGuidance;
        this.mechanicsNote = mechanicsNote;
        this.cannonEligibility = cannonEligibility == null
                ? CapabilityState.UNKNOWN : cannonEligibility;
        this.multiTargetMagicEligibility = multiTargetMagicEligibility == null
                ? CapabilityState.UNKNOWN : multiTargetMagicEligibility;
        this.wildernessVariantKnown = wildernessVariantKnown;
        this.ironObjectives = immutable(ironObjectives);
        this.taskDecisionGuidance = taskDecisionGuidance == null
                ? "Compare the live location, supplies, unlock value, drops, and session fit before keeping, extending, skipping, or blocking this task."
                : taskDecisionGuidance;
    }


    private static List<String> immutable(List<String> values)
    {
        return values == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}

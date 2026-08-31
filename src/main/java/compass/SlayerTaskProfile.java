package compass;

import java.util.*;

import lombok.Getter;

/** Conservative task-specific Slayer knowledge without fake DPS precision. */
@Getter
public final class SlayerTaskProfile
{
    private final String id;
    private final List<String> aliases;
    private final List<String> requiredProtection;
    private final String preferredLocation;
    private final String styleGuidance;
    private final String mechanicsNote;
    private final CapabilityState cannonEligibility;
    private final CapabilityState multiTargetMagicEligibility;
    private final boolean wildernessVariantKnown;
    private final List<String> ironObjectives;
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
                ? Text.get(891)
                : taskDecisionGuidance;
    }


    private static List<String> immutable(List<String> values)
    {
        return values == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }
}

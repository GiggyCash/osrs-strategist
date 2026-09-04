package compass;
import lombok.*;
import java.util.*;


import net.runelite.api.Skill;

/** Verified local facts and reusable utility for one infrastructure unlock. */
@Getter
public final class InfrastructureMilestone
{
    String id;
    String name;
    boolean membersOnly;
    Map<Skill, Integer> requiredSkills;
    Map<String, Boolean> requiredQuests;
    String prerequisiteMilestoneId;
    InfrastructureEvidenceKind evidenceKind;
    String evidenceKey;
    StorageKind storageCapability;
    Map<InfraBenefit, Priority> benefits;
    String action;
    String sourceUrl;

}

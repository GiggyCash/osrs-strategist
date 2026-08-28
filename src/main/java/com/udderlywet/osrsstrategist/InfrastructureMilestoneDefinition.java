package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

/** Verified local facts and reusable utility for one infrastructure unlock. */
public final class InfrastructureMilestoneDefinition
{
    private final String id;
    private final String name;
    private final boolean membersOnly;
    private final Skill requiredSkill;
    private final int requiredLevel;
    private final String requiredQuest;
    private final boolean questStartSuffices;
    private final String prerequisiteMilestoneId;
    private final InfrastructureEvidenceKind evidenceKind;
    private final String evidenceKey;
    private final StorageCapability storageCapability;
    private final Map<InfrastructureBenefit, StrategicPriority> benefits;
    private final String action;
    private final String sourceUrl;

    InfrastructureMilestoneDefinition(String id, String name,
            boolean membersOnly, Skill requiredSkill, int requiredLevel,
            String requiredQuest, boolean questStartSuffices,
            String prerequisiteMilestoneId,
            InfrastructureEvidenceKind evidenceKind, String evidenceKey,
            StorageCapability storageCapability,
            Map<InfrastructureBenefit, StrategicPriority> benefits,
            String action, String sourceUrl)
    {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("infrastructure id");
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("infrastructure name");
        if (evidenceKind == null)
            throw new IllegalArgumentException("infrastructure evidence");
        if ((evidenceKind == InfrastructureEvidenceKind.POH_FURNITURE
                || evidenceKind == InfrastructureEvidenceKind.TRANSPORT_ROUTE)
                && (evidenceKey == null || evidenceKey.trim().isEmpty()))
            throw new IllegalArgumentException("infrastructure evidence key");
        if (evidenceKind == InfrastructureEvidenceKind.STORAGE_CAPABILITY
                && storageCapability == null)
            throw new IllegalArgumentException("storage capability");
        if (benefits == null || benefits.isEmpty())
            throw new IllegalArgumentException("infrastructure benefits");
        if (action == null || action.trim().isEmpty())
            throw new IllegalArgumentException("infrastructure action");
        if (sourceUrl == null || !sourceUrl.startsWith("https://"))
            throw new IllegalArgumentException("infrastructure source");
        this.id = id;
        this.name = name;
        this.membersOnly = membersOnly;
        this.requiredSkill = requiredSkill;
        this.requiredLevel = Math.max(0, requiredLevel);
        this.requiredQuest = requiredQuest;
        this.questStartSuffices = questStartSuffices;
        this.prerequisiteMilestoneId = prerequisiteMilestoneId;
        this.evidenceKind = evidenceKind;
        this.evidenceKey = evidenceKey;
        this.storageCapability = storageCapability;
        EnumMap<InfrastructureBenefit, StrategicPriority> copy =
                new EnumMap<>(InfrastructureBenefit.class);
        if (benefits != null) copy.putAll(benefits);
        this.benefits = Collections.unmodifiableMap(copy);
        this.action = action;
        this.sourceUrl = sourceUrl;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isMembersOnly() { return membersOnly; }
    public Skill getRequiredSkill() { return requiredSkill; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public String getPrerequisiteMilestoneId() { return prerequisiteMilestoneId; }
    public InfrastructureEvidenceKind getEvidenceKind() { return evidenceKind; }
    public String getEvidenceKey() { return evidenceKey; }
    public StorageCapability getStorageCapability() { return storageCapability; }
    public Map<InfrastructureBenefit, StrategicPriority> getBenefits()
    {
        return benefits;
    }
    public String getAction() { return action; }
    public String getSourceUrl() { return sourceUrl; }
}

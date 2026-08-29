package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
    private final Map<Skill, Integer> requiredSkills;
    private final Map<String, Boolean> requiredQuests;
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
        this(id, name, membersOnly,
                skillRequirements(requiredSkill, requiredLevel),
                questRequirements(requiredQuest, questStartSuffices),
                prerequisiteMilestoneId, evidenceKind, evidenceKey,
                storageCapability, benefits, action, sourceUrl);
    }

    InfrastructureMilestoneDefinition(String id, String name,
            boolean membersOnly, Map<Skill, Integer> requiredSkills,
            Map<String, Boolean> requiredQuests,
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
        EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
        if (requiredSkills != null)
            requiredSkills.forEach((skill, level) -> {
                if (skill != null) skills.put(skill, Math.max(0,
                        level == null ? 0 : level));
            });
        this.requiredSkills = Collections.unmodifiableMap(skills);
        Map<String, Boolean> quests = new LinkedHashMap<>();
        if (requiredQuests != null)
            requiredQuests.forEach((quest, start) -> {
                if (quest != null && !quest.trim().isEmpty())
                    quests.put(quest, Boolean.TRUE.equals(start));
            });
        this.requiredQuests = Collections.unmodifiableMap(quests);
        Map.Entry<Skill, Integer> firstSkill = skills.containsKey(
                Skill.CONSTRUCTION)
                ? new java.util.AbstractMap.SimpleImmutableEntry<>(
                        Skill.CONSTRUCTION, skills.get(Skill.CONSTRUCTION))
                : skills.entrySet().stream().findFirst().orElse(null);
        this.requiredSkill = firstSkill == null ? null : firstSkill.getKey();
        this.requiredLevel = firstSkill == null ? 0 : firstSkill.getValue();
        Map.Entry<String, Boolean> firstQuest = quests.entrySet().stream()
                .findFirst().orElse(null);
        this.requiredQuest = firstQuest == null ? null : firstQuest.getKey();
        this.questStartSuffices = firstQuest != null && firstQuest.getValue();
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

    private static Map<Skill, Integer> skillRequirements(
            Skill skill, int level)
    {
        if (skill == null) return Collections.emptyMap();
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        result.put(skill, Math.max(0, level));
        return result;
    }

    private static Map<String, Boolean> questRequirements(
            String quest, boolean startSuffices)
    {
        if (quest == null) return Collections.emptyMap();
        return Collections.singletonMap(quest, startSuffices);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isMembersOnly() { return membersOnly; }
    public Skill getRequiredSkill() { return requiredSkill; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getRequiredQuest() { return requiredQuest; }
    public boolean isQuestStartSufficient() { return questStartSuffices; }
    public Map<Skill, Integer> getRequiredSkills() { return requiredSkills; }
    public Map<String, Boolean> getRequiredQuests() { return requiredQuests; }
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

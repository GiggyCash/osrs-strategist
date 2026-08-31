package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Verified local facts and reusable utility for one infrastructure unlock. */
public final class InfrastructureMilestone
{
    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final boolean membersOnly;
    @Getter
    private final Skill requiredSkill;
    @Getter
    private final int requiredLevel;
    @Getter
    private final String requiredQuest;
    private final boolean questStartSuffices;
    @Getter
    private final Map<Skill, Integer> requiredSkills;
    @Getter
    private final Map<String, Boolean> requiredQuests;
    @Getter
    private final String prerequisiteMilestoneId;
    @Getter
    private final InfrastructureEvidenceKind evidenceKind;
    @Getter
    private final String evidenceKey;
    @Getter
    private final StorageCapability storageCapability;
    @Getter
    private final Map<InfrastructureBenefit, StrategicPriority> benefits;
    @Getter
    private final String action;
    @Getter
    private final String sourceUrl;

    InfrastructureMilestone(String id, String name,
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

    InfrastructureMilestone(String id, String name,
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
            throw new IllegalArgumentException(Text.get(1251));
        if (evidenceKind == null)
            throw new IllegalArgumentException(Text.get(1252));
        if ((evidenceKind == InfrastructureEvidenceKind.POH_FURNITURE
                || evidenceKind == InfrastructureEvidenceKind.TRANSPORT_ROUTE)
                && (evidenceKey == null || evidenceKey.trim().isEmpty()))
            throw new IllegalArgumentException(Text.get(1253));
        if (evidenceKind == InfrastructureEvidenceKind.STORAGE_CAPABILITY
                && storageCapability == null)
            throw new IllegalArgumentException(Text.get(1254));
        if (benefits == null || benefits.isEmpty())
            throw new IllegalArgumentException(Text.get(1255));
        if (action == null || action.trim().isEmpty())
            throw new IllegalArgumentException(Text.get(1256));
        if (sourceUrl == null || !sourceUrl.startsWith("https://"))
            throw new IllegalArgumentException(Text.get(1257));
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

    public boolean isQuestStartSufficient() { return questStartSuffices; }
}

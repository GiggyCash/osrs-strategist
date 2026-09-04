package compass;
import static java.lang.Math.*;
import lombok.*;
import static net.runelite.api.Skill.*;
import static java.util.Collections.*;

import static compass.Text.get;

import java.util.*;


import net.runelite.api.Skill;

/** Verified local facts and reusable utility for one infrastructure unlock. */
public final class InfrastructureMilestone
{
    @Getter
    final String id;
    @Getter
    final String name;
    @Getter
    final boolean membersOnly;
    @Getter
    final Skill requiredSkill;
    @Getter
    final int requiredLevel;
    @Getter
    final String requiredQuest;
    final boolean questStartSuffices;
    @Getter
    final Map<Skill, Integer> requiredSkills;
    @Getter
    final Map<String, Boolean> requiredQuests;
    @Getter
    final String prerequisiteMilestoneId;
    @Getter
    final InfrastructureEvidenceKind evidenceKind;
    @Getter
    final String evidenceKey;
    @Getter
    final StorageKind storageCapability;
    @Getter
    final Map<InfraBenefit, Priority> benefits;
    @Getter
    final String action;
    @Getter
    final String sourceUrl;

    InfrastructureMilestone(String id, String name,
            boolean membersOnly, Skill requiredSkill, int requiredLevel,
            String requiredQuest, boolean questStartSuffices,
            String prerequisiteMilestoneId,
            InfrastructureEvidenceKind evidenceKind, String evidenceKey,
            StorageKind storageCapability,
            Map<InfraBenefit, Priority> benefits,
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
            StorageKind storageCapability,
            Map<InfraBenefit, Priority> benefits,
            String action, String sourceUrl)
    {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException(get(1732));
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException(get(1251));
        if (evidenceKind == null)
            throw new IllegalArgumentException(get(1252));
        if ((evidenceKind == InfrastructureEvidenceKind.POH_FURNITURE
                || evidenceKind == InfrastructureEvidenceKind.TRANSPORT_ROUTE)
                && (evidenceKey == null || evidenceKey.trim().isEmpty()))
            throw new IllegalArgumentException(get(1253));
        if (evidenceKind == InfrastructureEvidenceKind.STORAGE_CAPABILITY
                && storageCapability == null)
            throw new IllegalArgumentException(get(1254));
        if (benefits == null || benefits.isEmpty())
            throw new IllegalArgumentException(get(1255));
        if (action == null || action.trim().isEmpty())
            throw new IllegalArgumentException(get(1256));
        if (sourceUrl == null || !sourceUrl.startsWith("https://"))
            throw new IllegalArgumentException(get(1257));
        this.id = id;
        this.name = name;
        this.membersOnly = membersOnly;
        EnumMap<Skill, Integer> skills = new EnumMap<>(Skill.class);
        if (requiredSkills != null)
            requiredSkills.forEach((skill, level) -> {
                if (skill != null) skills.put(skill, max(0,
                        level == null ? 0 : level));
            });
        this.requiredSkills = unmodifiableMap(skills);
        Map<String, Boolean> quests = new LinkedHashMap<>();
        if (requiredQuests != null)
            requiredQuests.forEach((quest, start) -> {
                if (quest != null && !quest.trim().isEmpty())
                    quests.put(quest, Boolean.TRUE.equals(start));
            });
        this.requiredQuests = unmodifiableMap(quests);
        Map.Entry<Skill, Integer> firstSkill = skills.containsKey(
                CONSTRUCTION)
                ? new java.util.AbstractMap.SimpleImmutableEntry<>(
                        CONSTRUCTION, skills.get(CONSTRUCTION))
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
        EnumMap<InfraBenefit, Priority> copy =
                new EnumMap<>(InfraBenefit.class);
        if (benefits != null) copy.putAll(benefits);
        this.benefits = unmodifiableMap(copy);
        this.action = action;
        this.sourceUrl = sourceUrl;
    }

    private static Map<Skill, Integer> skillRequirements(
            Skill skill, int level)
    {
        if (skill == null) return emptyMap();
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        result.put(skill, max(0, level));
        return result;
    }

    private static Map<String, Boolean> questRequirements(
            String quest, boolean startSuffices)
    {
        if (quest == null) return emptyMap();
        return singletonMap(quest, startSuffices);
    }

    public boolean isQuestStartSufficient() { return questStartSuffices; }
}

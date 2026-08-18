package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/** Typed prerequisite in a resource acquisition route. */
public final class DependencyRequirement
{
    public enum Kind { RESOURCE, QUEST, SKILL, GEAR }

    private final String id;
    private final String label;
    private final Kind kind;
    private final ResourceNeed resource;
    private final Skill skill;
    private final int level;

    private DependencyRequirement(String id, String label, Kind kind,
            ResourceNeed resource, Skill skill, int level)
    {
        this.id = id;
        this.label = label;
        this.kind = kind;
        this.resource = resource;
        this.skill = skill;
        this.level = level;
    }

    public static DependencyRequirement resource(ResourceNeed need)
    {
        return new DependencyRequirement("resource:" + need.getItemId(),
                need.getItemName(), Kind.RESOURCE, need, null, 0);
    }

    public static DependencyRequirement quest(String name)
    {
        return new DependencyRequirement("quest:" + normalize(name), name,
                Kind.QUEST, null, null, 0);
    }

    public static DependencyRequirement skill(Skill skill, int level)
    {
        return new DependencyRequirement("skill:" + skill.name().toLowerCase(),
                skill.getName() + " " + level, Kind.SKILL, null, skill, level);
    }

    public static DependencyRequirement gear(String name)
    {
        return new DependencyRequirement("gear:" + normalize(name), name,
                Kind.GEAR, null, null, 0);
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Kind getKind() { return kind; }
    public ResourceNeed getResource() { return resource; }
    public Skill getSkill() { return skill; }
    public int getLevel() { return level; }

    private static String normalize(String value)
    {
        return value == null ? "unknown" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}

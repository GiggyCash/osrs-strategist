package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import net.runelite.api.Skill;

/** Compact persisted recap; raw XP events are intentionally not retained. */
public final class ProgressSessionSummary
{
    private static final int MAX_MILESTONES = 100;
    @Getter
    private final long startedAtMillis;
    @Getter
    private final long endedAtMillis;
    @Getter
    private final long activeDurationMillis;
    @Getter
    private final long totalXpGained;
    @Getter
    private final int levelsGained;
    @Getter
    private final Map<Skill, Integer> xpBySkill;
    @Getter
    private final List<ProgressMilestone> milestones;

    public ProgressSessionSummary(ProgressSessionSnapshot snapshot)
    {
        this(snapshot == null ? 0L : snapshot.getStartedAtMillis(),
                snapshot == null ? 0L : snapshot.getUpdatedAtMillis(),
                snapshot == null ? 0L : snapshot.getActiveDurationMillis(),
                snapshot == null ? 0L : snapshot.getTotalXpGained(),
                snapshot == null ? 0 : snapshot.getLevelsGained(),
                gains(snapshot), snapshot == null
                        ? Collections.emptyList() : snapshot.getMilestones());
    }

    ProgressSessionSummary(
            long startedAtMillis,
            long endedAtMillis,
            long activeDurationMillis,
            long totalXpGained,
            int levelsGained,
            Map<Skill, Integer> xpBySkill)
    {
        this(startedAtMillis, endedAtMillis, activeDurationMillis,
                totalXpGained, levelsGained, xpBySkill,
                Collections.emptyList());
    }

    ProgressSessionSummary(
            long startedAtMillis,
            long endedAtMillis,
            long activeDurationMillis,
            long totalXpGained,
            int levelsGained,
            Map<Skill, Integer> xpBySkill,
            List<ProgressMilestone> milestones)
    {
        this.startedAtMillis = Math.max(0L, startedAtMillis);
        this.endedAtMillis = Math.max(this.startedAtMillis, endedAtMillis);
        this.activeDurationMillis = Math.max(0L,
                Math.min(activeDurationMillis,
                        this.endedAtMillis - this.startedAtMillis));
        this.totalXpGained = Math.max(0L, totalXpGained);
        this.levelsGained = Math.max(0, levelsGained);
        EnumMap<Skill, Integer> copy = new EnumMap<>(Skill.class);
        if (xpBySkill != null)
            for (Map.Entry<Skill, Integer> entry : xpBySkill.entrySet())
                if (entry.getKey() != null && entry.getValue() != null
                        && entry.getValue() > 0)
                    copy.put(entry.getKey(), entry.getValue());
        this.xpBySkill = Collections.unmodifiableMap(copy);
        List<ProgressMilestone> milestoneCopy = new ArrayList<>(
                milestones == null ? Collections.emptyList() : milestones);
        while (milestoneCopy.size() > MAX_MILESTONES)
            milestoneCopy.remove(0);
        this.milestones = Collections.unmodifiableList(milestoneCopy);
    }

    private static Map<Skill, Integer> gains(ProgressSessionSnapshot snapshot)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        if (snapshot != null)
            for (SkillSessionProgress progress : snapshot.getSkills().values())
                if (progress.getXpGained() > 0)
                    result.put(progress.getSkill(), progress.getXpGained());
        return result;
    }

}

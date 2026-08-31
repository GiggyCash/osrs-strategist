package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Guaranteed XP available from unfinished quests on the selected goal path. */
public final class GoalQuestRewardForecast
{
    @Getter
    private final Skill skill;
    @Getter
    private final int experience;
    @Getter
    private final List<String> sourceQuests;

    GoalQuestRewardForecast(Skill skill, int experience, List<String> sourceQuests)
    {
        this.skill = skill;
        this.experience = Math.max(0, experience);
        this.sourceQuests = Collections.unmodifiableList(
                new ArrayList<>(sourceQuests));
    }

    public boolean hasGuaranteedExperience() { return experience > 0; }
}

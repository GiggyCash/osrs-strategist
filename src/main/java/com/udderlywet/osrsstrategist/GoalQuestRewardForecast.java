package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

import net.runelite.api.Skill;

/** Guaranteed XP available from unfinished quests on the selected goal path. */
@Getter
public final class GoalQuestRewardForecast
{
    private final Skill skill;
    private final int experience;
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

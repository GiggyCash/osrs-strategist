package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;

/** Guaranteed XP available from unfinished quests on the selected goal path. */
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

    public Skill getSkill() { return skill; }
    public int getExperience() { return experience; }
    public List<String> getSourceQuests() { return sourceQuests; }
    public boolean hasGuaranteedExperience() { return experience > 0; }
}

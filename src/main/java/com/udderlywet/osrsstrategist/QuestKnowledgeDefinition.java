package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Strategist metadata for a quest whose progression value is well understood. */
public final class QuestKnowledgeDefinition
{
    private final String questName;
    private final double progressionScore;
    private final String unlockSummary;
    private final List<String> prerequisiteQuests;
    private final boolean membersOnly;
    private final boolean hardcoreRisky;

    public QuestKnowledgeDefinition(String questName, double progressionScore,
            String unlockSummary, List<String> prerequisiteQuests,
            boolean membersOnly, boolean hardcoreRisky)
    {
        this.questName = questName;
        this.progressionScore = progressionScore;
        this.unlockSummary = unlockSummary;
        this.prerequisiteQuests = Collections.unmodifiableList(
                new ArrayList<>(prerequisiteQuests));
        this.membersOnly = membersOnly;
        this.hardcoreRisky = hardcoreRisky;
    }

    public String getQuestName() { return questName; }
    public double getProgressionScore() { return progressionScore; }
    public String getUnlockSummary() { return unlockSummary; }
    public List<String> getPrerequisiteQuests() { return prerequisiteQuests; }
    public boolean isMembersOnly() { return membersOnly; }
    public boolean isHardcoreRisky() { return hardcoreRisky; }
}

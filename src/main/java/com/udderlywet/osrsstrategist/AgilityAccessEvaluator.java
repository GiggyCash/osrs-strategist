package com.udderlywet.osrsstrategist;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Resolves the best Agility course from level, membership, quest state, and
 * direct region observations. Direct observation is the strongest evidence.
 */
@Singleton
public class AgilityAccessEvaluator
{
    private final AgilityCourseCatalog catalog;

    @Inject
    public AgilityAccessEvaluator(AgilityCourseCatalog catalog)
    {
        this.catalog = catalog;
    }

    public AgilityCourseDefinition bestStandardCourse(StrategyDataBundle data)
    {
        AgilityCourseDefinition best = null;
        for (AgilityCourseDefinition course : catalog.all())
        {
            if (course.isWilderness() || !isVerifiedAvailable(data, course))
            {
                continue;
            }
            if (best == null
                    || course.getRequiredLevel() > best.getRequiredLevel())
            {
                best = course;
            }
        }
        return best;
    }

    public RequirementCheck courseCheck(
            StrategyDataBundle data,
            AgilityCourseDefinition course)
    {
        if (course == null)
        {
            return new RequirementCheck(
                    "agility:course",
                    "Usable Agility course",
                    RequirementState.CHECK_NEEDED,
                    PlayerText.get("AAE1")
            );
        }

        AccountSnapshot account = data == null ? null : data.getAccount();
        if (account == null)
        {
            return unknown(course, "Account state is unavailable.");
        }

        int level = account.getSkillLevel(Skill.AGILITY);
        if (level < course.getRequiredLevel())
        {
            return new RequirementCheck(
                    "agility:" + course.getId(),
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    "Requires " + course.getRequiredLevel()
                            + " Agility; current level is " + level + "."
            );
        }

        if (account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return new RequirementCheck(
                    "agility:" + course.getId(),
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    PlayerText.get("AAE2")
            );
        }

        AccessMemorySnapshot memory = data.getAccessMemory();
        if (memory != null && memory.hasObserved(course.observationKey()))
        {
            return verified(
                    course,
                    PlayerText.get("AAE3")
            );
        }

        String quest = course.getRequiredQuest();
        if (quest != null)
        {
            QuestSnapshot quests = data.getQuests();
            QuestStatus status = quests == null
                    ? QuestStatus.UNKNOWN
                    : quests.statusOf(quest);

            if (status == QuestStatus.COMPLETE)
            {
                return verified(
                        course,
                        quest + PlayerText.get("AAE4")
                );
            }
            if (status == QuestStatus.NOT_STARTED
                    || status == QuestStatus.IN_PROGRESS)
            {
                return new RequirementCheck(
                        "agility:" + course.getId(),
                        course.getDisplayName(),
                        RequirementState.BLOCKED,
                        quest + " is not complete."
                );
            }
            return unknown(
                    course,
                    "Quest state for " + quest + " has not been proven yet."
            );
        }

        return verified(
                course,
                PlayerText.get("AAE5")
        );
    }

    public RequirementCheck wildernessCourseCheck(StrategyDataBundle data)
    {
        return courseCheck(data, catalog.wildernessCourse());
    }

    private boolean isVerifiedAvailable(
            StrategyDataBundle data,
            AgilityCourseDefinition course)
    {
        return courseCheck(data, course).getState() == RequirementState.VERIFIED;
    }

    private RequirementCheck verified(
            AgilityCourseDefinition course,
            String evidence)
    {
        return new RequirementCheck(
                "agility:" + course.getId(),
                course.getDisplayName(),
                RequirementState.VERIFIED,
                evidence
        );
    }

    private RequirementCheck unknown(
            AgilityCourseDefinition course,
            String evidence)
    {
        return new RequirementCheck(
                "agility:" + course.getId(),
                course.getDisplayName(),
                RequirementState.CHECK_NEEDED,
                evidence
        );
    }
}

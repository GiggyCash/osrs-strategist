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

    public AgilityCourseDefinition bestStandardCourse(GameData data)
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
            GameData data,
            AgilityCourseDefinition course)
    {
        if (course == null)
        {
            return new RequirementCheck(
                    "agility:course",
                    Text.get(1390),
                    RequirementState.CHECK_NEEDED,
                    Text.get(0)
            );
        }

        AccountSnapshot account = data == null ? null : data.account();
        if (account == null)
        {
            return unknown(course, Text.get(1391));
        }

        int level = account.getSkillLevel(Skill.AGILITY);
        if (level < course.getRequiredLevel())
        {
            return new RequirementCheck(
                    "agility:" + course.getId(),
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    "Requires " + course.getRequiredLevel()
                            + Text.get(1392) + level + "."
            );
        }

        if (account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return new RequirementCheck(
                    "agility:" + course.getId(),
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    Text.get(1)
            );
        }

        AccessMemorySnapshot memory = data.accessMemory();
        if (memory != null && memory.hasObserved(course.observationKey()))
        {
            return verified(
                    course,
                    Text.get(2)
            );
        }

        String quest = course.getRequiredQuest();
        if (quest != null)
        {
            QuestSnapshot quests = data.quests();
            QuestStatus status = quests == null
                    ? QuestStatus.UNKNOWN
                    : quests.statusOf(quest);

            if (status == QuestStatus.COMPLETE)
            {
                return verified(
                        course,
                        quest + Text.get(3)
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
                    "Quest state for " + quest + Text.get(1393)
            );
        }

        return verified(
                course,
                Text.get(4)
        );
    }

    public RequirementCheck wildernessCourseCheck(GameData data)
    {
        return courseCheck(data, catalog.wildernessCourse());
    }

    private boolean isVerifiedAvailable(
            GameData data,
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

package compass;
import static compass.Text.get;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Resolves the best Agility course from level, membership, quest state, and
 * direct region observations. Direct observation is the strongest evidence.
 */
@Singleton
@lombok.RequiredArgsConstructor(onConstructor_ = @Inject)
public class AgilityAccessEvaluator
{
    private final AgilityCourseCatalog catalog;

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
                    get(1390),
                    RequirementState.CHECK_NEEDED,
                    get(0)
            );
        }

        var account = data == null ? null : data.account();
        if (account == null)
        {
            return unknown(course, get(1391));
        }

        var level = account.level(Skill.AGILITY);
        if (level < course.getRequiredLevel())
        {
            return new RequirementCheck(
                    "agility:" + course.id,
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    "Requires " + course.getRequiredLevel()
                            + get(1392) + level + "."
            );
        }

        if (account.membership() != MembershipStatus.P2P)
        {
            return new RequirementCheck(
                    "agility:" + course.id,
                    course.getDisplayName(),
                    RequirementState.BLOCKED,
                    get(1)
            );
        }

        var memory = data.accessMemory();
        if (memory != null && memory.hasObserved(course.observationKey()))
        {
            return verified(
                    course,
                    get(2)
            );
        }

        var quest = course.getRequiredQuest();
        if (quest != null)
        {
            var quests = data.quests();
            QuestStatus status = quests == null
                    ? QuestStatus.UNKNOWN
                    : quests.statusOf(quest);

            if (status == QuestStatus.COMPLETE)
            {
                return verified(
                        course,
                        quest + get(3)
                );
            }
            if (status == QuestStatus.NOT_STARTED
                    || status == QuestStatus.IN_PROGRESS)
            {
                return new RequirementCheck(
                        "agility:" + course.id,
                        course.getDisplayName(),
                        RequirementState.BLOCKED,
                        quest + get(1690)
                );
            }
            return unknown(
                    course,
                    get(1691) + quest + get(1393)
            );
        }

        return verified(
                course,
                get(4)
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
                "agility:" + course.id,
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
                "agility:" + course.id,
                course.getDisplayName(),
                RequirementState.CHECK_NEEDED,
                evidence
        );
    }
}

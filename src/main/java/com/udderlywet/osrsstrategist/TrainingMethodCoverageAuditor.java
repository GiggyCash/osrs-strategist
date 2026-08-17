package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Developer-facing audit tool for method-catalog completeness.
 *
 * <p>Large method catalogs are difficult to review by eye. This class evaluates
 * every level from 1 through 99 and reports contiguous ranges where Strategist
 * cannot produce any catalog method for the selected membership mode.</p>
 *
 * <p>The audit intentionally checks catalog availability, not account-specific
 * readiness. A level band counts as covered when a method exists and is allowed
 * for the membership mode even if a particular character still needs a quest,
 * item, transport unlock, or other requirement.</p>
 */
@Singleton
public class TrainingMethodCoverageAuditor
{
    private static final EnumSet<Skill> F2P_SKILLS = EnumSet.of(
            Skill.ATTACK,
            Skill.STRENGTH,
            Skill.DEFENCE,
            Skill.HITPOINTS,
            Skill.RANGED,
            Skill.PRAYER,
            Skill.MAGIC,
            Skill.RUNECRAFT,
            Skill.MINING,
            Skill.SMITHING,
            Skill.FISHING,
            Skill.COOKING,
            Skill.FIREMAKING,
            Skill.WOODCUTTING,
            Skill.CRAFTING
    );

    private final TrainingMethodDatabase legacyDatabase;
    private final ExpandedTrainingMethodCatalog expandedCatalog;
    private final F2pBaselineMethodCatalog f2pBaselineCatalog;

    @Inject
    public TrainingMethodCoverageAuditor(
            TrainingMethodDatabase legacyDatabase,
            ExpandedTrainingMethodCatalog expandedCatalog,
            F2pBaselineMethodCatalog f2pBaselineCatalog)
    {
        this.legacyDatabase = legacyDatabase;
        this.expandedCatalog = expandedCatalog;
        this.f2pBaselineCatalog = f2pBaselineCatalog;
    }

    public List<TrainingMethodCoverageGap> gapsFor(
            Skill skill,
            MembershipStatus membership)
    {
        if (skill == null) return Collections.emptyList();

        MembershipStatus safeMembership = membership == null
                ? MembershipStatus.UNKNOWN
                : membership;

        // Members-only skills are not a defect in F2P coverage.
        if (safeMembership == MembershipStatus.F2P
                && !F2P_SKILLS.contains(skill))
        {
            return Collections.emptyList();
        }

        boolean[] covered = new boolean[100];
        for (CuratedTrainingMethod method : methodsFor(skill, safeMembership))
        {
            TrainingMethod record = method.getMethod();
            if (record == null
                    || record.getConfidence() == RecommendationConfidence.BLOCKED)
            {
                continue;
            }

            for (int level = 1; level <= 99; level++)
            {
                if (record.supportsLevel(level)
                        && ContentAccessRules.isMethodAvailable(
                                record, safeMembership))
                {
                    covered[level] = true;
                }
            }
        }

        return toGaps(skill, safeMembership, covered);
    }

    public List<TrainingMethodCoverageGap> allGaps(
            MembershipStatus membership)
    {
        List<TrainingMethodCoverageGap> result = new ArrayList<>();
        for (Skill skill : Skill.values())
        {
            result.addAll(gapsFor(skill, membership));
        }
        return Collections.unmodifiableList(result);
    }

    public int methodCount(
            Skill skill,
            MembershipStatus membership)
    {
        return methodsFor(skill, membership).size();
    }

    private List<CuratedTrainingMethod> methodsFor(
            Skill skill,
            MembershipStatus membership)
    {
        List<CuratedTrainingMethod> result = new ArrayList<>();

        // Mirror production selection policy. Legacy methods do not have fully
        // explicit F2P metadata, so F2P coverage only counts catalogs whose
        // access policy is explicit.
        if (membership != MembershipStatus.F2P && legacyDatabase != null)
        {
            for (TrainingMethod method : legacyDatabase.methodsFor(skill))
            {
                result.add(new CuratedTrainingMethod(
                        method,
                        TrainingMethodMetadata.legacy(method)));
            }
        }

        if (expandedCatalog != null)
        {
            for (CuratedTrainingMethod method : expandedCatalog.methodsFor(skill))
            {
                if (membership != MembershipStatus.F2P
                        || method.getMetadata().isFreeToPlayAllowed())
                {
                    result.add(method);
                }
            }
        }

        if (membership == MembershipStatus.F2P && f2pBaselineCatalog != null)
        {
            result.addAll(f2pBaselineCatalog.methodsFor(skill));
        }

        return result;
    }

    private static List<TrainingMethodCoverageGap> toGaps(
            Skill skill,
            MembershipStatus membership,
            boolean[] covered)
    {
        List<TrainingMethodCoverageGap> gaps = new ArrayList<>();
        int start = -1;

        for (int level = 1; level <= 99; level++)
        {
            if (!covered[level] && start < 0)
            {
                start = level;
            }

            boolean closes = start >= 0
                    && (covered[level] || level == 99);
            if (!closes) continue;

            int end = covered[level] ? level - 1 : level;
            gaps.add(new TrainingMethodCoverageGap(
                    skill, membership, start, end));
            start = -1;
        }

        return Collections.unmodifiableList(gaps);
    }
}

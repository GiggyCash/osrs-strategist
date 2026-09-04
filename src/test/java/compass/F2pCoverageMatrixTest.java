package compass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Ensures F2P remains complete and isolated across normal milestone bands. */
public class F2pCoverageMatrixTest
{
    private static final List<Skill> RECOMMENDED_F2P_SKILLS = Arrays.asList(
            Skill.ATTACK,
            Skill.STRENGTH,
            Skill.DEFENCE,
            Skill.RANGED,
            Skill.PRAYER,
            Skill.MAGIC,
            Skill.RUNECRAFT,
            Skill.CRAFTING,
            Skill.MINING,
            Skill.SMITHING,
            Skill.FISHING,
            Skill.COOKING,
            Skill.FIREMAKING,
            Skill.WOODCUTTING);

    private static final int[] LEVELS = {
            1, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 90, 98
    };

    @Test
    public void everyF2pSkillHasSafeRouteAtEveryMilestoneBand()
    {
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog baseline = new F2pBaselineMethodCatalog();

        for (Skill skill : RECOMMENDED_F2P_SKILLS)
        {
            for (int level : LEVELS)
            {
                List<CuratedTrainingMethod> candidates = new ArrayList<>();
                candidates.addAll(expanded.methodsFor(skill));
                candidates.addAll(baseline.methodsFor(skill));

                boolean found = false;
                for (CuratedTrainingMethod candidate : candidates)
                {
                    if (candidate == null || candidate.getMethod() == null
                            || candidate.getMetadata() == null)
                    {
                        continue;
                    }
                    TrainingMethod method = candidate.getMethod();
                    TrainingMethodMetadata metadata = candidate.getMetadata();
                    if (method.supportsLevel(level)
                            && metadata.isFreeToPlayAllowed()
                            && ContentAccessRules.isMethodAvailable(
                                    method, Membership.F2P))
                    {
                        found = true;
                        break;
                    }
                }

                assertTrue(skill.getName() + " has no F2P route at level " + level,
                        found);
            }
        }
    }

    @Test
    public void f2pTaggedMethodsCanNeverBeMembersOnly()
    {
        ExpandedTrainingMethodCatalog expanded = new ExpandedTrainingMethodCatalog();
        F2pBaselineMethodCatalog baseline = new F2pBaselineMethodCatalog();

        for (Skill skill : Skill.values())
        {
            List<CuratedTrainingMethod> all = new ArrayList<>();
            all.addAll(expanded.methodsFor(skill));
            all.addAll(baseline.methodsFor(skill));
            for (CuratedTrainingMethod candidate : all)
            {
                if (candidate == null || candidate.getMetadata() == null
                        || candidate.getMethod() == null)
                {
                    continue;
                }
                if (!candidate.getMetadata().isFreeToPlayAllowed()) continue;

                TrainingMethod method = candidate.getMethod();
                assertFalse(method.getId() + " is F2P-tagged but members-only",
                        method.isMembersOnly());
                assertTrue(method.getId() + " is F2P-tagged on a members skill",
                        ContentAccessRules.isSkillAvailable(
                                method.getSkill(), Membership.F2P));
                assertTrue(method.getId() + " is F2P-tagged but access rules reject it",
                        ContentAccessRules.isMethodAvailable(
                                method, Membership.F2P));
            }
        }
    }

    @Test
    public void fallbackRoutesNeverDelegateMethodOrLocationChoice()
    {
        String[] forbidden = {"best practical", "best sensible",
                "best available", "nearby low-risk", "convenient",
                "reachable f2p", "f2p anvil", "a f2p furnace"};
        F2pBaselineMethodCatalog baseline = new F2pBaselineMethodCatalog();
        for (Skill skill : Skill.values())
        {
            for (CuratedTrainingMethod candidate : baseline.methodsFor(skill))
            {
                String text = candidate.getMethod().getInstructions()
                        .toLowerCase(java.util.Locale.ROOT);
                for (String phrase : forbidden)
                    assertFalse(candidate.getMethod().getId() + ": " + phrase,
                            text.contains(phrase));
            }
        }
    }
}

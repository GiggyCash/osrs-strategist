package compass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;

/** Development-time census for strategic methods and RuneLite action evidence. */
public final class TrainingMethodCensus
{
    public static final class SkillCoverage
    {
        private final int curatedMethods;
        private final int runeLiteActions;
        private final Set<TrainingIntensity> intensities;
        private final Set<MethodCostTier> costTiers;
        private final boolean hasF2p;
        private final boolean hasSelfSource;
        private final boolean hasUim;
        private final boolean hasHardcoreSafe;

        private SkillCoverage(int curatedMethods, int runeLiteActions,
                Set<TrainingIntensity> intensities,
                Set<MethodCostTier> costTiers, boolean hasF2p,
                boolean hasSelfSource, boolean hasUim,
                boolean hasHardcoreSafe)
        {
            this.curatedMethods = curatedMethods;
            this.runeLiteActions = runeLiteActions;
            this.intensities = Collections.unmodifiableSet(intensities);
            this.costTiers = Collections.unmodifiableSet(costTiers);
            this.hasF2p = hasF2p;
            this.hasSelfSource = hasSelfSource;
            this.hasUim = hasUim;
            this.hasHardcoreSafe = hasHardcoreSafe;
        }

        public int getCuratedMethods() { return curatedMethods; }
        public int getRuneLiteActions() { return runeLiteActions; }
        public Set<TrainingIntensity> getIntensities() { return intensities; }
        public Set<MethodCostTier> getCostTiers() { return costTiers; }
        public boolean hasF2p() { return hasF2p; }
        public boolean hasSelfSource() { return hasSelfSource; }
        public boolean hasUim() { return hasUim; }
        public boolean hasHardcoreSafe() { return hasHardcoreSafe; }
    }

    private final Map<Skill, SkillCoverage> bySkill;
    private final int curatedMethodCount;
    private final int runeLiteActionCount;
    private final int duplicateIds;
    private final List<String> invalidMethods;

    public TrainingMethodCensus()
    {
        ExpandedTrainingMethodCatalog curated = new ExpandedTrainingMethodCatalog();
        RuneLiteSkillActionCatalog actions = new RuneLiteSkillActionCatalog();
        EnumMap<Skill, SkillCoverage> coverage = new EnumMap<>(Skill.class);
        Set<String> ids = new HashSet<>();
        List<String> invalid = new ArrayList<>();
        int curatedCount = 0;
        int actionCount = 0;
        int duplicates = 0;

        for (Skill skill : Skill.values())
        {
            List<CuratedTrainingMethod> methods = curated.methodsFor(skill);
            Set<TrainingIntensity> intensities = new HashSet<>();
            Set<MethodCostTier> costs = new HashSet<>();
            boolean f2p = false;
            boolean selfSource = false;
            boolean uim = false;
            boolean hardcore = false;
            for (CuratedTrainingMethod candidate : methods)
            {
                curatedCount++;
                TrainingMethod method = candidate.getMethod();
                TrainingMethodMetadata metadata = candidate.getMetadata();
                if (method == null || metadata == null)
                {
                    invalid.add(skill + ": null method or metadata");
                    continue;
                }
                if (!ids.add(method.getId())) duplicates++;
                if (method.getSkill() != skill || method.getName() == null
                        || method.getName().trim().isEmpty()
                        || method.getInstructions() == null
                        || method.getInstructions().trim().isEmpty()
                        || method.getMinLevel() < 1
                        || method.getMaxLevel() < method.getMinLevel()
                        || metadata.isFreeToPlayAllowed() && method.isMembersOnly())
                    invalid.add(method.getId());
                intensities.add(metadata.getIntensity());
                costs.add(metadata.getCostTier());
                f2p |= metadata.isFreeToPlayAllowed();
                selfSource |= metadata.isSelfSourceFriendly();
                uim |= metadata.isUimFriendly();
                hardcore |= metadata.isHardcoreSafe();
            }
            int actionSize = actions.actionsFor(skill).size();
            actionCount += actionSize;
            coverage.put(skill, new SkillCoverage(methods.size(), actionSize,
                    intensities, costs, f2p, selfSource, uim, hardcore));
        }
        bySkill = Collections.unmodifiableMap(coverage);
        curatedMethodCount = curatedCount;
        runeLiteActionCount = actionCount;
        duplicateIds = duplicates;
        invalidMethods = Collections.unmodifiableList(invalid);
    }

    public Map<Skill, SkillCoverage> getBySkill() { return bySkill; }
    public int getSkillCount() { return bySkill.size(); }
    public int getCuratedMethodCount() { return curatedMethodCount; }
    public int getRuneLiteActionCount() { return runeLiteActionCount; }
    public int getDuplicateIds() { return duplicateIds; }
    public List<String> getInvalidMethods() { return invalidMethods; }
    public int getSkillsWithRuneLiteActions()
    {
        int count = 0;
        for (SkillCoverage value : bySkill.values())
            if (value.getRuneLiteActions() > 0) count++;
        return count;
    }
}

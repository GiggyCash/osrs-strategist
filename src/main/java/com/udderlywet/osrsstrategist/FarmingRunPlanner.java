package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Builds the current herb/tree checklist from verified access and resources. */
@Singleton
public class FarmingRunPlanner
{
    private final FarmingRunCatalog catalog;
    private final FarmingSupplyCatalog supplyCatalog;
    private final ResourceReadinessService resources;

    @Inject
    public FarmingRunPlanner(
            FarmingRunCatalog catalog,
            FarmingSupplyCatalog supplyCatalog,
            ResourceReadinessService resources)
    {
        this.catalog = catalog;
        this.supplyCatalog = supplyCatalog;
        this.resources = resources;
    }

    /** Compatibility constructor retained for focused tests. */
    public FarmingRunPlanner(FarmingRunCatalog catalog)
    {
        this(catalog, new FarmingSupplyCatalog(), new ResourceReadinessService());
    }

    public GuidanceChecklist build(GameData data, String activityId)
    {
        List<GuidanceStep> steps = new ArrayList<>();
        if (data == null || data.account() == null)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    Text.get(1463), steps);
        }

        AccountSnapshot account = data.account();
        int farmingLevel = account.getSkillLevel(Skill.FARMING);
        if (account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    Text.get(1464), steps);
        }

        appendPrep(steps, data, farmingLevel);
        FarmingRunSnapshot snapshot = data.farmingRuns() == null
                ? FarmingRunSnapshot.empty() : data.farmingRuns();

        for (FarmingRunPatchDefinition patch : catalog.all())
        {
            if (farmingLevel < patch.getMinimumLevel()
                    || !isConfirmedReachable(data, patch)) continue;
            steps.add(stepFor(patch, snapshot.stateOf(patch.getId())));
        }

        return new GuidanceChecklist(
                activityId, "Farming run",
                Text.get(244), steps);
    }

    private void appendPrep(
            List<GuidanceStep> steps,
            GameData data,
            int farmingLevel)
    {
        FarmingSnapshot farming = data.farming();
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.rake(), toolState(farming, "rake"),
                Text.get(245)));
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.dibber(), toolState(farming, "dibber"),
                Text.get(246)));
        appendResource(steps, resources.evaluate(
                data, supplyCatalog.spade(), toolState(farming, "spade"),
                Text.get(247)));

        if (farmingLevel >= 9)
        {
            appendResource(steps, resources.evaluate(
                    data, supplyCatalog.herbSeedsForLevel(farmingLevel)));
        }
        if (farmingLevel >= 15)
        {
            appendResource(steps, resources.evaluate(
                    data, supplyCatalog.treeSaplingsForLevel(farmingLevel)));
        }
    }

    private CapabilityState toolState(FarmingSnapshot farming, String id)
    {
        return farming == null
                ? CapabilityState.UNKNOWN
                : farming.leprechaunToolState(id);
    }

    private void appendResource(
            List<GuidanceStep> steps,
            RequirementCheck check)
    {
        GuidanceStepState state = check.getState() == RequirementState.VERIFIED
                ? GuidanceStepState.COMPLETE
                : GuidanceStepState.CHECK_NEEDED;
        steps.add(new GuidanceStep(
                check.getId(), "Prep • " + check.getLabel(),
                check.getEvidence(), state));
    }

    private boolean isConfirmedReachable(
            GameData data,
            FarmingRunPatchDefinition patch)
    {
        AccessMemorySnapshot memory = data.accessMemory();
        if (memory != null)
        {
            for (Integer region : patch.getRegionIds())
            {
                if (memory.hasObserved("region." + region)) return true;
            }
        }
        String quest = patch.getRequiredQuest();
        if (quest == null) return true;
        QuestSnapshot quests = data.quests();
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private GuidanceStep stepFor(
            FarmingRunPatchDefinition patch,
            ObservedFarmingPatchState observed)
    {
        String prefix = patch.getKind() == FarmingPatchKind.HERB
                ? "Herb • " : "Tree • ";
        if (observed == null)
        {
            return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                    Text.get(248),
                    GuidanceStepState.CHECK_NEEDED);
        }
        switch (observed.getState())
        {
            case GROWING:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        "Planted", GuidanceStepState.COMPLETE);
            case READY:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        patch.getKind() == FarmingPatchKind.TREE
                                ? Text.get(1465) : Text.get(1466),
                        GuidanceStepState.ACTION);
            case EMPTY:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        "Plant this patch", GuidanceStepState.ACTION);
            case DISEASED:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        "Cure the crop", GuidanceStepState.WARNING);
            case DEAD:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        "Clear and replant", GuidanceStepState.WARNING);
            case UNKNOWN:
            default:
                return new GuidanceStep(patch.getId(), prefix + patch.getDisplayName(),
                        "Check patch state", GuidanceStepState.CHECK_NEEDED);
        }
    }
}

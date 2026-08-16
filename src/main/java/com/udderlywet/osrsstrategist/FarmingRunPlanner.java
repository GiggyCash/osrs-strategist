package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Builds the current herb/tree checklist from verified account state. */
@Singleton
public class FarmingRunPlanner
{
    private final FarmingRunCatalog catalog;

    @Inject
    public FarmingRunPlanner(FarmingRunCatalog catalog)
    {
        this.catalog = catalog;
    }

    public GuidanceChecklist build(StrategyDataBundle data, String activityId)
    {
        List<GuidanceStep> steps = new ArrayList<>();
        if (data == null || data.getAccount() == null)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    "Waiting for account evidence", steps);
        }

        AccountSnapshot account = data.getAccount();
        int farmingLevel = account.getSkillLevel(Skill.FARMING);
        if (account.getMembershipStatus() != MembershipStatus.P2P)
        {
            return new GuidanceChecklist(activityId, "Farming run",
                    "Member Farming access required", steps);
        }

        FarmingRunSnapshot snapshot = data.getFarmingRuns() == null
                ? FarmingRunSnapshot.empty() : data.getFarmingRuns();

        for (FarmingRunPatchDefinition patch : catalog.all())
        {
            if (farmingLevel < patch.getMinimumLevel()
                    || !isConfirmedReachable(data, patch))
            {
                continue;
            }
            steps.add(stepFor(patch, snapshot.stateOf(patch.getId())));
        }

        return new GuidanceChecklist(
                activityId,
                "Farming run",
                "Best confirmed herb/tree patches",
                steps);
    }

    private boolean isConfirmedReachable(
            StrategyDataBundle data,
            FarmingRunPatchDefinition patch)
    {
        AccessMemorySnapshot memory = data.getAccessMemory();
        if (memory != null)
        {
            for (Integer region : patch.getRegionIds())
            {
                if (memory.hasObserved("region." + region)) return true;
            }
        }

        String quest = patch.getRequiredQuest();
        if (quest == null) return true;
        QuestSnapshot quests = data.getQuests();
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
            return new GuidanceStep(patch.getId(),
                    prefix + patch.getDisplayName(),
                    "Visit once so Strategist can read this patch.",
                    GuidanceStepState.CHECK_NEEDED);
        }

        switch (observed.getState())
        {
            case GROWING:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(), "Planted",
                        GuidanceStepState.COMPLETE);
            case READY:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(),
                        patch.getKind() == FarmingPatchKind.TREE
                                ? "Check/clear and replant" : "Harvest and replant",
                        GuidanceStepState.ACTION);
            case EMPTY:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(), "Plant this patch",
                        GuidanceStepState.ACTION);
            case DISEASED:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(), "Cure the crop",
                        GuidanceStepState.WARNING);
            case DEAD:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(), "Clear and replant",
                        GuidanceStepState.WARNING);
            case UNKNOWN:
            default:
                return new GuidanceStep(patch.getId(),
                        prefix + patch.getDisplayName(), "Check patch state",
                        GuidanceStepState.CHECK_NEEDED);
        }
    }
}

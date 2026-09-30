package compass;

import java.util.*;

/** One evaluated contract shared by selection, final actionability and rendering. */
final class MethodReadiness
{
    final RequirementState access;
    final MethodPreparation preparation;
    final MethodCapacity capacity;
    final MethodIntelligenceCatalog.Method method;
    final MethodIntelligenceCatalog.Recipe recipe;
    final int batch;
    final int reviewLevel, currentXp;
    final String reason;

    MethodReadiness(RequirementState access, MethodPreparation preparation, MethodCapacity capacity,
            MethodIntelligenceCatalog.Method method, MethodIntelligenceCatalog.Recipe recipe,
            int batch, String reason, int reviewLevel, int currentXp)
    {
        this.access = access;
        this.preparation = preparation;
        this.capacity = capacity;
        this.method = method;
        this.recipe = recipe;
        this.batch = batch;
        this.reason = reason;
        this.reviewLevel = reviewLevel;
        this.currentXp = currentXp;
    }

    boolean actionable()
    {
        return access == RequirementState.VERIFIED && preparation != null && preparation.feasible()
                && capacity != null && capacity.working == RequirementState.VERIFIED;
    }

    List<EvidenceCheck> checks()
    {
        List<EvidenceCheck> result = new ArrayList<>();
        result.add(new EvidenceCheck("method:access", "Method access", access, reason));
        result.add(new EvidenceCheck("method:preparation", "Verified preparation",
                preparation != null && preparation.feasible() ? RequirementState.VERIFIED
                        : preparation != null && preparation.state == MethodPreparation.State.BLOCKED
                        ? RequirementState.BLOCKED : RequirementState.CHECK_NEEDED,
                preparation == null ? reason : preparation.explanation()));
        result.add(new EvidenceCheck("method:capacity", "Working inventory capacity",
                capacity == null ? RequirementState.CHECK_NEEDED : capacity.working,
                capacity == null ? "Capacity is unresolved." : capacity.reason));
        return result;
    }

    double adjustment()
    {
        if (!actionable()) return -10_000;
        double burden = 0;
        for (MethodPreparation.Step step : preparation.steps)
            if (step.kind == MethodPreparation.Kind.BUY) burden += 12;
            else if (step.kind == MethodPreparation.Kind.ACQUIRE) burden += 15 + step.input.quantity;
            else if (step.kind != MethodPreparation.Kind.CARRIED) burden += 3;
        return (preparation.state == MethodPreparation.State.READY ? 10 : 0) - burden;
    }

    String processingAction(int target)
    {
        target = Math.min(target, reviewLevel);
        int remaining = Math.max(0, net.runelite.api.Experience.getXpForLevel(target) - currentXp);
        return recipe.action + " Process at most " + batch + " actions in this batch. "
                + "Stop at level " + target + " or when this batch is used, whichever comes first; "
                + "review supplies and capacity before repeating. " + remaining + " "
                + recipe.skill.getName() + " XP remaining to this checkpoint.";
    }

    Guidance guidance(int target)
    {
        if (!actionable()) return null;
        List<String> pending = new ArrayList<>();
        for (MethodPreparation.Step step : preparation.steps)
            if (step.action != null && !step.action.trim().isEmpty()) pending.add(step.action);
        String first = pending.isEmpty() ? "" : "First, " + String.join(" ", pending) + " Then ";
        String action = first + processingAction(target);
        return new Guidance(action, preparation.explanation(), method.location,
                "Retain " + recipe.outputDescription + ". No disposal is assumed. "
                        + "A working batch is verified; processing outcomes and sustained efficiency are not inferred from available space.",
                method.bankLoop ? BankingMode.CONVENTIONAL_BANK_LOOP : BankingMode.LOCAL_PROCESSING);
    }
}

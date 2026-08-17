package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Connects a curated Strategist method to a deterministic RuneLite skill action.
 *
 * <p>The method catalog decides <em>which route</em> is strategically sensible.
 * This profile describes the repeatable unit inside that route so Strategist can
 * convert exact XP remaining into laps, catches, logs, casts, furniture builds,
 * items processed, etc. Methods with variable XP per completion deliberately do
 * not get a profile until a trustworthy model exists.</p>
 */
public final class MethodExecutionProfile
{
    public enum InputMode
    {
        NONE,
        ACTION_ITEM,
        RAW_ACTION_ITEM,
        LOG_FOR_BOW,
        BAR_FOR_SMITHED_ITEM,
        UNCUT_GEM,
        SAPLING_FOR_TREE,
        DART_TIP_FOR_DART,
        UNFINISHED_BOLT,
        FIXED
    }

    private final String methodId;
    private final List<String> actionTerms;
    private final String unitSingular;
    private final String unitPlural;
    private final double xpMultiplier;
    private final List<MethodInputRule> inputs;
    private final String note;

    /** Compatibility constructor for profiles with one material rule. */
    public MethodExecutionProfile(
            String methodId,
            String unitSingular,
            String unitPlural,
            double xpMultiplier,
            InputMode inputMode,
            String fixedInputName,
            double fixedInputPerAction,
            String note,
            String... actionTerms)
    {
        this(
                methodId,
                unitSingular,
                unitPlural,
                xpMultiplier,
                inputMode == null || inputMode == InputMode.NONE
                        ? Collections.emptyList()
                        : Collections.singletonList(new MethodInputRule(
                                inputMode,
                                fixedInputName,
                                fixedInputPerAction)),
                note,
                actionTerms
        );
    }

    public MethodExecutionProfile(
            String methodId,
            String unitSingular,
            String unitPlural,
            double xpMultiplier,
            List<MethodInputRule> inputs,
            String note,
            String... actionTerms)
    {
        this.methodId = methodId;
        this.actionTerms = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(actionTerms)));
        this.unitSingular = unitSingular;
        this.unitPlural = unitPlural;
        this.xpMultiplier = xpMultiplier <= 0 ? 1.0 : xpMultiplier;
        this.inputs = Collections.unmodifiableList(
                inputs == null ? new ArrayList<>() : new ArrayList<>(inputs));
        this.note = note;
    }

    public String getMethodId() { return methodId; }
    public List<String> getActionTerms() { return actionTerms; }
    public String getUnitSingular() { return unitSingular; }
    public String getUnitPlural() { return unitPlural; }
    public double getXpMultiplier() { return xpMultiplier; }
    public List<MethodInputRule> getInputs() { return inputs; }
    public String getNote() { return note; }

    /** Compatibility accessors return the first input rule, if one exists. */
    public InputMode getInputMode()
    {
        return inputs.isEmpty() ? InputMode.NONE : inputs.get(0).getMode();
    }

    public String getFixedInputName()
    {
        return inputs.isEmpty() ? null : inputs.get(0).getFixedName();
    }

    public double getFixedInputPerAction()
    {
        return inputs.isEmpty() ? 0.0 : inputs.get(0).getQuantityPerAction();
    }

    public String unit(int count)
    {
        return count == 1 ? unitSingular : unitPlural;
    }
}

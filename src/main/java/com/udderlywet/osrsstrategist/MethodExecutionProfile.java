package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Connects a curated Compass method to a deterministic RuneLite skill action.
 *
 * <p>The method catalog decides <em>which route</em> is strategically sensible.
 * This profile describes the repeatable unit inside that route so Compass can
 * convert exact XP remaining into laps, catches, logs, casts, furniture builds,
 * items processed, etc. Methods with variable XP per completion deliberately do
 * not get a profile until a trustworthy model exists.</p>
 */
public final class MethodExecutionProfile
{
    public enum ProgressEstimateMode
    {
        /** Every successful action represented by the chosen row awards one XP value. */
        EXACT_ACTIONS,
        /** One repeated interaction can yield several differently valued outputs. */
        VARIABLE_OUTPUT_RANGE,
        /** Only XP remaining is safe to show. */
        XP_ONLY
    }

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

    @Getter
    private final String methodId;
    @Getter
    private final List<String> actionTerms;
    @Getter
    private final String unitSingular;
    @Getter
    private final String unitPlural;
    @Getter
    private final double xpMultiplier;
    @Getter
    private final List<MethodInputRule> inputs;
    @Getter
    private final String note;
    @Getter
    private final ProgressEstimateMode progressEstimateMode;

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
                ProgressEstimateMode.EXACT_ACTIONS,
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
        this(methodId, unitSingular, unitPlural, xpMultiplier, inputs, note,
                ProgressEstimateMode.EXACT_ACTIONS, actionTerms);
    }

    public MethodExecutionProfile(
            String methodId,
            String unitSingular,
            String unitPlural,
            double xpMultiplier,
            List<MethodInputRule> inputs,
            String note,
            ProgressEstimateMode progressEstimateMode,
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
        this.progressEstimateMode = progressEstimateMode == null
                ? ProgressEstimateMode.XP_ONLY : progressEstimateMode;
    }


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

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
        FIXED
    }

    private final String methodId;
    private final List<String> actionTerms;
    private final String unitSingular;
    private final String unitPlural;
    private final double xpMultiplier;
    private final InputMode inputMode;
    private final String fixedInputName;
    private final double fixedInputPerAction;
    private final String note;

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
        this.methodId = methodId;
        this.actionTerms = Collections.unmodifiableList(
                new ArrayList<>(Arrays.asList(actionTerms)));
        this.unitSingular = unitSingular;
        this.unitPlural = unitPlural;
        this.xpMultiplier = xpMultiplier <= 0 ? 1.0 : xpMultiplier;
        this.inputMode = inputMode == null ? InputMode.NONE : inputMode;
        this.fixedInputName = fixedInputName;
        this.fixedInputPerAction = Math.max(0.0, fixedInputPerAction);
        this.note = note;
    }

    public String getMethodId() { return methodId; }
    public List<String> getActionTerms() { return actionTerms; }
    public String getUnitSingular() { return unitSingular; }
    public String getUnitPlural() { return unitPlural; }
    public double getXpMultiplier() { return xpMultiplier; }
    public InputMode getInputMode() { return inputMode; }
    public String getFixedInputName() { return fixedInputName; }
    public double getFixedInputPerAction() { return fixedInputPerAction; }
    public String getNote() { return note; }

    public String unit(int count)
    {
        return count == 1 ? unitSingular : unitPlural;
    }
}

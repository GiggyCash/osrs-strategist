package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Exact or partial consumed-input model for one deterministic skill action. */
public final class UniversalActionRecipe
{
    private final List<ResolvedMethodInput> inputs;
    private final String setup;
    private final boolean exactInputs;

    public UniversalActionRecipe(
            List<ResolvedMethodInput> inputs,
            String setup,
            boolean exactInputs)
    {
        List<ResolvedMethodInput> copy = new ArrayList<>();
        if (inputs != null) copy.addAll(inputs);
        this.inputs = Collections.unmodifiableList(copy);
        this.setup = setup;
        this.exactInputs = exactInputs;
    }

    public static UniversalActionRecipe noConsumedInputs(String setup)
    {
        return new UniversalActionRecipe(Collections.emptyList(), setup, true);
    }

    public static UniversalActionRecipe unknown(String setup)
    {
        return new UniversalActionRecipe(Collections.emptyList(), setup, false);
    }

    public List<ResolvedMethodInput> getInputs()
    {
        return inputs;
    }

    public String getSetup()
    {
        return setup;
    }

    public boolean hasExactInputs()
    {
        return exactInputs;
    }
}

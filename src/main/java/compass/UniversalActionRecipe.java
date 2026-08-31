package compass;

import java.util.*;

import lombok.Getter;

/** Exact or partial consumed-input model for one deterministic skill action. */
public final class UniversalActionRecipe
{
    @Getter
    private final List<MethodInput> inputs;
    @Getter
    private final String setup;
    private final boolean exactInputs;

    public UniversalActionRecipe(
            List<MethodInput> inputs,
            String setup,
            boolean exactInputs)
    {
        List<MethodInput> copy = new ArrayList<>();
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



    public boolean hasExactInputs()
    {
        return exactInputs;
    }
}

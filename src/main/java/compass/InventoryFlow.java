package compass;

/** Qualitative inventory change over one repeatable method loop. */
public enum InventoryFlow
{
    NEUTRAL,
    CONSUMES_CARRIED_INPUTS,
    GROWS_NONSTACKABLE_OUTPUTS,
    REPLACES_INPUTS_WITH_OUTPUTS
}

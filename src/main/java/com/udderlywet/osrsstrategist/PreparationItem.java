package com.udderlywet.osrsstrategist;

public final class PreparationItem
{
    private final String label;
    private final int required;
    private final int available;

    public PreparationItem(String label, int required, int available)
    {
        this.label = label;
        this.required = required;
        this.available = available;
    }

    public boolean ready() { return available >= required; }
    public String getLabel() { return label; }
    public int getRequired() { return required; }
    public int getAvailable() { return available; }
}

package com.udderlywet.osrsstrategist;

/** User-selectable text scale for the Strategist sidebar only. */
public enum SidebarTextSize
{
    STANDARD(1.00f),
    LARGE(1.12f),
    EXTRA_LARGE(1.24f);

    private final float scale;

    SidebarTextSize(float scale)
    {
        this.scale = scale;
    }

    public float getScale()
    {
        return scale;
    }
}

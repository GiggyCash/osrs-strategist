package compass;

/** User-selectable scaling limited to the Compass sidebar. */
public enum SidebarTextSize
{
    STANDARD("Standard", 1.00f),
    LARGE("Large", 1.12f),
    EXTRA_LARGE("Extra large", 1.24f);

    private final String displayName;
    private final float scale;

    SidebarTextSize(String displayName, float scale)
    {
        this.displayName = displayName;
        this.scale = scale;
    }

    float getScale() { return scale; }

    @Override
    public String toString() { return displayName; }
}

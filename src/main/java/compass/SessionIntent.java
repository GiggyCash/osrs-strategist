package compass;

public enum SessionIntent
{
    QUICK_20_MIN("Quick session"),
    ONE_HOUR("~1 hour"),
    LONG_SESSION("Long session"),
    AFK("AFK"),
    PICK_FOR_ME("Pick for me");

    private final String displayName;

    SessionIntent(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}

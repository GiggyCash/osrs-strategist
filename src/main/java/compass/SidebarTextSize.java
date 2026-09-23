package compass;

import lombok.RequiredArgsConstructor;
import lombok.Getter;

/** User-selectable scaling limited to the Compass sidebar. */
@RequiredArgsConstructor
@Getter
public enum SidebarTextSize
{
    STANDARD("Standard", 1.00f),
    LARGE("Large", 1.12f),
    EXTRA_LARGE("Extra large", 1.24f);

    final String displayName;
    final float scale;

    @Override
    public String toString() { return displayName; }
}

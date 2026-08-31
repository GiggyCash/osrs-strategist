package compass;

import java.util.*;
import javax.inject.Singleton;

/** Encounter-context gear progression loaded from the bundled catalog. */
@Singleton
public class GearProgressionCatalog extends CatalogStore<GearProgressionEntry>
{
    public GearProgressionCatalog() { super(Text.get(295), GearProgressionEntry[].class); }
    public List<GearProgressionEntry> forStyle(CombatStyle style)
    {
        return filter(entry -> entry.getStyle() == style);
    }
    public List<GearProgressionEntry> forContext(String contextId)
    {
        return filter(entry -> entry.getContextId().equals(contextId));
    }
}

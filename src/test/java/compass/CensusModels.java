package compass;

import java.util.*;
import net.runelite.api.Skill;

final class ContentCoverageEntry
{
    private final String id, name;
    private final ContentCoverageState state;
    private final String reason, provenance;
    ContentCoverageEntry(String id, String name, ContentCoverageState state,
            String reason, String provenance)
    {
        this.id = id; this.name = name; this.state = state;
        this.reason = reason; this.provenance = provenance;
    }
    String getId() { return id; }
    String getName() { return name; }
    ContentCoverageState getState() { return state; }
    String getReason() { return reason; }
    String getProvenance() { return provenance; }
}

enum ContentCoverageState
{
    STRUCTURED, PARTIAL_PREPARATION, CONSERVATIVE_FAIL_CLOSED,
    NOT_PROGRESSION_RELEVANT
}

enum TransportCategory
{
    FAIRY_RING, SPIRIT_TREE, GNOME, MINECART, BOAT, CHARTER, ITEM_TELEPORT,
    STANDARD_SPELL, ANCIENT_SPELL, LUNAR_SPELL, ARCEUUS_SPELL, JEWELLERY,
    DIARY, MINIGAME, QUEST, AGILITY_SHORTCUT, SLAYER, POH, SAILING
}

final class TransportDefinition
{
    private final String id, name;
    private final TransportCategory category;
    private final boolean membersOnly;
    private final String quest;
    private final boolean questStartSuffices;
    private final Skill skill;
    private final int level;
    private final String itemOrAccessCheck, pohFurniture;
    private final boolean wilderness;
    private final List<String> uses;
    private TransportDefinition()
    {
        id = name = quest = itemOrAccessCheck = pohFurniture = null;
        category = null; skill = null; level = 0;
        membersOnly = questStartSuffices = wilderness = false; uses = null;
    }
    String getId() { return id; }
    String getName() { return name; }
    TransportCategory getCategory() { return category; }
    int getLevel() { return level; }
    String getItemOrAccessCheck() { return itemOrAccessCheck; }
    boolean isQuestStartSufficient() { return questStartSuffices; }
    int getFanOut() { return uses.size(); }
}

final class TransportCatalog
{
    private final Map<String, TransportDefinition> routes = new LinkedHashMap<>();
    TransportCatalog()
    {
        for (TransportDefinition value : BundledCatalogLoader.array(
                "/content/catalogs/transports.json", TransportDefinition[].class))
            if (routes.put(value.getId(), value) != null)
                throw new IllegalStateException("Duplicate transport " + value.getId());
    }
    List<TransportDefinition> all() { return new ArrayList<>(routes.values()); }
    TransportDefinition get(String id) { return routes.get(id); }
}

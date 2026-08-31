package compass;

import javax.inject.Singleton;

/** Minigame definitions loaded from the bundled catalog. */
@Singleton
public class MinigameCatalog extends CatalogStore<MinigameDefinition>
{
    public MinigameCatalog() { super(Text.get(1808), MinigameDefinition[].class); }
    public MinigameDefinition byId(String id)
    {
        return id == null ? null : find(value -> id.equals(value.id));
    }
}

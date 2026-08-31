package compass;

import java.util.*;
import javax.inject.Singleton;

/** Qualitative money-making methods loaded from the bundled catalog. */
@Singleton
public class MoneyMakingCatalog extends CatalogStore<MoneyMakingDefinition>
{
    public MoneyMakingCatalog() { super(Text.get(1734), MoneyMakingDefinition[].class); }
    public List<MoneyMakingDefinition> forAccount(AccountMode mode)
    {
        return filter(method -> method.supports(mode));
    }
}

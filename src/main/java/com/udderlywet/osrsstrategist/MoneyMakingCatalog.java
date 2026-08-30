package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/** Qualitative money-making methods loaded from the bundled catalog. */
@Singleton
public class MoneyMakingCatalog
{
    private final List<MoneyMakingDefinition> methods = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array("/content/catalogs/money-making.json",
                    MoneyMakingDefinition[].class)));

    public List<MoneyMakingDefinition> all() { return methods; }
    public List<MoneyMakingDefinition> forAccount(AccountMode mode)
    {
        List<MoneyMakingDefinition> result = new ArrayList<>();
        for (MoneyMakingDefinition method : methods)
            if (method.supports(mode)) result.add(method);
        return Collections.unmodifiableList(result);
    }
}

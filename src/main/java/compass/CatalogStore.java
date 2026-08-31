package compass;

import java.util.*;
import java.util.function.Predicate;

/** Shared immutable loading and querying for small bundled definition catalogs. */
class CatalogStore<T>
{
    final List<T> values;

    CatalogStore(String resource, Class<T[]> type)
    {
        values = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(resource, type)));
    }

    public List<T> all() { return values; }

    final T find(Predicate<T> match)
    {
        for (T value : values) if (match.test(value)) return value;
        return null;
    }

    final List<T> filter(Predicate<T> match)
    {
        List<T> result = new ArrayList<>();
        for (T value : values) if (match.test(value)) result.add(value);
        return Collections.unmodifiableList(result);
    }
}

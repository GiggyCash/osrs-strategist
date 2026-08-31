package compass;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Function;

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

class IndexedCatalog<T>
{
    final List<T> values;
    final Map<String, T> index;

    IndexedCatalog(String resource, Class<T[]> type, Function<T, String> key)
    {
        this(resource, type, key, Function.identity());
    }

    IndexedCatalog(String resource, Class<T[]> type, Function<T, String> key,
            Function<String, String> normalize)
    {
        values = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(resource, type)));
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values)
        {
            String raw = value == null ? null : key.apply(value);
            String id = raw == null ? null : normalize.apply(raw);
            if (id == null || result.put(id, value) != null)
                throw new IllegalStateException("Invalid or duplicate catalog key in " + resource);
        }
        index = Collections.unmodifiableMap(result);
    }

    final T indexed(String key) { return key == null ? null : index.get(key); }
}

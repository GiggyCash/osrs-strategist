package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Future cloud-sync DTO. It contains only explicitly selected Compass data
 * categories. Game credentials, Jagex credentials, RuneLite credentials, chat,
 * and unrelated client data are intentionally outside this model.
 */
public final class PlusSyncEnvelope
{
    public static final int SCHEMA_VERSION = 1;

    private final String profileToken;
    private final long generatedAtMillis;
    private final Set<PlusDataCategory> categories;

    public PlusSyncEnvelope(
            String profileToken,
            long generatedAtMillis,
            Set<PlusDataCategory> categories)
    {
        this.profileToken = profileToken;
        this.generatedAtMillis = generatedAtMillis;
        EnumSet<PlusDataCategory> copy = EnumSet.noneOf(PlusDataCategory.class);
        if (categories != null)
        {
            copy.addAll(categories);
        }
        this.categories = Collections.unmodifiableSet(copy);
    }

    public String getProfileToken()
    {
        return profileToken;
    }

    public long getGeneratedAtMillis()
    {
        return generatedAtMillis;
    }

    public Set<PlusDataCategory> getCategories()
    {
        return categories;
    }
}

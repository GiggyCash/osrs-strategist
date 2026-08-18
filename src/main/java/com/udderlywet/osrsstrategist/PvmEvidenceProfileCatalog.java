package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Singleton;

/** Fully locally-verifiable subset; other encounters retain readiness floors. */
@Singleton
public class PvmEvidenceProfileCatalog
{
    private final Map<String, PvmEvidenceProfile> profiles = new LinkedHashMap<>();

    public PvmEvidenceProfileCatalog()
    {
        add(new PvmEvidenceProfile("pvm:brutus", "melee",
                Collections.emptyList(), 5, 0));
        add(new PvmEvidenceProfile("pvm:obor", "melee",
                Collections.singletonList("Giant key"), 5, 0));
        add(new PvmEvidenceProfile("pvm:bryophyta", "melee",
                Collections.singletonList("Mossy key"), 5, 0));
        add(new PvmEvidenceProfile("pvm:scurrius", "melee",
                Collections.emptyList(), 5, 1));
    }

    public PvmEvidenceProfile forActivity(String id)
    {
        return id == null ? null : profiles.get(id.toLowerCase());
    }

    public int size() { return profiles.size(); }

    private void add(PvmEvidenceProfile profile)
    {
        profiles.put(profile.getActivityId(), profile);
    }
}

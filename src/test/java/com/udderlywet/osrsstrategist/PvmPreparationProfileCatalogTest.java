package com.udderlywet.osrsstrategist;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PvmPreparationProfileCatalogTest
{
    @Test
    public void bulkProfilesAreUniqueReviewableAndCannotClaimReady()
    {
        PvmPreparationProfileCatalog catalog = new PvmPreparationProfileCatalog();
        assertEquals(71, catalog.all().size());
        Set<String> ids = new HashSet<>();
        for (PvmPreparationProfile profile : catalog.all().values())
        {
            assertFalse(profile.getChecks().isEmpty());
            assertFalse(profile.getProvenance().trim().isEmpty());
            assertFalse(profile.getAccountValue().trim().isEmpty());
            assertNotNull(new PvmActivityCatalog().byId(profile.getActivityId()));
            assertFalse(profile.getStyle().trim().isEmpty());
            assertFalse(profile.getActivityId(), !ids.add(profile.getActivityId()));
        }
        for (PvmActivityDefinition activity : new PvmActivityCatalog().all())
            assertNotNull(activity.getId(), catalog.forActivity(activity.getId()));
    }
}

package com.udderlywet.osrsstrategist;

import javax.inject.Singleton;

/** Gate between imported knowledge and production recommendation data. */
@Singleton
public class GameKnowledgeImportPolicy
{
    public boolean mayUseForPlanning(KnowledgeRecordMetadata metadata)
    {
        if (metadata == null
                || metadata.getRecordId() == null
                || metadata.getRecordId().trim().isEmpty()
                || metadata.getDomain() == null
                || metadata.getSource() == null)
        {
            return false;
        }

        // A source being reputable does not replace record validation. Wiki/API
        // imports can be staged automatically, but must be marked verified by
        // the update/test pipeline before they affect recommendations.
        return metadata.isVerifiedForPlanning()
                && metadata.getVerifiedAtMillis() > 0L;
    }
}

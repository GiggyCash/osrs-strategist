package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Honest coverage manifest for structured OSRS knowledge.
 *
 * <p>PARTIAL means useful verified records exist but the domain is not yet
 * exhaustive. SCAFFOLDED means the architecture has a typed home but exact game
 * data still needs verification/import. This prevents broad architecture from
 * being confused with complete game-data coverage.</p>
 */
@Singleton
public class GameKnowledgeManifest
{
    private final Map<GameKnowledgeDomain, KnowledgeCoverage> coverage;

    public GameKnowledgeManifest()
    {
        EnumMap<GameKnowledgeDomain, KnowledgeCoverage> values =
                new EnumMap<>(GameKnowledgeDomain.class);
        for (GameKnowledgeDomain domain : GameKnowledgeDomain.values())
        {
            values.put(domain, KnowledgeCoverage.SCAFFOLDED);
        }

        values.put(GameKnowledgeDomain.SKILLS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.TRAINING_METHODS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.QUESTS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.FARMING, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.RECURRING_COOLDOWNS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.WILDERNESS_CONTENT, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.RESOURCE_SOURCES, KnowledgeCoverage.PARTIAL);

        this.coverage = Collections.unmodifiableMap(values);
    }

    public KnowledgeCoverage coverageOf(GameKnowledgeDomain domain)
    {
        return coverage.getOrDefault(domain, KnowledgeCoverage.SCAFFOLDED);
    }

    public boolean hasTypedHomeForEveryDomain()
    {
        for (GameKnowledgeDomain domain : GameKnowledgeDomain.values())
        {
            if (!coverage.containsKey(domain)) return false;
        }
        return true;
    }

    public Map<GameKnowledgeDomain, KnowledgeCoverage> all()
    {
        return coverage;
    }
}

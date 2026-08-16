package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Honest coverage manifest for structured OSRS knowledge.
 *
 * <p>PARTIAL means useful verified records/readers exist but the domain is not
 * yet exhaustive. SCAFFOLDED means the architecture has a typed home but exact
 * game data/readers still need substantial verification. VERIFIED is reserved
 * for a domain whose maintained source and coverage are genuinely complete, not
 * simply because its catalog is large.</p>
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

        // RuneLite's Skill enum is the maintained source of truth for the set of
        // trainable skills, including Sailing. Method knowledge remains separate.
        values.put(GameKnowledgeDomain.SKILLS, KnowledgeCoverage.VERIFIED);

        values.put(GameKnowledgeDomain.TRAINING_METHODS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.QUESTS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.ACHIEVEMENT_DIARIES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.SAILING, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.SLAYER, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.FARMING, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.HUNTER, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.MINIGAMES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.SKILLING_BOSSES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.PVM, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.RAIDS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.COMBAT_ACHIEVEMENTS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.COLLECTION_LOG, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.CLUES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.GEAR_PROGRESSION, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.MONEY_MAKING, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.TRANSPORT, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.POH, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.RESOURCE_SOURCES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.USEFUL_UNTRADEABLES, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.RECURRING_COOLDOWNS, KnowledgeCoverage.PARTIAL);
        values.put(GameKnowledgeDomain.WILDERNESS_CONTENT, KnowledgeCoverage.PARTIAL);

        // Miniquests and exhaustive STASH-step knowledge still need a larger
        // verified data import before they should be described as PARTIAL.
        values.put(GameKnowledgeDomain.MINIQUESTS, KnowledgeCoverage.SCAFFOLDED);
        values.put(GameKnowledgeDomain.STASH, KnowledgeCoverage.SCAFFOLDED);

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

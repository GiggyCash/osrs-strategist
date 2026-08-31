package com.udderlywet.osrsstrategist;

import java.util.*;

import lombok.Getter;

@Getter
public final class Opportunity
{
    private final String id;
    private final OpportunityType type;
    private final String title;
    private final boolean ready;
    private final RecommendationConfidence confidence;
    private final List<String> preparation;
    private final boolean setupVerified;
    private final CandidateSafetyEvidence safetyEvidence;

    public Opportunity(
            String id,
            OpportunityType type,
            String title,
            boolean ready,
            RecommendationConfidence confidence,
            List<String> preparation)
    {
        this(id, type, title, ready, confidence, preparation, false,
                CandidateSafetyEvidence.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            RecommendationConfidence confidence, List<String> preparation,
            boolean setupVerified)
    {
        this(id, type, title, ready, confidence, preparation, setupVerified,
                CandidateSafetyEvidence.unknown());
    }

    public Opportunity(
            String id, OpportunityType type, String title, boolean ready,
            RecommendationConfidence confidence, List<String> preparation,
            boolean setupVerified, CandidateSafetyEvidence safetyEvidence)
    {
        this.id = id;
        this.type = type;
        this.title = title;
        this.ready = ready;
        this.confidence = confidence;
        this.preparation = Collections.unmodifiableList(
                preparation == null ? new ArrayList<>() : new ArrayList<>(preparation)
        );
        this.setupVerified = setupVerified;
        this.safetyEvidence = safetyEvidence == null
                ? CandidateSafetyEvidence.unknown() : safetyEvidence;
    }








}

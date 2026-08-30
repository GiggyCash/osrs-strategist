package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

public final class Opportunity
{
    @Getter
    private final String id;
    @Getter
    private final OpportunityType type;
    @Getter
    private final String title;
    @Getter
    private final boolean ready;
    @Getter
    private final RecommendationConfidence confidence;
    @Getter
    private final List<String> preparation;
    @Getter
    private final boolean setupVerified;
    @Getter
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

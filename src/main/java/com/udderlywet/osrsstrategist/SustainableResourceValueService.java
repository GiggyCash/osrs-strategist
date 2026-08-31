package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Separates "I own enough now" from "this account can cheaply replace it".
 * Exact quantity is used only for observed stacks; future usage is never
 * projected without a typed request from the activity model.
 */
@Singleton
public final class SustainableResourceValueService
{
    private final ResourceSourceCatalog sources;

    @Inject
    public SustainableResourceValueService(ResourceSourceCatalog sources)
    {
        this.sources = sources;
    }

    public SustainableResourceValueService()
    {
        this(new ResourceSourceCatalog());
    }

    public ResourcePipelineAssessment assess(StrategyContext context,
            ResourcePipelineRequest request)
    {
        if (context == null || context.data() == null || request == null
                || request.getNeed() == null)
        {
            return unknown(Text.get(805));
        }
        ResourceNeed need = request.getNeed();
        String name = need.getItemName();
        if (name == null || name.trim().isEmpty())
            return unknown(Text.get(807));

        ItemIndex items = new ItemIndex(context.data(),
                context.isUseGroupStorage());
        int observed = items.quantity(name);
        int required = Math.max(1, need.getQuantity());
        AccountMode mode = context.accountMode();
        MembershipStatus membership = context.data().account() == null
                ? MembershipStatus.UNKNOWN
                : context.data().account().getMembershipStatus();
        List<String> routes = sources.suggestions(name, mode, membership,
                context.isAllowWildernessMethods());
        boolean knownFamily = !sources.match(name).isEmpty();

        if (observed >= required)
        {
            int adjustment;
            if (request.getUseKind() == ResourceUseKind.REUSABLE)
            {
                adjustment = 5;
            }
            else
            {
                int replacement = replacementBurden(request, mode, context);
                if (request.getUseKind() == ResourceUseKind.RECURRING_CONSUMABLE)
                    replacement += 2;
                adjustment = Math.max(-10, Math.min(4,
                        4 - replacement - scarcityPenalty(request.getScarcity())));
            }
            return result(ResourcePipelineState.READY_CURRENT_SUPPLY,
                    adjustment, observed, required, routes,
                    Text.get(808)
                            + replacementEvidence(request, mode, routes));
        }

        if (!items.resourceContainersObserved())
        {
            return result(ResourcePipelineState.UNKNOWN_STORAGE, -2, observed,
                    required, routes,
                    Text.get(809));
        }

        if (!knownFamily)
        {
            return result(ResourcePipelineState.UNKNOWN_SOURCE, -4, observed,
                    required, routes,
                    Text.get(810));
        }

        int burden = replacementBurden(request, mode, context);
        int penalty = scarcityPenalty(request.getScarcity());
        if (request.getUseKind() == ResourceUseKind.RECURRING_CONSUMABLE)
            burden += 2;
        else if (request.getUseKind() == ResourceUseKind.REUSABLE)
            burden = Math.max(0, burden - 2);
        int adjustment = Math.max(-12, Math.min(3, 2 - burden - penalty));
        ResourcePipelineState state = burden <= 3
                && request.getScarcity() != ResourceScarcity.RESERVED_FOR_GOAL
                ? ResourcePipelineState.SUSTAINABLE_REPLACEMENT
                : ResourcePipelineState.ACQUISITION_NEEDED;
        return result(state, adjustment, observed, required, routes,
                Text.get(1433) + Math.max(0, required - observed)
                        + ". " + replacementEvidence(request, mode, routes));
    }

    public ResourcePortfolioAssessment assessAll(StrategyContext context,
            List<ResourcePipelineRequest> requests)
    {
        if (requests == null || requests.isEmpty())
            return new ResourcePortfolioAssessment(
                    ResourcePipelineState.READY_CURRENT_SUPPLY, 0,
                    Collections.emptyList(), Collections.emptyList());
        ResourcePipelineState worst = ResourcePipelineState.READY_CURRENT_SUPPLY;
        int adjustment = 0;
        List<String> routes = new ArrayList<>();
        List<ResourcePipelineAssessment> assessments = new ArrayList<>();
        for (ResourcePipelineRequest request : requests)
        {
            ResourcePipelineAssessment value = assess(context, request);
            assessments.add(value);
            if (severity(value.getState()) > severity(worst)) worst = value.getState();
            adjustment += value.getScoreAdjustment();
            for (String route : value.getAcquisitionRoutes())
                if (!routes.contains(route) && routes.size() < 4) routes.add(route);
        }
        return new ResourcePortfolioAssessment(worst,
                Math.max(-16, Math.min(6, adjustment)), assessments, routes);
    }

    private static int replacementBurden(ResourcePipelineRequest request,
            AccountMode mode, StrategyContext context)
    {
        if (mode.usesGrandExchange() && request.isTradeable()) return 1;
        int burden = mode.isIronLike() ? 5 : 4;
        if (mode.isGroupIronman() && context.isUseGroupStorage()
                && context.data().groupStorage() != null
                && context.data().groupStorage().isObserved())
            burden--;
        if (mode == AccountMode.ULTIMATE_IRONMAN) burden += 2;
        return burden;
    }

    private static int scarcityPenalty(ResourceScarcity scarcity)
    {
        switch (scarcity)
        {
            case ABUNDANT: return 0;
            case ORDINARY: return 1;
            case SCARCE: return 4;
            case RESERVED_FOR_GOAL: return 7;
            case UNKNOWN:
            default: return 2;
        }
    }

    private static String replacementEvidence(ResourcePipelineRequest request,
            AccountMode mode, List<String> routes)
    {
        String access;
        if (mode.usesGrandExchange() && request.isTradeable())
            access = Text.get(811);
        else if (mode == AccountMode.ULTIMATE_IRONMAN)
            access = Text.get(812);
        else if (mode.isIronLike())
            access = Text.get(813);
        else access = Text.get(814);
        return access + (routes.isEmpty() ? "."
                : Text.get(806));
    }

    private static ResourcePipelineAssessment unknown(String evidence)
    {
        return result(ResourcePipelineState.UNKNOWN_SOURCE, -4, 0, 0,
                Collections.emptyList(), evidence);
    }

    private static ResourcePipelineAssessment result(ResourcePipelineState state,
            int adjustment, int observed, int required, List<String> routes,
            String evidence)
    {
        return new ResourcePipelineAssessment(state, adjustment, observed,
                required, routes, evidence);
    }

    private static int severity(ResourcePipelineState state)
    {
        switch (state)
        {
            case READY_CURRENT_SUPPLY: return 0;
            case SUSTAINABLE_REPLACEMENT: return 1;
            case UNKNOWN_STORAGE: return 2;
            case ACQUISITION_NEEDED: return 3;
            case UNKNOWN_SOURCE:
            default: return 4;
        }
    }

}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * One engine for recurring and interrupt-driven opportunities.
 *
 * <p>Birdhouses, farming runs, clues, weekly content, Kingdom, and future
 * cooldown systems should all be represented here instead of growing separate
 * reminder implementations. A skill level alone is never enough to activate a
 * reminder. The relevant opportunity must first be observed/verified.</p>
 */
@Singleton
public class OpportunityEngine
{
    public List<Opportunity> evaluate(AccountSnapshot snapshot)
    {
        if (snapshot == null)
        {
            return Collections.emptyList();
        }

        return evaluate(
                StrategyDataBundle.builder(snapshot).build()
        );
    }

    public List<Opportunity> evaluate(StrategyDataBundle data)
    {
        List<Opportunity> opportunities = new ArrayList<>();

        if (data == null || data.getAccount() == null)
        {
            return opportunities;
        }

        RecurringOpportunitySnapshot recurring =
                data.getRecurringOpportunities();
        long now = System.currentTimeMillis();

        // Birdhouses and herb runs have detailed prep requirements, but they
        // still stay completely hidden until a future reader has verified the
        // account's relevant unlock/timer state. This avoids fake reminders.
        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:birdhouse",
                OpportunityType.BIRDHOUSE_RUN,
                "Birdhouse run",
                Arrays.asList(
                        "Fossil Island access",
                        "Hammer and chisel",
                        "4 clockworks",
                        "4 suitable logs",
                        "40 suitable seeds",
                        "Verified transport to the route"
                )
        );

        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:herb-run",
                OpportunityType.HERB_RUN,
                "Herb run",
                Arrays.asList(
                        "Herb seeds",
                        "Spade and seed dibber",
                        "Rake or verified Tool Leprechaun storage",
                        "Compost",
                        "Verified reachable herb patches",
                        "Patch transport"
                )
        );

        ClueSnapshot clue = data.getClue();
        if (clue != null && clue.isCluePresent())
        {
            opportunities.add(
                    new Opportunity(
                            "opportunity:clue",
                            OpportunityType.CLUE,
                            clue.getClueType() == null
                                    ? "Pending clue"
                                    : clue.getClueType() + " clue",
                            clue.getConfidence()
                                    == RecommendationConfidence.VERIFIED,
                            clue.getConfidence(),
                            Arrays.asList(
                                    "Required clue equipment",
                                    "Spade when needed",
                                    "Teleports/transport",
                                    "Combat supplies when needed",
                                    "Verified STASH state when relevant"
                            )
                    )
            );
        }

        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:tree-run",
                OpportunityType.TREE_RUN,
                "Tree run",
                Collections.emptyList()
        );
        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:farming-contract",
                OpportunityType.FARMING_CONTRACT,
                "Farming contract",
                Collections.emptyList()
        );
        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:tears-of-guthix",
                OpportunityType.TEARS_OF_GUTHIX,
                "Tears of Guthix",
                Collections.emptyList()
        );
        addPreparedTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:kingdom",
                OpportunityType.KINGDOM,
                "Kingdom",
                Collections.emptyList()
        );

        return opportunities;
    }

    /**
     * A recurring opportunity is only created after a reader has put a timer
     * into RecurringOpportunitySnapshot. If no timer is present, Strategist has
     * no verified evidence that this character should see the reminder.
     */
    private static void addPreparedTimedOpportunity(
            List<Opportunity> result,
            RecurringOpportunitySnapshot recurring,
            long now,
            String id,
            OpportunityType type,
            String title,
            List<String> preparation)
    {
        if (recurring == null || recurring.readyAt(id) == null)
        {
            return;
        }

        boolean ready = recurring.isReadyNow(id, now);

        result.add(
                new Opportunity(
                        id,
                        type,
                        title,
                        ready,
                        RecommendationConfidence.VERIFIED,
                        preparation
                )
        );
    }
}

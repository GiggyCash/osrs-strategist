package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * One engine for recurring and interrupt-driven opportunities.
 *
 * <p>Birdhouses, farming runs, clues, weekly content, Kingdom, and future
 * cooldown systems should all be represented here instead of growing separate
 * reminder implementations. Readiness is only marked true when the relevant
 * state has actually been observed.</p>
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

        AccountSnapshot snapshot = data.getAccount();
        RecurringOpportunitySnapshot recurring =
                data.getRecurringOpportunities();
        long now = System.currentTimeMillis();

        if (snapshot.getSkillLevel(Skill.HUNTER) >= 5)
        {
            boolean ready = recurring != null
                    && recurring.isReadyNow(
                    "opportunity:birdhouse",
                    now
            );

            opportunities.add(
                    new Opportunity(
                            "opportunity:birdhouse",
                            OpportunityType.BIRDHOUSE_RUN,
                            "Birdhouse run",
                            ready,
                            ready
                                    ? RecommendationConfidence.VERIFIED
                                    : RecommendationConfidence.CHECK_NEEDED,
                            Arrays.asList(
                                    "Fossil Island access",
                                    "Hammer and chisel",
                                    "4 clockworks",
                                    "4 suitable logs",
                                    "40 suitable seeds",
                                    "Verified transport to the route"
                            )
                    )
            );
        }

        if (snapshot.getSkillLevel(Skill.FARMING) >= 9)
        {
            boolean ready = recurring != null
                    && recurring.isReadyNow(
                    "opportunity:herb-run",
                    now
            );

            opportunities.add(
                    new Opportunity(
                            "opportunity:herb-run",
                            OpportunityType.HERB_RUN,
                            "Herb run",
                            ready,
                            ready
                                    ? RecommendationConfidence.VERIFIED
                                    : RecommendationConfidence.CHECK_NEEDED,
                            Arrays.asList(
                                    "Herb seeds",
                                    "Spade and seed dibber",
                                    "Rake or verified Tool Leprechaun storage",
                                    "Compost",
                                    "Verified reachable herb patches",
                                    "Patch transport"
                            )
                    )
            );
        }

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

        addTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:tree-run",
                OpportunityType.TREE_RUN,
                "Tree run"
        );
        addTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:farming-contract",
                OpportunityType.FARMING_CONTRACT,
                "Farming contract"
        );
        addTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:tears-of-guthix",
                OpportunityType.TEARS_OF_GUTHIX,
                "Tears of Guthix"
        );
        addTimedOpportunity(
                opportunities,
                recurring,
                now,
                "opportunity:kingdom",
                OpportunityType.KINGDOM,
                "Kingdom"
        );

        return opportunities;
    }

    /**
     * Only creates these optional opportunities after a reader has observed a
     * timer for them. This keeps the engine from advertising content it cannot
     * yet prove the player has unlocked.
     */
    private static void addTimedOpportunity(
            List<Opportunity> result,
            RecurringOpportunitySnapshot recurring,
            long now,
            String id,
            OpportunityType type,
            String title)
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
                        Collections.emptyList()
                )
        );
    }
}

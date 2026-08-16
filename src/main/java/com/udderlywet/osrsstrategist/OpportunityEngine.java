package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.Skill;

@Singleton
public class OpportunityEngine
{
    public List<Opportunity> evaluate(AccountSnapshot snapshot)
    {
        List<Opportunity> opportunities = new ArrayList<>();

        if (snapshot.getSkillLevel(Skill.HUNTER) >= 5)
        {
            opportunities.add(
                    new Opportunity(
                            "opportunity:birdhouse",
                            OpportunityType.BIRDHOUSE_RUN,
                            "Birdhouse run",
                            false,
                            RecommendationConfidence.CHECK_NEEDED,
                            Arrays.asList(
                                    "Confirm Fossil Island access",
                                    "Hammer and chisel",
                                    "4 clockworks",
                                    "4 suitable logs",
                                    "40 suitable seeds",
                                    "Transport to the birdhouse route"
                            )
                    )
            );
        }

        if (snapshot.getSkillLevel(Skill.FARMING) >= 9)
        {
            opportunities.add(
                    new Opportunity(
                            "opportunity:herb-run",
                            OpportunityType.HERB_RUN,
                            "Herb run",
                            false,
                            RecommendationConfidence.CHECK_NEEDED,
                            Arrays.asList(
                                    "Herb seeds",
                                    "Spade and seed dibber",
                                    "Rake or confirmed Tool Leprechaun storage",
                                    "Compost",
                                    "Only include confirmed reachable patches",
                                    "Patch transport"
                            )
                    )
            );
        }

        return opportunities;
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

/**
 * One engine for recurring and interrupt-driven opportunities.
 *
 * <p>Nothing appears merely because content exists in OSRS. A recurring entry
 * is surfaced only after a reader has observed a ready/cooldown timestamp for
 * that character. Membership is also enforced here so stale observations from
 * a previously-member account cannot leak members-only opportunities into an
 * F2P session.</p>
 */
@Singleton
public class OpportunityEngine
{
    public List<Opportunity> evaluate(AccountSnapshot snapshot)
    {
        if (snapshot == null) return Collections.emptyList();
        return evaluate(StrategyDataBundle.builder(snapshot).build());
    }

    public List<Opportunity> evaluate(StrategyDataBundle data)
    {
        List<Opportunity> opportunities = new ArrayList<>();
        if (data == null || data.getAccount() == null) return opportunities;

        MembershipStatus membership = data.getAccount().getMembershipStatus();
        RecurringOpportunitySnapshot recurring = data.getRecurringOpportunities();
        long now = System.currentTimeMillis();

        // Every currently-modelled recurring activity below is members content.
        // Keep the entire family out of an F2P plan even if its timer was
        // observed while this character previously had membership.
        if (ContentAccessRules.hasVerifiedMembership(membership))
        {
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:birdhouse", OpportunityType.BIRDHOUSE_RUN,
                    "Birdhouse run", Arrays.asList(
                            "Fossil Island access", "Hammer and chisel",
                            "4 clockworks", "4 suitable logs", "40 suitable seeds",
                            "Verified transport to the route"));

            addHerbRunOpportunity(opportunities, data, recurring, now);

            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:tree-run", OpportunityType.TREE_RUN,
                    "Tree run", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:farming-contract", OpportunityType.FARMING_CONTRACT,
                    "Farming contract", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:tears-of-guthix", OpportunityType.TEARS_OF_GUTHIX,
                    "Tears of Guthix", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:kingdom", OpportunityType.KINGDOM,
                    "Kingdom", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:kingdom-approval", OpportunityType.KINGDOM_APPROVAL,
                    "Kingdom approval", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:battlestaves", OpportunityType.BATTLESTAVES,
                    "Daily battlestaves", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:dynamite", OpportunityType.DYNAMITE,
                    "Daily dynamite", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:diary-daily", OpportunityType.DAILY_DIARY_REWARD,
                    "Daily diary reward", Collections.emptyList());
        }

        ClueSnapshot clue = data.getClue();
        if (clue != null && clue.isCluePresent())
        {
            ClueTier tier = ClueTier.fromText(clue.getClueType());
            if (tier.isAvailableFor(membership))
            {
                opportunities.add(new Opportunity(
                        "opportunity:clue",
                        OpportunityType.CLUE,
                        clue.getClueType() == null ? "Pending clue" : clue.getClueType() + " clue",
                        clue.getConfidence() == RecommendationConfidence.VERIFIED,
                        clue.getConfidence(),
                        Arrays.asList(
                                "Required clue equipment", "Spade when needed",
                                "Teleports/transport", "Combat supplies when needed",
                                "Verified STASH state when relevant"),
                        false,
                        CandidateSafetyEvidence.potentiallyIrreversible(
                                tier == ClueTier.BEGINNER)
                ));
            }
        }

        return opportunities;
    }

    private static void addPreparedTimedOpportunity(
            List<Opportunity> result,
            RecurringOpportunitySnapshot recurring,
            long now,
            String id,
            OpportunityType type,
            String title,
            List<String> preparation)
    {
        if (recurring == null || recurring.readyAt(id) == null) return;
        boolean ready = recurring.isReadyNow(id, now);
        result.add(new Opportunity(
                id, type, title, ready,
                RecommendationConfidence.VERIFIED, preparation));
    }

    private static void addHerbRunOpportunity(List<Opportunity> result,
            StrategyDataBundle data, RecurringOpportunitySnapshot recurring,
            long now)
    {
        String id = "opportunity:herb-run";
        if (recurring == null || recurring.readyAt(id) == null) return;

        List<String> missing = new ArrayList<>();
        InventorySnapshot inventory = data.getInventory();
        if (!inventoryHas(inventory, "spade")) missing.add("Carry a spade");
        if (!inventoryHas(inventory, "seed dibber")) missing.add("Carry a seed dibber");
        int farmingLevel = data.getAccount().getSkillLevel(net.runelite.api.Skill.FARMING);
        ResourceRequirement herbSeeds = new FarmingSupplyCatalog()
                .herbSeedsForLevel(farmingLevel);
        if (!inventoryHasAnyId(inventory, herbSeeds.getItemIds()))
            missing.add("Carry a suitable herb seed");
        if (farmingLevel < 9)
            missing.add("Reach Farming level 9 for the modeled herb patches");
        FarmingSnapshot farming = data.getFarming();
        if (!hasReachableHerbPatch(farming))
            missing.add("Verify at least one reachable herb patch");

        boolean ready = recurring.isReadyNow(id, now);
        boolean setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.HERB_RUN, "Herb run",
                ready, RecommendationConfidence.VERIFIED, missing,
                setupVerified, CandidateSafetyEvidence.skill(false,
                        net.runelite.api.Skill.FARMING)));
    }

    private static boolean inventoryHas(InventorySnapshot inventory, String expected)
    {
        if (inventory == null) return false;
        for (ItemStackSnapshot item : inventory.getItems())
            if (item != null && item.getQuantity() > 0 && item.getName() != null
                    && item.getName().equalsIgnoreCase(expected)) return true;
        return false;
    }

    private static boolean inventoryHasAnyId(InventorySnapshot inventory, int[] itemIds)
    {
        if (inventory == null || itemIds == null) return false;
        for (ItemStackSnapshot item : inventory.getItems())
            if (item != null && item.getQuantity() > 0)
                for (int itemId : itemIds)
                    if (item.getItemId() == itemId) return true;
        return false;
    }

    private static boolean hasReachableHerbPatch(FarmingSnapshot farming)
    {
        if (farming == null) return false;
        for (FarmingAccessDefinition patch : new FarmingAccessCatalog().all())
            if (patch.isHerbPatch() && farming.isPatchReachable(patch.getId()))
                return true;
        return false;
    }
}

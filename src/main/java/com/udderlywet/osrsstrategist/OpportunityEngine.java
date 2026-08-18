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
            addBirdhouseOpportunity(opportunities, data, recurring, now);

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

    private static void addBirdhouseOpportunity(List<Opportunity> result,
            StrategyDataBundle data, RecurringOpportunitySnapshot recurring,
            long now)
    {
        String id = "opportunity:birdhouse";
        if (recurring == null || recurring.readyAt(id) == null) return;
        List<String> missing = new ArrayList<>();
        QuestSnapshot quests = data.getQuests();
        if (quests == null || quests.statusOf("Bone Voyage") != QuestStatus.COMPLETE)
            missing.add("Complete Bone Voyage for Fossil Island access");
        InventorySnapshot inventory = data.getInventory();
        if (!inventoryHas(inventory, "hammer")) missing.add("Carry a hammer");
        if (!inventoryHas(inventory, "chisel")) missing.add("Carry a chisel");
        if (inventoryQuantity(inventory, "clockwork") < 4)
            missing.add("Carry 4 clockworks");
        if (matchingQuantity(inventory, " log", "logs") < 4)
            missing.add("Carry 4 suitable logs");
        if (birdhouseSeedQuantity(inventory) < 40)
            missing.add("Carry 40 suitable hop or herb seeds");
        if (data.getAccount().getSkillLevel(net.runelite.api.Skill.HUNTER) < 5)
            missing.add("Reach Hunter level 5");
        if (data.getAccount().getSkillLevel(net.runelite.api.Skill.CRAFTING) < 5)
            missing.add("Reach Crafting level 5");
        boolean ready = recurring.isReadyNow(id, now);
        boolean setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.BIRDHOUSE_RUN,
                "Birdhouse run", ready, RecommendationConfidence.VERIFIED,
                missing, setupVerified,
                CandidateSafetyEvidence.skill(false,
                        net.runelite.api.Skill.HUNTER)));
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

    private static int inventoryQuantity(InventorySnapshot inventory, String name)
    {
        if (inventory == null || name == null) return 0;
        int quantity = 0;
        for (ItemStackSnapshot item : inventory.getItems())
            if (item != null && item.getName() != null
                    && item.getName().equalsIgnoreCase(name)) quantity += item.getQuantity();
        return quantity;
    }

    private static int matchingQuantity(InventorySnapshot inventory,
            String suffix, String exact)
    {
        if (inventory == null) return 0;
        int quantity = 0;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            if (item == null || item.getName() == null) continue;
            String name = item.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(suffix) || name.endsWith(" " + exact)
                    || name.equals(exact)) quantity += item.getQuantity();
        }
        return quantity;
    }

    private static int birdhouseSeedQuantity(InventorySnapshot inventory)
    {
        if (inventory == null) return 0;
        int quantity = 0;
        for (ItemStackSnapshot item : inventory.getItems())
        {
            if (item == null || item.getName() == null) continue;
            String name = item.getName().toLowerCase(java.util.Locale.ROOT);
            boolean hops = name.equals("barley seed") || name.equals("hammerstone seed")
                    || name.equals("asgarnian seed") || name.equals("jute seed")
                    || name.equals("yanillian seed") || name.equals("krandorian seed")
                    || name.equals("wildblood seed");
            boolean herb = name.equals("guam seed") || name.equals("marrentill seed")
                    || name.equals("tarromin seed") || name.equals("harralander seed")
                    || name.equals("ranarr seed") || name.equals("toadflax seed")
                    || name.equals("irit seed") || name.equals("avantoe seed")
                    || name.equals("kwuarm seed") || name.equals("snapdragon seed")
                    || name.equals("cadantine seed") || name.equals("lantadyme seed")
                    || name.equals("dwarf weed seed") || name.equals("torstol seed");
            if (hops || herb) quantity += item.getQuantity();
        }
        return quantity;
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

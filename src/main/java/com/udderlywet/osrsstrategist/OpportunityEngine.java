package com.udderlywet.osrsstrategist;

import java.util.*;
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
    private final FarmingAccessCatalog farmingAccessCatalog =
            new FarmingAccessCatalog();
    private final FarmingSupplyCatalog farmingSupplyCatalog =
            new FarmingSupplyCatalog();

    public List<Opportunity> evaluate(AccountSnapshot snapshot)
    {
        if (snapshot == null) return Collections.emptyList();
        return evaluate(GameData.builder(snapshot).build());
    }

    public List<Opportunity> evaluate(GameData data)
    {
        List<Opportunity> opportunities = new ArrayList<>();
        if (data == null || data.account() == null) return opportunities;

        MembershipStatus membership = data.account().getMembershipStatus();
        RecurringOpportunitySnapshot recurring = data.recurringOpportunities();
        long now = System.currentTimeMillis();

        // Every currently-modelled recurring activity below is members content.
        // Keep the entire family out of an F2P plan even if its timer was
        // observed while this character previously had membership.
        if (ContentAccessRules.hasVerifiedMembership(membership))
        {
            addBirdhouseOpportunity(opportunities, data, recurring, now);

            addHerbRunOpportunity(opportunities, data, recurring, now,
                    farmingAccessCatalog, farmingSupplyCatalog);

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
                    Text.get(1522), Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:dynamite", OpportunityType.DYNAMITE,
                    "Daily dynamite", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:diary-daily", OpportunityType.DAILY_DIARY_REWARD,
                    Text.get(1523), Collections.emptyList());
        }

        ClueSnapshot clue = data.clue();
        if (clue != null && clue.isCluePresent())
        {
            ClueTier tier = ClueTier.fromText(clue.getClueType());
            if (tier.isAvailableFor(membership))
            {
                ClueStepSnapshot step = clue.getCurrentStep();
                List<String> preparation = new ArrayList<>();
                if (step == null)
                    preparation.add(Text.get(392));
                else
                {
                    preparation.addAll(step.getItemRequirements());
                    if (step.isRequiresSpade()) preparation.add("Spade");
                    if (step.isRequiresLight()) preparation.add("Light source");
                    if (step.hasEnemy()) preparation.add(
                            Text.get(1524) + step.getEnemy());
                    if (step.isWilderness()) preparation.add(
                            Text.get(1525));
                    if (step.hasStashUnit()) preparation.add(
                            "Observe the " + step.getStashUnit()
                                    + Text.get(1526));
                }
                boolean ready = step != null
                        && tier == ClueTier.BEGINNER
                        && !step.requiresPreparation();
                opportunities.add(new Opportunity(
                        "opportunity:clue",
                        OpportunityType.CLUE,
                        clue.getClueType() == null ? "Pending clue"
                                : clue.getClueType() + " clue"
                                + (step == null ? "" : ": " + step.getKind()),
                        ready,
                        ready ? Confidence.VERIFIED
                                : Confidence.CHECK_NEEDED,
                        preparation,
                        false,
                        step != null && step.hasEnemy()
                                ? SafetyEvidence.potentiallyIrreversible(
                                        tier == ClueTier.BEGINNER)
                                : SafetyEvidence.harmless(
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
                Confidence.VERIFIED, preparation));
    }

    private static void addHerbRunOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now, FarmingAccessCatalog accessCatalog,
            FarmingSupplyCatalog supplyCatalog)
    {
        String id = "opportunity:herb-run";
        if (recurring == null || recurring.readyAt(id) == null) return;

        List<String> missing = new ArrayList<>();
        ItemsState inventory = data.inventory();
        if (!inventoryHas(inventory, "spade")) missing.add("Carry a spade");
        if (!inventoryHas(inventory, "seed dibber")) missing.add(Text.get(1527));
        int farmingLevel = data.account().getSkillLevel(net.runelite.api.Skill.FARMING);
        ResourceRequirement herbSeeds = supplyCatalog.herbSeedsForLevel(
                farmingLevel);
        if (!inventoryHasAnyId(inventory, herbSeeds.getItemIds()))
            missing.add(Text.get(1528));
        if (farmingLevel < 9)
            missing.add(Text.get(393));
        FarmingSnapshot farming = data.farming();
        if (!hasReachableHerbPatch(farming, accessCatalog))
            missing.add(Text.get(394));

        boolean ready = recurring.isReadyNow(id, now);
        boolean setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.HERB_RUN, "Herb run",
                ready, Confidence.VERIFIED, missing,
                setupVerified, SafetyEvidence.skill(false,
                net.runelite.api.Skill.FARMING)));
    }

    private static void addBirdhouseOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now)
    {
        String id = "opportunity:birdhouse";
        if (recurring == null || recurring.readyAt(id) == null) return;
        List<String> missing = new ArrayList<>();
        QuestSnapshot quests = data.quests();
        if (quests == null || quests.statusOf("Bone Voyage") != QuestStatus.COMPLETE)
            missing.add(Text.get(395));
        ItemsState inventory = data.inventory();
        if (!inventoryHas(inventory, "hammer")) missing.add("Carry a hammer");
        if (!inventoryHas(inventory, "chisel")) missing.add("Carry a chisel");
        if (inventoryQuantity(inventory, "clockwork") < 4)
            missing.add(Text.get(1529));
        if (matchingQuantity(inventory, " log", "logs") < 4)
            missing.add(Text.get(1530));
        if (birdhouseSeedQuantity(inventory) < 40)
            missing.add(Text.get(1531));
        if (data.account().getSkillLevel(net.runelite.api.Skill.HUNTER) < 5)
            missing.add(Text.get(1532));
        if (data.account().getSkillLevel(net.runelite.api.Skill.CRAFTING) < 5)
            missing.add(Text.get(1533));
        boolean ready = recurring.isReadyNow(id, now);
        boolean setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.BIRDHOUSE_RUN,
                "Birdhouse run", ready, Confidence.VERIFIED,
                missing, setupVerified,
                SafetyEvidence.skill(false,
                        net.runelite.api.Skill.HUNTER)));
    }

    private static boolean inventoryHas(ItemsState inventory, String expected)
    {
        if (inventory == null) return false;
        for (ItemState item : inventory.getItems())
            if (item != null && item.getQuantity() > 0 && item.getName() != null
                    && item.getName().equalsIgnoreCase(expected)) return true;
        return false;
    }

    private static boolean inventoryHasAnyId(ItemsState inventory, int[] itemIds)
    {
        if (inventory == null || itemIds == null) return false;
        for (ItemState item : inventory.getItems())
            if (item != null && item.getQuantity() > 0)
                for (int itemId : itemIds)
                    if (item.getItemId() == itemId) return true;
        return false;
    }

    private static int inventoryQuantity(ItemsState inventory, String name)
    {
        if (inventory == null || name == null) return 0;
        int quantity = 0;
        for (ItemState item : inventory.getItems())
            if (item != null && item.getName() != null
                    && item.getName().equalsIgnoreCase(name)) quantity += item.getQuantity();
        return quantity;
    }

    private static int matchingQuantity(ItemsState inventory,
            String suffix, String exact)
    {
        if (inventory == null) return 0;
        int quantity = 0;
        for (ItemState item : inventory.getItems())
        {
            if (item == null || item.getName() == null) continue;
            String name = item.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(suffix) || name.endsWith(" " + exact)
                    || name.equals(exact)) quantity += item.getQuantity();
        }
        return quantity;
    }

    private static int birdhouseSeedQuantity(ItemsState inventory)
    {
        if (inventory == null) return 0;
        int quantity = 0;
        for (ItemState item : inventory.getItems())
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

    private static boolean hasReachableHerbPatch(FarmingSnapshot farming,
            FarmingAccessCatalog catalog)
    {
        if (farming == null) return false;
        for (FarmingAccessDefinition patch : catalog.all())
            if (patch.isHerbPatch() && farming.isPatchReachable(patch.getId()))
                return true;
        return false;
    }
}

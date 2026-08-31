package com.udderlywet.osrsstrategist;
import static com.udderlywet.osrsstrategist.Text.get;

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
    private static final Set<String> BIRDHOUSE_SEEDS = new HashSet<>(Arrays.asList(
            "barley seed", "hammerstone seed", "asgarnian seed", "jute seed",
            "yanillian seed", "krandorian seed", "wildblood seed", "guam seed",
            "marrentill seed", "tarromin seed", "harralander seed", "ranarr seed",
            "toadflax seed", "irit seed", "avantoe seed", "kwuarm seed",
            "snapdragon seed", "cadantine seed", "lantadyme seed",
            "dwarf weed seed", "torstol seed"));
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

        var membership = data.account().getMembershipStatus();
        var recurring = data.recurringOpportunities();
        var now = System.currentTimeMillis();

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
                    get(1522), Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:dynamite", OpportunityType.DYNAMITE,
                    "Daily dynamite", Collections.emptyList());
            addPreparedTimedOpportunity(opportunities, recurring, now,
                    "opportunity:diary-daily", OpportunityType.DAILY_DIARY_REWARD,
                    get(1523), Collections.emptyList());
        }

        var clue = data.clue();
        if (clue != null && clue.isCluePresent())
        {
            var tier = ClueTier.fromText(clue.getClueType());
            if (tier.isAvailableFor(membership))
            {
                var step = clue.getCurrentStep();
                List<String> preparation = new ArrayList<>();
                if (step == null)
                    preparation.add(get(392));
                else
                {
                    preparation.addAll(step.getItemRequirements());
                    if (step.isRequiresSpade()) preparation.add("Spade");
                    if (step.isRequiresLight()) preparation.add("Light source");
                    if (step.hasEnemy()) preparation.add(
                            get(1524) + step.getEnemy());
                    if (step.isWilderness()) preparation.add(
                            get(1525));
                    if (step.hasStashUnit()) preparation.add(
                            "Observe the " + step.getStashUnit()
                                    + get(1526));
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
        var ready = recurring.isReadyNow(id, now);
        result.add(new Opportunity(
                id, type, title, ready,
                Confidence.VERIFIED, preparation));
    }

    private static void addHerbRunOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now, FarmingAccessCatalog accessCatalog,
            FarmingSupplyCatalog supplyCatalog)
    {
        var id = "opportunity:herb-run";
        if (recurring == null || recurring.readyAt(id) == null) return;

        List<String> missing = new ArrayList<>();
        var inventory = data.inventory();
        if (quantity(inventory, "spade") == 0) missing.add("Carry a spade");
        if (quantity(inventory, "seed dibber") == 0) missing.add(get(1527));
        var farmingLevel = data.account().getSkillLevel(net.runelite.api.Skill.FARMING);
        ResourceRequirement herbSeeds = supplyCatalog.herbSeedsForLevel(
                farmingLevel);
        if (inventory == null || inventory.quantityOf(herbSeeds.getItemIds()) == 0)
            missing.add(get(1528));
        if (farmingLevel < 9)
            missing.add(get(393));
        var farming = data.farming();
        if (!hasReachableHerbPatch(farming, accessCatalog))
            missing.add(get(394));

        var ready = recurring.isReadyNow(id, now);
        var setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.HERB_RUN, "Herb run",
                ready, Confidence.VERIFIED, missing,
                setupVerified, SafetyEvidence.skill(false,
                net.runelite.api.Skill.FARMING)));
    }

    private static void addBirdhouseOpportunity(List<Opportunity> result,
            GameData data, RecurringOpportunitySnapshot recurring,
            long now)
    {
        var id = "opportunity:birdhouse";
        if (recurring == null || recurring.readyAt(id) == null) return;
        List<String> missing = new ArrayList<>();
        var quests = data.quests();
        if (quests == null || quests.statusOf("Bone Voyage") != QuestStatus.COMPLETE)
            missing.add(get(395));
        var inventory = data.inventory();
        if (quantity(inventory, "hammer") == 0) missing.add("Carry a hammer");
        if (quantity(inventory, "chisel") == 0) missing.add("Carry a chisel");
        if (quantity(inventory, "clockwork") < 4)
            missing.add(get(1529));
        if (inventory == null || inventory.quantityWhere(name ->
                name.endsWith(" log") || name.endsWith(" logs")
                        || name.equals("logs")) < 4)
            missing.add(get(1530));
        if (birdhouseSeedQuantity(inventory) < 40)
            missing.add(get(1531));
        if (data.account().getSkillLevel(net.runelite.api.Skill.HUNTER) < 5)
            missing.add(get(1532));
        if (data.account().getSkillLevel(net.runelite.api.Skill.CRAFTING) < 5)
            missing.add(get(1533));
        var ready = recurring.isReadyNow(id, now);
        var setupVerified = ready && missing.isEmpty();
        result.add(new Opportunity(id, OpportunityType.BIRDHOUSE_RUN,
                "Birdhouse run", ready, Confidence.VERIFIED,
                missing, setupVerified,
                SafetyEvidence.skill(false,
                        net.runelite.api.Skill.HUNTER)));
    }

    private static int quantity(ItemsState inventory, String name)
    {
        return inventory == null ? 0 : inventory.quantityNamed(name);
    }

    private static int birdhouseSeedQuantity(ItemsState inventory)
    {
        return inventory == null ? 0
                : inventory.quantityWhere(BIRDHOUSE_SEEDS::contains);
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

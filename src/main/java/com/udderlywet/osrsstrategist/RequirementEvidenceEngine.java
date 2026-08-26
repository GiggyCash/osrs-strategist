package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Turns static method requirements into account-specific evidence checks.
 * Dedicated evaluators replace generic Needs Info rows one system at a time.
 */
@Singleton
public class RequirementEvidenceEngine
{
    private final FarmingAccessEvaluator farmingAccessEvaluator;
    private final AgilityAccessEvaluator agilityAccessEvaluator;
    private final FarmingSupplyCatalog farmingSupplyCatalog;
    private final RunecraftSupplyCatalog runecraftSupplyCatalog;
    private final ResourceReadinessService resourceReadinessService;

    @Inject
    public RequirementEvidenceEngine(
            FarmingAccessEvaluator farmingAccessEvaluator,
            AgilityAccessEvaluator agilityAccessEvaluator,
            FarmingSupplyCatalog farmingSupplyCatalog,
            RunecraftSupplyCatalog runecraftSupplyCatalog,
            ResourceReadinessService resourceReadinessService)
    {
        this.farmingAccessEvaluator = farmingAccessEvaluator;
        this.agilityAccessEvaluator = agilityAccessEvaluator;
        this.farmingSupplyCatalog = farmingSupplyCatalog;
        this.runecraftSupplyCatalog = runecraftSupplyCatalog;
        this.resourceReadinessService = resourceReadinessService;
    }

    /** Compatibility constructor retained for focused tests. */
    public RequirementEvidenceEngine(
            FarmingAccessEvaluator farmingAccessEvaluator,
            AgilityAccessEvaluator agilityAccessEvaluator)
    {
        this(
                farmingAccessEvaluator,
                agilityAccessEvaluator,
                new FarmingSupplyCatalog(),
                new RunecraftSupplyCatalog(),
                new ResourceReadinessService()
        );
    }

    /** Compatibility constructor retained for older focused tests. */
    public RequirementEvidenceEngine(FarmingAccessEvaluator farmingAccessEvaluator)
    {
        this(farmingAccessEvaluator, null);
    }

    public List<RequirementCheck> evaluate(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        return evaluate(data, method, false);
    }

    public List<RequirementCheck> evaluate(
            StrategyDataBundle data,
            TrainingMethod method,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        if (method == null)
        {
            return checks;
        }
        if (method.getSkill() == Skill.FARMING)
        {
            return evaluateFarming(data, method, useGroupStorage);
        }
        if (method.getSkill() == Skill.AGILITY && agilityAccessEvaluator != null)
        {
            return evaluateAgility(data, method);
        }
        if (method.getSkill() == Skill.RUNECRAFT
                && runecraftSupplyCatalog.supports(method.getId()))
        {
            return evaluateRunecraft(data, method, useGroupStorage);
        }
        if (method.getSkill() == Skill.MAGIC
                && ("magic_f2p_combat".equals(method.getId())
                    || "magic_f2p_fire_bolt".equals(method.getId())
                    || "magic_f2p_fire_blast".equals(method.getId())))
        {
            return evaluateF2pCombatMagic(data, method.getId(),
                    useGroupStorage);
        }
        if (method.getSkill() == Skill.COOKING
                && "cooking_f2p_fish".equals(method.getId()))
        {
            return evaluateCookedFish(data, useGroupStorage);
        }
        if (method.getSkill() == Skill.FISHING)
        {
            List<RequirementCheck> fishing = evaluateFishing(
                    data, method, useGroupStorage);
            if (fishing != null) return fishing;
        }
        if (method.getSkill() == Skill.HUNTER)
        {
            List<RequirementCheck> hunter = evaluateHunter(
                    data, method, useGroupStorage);
            if (hunter != null) return hunter;
        }
        if (method.getSkill() == Skill.CONSTRUCTION
                && "construction_crude_chairs".equals(method.getId()))
        {
            return evaluateCrudeChairs(data, useGroupStorage);
        }
        if (method.getSkill() == Skill.CONSTRUCTION
                && "construction_oak_larders".equals(method.getId()))
        {
            return evaluateOakLarders(data, useGroupStorage);
        }
        if (method.getSkill() == Skill.MINING)
        {
            return evaluateTool(data, method, ItemRequirementClass.PICKAXE,
                    useGroupStorage,
                    "resource:usable-pickaxe", "Usable pickaxe");
        }
        if (method.getSkill() == Skill.WOODCUTTING)
        {
            return evaluateTool(data, method, ItemRequirementClass.AXE,
                    useGroupStorage,
                    "resource:usable-axe", "Usable axe");
        }

        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    private List<RequirementCheck> evaluateF2pCombatMagic(
            StrategyDataBundle data, String methodId,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        int airPerCast = "magic_f2p_fire_blast".equals(methodId) ? 4
                : "magic_f2p_fire_bolt".equals(methodId) ? 3 : 1;
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:combat_magic_air",
                        "Air runes for one cast", airPerCast,
                        ItemID.AIRRUNE),
                useGroupStorage));
        if ("magic_f2p_fire_bolt".equals(methodId)
                || "magic_f2p_fire_blast".equals(methodId))
        {
            int firePerCast = "magic_f2p_fire_blast".equals(methodId) ? 5 : 4;
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:combat_magic_fire",
                            "Fire runes for one cast", firePerCast,
                            ItemID.FIRERUNE), useGroupStorage));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:combat_magic_catalytic",
                            "Catalytic rune for one cast", 1,
                            "magic_f2p_fire_blast".equals(methodId)
                                    ? ItemID.DEATHRUNE : ItemID.CHAOSRUNE),
                    useGroupStorage));
            return checks;
        }
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:wind_strike_mind",
                        "Mind rune", 1, ItemID.MINDRUNE),
                useGroupStorage));
        return checks;
    }

    private List<RequirementCheck> evaluateCookedFish(
            StrategyDataBundle data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        int level = data == null || data.getAccount() == null ? 1
                : data.getAccount().getSkillLevel(Skill.COOKING);
        List<Integer> legal = new ArrayList<>();
        legal.add(ItemID.RAW_SHRIMP);
        legal.add(ItemID.RAW_SARDINE);
        if (level >= 5) legal.add(ItemID.RAW_HERRING);
        if (level >= 15) legal.add(ItemID.RAW_TROUT);
        if (level >= 20) legal.add(ItemID.RAW_PIKE);
        if (level >= 25) legal.add(ItemID.RAW_SALMON);
        if (level >= 30) legal.add(ItemID.RAW_TUNA);
        if (level >= 40) legal.add(ItemID.RAW_LOBSTER);
        if (level >= 45) legal.add(ItemID.RAW_SWORDFISH);
        int[] legalIds = new int[legal.size()];
        for (int i = 0; i < legal.size(); i++) legalIds[i] = legal.get(i);
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:raw_fish", "Raw fish legal at the current level", 1,
                        legalIds),
                useGroupStorage));
        return checks;
    }

    private List<RequirementCheck> evaluateFishing(
            StrategyDataBundle data, TrainingMethod method,
            boolean useGroupStorage)
    {
        if ("fishing_lumbridge_shrimps".equals(method.getId()))
        {
            // The tutor beside this exact spot replaces a lost net, so lack of
            // an observed net does not block the first executable trip.
            return new ArrayList<>();
        }
        if (!"fishing_f2p_fly".equals(method.getId())) return null;
        List<RequirementCheck> checks = new ArrayList<>();
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:fly_rod", "Fly fishing rod", 1,
                        ItemID.FLY_FISHING_ROD), useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:fly_feathers", "Feathers", 1,
                        ItemID.FEATHER), useGroupStorage));
        return checks;
    }

    private List<RequirementCheck> evaluateHunter(
            StrategyDataBundle data, TrainingMethod method,
            boolean useGroupStorage)
    {
        if ("hunter_falconry".equals(method.getId()))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:falcon_rental", "500 coins for falcon rental",
                            500, ItemID.COINS), useGroupStorage));
            return checks;
        }
        if ("hunter_bird_traps".equals(method.getId()))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:bird_snare", "Bird snare", 1,
                            ItemID.HUNTING_SNARE), useGroupStorage));
            return checks;
        }
        return null;
    }

    private List<RequirementCheck> evaluateCrudeChairs(
            StrategyDataBundle data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        PohSnapshot poh = data == null ? null : data.getPoh();
        CapabilityState house = poh == null
                ? CapabilityState.UNKNOWN : poh.getHouseAccess();
        CapabilityState parlour = poh == null
                ? CapabilityState.UNKNOWN : poh.furnitureState("room:parlour");
        checks.add(capabilityCheck("construction:poh", "Player-owned house",
                house, "POH access has not been observed for this character."));
        checks.add(capabilityCheck("construction:parlour", "POH Parlour",
                parlour, "A Parlour chair hotspot has not been observed."));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_planks",
                        "Planks", 2, ItemID.WOODPLANK), useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_nails",
                        "Nails", 2, ItemID.NAILS_BRONZE, ItemID.NAILS_IRON,
                        ItemID.NAILS, ItemID.NAILS_BLACK, ItemID.NAILS_MITHRIL,
                        ItemID.NAILS_ADAMANT, ItemID.NAILS_RUNE),
                useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_hammer",
                        "Hammer", 1, ItemID.HAMMER), useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_saw",
                        "Saw", 1, ItemID.POH_SAW,
                        ItemID.EYEGLO_CRYSTAL_SAW, ItemID.WEARABLE_SAW),
                useGroupStorage));
        return checks;
    }

    private List<RequirementCheck> evaluateOakLarders(
            StrategyDataBundle data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        PohSnapshot poh = data == null ? null : data.getPoh();
        CapabilityState house = poh == null
                ? CapabilityState.UNKNOWN : poh.getHouseAccess();
        CapabilityState kitchen = poh == null
                ? CapabilityState.UNKNOWN : poh.furnitureState("room:kitchen");
        checks.add(capabilityCheck("construction:poh", "Player-owned house",
                house, "POH access has not been observed for this character."));
        checks.add(capabilityCheck("construction:kitchen", "POH Kitchen",
                kitchen, "An oak-larder hotspot has not been observed."));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_oak_planks",
                        "Oak planks", 8, ItemID.PLANK_OAK), useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_hammer",
                        "Hammer", 1, ItemID.HAMMER), useGroupStorage));
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:construction_saw",
                        "Saw", 1, ItemID.POH_SAW,
                        ItemID.EYEGLO_CRYSTAL_SAW, ItemID.WEARABLE_SAW),
                useGroupStorage));
        return checks;
    }

    private static RequirementCheck capabilityCheck(
            String id, String label, CapabilityState state,
            String unknownEvidence)
    {
        return new RequirementCheck(id, label,
                state == CapabilityState.VERIFIED
                        ? RequirementState.VERIFIED
                        : RequirementState.CHECK_NEEDED,
                state == CapabilityState.VERIFIED
                        ? label + " is observed for this character."
                        : unknownEvidence);
    }

    private List<RequirementCheck> evaluateTool(
            StrategyDataBundle data,
            TrainingMethod method,
            ItemRequirementClass itemClass,
            boolean useGroupStorage,
            String requirementId,
            String label)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        ObservedItemIndex items = new ObservedItemIndex(data, useGroupStorage);
        int usable = items.quantityMatching(itemClass,
                java.util.Collections.emptyList());
        checks.add(new RequirementCheck(
                requirementId,
                label,
                usable > 0 ? RequirementState.VERIFIED
                        : RequirementState.CHECK_NEEDED,
                usable > 0
                        ? label + " is observed in immediately usable ownership."
                        : "No " + label.toLowerCase()
                                + " is observed in immediately usable ownership."));
        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    /**
     * Conventional F2P altar routes are resource-driven. The player does not
     * need to manually confirm them once Compass has observed the essence and
     * the matching talisman/tiara in equipment, inventory, bank, or safe
     * account-specific storage.
     */
    private List<RequirementCheck> evaluateRunecraft(
            StrategyDataBundle data,
            TrainingMethod method,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        checks.add(resourceReadinessService.evaluate(
                data,
                runecraftSupplyCatalog.runeEssence(), useGroupStorage
        ));
        ResourceRequirement entry = runecraftSupplyCatalog.altarEntryFor(method.getId());
        if (entry != null)
        {
            checks.add(resourceReadinessService.evaluate(
                    data, entry, useGroupStorage));
        }
        return checks;
    }

    private List<RequirementCheck> evaluateAgility(
            StrategyDataBundle data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        if ("agility_wilderness".equals(method.getId()))
        {
            checks.add(agilityAccessEvaluator.wildernessCourseCheck(data));
            checks.add(new RequirementCheck(
                    "agility:wilderness_risk",
                    "Wilderness risk accepted",
                    RequirementState.VERIFIED,
                    "This method only reaches the evaluator when Wilderness methods are enabled."
            ));
            return checks;
        }

        AgilityCourseDefinition course =
                agilityAccessEvaluator.bestStandardCourse(data);
        checks.add(agilityAccessEvaluator.courseCheck(data, course));
        return checks;
    }

    private List<RequirementCheck> evaluateFarming(
            StrategyDataBundle data,
            TrainingMethod method,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        AccountSnapshot account = data == null ? null : data.getAccount();
        FarmingSnapshot farming = data == null ? null : data.getFarming();
        int level = account == null ? 1 : account.getSkillLevel(Skill.FARMING);

        if ("farming_early".equals(method.getId()))
        {
            String patch = farmingAccessEvaluator.firstReachablePatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:reachable_patch",
                    "Reachable Farming patch",
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? "Quest/access checks and observed-area memory have not proven a patch yet."
                            : patch + " is available from quest/access evidence."
            ));
            checks.add(new RequirementCheck(
                    "farming:supplies",
                    "Seeds and farming tools",
                    RequirementState.CHECK_NEEDED,
                    "The early-Farming seed source is not verified yet; choose and verify a usable seed before starting."
            ));
            return checks;
        }

        if ("farming_falador_potatoes".equals(method.getId())
                || "farming_falador_watermelons".equals(method.getId()))
        {
            boolean reachable = farming != null
                    && farming.isPatchReachable("falador");
            checks.add(new RequirementCheck(
                    "farming:falador_patch", "South Falador allotments",
                    reachable ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    reachable
                            ? "Falador patches are reachable from account access evidence."
                            : "South Falador patch access has not been proven for this character."));
            RequirementCheck seeds = resourceReadinessService.evaluate(data,
                    "farming_falador_watermelons".equals(method.getId())
                            ? farmingSupplyCatalog.watermelonSeeds()
                            : farmingSupplyCatalog.potatoSeeds(),
                    useGroupStorage);
            checks.add(seeds);
            if ("farming_falador_watermelons".equals(method.getId())
                    && seeds.getState() != RequirementState.VERIFIED
                    && (account == null
                        || account.getSkillLevel(Skill.THIEVING) < 38))
            {
                checks.add(new RequirementCheck(
                        "farming:watermelon_seed_source",
                        "38 Thieving for the safe seed-acquisition loop",
                        RequirementState.BLOCKED,
                        "Watermelon seeds are not observed and Master Farmers cannot be pickpocketed below 38 Thieving."));
            }
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.rake(),
                    "rake", "Rake is verified in Tool Leprechaun storage.",
                    useGroupStorage));
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.dibber(),
                    "dibber", "Seed dibber is verified in Tool Leprechaun storage.",
                    useGroupStorage));
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.spade(),
                    "spade", "Spade is verified in Tool Leprechaun storage.",
                    useGroupStorage));
            return checks;
        }

        if ("farming_herbs".equals(method.getId()))
        {
            checks.add(new RequirementCheck(
                    "farming:level_9",
                    "9 Farming",
                    level >= 9
                            ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED,
                    "Current Farming level is " + level + "."
            ));

            String patch = farmingAccessEvaluator.firstReachableHerbPatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:herb_patch",
                    "Reachable herb patch",
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? "No herb patch has been proven by quest/access checks or prior observation yet."
                            : patch + " is available from quest/access evidence."
            ));

            checks.add(resourceReadinessService.evaluate(
                    data,
                    farmingSupplyCatalog.herbSeedsForLevel(level),
                    useGroupStorage
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.rake(),
                    "rake",
                    "Rake was previously verified in Tool Leprechaun storage.",
                    useGroupStorage
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.dibber(),
                    "dibber",
                    "Seed dibber was previously verified in Tool Leprechaun storage.",
                    useGroupStorage
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.spade(),
                    "spade",
                    "Spade was previously verified in Tool Leprechaun storage.",
                    useGroupStorage
            ));
            return checks;
        }

        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    private RequirementCheck toolCheck(
            StrategyDataBundle data,
            FarmingSnapshot farming,
            ResourceRequirement requirement,
            String toolId,
            String leprechaunEvidence,
            boolean useGroupStorage)
    {
        CapabilityState stored = farming == null
                ? CapabilityState.UNKNOWN
                : farming.leprechaunToolState(toolId);
        if (stored == CapabilityState.VERIFIED)
            return resourceReadinessService.evaluate(data, requirement,
                    stored, leprechaunEvidence);
        return resourceReadinessService.evaluate(
                data, requirement, useGroupStorage);
    }

    private RequirementCheck generic(String requirement)
    {
        return new RequirementCheck(
                "generic:" + requirement,
                requirement,
                RequirementState.CHECK_NEEDED,
                "More account state must be observed before this can be proven."
        );
    }
}

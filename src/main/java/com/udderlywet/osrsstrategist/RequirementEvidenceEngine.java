package com.udderlywet.osrsstrategist;

import java.util.*;
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
            GameData data,
            TrainingMethod method)
    {
        return evaluate(data, method, false);
    }

    public List<RequirementCheck> evaluate(
            GameData data,
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
        if (method.getSkill() == Skill.SAILING)
        {
            return evaluateSailing(data, method, useGroupStorage);
        }
        if (method.getSkill() == Skill.RUNECRAFT
                && runecraftSupplyCatalog.supports(method.getId()))
        {
            return evaluateRunecraft(data, method, useGroupStorage);
        }
        if (method.getSkill() == Skill.MAGIC
                && ("magic_f2p_combat".equals(method.getId())
                    || "magic_f2p_fire_bolt".equals(method.getId())
                    || "magic_f2p_fire_blast".equals(method.getId())
                    || "magic_f2p_fire_strike_splash".equals(method.getId())
                    || "magic_f2p_curse".equals(method.getId())))
        {
            return evaluateF2pCombatMagic(data, method.getId(),
                    useGroupStorage);
        }
        if (method.getSkill() == Skill.COOKING
                && "cooking_f2p_fish".equals(method.getId()))
        {
            return evaluateCookedFish(data, useGroupStorage);
        }
        if (method.getSkill() == Skill.COOKING
                && ("cooking_hosidius".equals(method.getId())
                    || "cooking_wines".equals(method.getId())))
        {
            return evaluateMembersCooking(data, method.getId(), useGroupStorage);
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
        if ("runecraft_gotr".equals(method.getId()))
        {
            List<RequirementCheck> gotr = evaluateQuestCompletion(
                    data, "Temple of the Eye", "quest:temple_of_the_eye");
            gotr.add(usableToolCheck(data, ItemRequirementClass.PICKAXE,
                    useGroupStorage, "resource:gotr_pickaxe", "Usable pickaxe"));
            gotr.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:gotr_chisel", "Chisel",
                            1, ItemID.CHISEL), useGroupStorage));
            return gotr;
        }
        if ("runecraft_zmi".equals(method.getId()))
        {
            List<RequirementCheck> zmiChecks = new ArrayList<>();
            zmiChecks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:zmi_pure_essence",
                            "Pure essence", 1, ItemID.BLANKRUNE_HIGH),
                    useGroupStorage));
            return zmiChecks;
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
            GameData data, String methodId,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        CombatEvidenceSnapshot combat = data == null ? null
                : data.combatEvidence();
        boolean spellbookObserved = combat != null;
        boolean standard = spellbookObserved
                && combat.getSpellbookSelector() == 0;
        checks.add(new RequirementCheck(
                "spellbook:standard", Text.get(1534),
                !spellbookObserved ? RequirementState.CHECK_NEEDED
                        : standard ? RequirementState.VERIFIED
                                : RequirementState.BLOCKED,
                !spellbookObserved
                        ? Text.get(613)
                        : standard
                                ? Text.get(1535)
                                : Text.get(624)));
        if ("magic_f2p_curse".equals(methodId))
        {
            checks.add(resource(data, useGroupStorage, "curse_body",
                    "Body rune", 1, ItemID.BODYRUNE));
            checks.add(resource(data, useGroupStorage, "curse_earth",
                    "Earth runes", 3, ItemID.EARTHRUNE));
            checks.add(resource(data, useGroupStorage, "curse_water",
                    "Water runes", 2, ItemID.WATERRUNE));
            checks.add(splashingEquipmentCheck(data));
            return checks;
        }
        boolean fireStrikeSplash =
                "magic_f2p_fire_strike_splash".equals(methodId);
        int airPerCast = "magic_f2p_fire_blast".equals(methodId) ? 4
                : "magic_f2p_fire_bolt".equals(methodId) ? 3
                : fireStrikeSplash ? 2 : 1;
        checks.add(resourceReadinessService.evaluate(data,
                new ResourceRequirement(
                        "resource:combat_magic_air",
                        Text.get(1536), airPerCast,
                        ItemID.AIRRUNE),
                useGroupStorage));
        if (fireStrikeSplash)
        {
            checks.add(resource(data, useGroupStorage, "splash_fire",
                    "Fire runes", 3, ItemID.FIRERUNE));
            checks.add(resource(data, useGroupStorage, "splash_mind",
                    "Mind rune", 1, ItemID.MINDRUNE));
            checks.add(splashingEquipmentCheck(data));
            return checks;
        }
        if ("magic_f2p_fire_bolt".equals(methodId)
                || "magic_f2p_fire_blast".equals(methodId))
        {
            int firePerCast = "magic_f2p_fire_blast".equals(methodId) ? 5 : 4;
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:combat_magic_fire",
                            Text.get(1537), firePerCast,
                            ItemID.FIRERUNE), useGroupStorage));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:combat_magic_catalytic",
                            Text.get(1538), 1,
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

    private List<RequirementCheck> evaluateSailing(
            GameData data, TrainingMethod method,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = evaluateQuestCompletion(
                data, "Pandemonium", "quest:pandemonium");
        String id = method.getId();
        if ("sailing_courier".equals(id))
        {
            SailingSnapshot sailing = data == null ? null : data.sailing();
            boolean route = sailing != null
                    && sailing.hasPort(SailingSnapshot.PORT_SARIM)
                    && sailing.hasPort(SailingSnapshot.PORT_PANDEMONIUM)
                    && sailing.hasActivity(SailingSnapshot.ACTIVITY_COURIER);
            checks.add(new RequirementCheck(
                    "sailing:courier-route",
                    Text.get(1539),
                    route ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    route
                            ? Text.get(635)
                            : Text.get(646)));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:captains-log",
                            "Captain's log", 1,
                            ItemID.SAILING_LOG_INITIAL, ItemID.SAILING_LOG),
                    useGroupStorage));
            return checks;
        }
        if ("sailing_charting".equals(id))
        {
            checks.add(new RequirementCheck(
                    "sailing:uncompleted-chart", Text.get(1540),
                    RequirementState.CHECK_NEEDED,
                    Text.get(657)));
            return checks;
        }
        if (id != null && id.startsWith("sailing_barracuda_"))
        {
            if (id.contains("jubbly"))
                checks.addAll(evaluateQuestCompletion(data,
                        Text.get(1541), "quest:zogre-flesh-eaters"));
            if (id.contains("gwenith"))
                checks.addAll(evaluateQuestCompletion(data,
                        "Regicide", "quest:regicide"));
            checks.add(new RequirementCheck(
                    "preparation:sailing-trial-boat", "Trial-ready boat",
                    RequirementState.CHECK_NEEDED,
                    Text.get(658)));
            return checks;
        }
        checks.add(new RequirementCheck(
                "sailing:live-route", Text.get(659),
                RequirementState.CHECK_NEEDED,
                Text.get(660)));
        return checks;
    }

    private RequirementCheck resource(GameData data,
            boolean useGroupStorage, String id, String label, int quantity,
            int itemId)
    {
        return resourceReadinessService.evaluate(data,
                new ResourceRequirement("resource:" + id, label, quantity,
                        itemId), useGroupStorage);
    }

    private static RequirementCheck splashingEquipmentCheck(
            GameData data)
    {
        ItemsState equipment = data == null ? null : data.equipment();
        boolean verified = equipment != null && hasSplashingSet(equipment);
        return new RequirementCheck("equipment:f2p_splashing",
                Text.get(661),
                verified ? RequirementState.VERIFIED
                        : RequirementState.CHECK_NEEDED,
                verified
                        ? Text.get(614)
                        : Text.get(615));
    }

    private static boolean hasSplashingSet(ItemsState equipment)
    {
        boolean helm = false;
        boolean body = false;
        boolean legs = false;
        boolean shield = false;
        boolean boots = false;
        boolean staff = false;
        for (ItemState item : equipment.getEquippedItems())
        {
            if (item == null || item.getName() == null
                    || item.getQuantity() <= 0) continue;
            String name = item.getName().toLowerCase(java.util.Locale.ROOT);
            helm |= isMetal(name, "full helm");
            body |= isMetal(name, "platebody");
            legs |= isMetal(name, "platelegs") || isMetal(name, "plateskirt");
            shield |= isMetal(name, "kiteshield");
            boots |= name.equals("fancy boots") || name.equals("fighting boots")
                    || name.equals("decorative boots");
            staff |= name.equals(Text.get(1542));
        }
        return helm && body && legs && shield && boots && staff;
    }

    private static boolean isMetal(String name, String piece)
    {
        if (name == null || !name.endsWith(piece)) return false;
        return name.startsWith("bronze ") || name.startsWith("iron ")
                || name.startsWith("steel ") || name.startsWith("black ")
                || name.startsWith("mithril ") || name.startsWith("adamant ")
                || name.startsWith("rune ") || name.startsWith("gilded ");
    }

    private List<RequirementCheck> evaluateCookedFish(
            GameData data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        int level = data == null || data.account() == null ? 1
                : data.account().getSkillLevel(Skill.COOKING);
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
                        "resource:raw_fish", Text.get(1543), 1,
                        legalIds),
                useGroupStorage));
        return checks;
    }

    private List<RequirementCheck> evaluateMembersCooking(
            GameData data, String methodId,
            boolean useGroupStorage)
    {
        if ("cooking_wines".equals(methodId))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:wine_grapes", "Grapes",
                            1, ItemID.GRAPES), useGroupStorage));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:wine_water", "Jug of water",
                            1, ItemID.JUG_WATER), useGroupStorage));
            return checks;
        }

        List<RequirementCheck> checks = evaluateCookedFish(data, useGroupStorage);
        DiarySnapshot diaries = data == null ? null : data.diaries();
        boolean kitchen = diaries != null
                && diaries.isTierComplete("Kourend & Kebos", DiaryTier.EASY);
        checks.add(new RequirementCheck(
                "access:hosidius_kitchen", Text.get(1544),
                kitchen ? RequirementState.VERIFIED : RequirementState.CHECK_NEEDED,
                kitchen
                        ? Text.get(616)
                        : Text.get(617)));
        return checks;
    }

    private List<RequirementCheck> evaluateFishing(
            GameData data, TrainingMethod method,
            boolean useGroupStorage)
    {
        if ("fishing_lumbridge_shrimps".equals(method.getId()))
        {
            // The tutor beside this exact spot replaces a lost net, so lack of
            // an observed net does not block the first executable trip.
            return new ArrayList<>();
        }
        if ("fishing_tempoross".equals(method.getId()))
        {
            // Membership and the method's 35 Fishing level gate are the only
            // access requirements. Required tools are available at the cove.
            return new ArrayList<>();
        }
        if ("fishing_karambwan".equals(method.getId()))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            QuestSnapshot quests = data == null ? null : data.quests();
            boolean questComplete = quests != null
                    && quests.statusOf(Text.get(1545))
                            == QuestStatus.COMPLETE;
            checks.add(new RequirementCheck(
                    "quest:tai_bwo_wannai_trio",
                    Text.get(1546),
                    questComplete ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    questComplete
                            ? Text.get(618)
                            : Text.get(619)));
            TransportSnapshot transport = data == null ? null
                    : data.transport();
            boolean observedFairyRoute = transport != null
                    && transport.hasVerifiedRoute("fairy-rings");
            boolean fairyQuestComplete = quests != null
                    && quests.statusOf(Text.get(1547))
                            == QuestStatus.COMPLETE;
            boolean fairyRings = observedFairyRoute || fairyQuestComplete;
            checks.add(new RequirementCheck(
                    "transport:fairy-rings", Text.get(1548),
                    fairyRings ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    fairyRings
                            ? Text.get(620)
                            : Text.get(621)));
            DiarySnapshot diaries = data == null ? null : data.diaries();
            boolean staffless = diaries != null
                    && diaries.isTierComplete(Text.get(1152),
                            DiaryTier.ELITE);
            int staff = resourceReadinessService.observedQuantity(data,
                    useGroupStorage, ItemID.DRAMEN_STAFF,
                    ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF);
            boolean staffReady = observedFairyRoute || staffless || staff > 0;
            checks.add(new RequirementCheck(
                    "resource:fairy_ring_staff",
                    Text.get(622),
                    staffReady ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    staffReady
                            ? Text.get(623)
                            : Text.get(625)));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:karambwan_vessel",
                            "Karambwan vessel", 1,
                            ItemID.TBWT_KARAMBWAN_VESSEL,
                            ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI),
                    useGroupStorage));
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement("resource:karambwanji",
                            Text.get(1549), 1,
                            ItemID.TBWT_RAW_KARAMBWANJI,
                            ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI),
                    useGroupStorage));
            return checks;
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
            GameData data, TrainingMethod method,
            boolean useGroupStorage)
    {
        if ("hunter_falconry".equals(method.getId()))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            checks.add(resourceReadinessService.evaluate(data,
                    new ResourceRequirement(
                            "resource:falcon_rental", Text.get(1550),
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
        if ("hunter_herbiboar".equals(method.getId()))
        {
            List<RequirementCheck> checks = new ArrayList<>();
            AccountSnapshot account = data == null ? null : data.account();
            int herblore = account == null ? 1
                    : account.getSkillLevel(Skill.HERBLORE);
            checks.add(new RequirementCheck(
                    "skill:herbiboar_herblore", "31 Herblore",
                    herblore >= 31 ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED,
                    Text.get(1551) + herblore + "."));
            QuestSnapshot quests = data == null ? null : data.quests();
            boolean boneVoyage = quests != null
                    && quests.statusOf("Bone Voyage") == QuestStatus.COMPLETE;
            checks.add(new RequirementCheck(
                    "quest:bone_voyage", Text.get(1552),
                    boneVoyage ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    boneVoyage
                            ? Text.get(626)
                            : Text.get(627)));
            return checks;
        }
        return null;
    }

    private static List<RequirementCheck> evaluateQuestCompletion(
            GameData data, String questName, String id)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        QuestSnapshot quests = data == null ? null : data.quests();
        boolean complete = quests != null
                && quests.statusOf(questName) == QuestStatus.COMPLETE;
        checks.add(new RequirementCheck(
                id, questName + " completed",
                complete ? RequirementState.VERIFIED
                        : RequirementState.CHECK_NEEDED,
                complete
                        ? questName + Text.get(628)
                        : questName + Text.get(1553)));
        return checks;
    }

    private List<RequirementCheck> evaluateCrudeChairs(
            GameData data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        PohSnapshot poh = data == null ? null : data.poh();
        CapabilityState house = poh == null
                ? CapabilityState.UNKNOWN : poh.getHouseAccess();
        CapabilityState parlour = poh == null
                ? CapabilityState.UNKNOWN : poh.furnitureState("room:parlour");
        checks.add(capabilityCheck("construction:poh", Text.get(1554),
                house, Text.get(629)));
        checks.add(capabilityCheck("construction:parlour", "POH Parlour",
                parlour, Text.get(630)));
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
            GameData data, boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        PohSnapshot poh = data == null ? null : data.poh();
        CapabilityState house = poh == null
                ? CapabilityState.UNKNOWN : poh.getHouseAccess();
        CapabilityState kitchen = poh == null
                ? CapabilityState.UNKNOWN : poh.furnitureState("room:kitchen");
        checks.add(capabilityCheck("construction:poh", Text.get(1554),
                house, Text.get(631)));
        checks.add(capabilityCheck("construction:kitchen", "POH Kitchen",
                kitchen, Text.get(632)));
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
                        ? label + Text.get(1555)
                        : unknownEvidence);
    }

    private List<RequirementCheck> evaluateTool(
            GameData data,
            TrainingMethod method,
            ItemRequirementClass itemClass,
            boolean useGroupStorage,
            String requirementId,
            String label)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        checks.add(usableToolCheck(data, itemClass, useGroupStorage,
                requirementId, label));
        for (String requirement : method.getRequirements())
        {
            checks.add(generic(requirement));
        }
        return checks;
    }

    private static RequirementCheck usableToolCheck(
            GameData data,
            ItemRequirementClass itemClass,
            boolean useGroupStorage,
            String requirementId,
            String label)
    {
        ItemIndex items = new ItemIndex(data, useGroupStorage);
        int usable = items.quantityMatching(itemClass,
                java.util.Collections.emptyList());
        return new RequirementCheck(
                requirementId,
                label,
                usable > 0 ? RequirementState.VERIFIED
                        : RequirementState.CHECK_NEEDED,
                usable > 0
                        ? label + Text.get(633)
                        : "No " + label.toLowerCase()
                                + Text.get(634));
    }

    /**
     * Conventional F2P altar routes are resource-driven. The player does not
     * need to manually confirm them once Compass has observed the essence and
     * the matching talisman/tiara in equipment, inventory, bank, or safe
     * account-specific storage.
     */
    private List<RequirementCheck> evaluateRunecraft(
            GameData data,
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
            GameData data,
            TrainingMethod method)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        if ("agility_wilderness".equals(method.getId()))
        {
            checks.add(agilityAccessEvaluator.wildernessCourseCheck(data));
            checks.add(new RequirementCheck(
                    "agility:wilderness_risk",
                    Text.get(1556),
                    RequirementState.VERIFIED,
                    Text.get(636)
            ));
            return checks;
        }

        AgilityCourseDefinition course =
                agilityAccessEvaluator.bestStandardCourse(data);
        checks.add(agilityAccessEvaluator.courseCheck(data, course));
        return checks;
    }

    private List<RequirementCheck> evaluateFarming(
            GameData data,
            TrainingMethod method,
            boolean useGroupStorage)
    {
        List<RequirementCheck> checks = new ArrayList<>();
        AccountSnapshot account = data == null ? null : data.account();
        FarmingSnapshot farming = data == null ? null : data.farming();
        int level = account == null ? 1 : account.getSkillLevel(Skill.FARMING);

        if ("farming_early".equals(method.getId()))
        {
            String patch = farmingAccessEvaluator.firstReachablePatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:reachable_patch",
                    Text.get(1557),
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? Text.get(637)
                            : patch + Text.get(638)
            ));
            checks.add(new RequirementCheck(
                    "farming:supplies",
                    Text.get(1558),
                    RequirementState.CHECK_NEEDED,
                    Text.get(639)
            ));
            return checks;
        }

        if ("farming_falador_potatoes".equals(method.getId())
                || "farming_falador_watermelons".equals(method.getId()))
        {
            boolean reachable = farming != null
                    && farming.isPatchReachable("falador");
            checks.add(new RequirementCheck(
                    "farming:falador_patch", Text.get(1559),
                    reachable ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    reachable
                            ? Text.get(640)
                            : Text.get(641)));
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
                        Text.get(642),
                        RequirementState.BLOCKED,
                        Text.get(643)));
            }
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.rake(),
                    "rake", Text.get(644),
                    useGroupStorage));
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.dibber(),
                    "dibber", Text.get(645),
                    useGroupStorage));
            checks.add(toolCheck(data, farming, farmingSupplyCatalog.spade(),
                    "spade", Text.get(647),
                    useGroupStorage));
            return checks;
        }

        if ("farming_tithe".equals(method.getId()))
        {
            // Membership and the method's 34 Farming level gate are the only
            // access requirements. Everything below is ordinary preparation.
            List<RequirementCheck> tithe = new ArrayList<>();
            tithe.add(resourceReadinessService.evaluate(data,
                    farmingSupplyCatalog.spade(), useGroupStorage));
            tithe.add(resourceReadinessService.evaluate(data,
                    farmingSupplyCatalog.dibber(), useGroupStorage));
            int cans = resourceReadinessService.observedQuantity(data,
                    useGroupStorage, ItemID.WATERING_CAN_1,
                    ItemID.WATERING_CAN_2, ItemID.WATERING_CAN_3,
                    ItemID.WATERING_CAN_4, ItemID.WATERING_CAN_5,
                    ItemID.WATERING_CAN_6, ItemID.WATERING_CAN_7,
                    ItemID.WATERING_CAN_8);
            int gricoller = resourceReadinessService.observedQuantity(data,
                    useGroupStorage, ItemID.ZEAH_WATERINGCAN);
            boolean waterReady = cans >= 8 || gricoller > 0;
            tithe.add(new RequirementCheck(
                    "resource:tithe_watering", Text.get(648),
                    waterReady ? RequirementState.VERIFIED
                            : RequirementState.CHECK_NEEDED,
                    waterReady
                            ? Text.get(649)
                            : Text.get(650)));
            return tithe;
        }

        if ("farming_herbs".equals(method.getId())
                || "farming_herbs_expanded".equals(method.getId()))
        {
            checks.add(new RequirementCheck(
                    "farming:level_9",
                    "9 Farming",
                    level >= 9
                            ? RequirementState.VERIFIED
                            : RequirementState.BLOCKED,
                    Text.get(1560) + level + "."
            ));

            String patch = farmingAccessEvaluator.firstReachableHerbPatchName(farming);
            checks.add(new RequirementCheck(
                    "farming:herb_patch",
                    Text.get(1561),
                    patch == null
                            ? RequirementState.CHECK_NEEDED
                            : RequirementState.VERIFIED,
                    patch == null
                            ? Text.get(651)
                            : patch + Text.get(652)
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
                    Text.get(653),
                    useGroupStorage
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.dibber(),
                    "dibber",
                    Text.get(654),
                    useGroupStorage
            ));
            checks.add(toolCheck(
                    data,
                    farming,
                    farmingSupplyCatalog.spade(),
                    "spade",
                    Text.get(655),
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
            GameData data,
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
                Text.get(656)
        );
    }
}

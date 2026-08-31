package compass;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

/**
 * Indexed sourced method strategy. Shared methods stay shared; material
 * account differences are represented before ranking.
 */
@Singleton
public final class MethodStrategyKnowledgeCatalog
{
    private static final Set<AccountMode> ALL_KNOWN =
            EnumSet.allOf(AccountMode.class);
    private static final Set<String> CONVENTIONAL_BANK_LOOPS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    Text.get(1863),
                    Text.get(1735), Text.get(1864),
                    Text.get(1577), "cooking_wines",
                    Text.get(1771), "mining_mlm",
                    Text.get(1636),
                    Text.get(1865),
                    Text.get(1866), Text.get(1867),
                    Text.get(1868), Text.get(1631),
                    Text.get(1869),
                    Text.get(1870), Text.get(1871),
                    "crafting_gems", "crafting_dhide", "fletching_bows",
                    Text.get(1872), Text.get(1873),
                    Text.get(1874),
                    Text.get(1579), Text.get(1875),
                    Text.get(1876), Text.get(1877),
                    Text.get(1878), Text.get(1879),
                    Text.get(1880), Text.get(1633),
                    Text.get(1634), "thieving_vyres")));

    private final Map<String, java.util.List<MethodStrategyProfile>> exact =
            new HashMap<>();
    private final Map<String, MethodStrategyProfile> generated =
            new ConcurrentHashMap<>();
    private final MethodExecutionProfileCatalog executionProfiles =
            new MethodExecutionProfileCatalog();

    public MethodStrategyKnowledgeCatalog()
    {
        for (MethodStrategyProfile profile : BundledCatalogLoader.array(
                Text.get(384),
                MethodStrategyProfile[].class))
        {
            if (profile.getMethodId() == null || profile.getTier() == null)
                throw new IllegalStateException(Text.get(385));
            addExact(profile);
        }
    }

    public MethodStrategyProfile profileFor(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode)
    {
        if (method == null || metadata == null || mode == null) return null;
        java.util.List<MethodStrategyProfile> specific = exact.get(
                method.id);
        if (specific != null)
        {
            MethodStrategyProfile selected = null;
            for (MethodStrategyProfile profile : specific)
                if (profile.supports(mode)
                        && (selected == null || profile.getTier().ordinal()
                                < selected.getTier().ordinal()))
                    selected = profile;
            return selected;
        }

        var bankLoop = CONVENTIONAL_BANK_LOOPS.contains(method.id);
        if (mode == AccountMode.UNKNOWN && bankLoop) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && (bankLoop || !metadata.isUimFriendly())) return null;

        var modes = EnumSet.copyOf(ALL_KNOWN);
        if (!metadata.isUimFriendly() || bankLoop)
            modes.remove(AccountMode.ULTIMATE_IRONMAN);
        if (!modes.contains(mode)) return null;

        var key = mode.name() + ':' + method.id;
        return generated.computeIfAbsent(key, ignored -> genericProfile(
                method, metadata, mode, bankLoop, modes,
                executionProfiles.forMethod(method.id)));
    }

    private static MethodStrategyProfile genericProfile(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode,
            boolean bankLoop, Set<AccountMode> modes,
            MethodProfile executionProfile)
    {
        StrategySourceId source = accountSkillSource(
                method.getSkill(), mode, metadata.isFreeToPlayAllowed());
        String reason = metadata.isSelfSourceFriendly() && mode.isIronLike()
                ? Text.get(386)
                : Text.get(387);
        return new MethodStrategyProfile(method.id,
                StrategyKnowledgeTier.VERIFIED_SHARED, modes,
                bankLoop ? MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                        : MethodBankingBehavior.NONE,
                typedFootprint(method, metadata, executionProfile),
                metadata.isSelfSourceFriendly() && mode.isIronLike() ? 0.55 : 0.35,
                reason, Collections.singletonList(source));
    }

    /**
     * Conservative family defaults use typed skill/input/setup properties.
     * Account-specific routes with materially different behavior stay in the
     * exact sourced records above; method names and IDs never change a
     * footprint.
     */
    private static MethodInventoryFootprint typedFootprint(
            TrainingMethod method, TrainingMethodMetadata metadata,
            MethodProfile executionProfile)
    {
        var tearsDown = method.getSetupMinutes() >= 8;
        switch (method.getSkill())
        {
            case AGILITY:
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case MAGIC:
            case SLAYER:
            case THIEVING:
                return new MethodInventoryFootprint(0, 0, 0,
                        InventoryFlow.NEUTRAL, tearsDown);
            case MINING:
            case FISHING:
            case WOODCUTTING:
                return new MethodInventoryFootprint(1, 1, 0,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            case HUNTER:
                return new MethodInventoryFootprint(3, 2, 1,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            default:
                break;
        }
        boolean consumesInputs = executionProfile != null
                && !executionProfile.getInputs().isEmpty();
        return new MethodInventoryFootprint(2, 1, 1,
                consumesInputs ? InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS
                        : InventoryFlow.NEUTRAL,
                tearsDown);
    }

    private static StrategySourceId accountSkillSource(
            net.runelite.api.Skill skill, AccountMode mode, boolean f2p)
    {
        if (skill == net.runelite.api.Skill.SAILING)
            return StrategySourceId.SAILING_TRAINING;
        if (skill == null || mode == null || !mode.isIronLike())
            return f2p ? StrategySourceId.F2P_SKILL_TRAINING
                    : StrategySourceId.GENERAL_SKILL_TRAINING;
        var prefix = mode == AccountMode.ULTIMATE_IRONMAN
                ? "UIM_" : "IRONMAN_";
        try
        {
            return StrategySourceId.valueOf(prefix + skill.name());
        }
        catch (IllegalArgumentException absentSpecializedGuide)
        {
            return mode == AccountMode.ULTIMATE_IRONMAN
                    ? StrategySourceId.UIM_SKILL_GUIDES
                    : StrategySourceId.IRONMAN_SKILL_GUIDES;
        }
    }

    private void addExact(MethodStrategyProfile profile)
    {
        exact.computeIfAbsent(profile.getMethodId(),
                ignored -> new java.util.ArrayList<>()).add(profile);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Singleton;

/** UIM-specific planning guardrails and inventory-pressure signals. */
@Singleton
public class UimStrategyModule implements StrategyModule
{
    private static final List<StorageCapability> UIM_CAPABILITIES = Arrays.asList(
            StorageCapability.TOOL_LEPRECHAUN,
            StorageCapability.STASH,
            StorageCapability.LOOTING_BAG,
            StorageCapability.POH_COSTUME_ROOM,
            StorageCapability.POH_STORAGE,
            StorageCapability.SEED_BOX,
            StorageCapability.HERB_SACK,
            StorageCapability.DEATH_STORAGE,
            StorageCapability.HESPORI_ITEM_RETRIEVAL,
            StorageCapability.ZULRAH_ITEM_RETRIEVAL,
            StorageCapability.VOLCANIC_MINE_ITEM_RETRIEVAL,
            StorageCapability.DEATHPILE
    );

    @Override
    public String getId()
    {
        return "uim";
    }

    @Override
    public List<StrategySignal> analyze(StrategyContext context)
    {
        List<StrategySignal> signals = new ArrayList<>();
        if (context == null
                || context.getAccountMode() != AccountMode.ULTIMATE_IRONMAN)
        {
            return signals;
        }

        signals.add(new StrategySignal(
                "uim:no-bank-routing",
                StrategySignalCategory.ACCOUNT_MODE,
                "UIM mode: normal bank routing is disabled and storage is capability-gated",
                0.0,
                RecommendationConfidence.VERIFIED
        ));

        StrategyDataBundle data = context.getData();
        InventorySnapshot inventory = data == null ? null : data.getInventory();
        if (inventory != null)
        {
            int occupiedStacks = inventory.getItems().size();
            if (occupiedStacks >= 24)
            {
                signals.add(new StrategySignal(
                        "uim:inventory-pressure",
                        StrategySignalCategory.STORAGE,
                        "High UIM inventory pressure: " + occupiedStacks
                                + " occupied item stacks observed",
                        3.0,
                        RecommendationConfidence.VERIFIED
                ));
            }
        }

        StorageSnapshot storage = data == null ? null : data.getStorage();
        int verified = 0;
        int unknown = 0;
        for (StorageCapability capability : UIM_CAPABILITIES)
        {
            CapabilityState state = storage == null
                    ? CapabilityState.UNKNOWN
                    : storage.stateOf(capability);
            if (state == CapabilityState.VERIFIED)
            {
                verified++;
            }
            else if (state == CapabilityState.UNKNOWN)
            {
                unknown++;
            }
        }

        signals.add(new StrategySignal(
                "uim:capability-memory",
                StrategySignalCategory.STORAGE,
                "UIM storage knowledge: " + verified + " verified, "
                        + unknown + " still unknown",
                0.0,
                unknown > 0
                        ? RecommendationConfidence.CHECK_NEEDED
                        : RecommendationConfidence.VERIFIED
        ));

        if (storage != null && hasDangerousStorage(storage))
        {
            signals.add(new StrategySignal(
                    "uim:death-storage-risk",
                    StrategySignalCategory.STORAGE,
                    "Death-based storage is known, but every use must still pass an explicit risk/precondition check",
                    0.0,
                    RecommendationConfidence.VERIFIED
            ));
        }

        return signals;
    }

    private static boolean hasDangerousStorage(StorageSnapshot storage)
    {
        for (StorageCapability capability : StorageCapability.values())
            if (UimStorageMechanics.isDangerous(capability)
                    && storage.verified(capability)) return true;
        return storage.verified(StorageCapability.DEATH_STORAGE);
    }
}

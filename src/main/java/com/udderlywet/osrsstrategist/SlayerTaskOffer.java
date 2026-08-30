package com.udderlywet.osrsstrategist;

import lombok.Getter;

/** One live Mortimer task/modifier option decoded from RuneLite game data. */
public final class SlayerTaskOffer
{
    @Getter
    private final String taskName;
    @Getter
    private final String modifierName;
    @Getter
    private final int modifierValue;
    @Getter
    private final boolean negativeModifier;

    public SlayerTaskOffer(String taskName, String modifierName,
            int modifierValue, boolean negativeModifier)
    {
        this.taskName = taskName;
        this.modifierName = modifierName;
        this.modifierValue = Math.max(0, modifierValue);
        this.negativeModifier = negativeModifier;
    }

}

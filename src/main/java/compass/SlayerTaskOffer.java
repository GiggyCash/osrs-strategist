package compass;

import lombok.Getter;

/** One live Mortimer task/modifier option decoded from RuneLite game data. */
@Getter
public final class SlayerTaskOffer
{
    private final String taskName;
    private final String modifierName;
    private final int modifierValue;
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

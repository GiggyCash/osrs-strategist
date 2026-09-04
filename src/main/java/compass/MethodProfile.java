package compass;
import lombok.*;

import java.util.*;


/**
 * Connects a curated Compass method to a deterministic RuneLite skill action.
 *
 * <p>The method catalog decides <em>which route</em> is strategically sensible.
 * This profile describes the repeatable unit inside that route so Compass can
 * convert exact XP remaining into laps, catches, logs, casts, furniture builds,
 * items processed, etc. Methods with variable XP per completion deliberately do
 * not get a profile until a trustworthy model exists.</p>
 */
@Getter
public final class MethodProfile
{
    public enum ProgressEstimateMode
    {
        /** Every successful action represented by the chosen row awards one XP value. */
        EXACT_ACTIONS,
        /** One repeated interaction can yield several differently valued outputs. */
        VARIABLE_OUTPUT_RANGE,
        /** Only XP remaining is safe to show. */
        XP_ONLY
    }

    public enum InputMode
    {
        NONE,
        ACTION_ITEM,
        RAW_ACTION_ITEM,
        LOG_FOR_BOW,
        BAR_FOR_SMITHED_ITEM,
        UNCUT_GEM,
        SAPLING_FOR_TREE,
        DART_TIP_FOR_DART,
        UNFINISHED_BOLT,
        FIXED
    }

    String methodId;
    List<String> actionTerms;
    String unitSingular;
    String unitPlural;
    double xpMultiplier;
    List<MethodInputRule> inputs;
    String note;
    ProgressEstimateMode progressEstimateMode;

    public String unit(int count)
    {
        return count == 1 ? unitSingular : unitPlural;
    }
}

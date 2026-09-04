package compass;
import static java.util.Collections.*;
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

    final String methodId;
    final List<String> actionTerms;
    final String unitSingular;
    final String unitPlural;
    final double xpMultiplier;
    final List<MethodInputRule> inputs;
    final String note;
    final ProgressEstimateMode progressEstimateMode;

    public MethodProfile(
            String methodId,
            String unitSingular,
            String unitPlural,
            double xpMultiplier,
            List<MethodInputRule> inputs,
            String note,
            String... actionTerms)
    {
        this(methodId, unitSingular, unitPlural, xpMultiplier, inputs, note,
                ProgressEstimateMode.EXACT_ACTIONS, actionTerms);
    }

    public MethodProfile(
            String methodId,
            String unitSingular,
            String unitPlural,
            double xpMultiplier,
            List<MethodInputRule> inputs,
            String note,
            ProgressEstimateMode progressEstimateMode,
            String... actionTerms)
    {
        this.methodId = methodId;
        this.actionTerms = unmodifiableList(
                new ArrayList<>(Arrays.asList(actionTerms)));
        this.unitSingular = unitSingular;
        this.unitPlural = unitPlural;
        this.xpMultiplier = xpMultiplier <= 0 ? 1.0 : xpMultiplier;
        this.inputs = unmodifiableList(
                inputs == null ? new ArrayList<>() : new ArrayList<>(inputs));
        this.note = note;
        this.progressEstimateMode = progressEstimateMode == null
                ? ProgressEstimateMode.XP_ONLY : progressEstimateMode;
    }

    public String unit(int count)
    {
        return count == 1 ? unitSingular : unitPlural;
    }
}

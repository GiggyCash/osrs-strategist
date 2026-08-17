package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;

/** Resolves profile input rules against a concrete RuneLite action. */
@Singleton
public class MethodInputResolver
{
    public List<ResolvedMethodInput> resolve(
            MethodExecutionProfile profile,
            RuneLiteSkillActionDefinition action,
            int actions)
    {
        Map<String, ResolvedMethodInput> merged = new LinkedHashMap<>();
        if (profile == null || action == null || actions <= 0)
        {
            return new ArrayList<>();
        }

        for (MethodInputRule rule : profile.getInputs())
        {
            ResolvedMethodInput input = resolveOne(rule, action, actions);
            if (input == null || input.getQuantity() <= 0) continue;
            String key = input.getItemId() > 0
                    ? "id:" + input.getItemId()
                    : "name:" + input.getName().toLowerCase(Locale.ROOT);
            ResolvedMethodInput previous = merged.get(key);
            if (previous == null)
            {
                merged.put(key, input);
            }
            else
            {
                merged.put(key, new ResolvedMethodInput(
                        previous.getName(),
                        previous.getItemId(),
                        previous.getQuantity() + input.getQuantity()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static ResolvedMethodInput resolveOne(
            MethodInputRule rule,
            RuneLiteSkillActionDefinition action,
            int actions)
    {
        if (rule == null
                || rule.getMode() == MethodExecutionProfile.InputMode.NONE)
        {
            return null;
        }

        String name;
        int itemId = -1;
        double perAction = rule.getQuantityPerAction();
        switch (rule.getMode())
        {
            case ACTION_ITEM:
                name = action.getName();
                itemId = action.getItemId();
                if (perAction <= 0) perAction = 1.0;
                break;
            case RAW_ACTION_ITEM:
                name = rawName(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case LOG_FOR_BOW:
                name = logForBow(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case BAR_FOR_SMITHED_ITEM:
                name = barForSmithing(action.getName());
                if (name == null) return null;
                if (perAction <= 0)
                {
                    perAction = normalize(action.getName()).contains("platebody")
                            ? 5.0 : 1.0;
                }
                break;
            case UNCUT_GEM:
                name = uncutGem(action.getName());
                if (perAction <= 0) perAction = 1.0;
                break;
            case SAPLING_FOR_TREE:
                name = saplingForTree(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case DART_TIP_FOR_DART:
                name = dartTipForDart(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case UNFINISHED_BOLT:
                name = unfinishedBolt(action.getName());
                if (name == null) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case FIXED:
                name = rule.getFixedName();
                if (name == null || name.trim().isEmpty()) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case NONE:
            default:
                return null;
        }

        return new ResolvedMethodInput(
                name,
                itemId,
                (int) Math.ceil(actions * perAction));
    }

    private static String rawName(String actionName)
    {
        String clean = actionName == null ? "" : actionName.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("cooked "))
            clean = clean.substring(7);
        return "Raw " + clean;
    }

    private static String logForBow(String actionName)
    {
        String clean = actionName == null ? "" : actionName
                .replace("(u)", "").trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        String[] woods = {"oak", "willow", "maple", "yew", "magic", "redwood"};
        for (String wood : woods)
        {
            if (lower.startsWith(wood + " "))
                return capitalize(wood) + " logs";
        }
        return "Logs";
    }

    private static String barForSmithing(String actionName)
    {
        String lower = normalize(actionName);
        if (lower.contains("bronze")) return "Bronze bar";
        if (lower.contains("iron")) return "Iron bar";
        if (lower.contains("steel")) return "Steel bar";
        if (lower.contains("mithril")) return "Mithril bar";
        if (lower.contains("adamant")) return "Adamantite bar";
        if (lower.contains("rune")) return "Runite bar";
        return null;
    }

    private static String uncutGem(String actionName)
    {
        String clean = actionName == null ? "gem" : actionName.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("uncut ")) return clean;
        return "Uncut " + clean.toLowerCase(Locale.ROOT);
    }

    private static String saplingForTree(String actionName)
    {
        if (actionName == null) return null;
        String clean = actionName.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (lower.equals("spirit tree")) return "Spirit seed";
        if (lower.equals("crystal tree")) return "Crystal acorn";
        if (!lower.endsWith(" tree")) return null;
        String tree = clean.substring(0, clean.length() - 5).trim();
        if (tree.isEmpty()) return null;
        return tree + " sapling";
    }

    private static String dartTipForDart(String actionName)
    {
        if (actionName == null) return null;
        String clean = actionName.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(" dart")) return null;
        return clean.substring(0, clean.length() - 5).trim() + " dart tip";
    }

    private static String unfinishedBolt(String actionName)
    {
        if (actionName == null) return null;
        String clean = actionName.trim();
        String lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(" bolts")) return null;
        return clean + " (unf)";
    }

    private static String normalize(String value)
    {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static String capitalize(String value)
    {
        if (value == null || value.isEmpty()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

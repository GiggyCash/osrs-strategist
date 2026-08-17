package com.udderlywet.osrsstrategist;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Converts deterministic curated methods into account-specific milestone work.
 *
 * <p>RuneLite supplies the maintained XP-per-action data. Strategist supplies
 * route selection, account-mode policy, observed XP modifiers, resource
 * ownership, and acquisition advice. If an action cannot be matched confidently
 * this service returns null rather than making up a number.</p>
 */
@Singleton
public class AdaptiveMilestoneGuidanceService
{
    private final RuneLiteSkillActionCatalog actionCatalog;
    private final MethodExecutionProfileCatalog profileCatalog;
    private final SkillingXpModifierService xpModifierService;

    @Inject
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog,
            SkillingXpModifierService xpModifierService)
    {
        this.actionCatalog = actionCatalog;
        this.profileCatalog = profileCatalog;
        this.xpModifierService = xpModifierService;
    }

    /** Compatibility constructor retained for focused tests/older callers. */
    public AdaptiveMilestoneGuidanceService(
            RuneLiteSkillActionCatalog actionCatalog,
            MethodExecutionProfileCatalog profileCatalog)
    {
        this(actionCatalog, profileCatalog, new SkillingXpModifierService());
    }

    /** Compatibility constructor for tests that do not have RuneLite injection. */
    public AdaptiveMilestoneGuidanceService()
    {
        this(new RuneLiteSkillActionCatalog(),
                new MethodExecutionProfileCatalog(),
                new SkillingXpModifierService());
    }

    public RecommendationGuidance build(
            StrategyDataBundle data,
            Skill skill,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan,
            boolean useGroupStorage)
    {
        if (data == null || data.getAccount() == null || skill == null
                || plan == null || plan.getMethod() == null)
        {
            return null;
        }

        MethodExecutionProfile profile = profileCatalog.forMethod(
                plan.getMethod().getId());
        if (profile == null) return null;

        RuneLiteSkillActionDefinition action = selectAction(
                actionCatalog.actionsFor(skill),
                profile,
                currentLevel,
                data.getAccount().getMembershipStatus());
        if (action == null || action.getXp() <= 0) return null;

        int currentXp = data.getAccount().getSkillExperience(skill);
        if (currentXp <= 0)
        {
            currentXp = Experience.getXpForLevel(currentLevel);
        }
        int targetXp = Experience.getXpForLevel(targetLevel);
        int xpNeeded = Math.max(0, targetXp - currentXp);

        SkillingXpModifier modifier = xpModifierService == null
                ? SkillingXpModifier.none()
                : xpModifierService.modifier(data, skill, useGroupStorage);
        double xpPerAction = action.getXp()
                * profile.getXpMultiplier()
                * modifier.getMultiplier();
        if (xpPerAction <= 0) return null;
        int actionsNeeded = divideRoundUp(xpNeeded, xpPerAction);

        String actionText = "Use " + action.getName() + " for about "
                + actionsNeeded + " " + profile.unit(actionsNeeded)
                + " to reach level " + targetLevel + ". "
                + format(xpNeeded) + " XP remains; this action gives "
                + format(xpPerAction) + " XP each in the modeled setup.";

        SupplyNeed supply = supplyNeed(profile, action, actionsNeeded);
        String supplies = supply == null
                ? null
                : supplyGuidance(
                        data,
                        data.getAccount(),
                        supply,
                        useGroupStorage);

        String location = plan.getMethod().getInstructions();
        String note = profile.getNote();
        if (note == null || note.trim().isEmpty())
        {
            note = "Action count uses RuneLite's maintained base action XP.";
        }
        else
        {
            note += " Action count uses RuneLite's maintained action XP.";
        }

        if (modifier.getMultiplier() > 1.0 && modifier.getLabel() != null)
        {
            note += " Count assumes you wear the " + modifier.getLabel() + ".";
        }
        else
        {
            note += " Any unmodeled XP bonus can reduce the remaining count.";
        }

        return new RecommendationGuidance(
                actionText,
                supplies,
                location,
                note);
    }

    RuneLiteSkillActionDefinition selectAction(
            List<RuneLiteSkillActionDefinition> actions,
            MethodExecutionProfile profile,
            int currentLevel,
            MembershipStatus membership)
    {
        RuneLiteSkillActionDefinition best = null;
        for (RuneLiteSkillActionDefinition action : actions)
        {
            if (action == null || action.getLevel() > currentLevel) continue;
            if (membership == MembershipStatus.F2P
                    && action.getMembership() == MembershipStatus.P2P)
            {
                continue;
            }
            if (!matches(action, profile.getActionTerms())) continue;

            if (best == null
                    || action.getLevel() > best.getLevel()
                    || (action.getLevel() == best.getLevel()
                    && action.getXp() > best.getXp()))
            {
                best = action;
            }
        }
        return best;
    }

    private static boolean matches(
            RuneLiteSkillActionDefinition action,
            List<String> terms)
    {
        if (terms == null || terms.isEmpty()) return false;
        String haystack = normalize(action.getId()) + " "
                + normalize(action.getName()) + " "
                + normalize(action.getCategory());
        for (String term : terms)
        {
            if (haystack.contains(normalize(term))) return true;
        }
        return false;
    }

    private static SupplyNeed supplyNeed(
            MethodExecutionProfile profile,
            RuneLiteSkillActionDefinition action,
            int actions)
    {
        MethodExecutionProfile.InputMode mode = profile.getInputMode();
        if (mode == MethodExecutionProfile.InputMode.NONE) return null;

        String name;
        int itemId = -1;
        double perAction = profile.getFixedInputPerAction();

        switch (mode)
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
            case FIXED:
                name = profile.getFixedInputName();
                if (name == null || name.trim().isEmpty()) return null;
                if (perAction <= 0) perAction = 1.0;
                break;
            case NONE:
            default:
                return null;
        }

        int quantity = (int) Math.ceil(actions * perAction);
        return new SupplyNeed(name, itemId, Math.max(0, quantity));
    }

    private static String supplyGuidance(
            StrategyDataBundle data,
            AccountSnapshot account,
            SupplyNeed need,
            boolean useGroupStorage)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        int inventory = quantityInItems(
                data.getInventory() == null ? null : data.getInventory().getItems(),
                need);

        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            int safeStorage = quantityInSafeUimStorage(data.getStorage(), need);
            int verified = inventory + safeStorage;
            int missing = Math.max(0, need.quantity - verified);
            if (missing == 0)
            {
                return "Need about " + need.quantity + " " + need.name
                        + ". You have " + verified
                        + " directly usable across inventory and verified safe UIM storage.";
            }
            return "Need about " + need.quantity + " " + need.name
                    + ". You have " + verified + " directly usable, so acquire "
                    + missing + " more just in time. Normal bank state is ignored for UIM.";
        }

        int bank = quantityInItems(
                data.getBank() == null ? null : data.getBank().getItems(), need);
        int group = 0;
        if (useGroupStorage && mode.isGroupIronman()
                && data.getGroupStorage() != null
                && data.getGroupStorage().isObserved())
        {
            group = quantityInItems(data.getGroupStorage().getItems(), need);
        }
        int verified = inventory + bank + group;
        int missing = Math.max(0, need.quantity - verified);

        if (data.getBank() == null)
        {
            return "Need about " + need.quantity + " " + need.name
                    + ". " + inventory + " is verified in inventory"
                    + (group > 0 ? " and " + group + " in Group Storage" : "")
                    + ". Open your bank once so Strategist can verify stored supply before deciding the shortfall.";
        }

        if (missing == 0)
        {
            return "Need about " + need.quantity + " " + need.name
                    + ". You have " + verified
                    + " verified across usable storage, so no extra supply is needed.";
        }

        if (mode.usesGrandExchange())
        {
            return "Need about " + need.quantity + " " + need.name
                    + ". You have " + verified + " verified, so buy "
                    + missing + " more at the Grand Exchange. Price/GP checks are a separate validation step.";
        }

        if (mode.isGroupIronman())
        {
            return "Need about " + need.quantity + " " + need.name
                    + ". You have " + verified + " verified, so source "
                    + missing + " more"
                    + (useGroupStorage ? " after checking observed Group Storage" : "")
                    + ".";
        }

        return "Need about " + need.quantity + " " + need.name
                + ". You have " + verified + " verified, so self-source "
                + missing + " more.";
    }

    private static int quantityInItems(
            Iterable<ItemStackSnapshot> items,
            SupplyNeed need)
    {
        if (items == null || need == null) return 0;
        int total = 0;
        for (ItemStackSnapshot item : items)
        {
            if (item == null) continue;
            if (need.itemId > 0 && item.getItemId() == need.itemId)
            {
                total += Math.max(0, item.getQuantity());
            }
            else if (item.getName() != null
                    && item.getName().equalsIgnoreCase(need.name))
            {
                total += Math.max(0, item.getQuantity());
            }
        }
        return total;
    }

    private static int quantityInSafeUimStorage(
            StorageSnapshot storage,
            SupplyNeed need)
    {
        if (storage == null) return 0;
        int total = 0;
        for (Map.Entry<StorageCapability, List<ItemStackSnapshot>> entry
                : storage.getObservedContents().entrySet())
        {
            StorageCapability capability = entry.getKey();
            if (!storage.verified(capability)
                    || capability == StorageCapability.LOOTING_BAG
                    || capability == StorageCapability.DEATH_STORAGE
                    || capability == StorageCapability.DEATHPILE)
            {
                continue;
            }
            total += quantityInItems(entry.getValue(), need);
        }
        return total;
    }

    private static String rawName(String actionName)
    {
        String clean = actionName == null ? "" : actionName.trim();
        if (clean.toLowerCase(Locale.ROOT).startsWith("cooked "))
        {
            clean = clean.substring(7);
        }
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
            {
                return capitalize(wood) + " logs";
            }
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

    private static int divideRoundUp(int numerator, double denominator)
    {
        if (numerator <= 0) return 0;
        return (int) Math.ceil(numerator / denominator);
    }

    private static String format(double value)
    {
        if (Math.abs(value - Math.rint(value)) < 0.001)
        {
            return String.format(Locale.ROOT, "%,d", (long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%,.1f", value);
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

    private static final class SupplyNeed
    {
        private final String name;
        private final int itemId;
        private final int quantity;

        private SupplyNeed(String name, int itemId, int quantity)
        {
            this.name = name;
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }
}

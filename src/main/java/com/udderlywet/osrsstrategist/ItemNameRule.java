package com.udderlywet.osrsstrategist;

import java.util.Locale;
import net.runelite.api.Skill;

/**
 * One safe item-name matching rule used by named resource requirements.
 *
 * <p>RuneLite gameval IDs remain preferable for one specific item, but many
 * training requirements describe a family of valid items: any log the player
 * can fletch, any usable bone, or either the normal or double ammo mould. This
 * small rule model lets Strategist prove those families from the names RuneLite
 * already observed without hardcoding dozens of numeric IDs.</p>
 *
 * <p>An optional skill gate prevents a high-tier item from proving readiness at
 * a level where the player cannot actually use it. For example, having magic
 * logs at 20 Fletching does not prove that a bow-making route is ready.</p>
 */
public final class ItemNameRule
{
    public enum MatchType
    {
        EXACT,
        PREFIX,
        SUFFIX,
        TOKEN
    }

    private final MatchType matchType;
    private final String value;
    private final Skill requiredSkill;
    private final int minimumLevel;

    private ItemNameRule(
            MatchType matchType,
            String value,
            Skill requiredSkill,
            int minimumLevel)
    {
        this.matchType = matchType;
        this.value = normalize(value);
        this.requiredSkill = requiredSkill;
        this.minimumLevel = Math.max(1, minimumLevel);
    }

    public static ItemNameRule exact(String name)
    {
        return new ItemNameRule(MatchType.EXACT, name, null, 1);
    }

    public static ItemNameRule exactAt(
            Skill skill,
            int minimumLevel,
            String name)
    {
        return new ItemNameRule(
                MatchType.EXACT, name, skill, minimumLevel);
    }

    public static ItemNameRule prefix(String prefix)
    {
        return new ItemNameRule(MatchType.PREFIX, prefix, null, 1);
    }

    public static ItemNameRule suffix(String suffix)
    {
        return new ItemNameRule(MatchType.SUFFIX, suffix, null, 1);
    }

    public static ItemNameRule token(String token)
    {
        return new ItemNameRule(MatchType.TOKEN, token, null, 1);
    }

    public boolean matches(String itemName, AccountSnapshot account)
    {
        if (!skillRequirementMet(account)) return false;
        String candidate = normalize(itemName);
        if (candidate.isEmpty() || value.isEmpty()) return false;

        switch (matchType)
        {
            case EXACT:
                return candidate.equals(value);
            case PREFIX:
                return candidate.startsWith(value);
            case SUFFIX:
                return candidate.endsWith(value);
            case TOKEN:
                for (String token : candidate.split("[^a-z0-9]+"))
                {
                    if (value.equals(token)) return true;
                }
                return false;
            default:
                return false;
        }
    }

    private boolean skillRequirementMet(AccountSnapshot account)
    {
        if (requiredSkill == null) return true;
        return account != null
                && account.getSkillLevel(requiredSkill) >= minimumLevel;
    }

    private static String normalize(String value)
    {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'')
                .replaceAll("\\s+", " ");
    }
}

package com.udderlywet.osrsstrategist;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;

/**
 * Adapts RuneLite's maintained skill-calculator action enums into Compass.
 * Reflection deliberately keeps Compass decoupled from individual enum
 * constants so RuneLite can add actions without requiring us to mirror them.
 */
@Singleton
public class RuneLiteSkillActionCatalog
{
    private static final String PACKAGE =
            "net.runelite.client.plugins.skillcalculator.skills.";

    private final ItemManager itemManager;
    private final Map<Skill, String> enumClasses = new LinkedHashMap<>();

    @Inject
    public RuneLiteSkillActionCatalog(ItemManager itemManager)
    {
        this.itemManager = itemManager;
        seedClassMap();
    }

    /** Test/diagnostic constructor. Membership remains UNKNOWN without ItemManager. */
    public RuneLiteSkillActionCatalog()
    {
        this.itemManager = null;
        seedClassMap();
    }

    public List<RuneLiteSkillActionDefinition> actionsFor(Skill skill)
    {
        String className = enumClasses.get(skill);
        if (className == null) return Collections.emptyList();
        try
        {
            Class<?> type = Class.forName(PACKAGE + className);
            Object[] constants = type.getEnumConstants();
            if (constants == null) return Collections.emptyList();

            Method getLevel = type.getMethod("getLevel");
            Method getXp = type.getMethod("getXp");
            Method getCategory = optionalMethod(type, "getCategory");
            Method getItemId = optionalMethod(type, "getItemId");
            Method getName = itemManager == null ? null
                    : optionalMethod(type, "getName", ItemManager.class);
            Method isMembers = itemManager == null ? null
                    : optionalMethod(type, "isMembers", ItemManager.class);

            List<RuneLiteSkillActionDefinition> actions = new ArrayList<>();
            for (Object constant : constants)
            {
                Enum<?> enumValue = (Enum<?>) constant;
                int level = ((Number) getLevel.invoke(constant)).intValue();
                float xp = ((Number) getXp.invoke(constant)).floatValue();
                Object rawCategory = getCategory == null
                        ? null : getCategory.invoke(constant);
                String category = rawCategory == null ? null : rawCategory.toString();
                String name = getName == null
                        ? pretty(enumValue.name())
                        : String.valueOf(getName.invoke(constant, itemManager));
                MembershipStatus membership = MembershipStatus.UNKNOWN;
                if (isMembers != null)
                {
                    boolean members = (Boolean) isMembers.invoke(constant, itemManager);
                    membership = members ? MembershipStatus.P2P : MembershipStatus.F2P;
                }
                int itemId = -1;
                if (getItemId != null)
                {
                    Object rawItemId = getItemId.invoke(constant);
                    if (rawItemId instanceof Number)
                    {
                        itemId = ((Number) rawItemId).intValue();
                    }
                }
                actions.add(new RuneLiteSkillActionDefinition(
                        skill,
                        "runelite:" + skill.name().toLowerCase(Locale.ROOT)
                                + ":" + enumValue.name().toLowerCase(Locale.ROOT),
                        name,
                        level,
                        xp,
                        category,
                        membership,
                        itemId));
            }
            return Collections.unmodifiableList(actions);
        }
        catch (ReflectiveOperationException | LinkageError ex)
        {
            // A RuneLite release may rename/remove a calculator class or core
            // action accessor. That should reduce coverage explicitly rather
            // than break the plugin.
            return Collections.emptyList();
        }
    }

    public Map<Skill, Integer> coverageCounts()
    {
        Map<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (Skill skill : enumClasses.keySet())
        {
            result.put(skill, actionsFor(skill).size());
        }
        return Collections.unmodifiableMap(result);
    }

    private void seedClassMap()
    {
        enumClasses.put(Skill.AGILITY, "AgilityAction");
        enumClasses.put(Skill.COOKING, "CookingAction");
        enumClasses.put(Skill.CONSTRUCTION, "ConstructionAction");
        enumClasses.put(Skill.CRAFTING, "CraftingAction");
        enumClasses.put(Skill.FARMING, "FarmingAction");
        enumClasses.put(Skill.FIREMAKING, "FiremakingAction");
        enumClasses.put(Skill.FISHING, "FishingAction");
        enumClasses.put(Skill.FLETCHING, "FletchingAction");
        enumClasses.put(Skill.HERBLORE, "HerbloreAction");
        enumClasses.put(Skill.HUNTER, "HunterAction");
        enumClasses.put(Skill.MAGIC, "MagicAction");
        enumClasses.put(Skill.MINING, "MiningAction");
        enumClasses.put(Skill.PRAYER, "PrayerAction");
        enumClasses.put(Skill.RUNECRAFT, "RunecraftAction");
        enumClasses.put(Skill.SMITHING, "SmithingAction");
        enumClasses.put(Skill.THIEVING, "ThievingAction");
        enumClasses.put(Skill.WOODCUTTING, "WoodcuttingAction");
    }

    private static Method optionalMethod(
            Class<?> type,
            String name,
            Class<?>... parameterTypes)
    {
        try
        {
            return type.getMethod(name, parameterTypes);
        }
        catch (NoSuchMethodException ex)
        {
            return null;
        }
    }

    private static String pretty(String value)
    {
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.skillcalculator.skills.AgilityAction;
import net.runelite.client.plugins.skillcalculator.skills.ConstructionAction;
import net.runelite.client.plugins.skillcalculator.skills.CookingAction;
import net.runelite.client.plugins.skillcalculator.skills.CraftingAction;
import net.runelite.client.plugins.skillcalculator.skills.FarmingAction;
import net.runelite.client.plugins.skillcalculator.skills.FiremakingAction;
import net.runelite.client.plugins.skillcalculator.skills.FishingAction;
import net.runelite.client.plugins.skillcalculator.skills.FletchingAction;
import net.runelite.client.plugins.skillcalculator.skills.HerbloreAction;
import net.runelite.client.plugins.skillcalculator.skills.HunterAction;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.client.plugins.skillcalculator.skills.MiningAction;
import net.runelite.client.plugins.skillcalculator.skills.PrayerAction;
import net.runelite.client.plugins.skillcalculator.skills.RunecraftAction;
import net.runelite.client.plugins.skillcalculator.skills.SkillAction;
import net.runelite.client.plugins.skillcalculator.skills.SmithingAction;
import net.runelite.client.plugins.skillcalculator.skills.ThievingAction;
import net.runelite.client.plugins.skillcalculator.skills.WoodcuttingAction;

/**
 * Adapts RuneLite's maintained skill-calculator action enums into Compass.
 * The enum types are wired explicitly because Plugin Hub review forbids Java
 * reflection; their maintained {@code values()} still provide the full catalog.
 */
@Singleton
public class RuneLiteSkillActionCatalog
{
    private final ItemManager itemManager;
    private final Map<Skill, SkillAction[]> actionsBySkill = new LinkedHashMap<>();

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
        SkillAction[] constants = actionsBySkill.get(skill);
        if (constants == null) return Collections.emptyList();
        List<RuneLiteSkillActionDefinition> actions = new ArrayList<>();
        for (SkillAction action : constants)
        {
            Enum<?> enumValue = (Enum<?>) action;
            String id = "runelite:" + skill.name().toLowerCase(Locale.ROOT)
                    + ":" + enumValue.name().toLowerCase(Locale.ROOT);
            String name = itemManager == null ? pretty(enumValue.name())
                    : action.getName(itemManager);
            MembershipStatus membership = itemManager == null
                    ? MembershipStatus.UNKNOWN
                    : action.isMembers(itemManager)
                            ? MembershipStatus.P2P : MembershipStatus.F2P;
            actions.add(new RuneLiteSkillActionDefinition(
                    skill,
                    id,
                    name,
                    CurrentLiveSkillActionOverrides.level(id, action.getLevel()),
                    CurrentLiveSkillActionOverrides.xp(id, action.getXp()),
                    null,
                    membership,
                    action.getIcon()));
        }
        return Collections.unmodifiableList(actions);
    }

    public Map<Skill, Integer> coverageCounts()
    {
        Map<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (Skill skill : actionsBySkill.keySet())
        {
            result.put(skill, actionsFor(skill).size());
        }
        return Collections.unmodifiableMap(result);
    }

    private void seedClassMap()
    {
        actionsBySkill.put(Skill.AGILITY, AgilityAction.values());
        actionsBySkill.put(Skill.COOKING, CookingAction.values());
        actionsBySkill.put(Skill.CONSTRUCTION, ConstructionAction.values());
        actionsBySkill.put(Skill.CRAFTING, CraftingAction.values());
        actionsBySkill.put(Skill.FARMING, FarmingAction.values());
        actionsBySkill.put(Skill.FIREMAKING, FiremakingAction.values());
        actionsBySkill.put(Skill.FISHING, FishingAction.values());
        actionsBySkill.put(Skill.FLETCHING, FletchingAction.values());
        actionsBySkill.put(Skill.HERBLORE, HerbloreAction.values());
        actionsBySkill.put(Skill.HUNTER, HunterAction.values());
        actionsBySkill.put(Skill.MAGIC, MagicAction.values());
        actionsBySkill.put(Skill.MINING, MiningAction.values());
        actionsBySkill.put(Skill.PRAYER, PrayerAction.values());
        actionsBySkill.put(Skill.RUNECRAFT, RunecraftAction.values());
        actionsBySkill.put(Skill.SMITHING, SmithingAction.values());
        actionsBySkill.put(Skill.THIEVING, ThievingAction.values());
        actionsBySkill.put(Skill.WOODCUTTING, WoodcuttingAction.values());
    }

    private static String pretty(String value)
    {
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

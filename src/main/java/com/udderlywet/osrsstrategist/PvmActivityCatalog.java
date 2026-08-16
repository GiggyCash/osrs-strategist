package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Singleton;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/** Uses RuneLite's current hiscore boss enum as the authoritative identity list. */
@Singleton
public class PvmActivityCatalog
{
    public List<PvmActivityDefinition> all()
    {
        List<PvmActivityDefinition> result = new ArrayList<>();
        for (HiscoreSkill skill : HiscoreSkill.values())
        {
            if (skill.getType() != HiscoreSkillType.BOSS) continue;
            String enumName = skill.name();
            boolean wilderness = isWilderness(enumName);
            boolean raid = isRaid(enumName);
            RiskLevel risk = wilderness ? RiskLevel.HIGH
                    : raid || isHighEnd(enumName) ? RiskLevel.HIGH : RiskLevel.MEDIUM;
            boolean hardcoreSafe = "TEMPOROSS".equals(enumName);
            result.add(new PvmActivityDefinition(
                    "pvm:" + enumName.toLowerCase(Locale.ROOT),
                    skill.getName(), wilderness, raid, risk, hardcoreSafe));
        }
        return Collections.unmodifiableList(result);
    }

    public PvmActivityDefinition byId(String id)
    {
        if (id == null) return null;
        for (PvmActivityDefinition definition : all())
            if (id.equals(definition.getId())) return definition;
        return null;
    }

    private static boolean isRaid(String name)
    {
        return name.startsWith("CHAMBERS_OF_XERIC")
                || name.startsWith("THEATRE_OF_BLOOD")
                || name.startsWith("TOMBS_OF_AMASCUT");
    }

    private static boolean isWilderness(String name)
    {
        switch (name)
        {
            case "ARTIO":
            case "CALLISTO":
            case "CALVARION":
            case "CHAOS_ELEMENTAL":
            case "CHAOS_FANATIC":
            case "CRAZY_ARCHAEOLOGIST":
            case "SCORPIA":
            case "SPINDEL":
            case "VENENATIS":
            case "VETION":
                return true;
            default:
                return false;
        }
    }

    private static boolean isHighEnd(String name)
    {
        return name.equals("NEX")
                || name.equals("TZKAL_ZUK")
                || name.equals("SOL_HEREDIT")
                || name.equals("PHOSANIS_NIGHTMARE")
                || name.equals("DOOM_OF_MOKHAIOTL")
                || name.equals("YAMA")
                || name.equals("THE_CORRUPTED_GAUNTLET");
    }
}

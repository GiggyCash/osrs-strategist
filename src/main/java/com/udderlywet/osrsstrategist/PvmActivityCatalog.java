package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/** Uses RuneLite's current hiscore boss enum as the authoritative identity list. */
@Singleton
public class PvmActivityCatalog
{
    private static final Set<String> PROFILED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "pvm:brutus", "pvm:obor", "pvm:bryophyta",
                    "pvm:barrows_chests", "pvm:scurrius", "pvm:giant_mole",
                    "pvm:sarachnis", "pvm:hespori", "pvm:zulrah", "pvm:vorkath",
                    "pvm:the_gauntlet", "pvm:the_corrupted_gauntlet",
                    "pvm:tombs_of_amascut", "pvm:chambers_of_xeric",
                    "pvm:theatre_of_blood", "pvm:alchemical_hydra",
                    "pvm:cerberus", "pvm:araxxor", "pvm:kraken",
                    "pvm:tztok_jad")));

    public List<PvmActivityDefinition> all()
    {
        List<PvmActivityDefinition> result = new ArrayList<>();
        for (HiscoreSkill skill : HiscoreSkill.values())
        {
            if (skill.getType() != HiscoreSkillType.BOSS) continue;
            String enumName = skill.name();
            boolean wilderness = isWilderness(enumName);
            boolean raid = isRaid(enumName);
            boolean f2p = isFreeToPlay(enumName);
            RiskLevel risk = wilderness ? RiskLevel.HIGH
                    : raid || isHighEnd(enumName) ? RiskLevel.HIGH : RiskLevel.MEDIUM;

            // Hardcore is deny-by-default for bossing. Non-lethal skilling-boss
            // style encounters can be whitelisted as we verify their mechanics.
            boolean hardcoreSafe = "TEMPOROSS".equals(enumName);
            result.add(new PvmActivityDefinition(
                    "pvm:" + enumName.toLowerCase(Locale.ROOT),
                    skill.getName(), wilderness, raid, f2p, risk, hardcoreSafe));
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

    public PvmActivityDefinition match(String rawKey)
    {
        if (rawKey == null) return null;
        String normalized = normalize(rawKey);
        for (PvmActivityDefinition definition : all())
        {
            if (normalize(definition.getId()).equals(normalized)
                    || normalize(definition.getName()).equals(normalized))
                return definition;
        }
        return null;
    }

    /** Encounters with a curated floor beyond the generic fail-closed profile. */
    public boolean hasCuratedReadinessProfile(String id)
    {
        return id != null && PROFILED.contains(id.toLowerCase(Locale.ROOT));
    }

    public int curatedReadinessProfileCount() { return PROFILED.size(); }

    private static String normalize(String value)
    {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("pvm:", "")
                .replace("'", "")
                .replace(":", "")
                .replace("-", "_")
                .replace(" ", "_");
        while (normalized.contains("__")) normalized = normalized.replace("__", "_");
        return normalized;
    }

    private static boolean isRaid(String name)
    {
        return name.startsWith("CHAMBERS_OF_XERIC")
                || name.startsWith("THEATRE_OF_BLOOD")
                || name.startsWith("TOMBS_OF_AMASCUT");
    }

    private static boolean isFreeToPlay(String name)
    {
        return "OBOR".equals(name)
                || "BRYOPHYTA".equals(name)
                || "BRUTUS".equals(name);
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

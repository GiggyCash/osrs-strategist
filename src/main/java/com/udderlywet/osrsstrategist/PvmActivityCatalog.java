package com.udderlywet.osrsstrategist;

import java.util.*;
import javax.inject.Singleton;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;

/** RuneLite boss identities enriched by the audited bundled safety catalog. */
@Singleton
public class PvmActivityCatalog
{
    private static final Set<String> PROFILED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "pvm:brutus", "pvm:obor", "pvm:bryophyta", "pvm:barrows_chests",
                    "pvm:scurrius", "pvm:giant_mole", "pvm:sarachnis", "pvm:hespori",
                    "pvm:zulrah", "pvm:vorkath", "pvm:the_gauntlet",
                    "pvm:the_corrupted_gauntlet", "pvm:tombs_of_amascut",
                    "pvm:chambers_of_xeric", "pvm:theatre_of_blood",
                    "pvm:alchemical_hydra", "pvm:cerberus", "pvm:araxxor", "pvm:kraken",
                    "pvm:tztok_jad", "pvm:tzkal_zuk", "pvm:sol_heredit", "pvm:nex",
                    "pvm:commander_zilyana", "pvm:general_graardor", "pvm:kreearra",
                    "pvm:kril_tsutsaroth", "pvm:duke_sucellus", "pvm:the_leviathan",
                    "pvm:vardorvis", "pvm:the_whisperer",
                    "pvm:chambers_of_xeric_challenge_mode",
                    "pvm:theatre_of_blood_hard_mode", "pvm:tombs_of_amascut_expert")));
    private final List<PvmActivityDefinition> activities;
    private final Map<String, PvmActivityDefinition> byId;

    public PvmActivityCatalog()
    {
        Map<String, PvmActivityDefinition> values = new LinkedHashMap<>();
        for (PvmActivityDefinition value : BundledCatalogLoader.array(
                "/content/catalogs/pvm-activities.json", PvmActivityDefinition[].class))
            if (value.getId() == null || values.put(value.getId(), value) != null)
                throw new IllegalStateException(Text.get(1199));
        var bosses = 0;
        for (HiscoreSkill skill : HiscoreSkill.values())
            if (skill.getType() == HiscoreSkillType.BOSS)
            {
                bosses++;
                var id = "pvm:" + skill.name().toLowerCase(Locale.ROOT);
                if (!values.containsKey(id))
                    throw new IllegalStateException(Text.get(1200) + id);
            }
        if (values.size() != bosses)
            throw new IllegalStateException(Text.get(1201));
        byId = Collections.unmodifiableMap(values);
        activities = Collections.unmodifiableList(new ArrayList<>(values.values()));
    }

    public List<PvmActivityDefinition> all() { return activities; }
    public PvmActivityDefinition byId(String id) { return byId.get(id); }
    public PvmActivityDefinition match(String raw)
    {
        if (raw == null) return null;
        var key = normalize(raw);
        for (PvmActivityDefinition value : activities)
            if (normalize(value.getId()).equals(key)
                    || normalize(value.getName()).equals(key)) return value;
        return null;
    }
    public boolean hasCuratedReadinessProfile(String id)
    {
        return id != null && PROFILED.contains(id.toLowerCase(Locale.ROOT));
    }
    public int curatedReadinessProfileCount() { return PROFILED.size(); }
    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT).replace("pvm:", "")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}

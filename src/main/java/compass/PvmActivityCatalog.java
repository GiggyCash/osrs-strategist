package compass;

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
                    "pvm:brutus", "pvm:obor", "pvm:bryophyta", Text.get(1815),
                    "pvm:scurrius", "pvm:giant_mole", "pvm:sarachnis", "pvm:hespori",
                    "pvm:zulrah", "pvm:vorkath", Text.get(1816),
                    Text.get(1817), Text.get(1818),
                    Text.get(1819), Text.get(1820),
                    Text.get(1821), "pvm:cerberus", "pvm:araxxor", "pvm:kraken",
                    "pvm:tztok_jad", "pvm:tzkal_zuk", "pvm:sol_heredit", "pvm:nex",
                    Text.get(1822), Text.get(1823), "pvm:kreearra",
                    Text.get(1824), Text.get(1825), Text.get(1826),
                    "pvm:vardorvis", Text.get(1827),
                    Text.get(1828),
                    Text.get(1829), Text.get(1830))));
    private final List<PvmActivityDefinition> activities;
    private final Map<String, PvmActivityDefinition> byId;

    public PvmActivityCatalog()
    {
        Map<String, PvmActivityDefinition> values = new LinkedHashMap<>();
        for (PvmActivityDefinition value : BundledCatalogLoader.array(
                Text.get(1831), PvmActivityDefinition[].class))
            if (value.id == null || values.put(value.id, value) != null)
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
            if (normalize(value.id).equals(key)
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

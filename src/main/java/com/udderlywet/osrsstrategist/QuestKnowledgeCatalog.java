package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Curated quest knowledge plus fail-closed authoritative enrichment. */
@Singleton
public class QuestKnowledgeCatalog
{
    private static final String RESOURCE = PlayerText.get("QKC1");
    private static final Pattern REWARD_XP = Pattern.compile(
            "\\{\\{SCP\\|([^|}]+)\\|([0-9,]+)", Pattern.CASE_INSENSITIVE);
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();

    public QuestKnowledgeCatalog()
    {
        for (QuestDefinition definition
                : BundledCatalogLoader.array(RESOURCE, QuestDefinition[].class))
            add(definition);
        seedImportedRequirements();
    }

    private void seedImportedRequirements()
    {
        AuthoritativeQuestEnrichmentCatalog enrichment =
                new AuthoritativeQuestEnrichmentCatalog();
        for (AuthoritativeQuestRequirementCatalog.Record record
                : new AuthoritativeQuestRequirementCatalog().all().values())
        {
            if (definitionFor(record.getName()) != null) continue;
            AuthoritativeQuestEnrichmentCatalog.Record details =
                    enrichment.recordFor(record.getName());
            List<String> checks = new ArrayList<>(record.getOtherChecks());
            List<String> uncertainties = new ArrayList<>();
            if (details == null)
                uncertainties.addAll(Arrays.asList("items", "access/combat",
                        "rewards/unlocks", "start location"));
            else
            {
                addEvidenceCheck(checks, "Required items", details.getItems());
                addEvidenceCheck(checks, "Access requirements", details.getRequirements());
                addEvidenceCheck(checks, "Combat encounters", details.getEnemies());
                if (!details.hasItemEvidence()) uncertainties.add("items");
                if (!details.hasRequirementEvidence() || !details.hasCombatEvidence())
                    uncertainties.add("access/combat");
                if (!details.hasRewardEvidence()) uncertainties.add("rewards/unlocks");
                else if (hasUnparsedCombatXp(details.getRewards(), rewardXp(details.getRewards())))
                    uncertainties.add("irreversible xp");
            }
            String start = record.getStartLocation();
            if (start.trim().isEmpty() && details != null && details.hasStartEvidence())
                start = plain(details.getStart());
            if (start.trim().isEmpty() && !uncertainties.contains("start location"))
                uncertainties.add("start location");
            List<String> unlocks = details != null && details.hasRewardEvidence()
                    ? Collections.singletonList("Quest rewards: "
                            + abbreviate(plain(details.getRewards()), 500))
                    : Collections.emptyList();
            add(new QuestDefinition(record.getName(),
                    QuestMembershipPolicy.isFreeToPlayQuest(record.getName()),
                    record.getPrerequisites(), record.getSkills(), Collections.emptyList(), null,
                    record.getQuestPoints(), checks, start, unlocks,
                    details == null ? Collections.emptyMap() : rewardXp(details.getRewards()),
                    uncertainties));
        }
    }

    private static void addEvidenceCheck(List<String> checks, String label, String value)
    {
        if (value == null || value.trim().isEmpty() || "none".equalsIgnoreCase(value.trim()))
            return;
        checks.add(label + ": " + abbreviate(plain(value), 500));
    }

    private static Map<Skill, Integer> rewardXp(String rewards)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        Matcher matcher = REWARD_XP.matcher(rewards == null ? "" : rewards);
        while (matcher.find())
        {
            Skill skill;
            try
            {
                skill = Skill.valueOf(matcher.group(1).trim()
                        .toUpperCase(Locale.ROOT).replace(' ', '_'));
            }
            catch (IllegalArgumentException ex) { continue; }
            result.merge(skill, Integer.parseInt(matcher.group(2).replace(",", "")), Integer::sum);
        }
        return result;
    }

    private static boolean hasUnparsedCombatXp(String rewards, Map<Skill, Integer> parsed)
    {
        String text = plain(rewards).toLowerCase(Locale.ROOT);
        for (Skill skill : Arrays.asList(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
                Skill.HITPOINTS, Skill.PRAYER, Skill.RANGED, Skill.MAGIC))
            if (text.matches("(?s).*\\b" + skill.getName().toLowerCase(Locale.ROOT)
                    + "\\b.*\\bexperience\\b.*") && !parsed.containsKey(skill))
                return true;
        return false;
    }

    private static String plain(String wiki)
    {
        if (wiki == null) return "";
        return wiki.replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll("\\[\\[(?:[^]|]+\\|)?([^]]+)]]", "$1")
                .replaceAll("\\{\\{SCP\\|([^|}]+)\\|?([^}]*)}}", "$2 $1")
                .replaceAll("\\{\\{[^}]+}}", " ").replaceAll("'{2,}", "")
                .replaceAll("[\\r\\n*#]+", " ").replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String abbreviate(String value, int length)
    {
        return value.length() <= length ? value : value.substring(0, length - 1).trim() + "…";
    }

    public QuestDefinition definitionFor(String name) { return definitions.get(normalize(name)); }
    public Map<String, QuestDefinition> all() { return Collections.unmodifiableMap(definitions); }

    private void add(QuestDefinition definition)
    {
        if (definition == null || definition.getName() == null)
            throw new IllegalStateException("Incomplete quest definition in " + RESOURCE);
        String id = normalize(definition.getName());
        if (definitions.put(id, definition) != null)
            throw new IllegalStateException("Duplicate quest identity: " + definition.getName());
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}

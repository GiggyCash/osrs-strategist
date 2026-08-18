package com.udderlywet.osrsstrategist;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** Small verified quest corpus. Unknown quests remain fail-closed and partial. */
@Singleton
public class QuestKnowledgeCatalog
{
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();

    public QuestKnowledgeCatalog()
    {
        add(new QuestDefinition("Rune Mysteries", true,
                Collections.emptyList(), skills(), Collections.emptyList(), 0,
                Collections.emptyList(),
                "Talk to Duke Horacio on the first floor of Lumbridge Castle.",
                Arrays.asList("Rune essence mine access",
                        "Runecraft lamp and book-of-knowledge access"), skills()));

        add(new QuestDefinition("Druidic Ritual", false,
                Collections.emptyList(), skills(), Arrays.asList(
                item("Raw bear meat", 1), item("Raw rat meat", 1),
                item("Raw beef", 1), item("Raw chicken", 1)), 0,
                Collections.emptyList(),
                "Talk to Kaqemeex at the stone circle north of Taverley.",
                Collections.singletonList("Herblore progression"),
                skills(Skill.HERBLORE, 250)));

        add(new QuestDefinition("Bone Voyage", false,
                Collections.singletonList("The Dig Site"), skills(), Arrays.asList(
                item("Vodka", 2), item("Marrentill potion (unf)", 1)), 0,
                Collections.singletonList("Verify at least 100 Museum Kudos"),
                "Talk to Curator Haig Halen in Varrock Museum.",
                Collections.singletonList("Fossil Island access"), skills()));

        add(new QuestDefinition("Dragon Slayer I", true,
                Collections.emptyList(), skills(), Collections.emptyList(), 32,
                Collections.singletonList(
                        "Verify the combat setup and dragonfire-protection route"),
                "Talk to the Guildmaster in the Champions' Guild.",
                Arrays.asList("Crandor access", "Corsair Cove Resource Area access",
                        "Rune platebody and green d'hide body equipment progression"),
                skills(Skill.STRENGTH, 18_650, Skill.DEFENCE, 18_650)));
    }

    public QuestDefinition definitionFor(String name)
    {
        return definitions.get(normalize(name));
    }

    public Map<String, QuestDefinition> all()
    {
        return Collections.unmodifiableMap(definitions);
    }

    private void add(QuestDefinition definition)
    {
        definitions.put(normalize(definition.getName()), definition);
    }

    private static QuestDefinition.QuestItemRequirement item(String name, int quantity)
    {
        return new QuestDefinition.QuestItemRequirement(name, quantity);
    }

    private static Map<Skill, Integer> skills(Object... values)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (int i = 0; i + 1 < values.length; i += 2)
            result.put((Skill) values[i], (Integer) values[i + 1]);
        return result;
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replaceAll("[^a-z0-9]+", " ").trim();
    }
}

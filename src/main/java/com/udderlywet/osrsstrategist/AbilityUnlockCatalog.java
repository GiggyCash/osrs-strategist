package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/** High-value current prayer, spellbook, and spell unlocks. */
@Singleton
public final class AbilityUnlockCatalog
{
    public static final String PROVENANCE =
            "Maintained current-live quest and ability evidence; audited 2026-08-25";
    private final Map<String, AbilityUnlockDefinition> definitions =
            new LinkedHashMap<>();

    public AbilityUnlockCatalog()
    {
        add("ancient-magicks", "Ancient Magicks", GoalNodeKind.SPELLBOOK,
                "Desert Treasure I", null, 0, null, 0, null, null,
                "Verify the Ancient spellbook is currently selected before relying on Ancient spells");
        add("lunar-spellbook", "Lunar spellbook", GoalNodeKind.SPELLBOOK,
                "Lunar Diplomacy", null, 0, null, 0, null, null,
                "Verify the Lunar spellbook is currently selected before relying on Lunar spells");
        add("arceuus-spellbook", "Arceuus spellbook", GoalNodeKind.SPELLBOOK,
                null, null, 0, null, 0, null, null,
                "Verify access to a current spellbook-switching route and that Arceuus is selected");
        add("chivalry", "Chivalry", GoalNodeKind.PRAYER,
                "King's Ransom", Skill.PRAYER, 60, Skill.DEFENCE, 65,
                null, null, "Complete the Knight Waves training and verify the prayer unlock");
        add("piety", "Piety", GoalNodeKind.PRAYER,
                "King's Ransom", Skill.PRAYER, 70, Skill.DEFENCE, 70,
                null, null, "Complete the Knight Waves training and verify the prayer unlock");
        add("preserve", "Preserve", GoalNodeKind.PRAYER,
                null, Skill.PRAYER, 55, null, 0, "Torn prayer scroll",
                "pvm:chambers_of_xeric",
                "Read the scroll and verify the prayer is unlocked");
        add("rigour", "Rigour", GoalNodeKind.PRAYER,
                null, Skill.PRAYER, 74, null, 0, "Dexterous prayer scroll",
                "pvm:chambers_of_xeric",
                "Read the scroll and verify the prayer is unlocked");
        add("augury", "Augury", GoalNodeKind.PRAYER,
                null, Skill.PRAYER, 77, null, 0, "Arcane prayer scroll",
                "pvm:chambers_of_xeric",
                "Read the scroll and verify the prayer is unlocked");
        add("iban-blast", "Iban Blast", GoalNodeKind.SPELL,
                "Underground Pass", Skill.MAGIC, 50, null, 0,
                "Iban's staff", null,
                "Verify the staff is charged and the current spellbook/runes support the cast");
        add("ice-barrage", "Ice Barrage", GoalNodeKind.SPELL,
                "Desert Treasure I", Skill.MAGIC, 94, null, 0, null, null,
                "Select Ancient Magicks and verify the exact carried rune combination before use");
        add("barrows-teleport", "Barrows Teleport", GoalNodeKind.SPELL,
                null, Skill.MAGIC, 83, null, 0, null, null,
                "Select the Arceuus spellbook and verify exact carried runes before relying on the route");
    }

    public AbilityUnlockDefinition get(String id)
    {
        return id == null ? null : definitions.get(normalize(id));
    }

    public List<AbilityUnlockDefinition> all()
    {
        return Collections.unmodifiableList(
                new ArrayList<>(definitions.values()));
    }

    private void add(String id, String name, GoalNodeKind kind, String quest,
            Skill skill, int level, Skill secondarySkill, int secondaryLevel,
            String item, String encounter, String accessCheck)
    {
        AbilityUnlockDefinition definition = new AbilityUnlockDefinition(id,
                name, kind, quest, skill, level, secondarySkill,
                secondaryLevel, item, encounter, accessCheck);
        if (definitions.put(normalize(id), definition) != null)
            throw new IllegalStateException("Duplicate ability unlock " + id);
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}

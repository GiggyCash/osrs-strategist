package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Static practical-readiness envelope for a boss, raid, or combat challenge. */
public final class PvmEncounterDefinition
{
    private final String id;
    private final int attack;
    private final int strength;
    private final int defence;
    private final int ranged;
    private final int magic;
    private final int prayer;
    private final int hitpoints;
    private final List<String> quests;
    private final boolean wilderness;
    private final boolean highDeathRisk;
    private final String note;

    public PvmEncounterDefinition(String id, int attack, int strength, int defence,
            int ranged, int magic, int prayer, int hitpoints, List<String> quests,
            boolean wilderness, boolean highDeathRisk, String note)
    {
        this.id = id;
        this.attack = attack;
        this.strength = strength;
        this.defence = defence;
        this.ranged = ranged;
        this.magic = magic;
        this.prayer = prayer;
        this.hitpoints = hitpoints;
        this.quests = Collections.unmodifiableList(new ArrayList<>(quests));
        this.wilderness = wilderness;
        this.highDeathRisk = highDeathRisk;
        this.note = note;
    }

    public String getId() { return id; }
    public int getAttack() { return attack; }
    public int getStrength() { return strength; }
    public int getDefence() { return defence; }
    public int getRanged() { return ranged; }
    public int getMagic() { return magic; }
    public int getPrayer() { return prayer; }
    public int getHitpoints() { return hitpoints; }
    public List<String> getQuests() { return quests; }
    public boolean isWilderness() { return wilderness; }
    public boolean isHighDeathRisk() { return highDeathRisk; }
    public String getNote() { return note; }
}

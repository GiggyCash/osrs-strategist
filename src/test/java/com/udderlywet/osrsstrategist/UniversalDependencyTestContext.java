package com.udderlywet.osrsstrategist;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

final class UniversalDependencyTestContext
{
    private UniversalDependencyTestContext() { }

    static StrategyContext p2p(int level, boolean wilderness)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, level);
            xp.put(skill, 0);
        }
        AccountSnapshot account = new AccountSnapshot("Dependency", 77L, 1,
                "IRONMAN", MembershipStatus.P2P, 1,
                level * Skill.values().length, 0L, levels, xp);
        StrategyDataBundle data = StrategyDataBundle.builder(account)
                .inventory(new InventorySnapshot(Collections.emptyList()))
                .equipment(new EquipmentSnapshot(Collections.emptyList()))
                .bank(new BankSnapshot(Collections.emptyList(), 1L))
                .quests(new QuestSnapshot(Collections.emptyMap()))
                .build();
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.LONG_SESSION, QuestTolerance.NORMAL,
                GoalType.DIARY_CAPE, false, false, wilderness,
                new PreferenceProfile());
    }
}

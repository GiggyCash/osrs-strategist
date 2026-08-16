package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Skill;

/**
 * Produces conservative baseline PvM readiness from live stats and quest state.
 * Gear/supply-specific confidence is deliberately left CHECK_NEEDED unless a
 * future equipment evaluator proves the actual loadout.
 */
@Singleton
public class PvmReadinessAnalyzer
{
    private final PvmEncounterCatalog catalog;
    private final CurrentBossSupplementCatalog supplement;

    @Inject
    public PvmReadinessAnalyzer(
            PvmEncounterCatalog catalog,
            CurrentBossSupplementCatalog supplement)
    {
        this.catalog = catalog;
        this.supplement = supplement;
    }

    /** Compatibility constructor retained for unit tests and callers. */
    public PvmReadinessAnalyzer(PvmEncounterCatalog catalog)
    {
        this(catalog, new CurrentBossSupplementCatalog());
    }

    public PvmSnapshot analyze(AccountSnapshot account, QuestSnapshot quests)
    {
        if (account == null) return PvmSnapshot.unknown();
        Map<String, PvmReadiness> result = new LinkedHashMap<>();
        analyzeAll(result, catalog.all(), account, quests);
        analyzeAll(result, supplement.all(), account, quests);
        return new PvmSnapshot(result);
    }

    private void analyzeAll(Map<String, PvmReadiness> result,
            List<PvmEncounterDefinition> encounters,
            AccountSnapshot account,
            QuestSnapshot quests)
    {
        AccountMode mode = AccountMode.fromTypeCode(account.getAccountTypeCode());
        for (PvmEncounterDefinition encounter : encounters)
        {
            List<String> missing = new ArrayList<>();
            req(missing, account, Skill.ATTACK, encounter.getAttack());
            req(missing, account, Skill.STRENGTH, encounter.getStrength());
            req(missing, account, Skill.DEFENCE, encounter.getDefence());
            req(missing, account, Skill.RANGED, encounter.getRanged());
            req(missing, account, Skill.MAGIC, encounter.getMagic());
            req(missing, account, Skill.PRAYER, encounter.getPrayer());
            req(missing, account, Skill.HITPOINTS, encounter.getHitpoints());

            for (String quest : encounter.getQuests())
            {
                if (quests == null)
                {
                    missing.add("Quest state not observed: " + quest);
                }
                else if (!quests.isComplete(quest))
                {
                    missing.add("Quest: " + quest);
                }
            }

            if (encounter.isWilderness())
            {
                missing.add("Wilderness permission/risk plan");
            }

            boolean hardcore = mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN;
            if (hardcore && encounter.isHighDeathRisk())
            {
                missing.add("Hardcore safety approval and encounter-specific survival plan");
            }

            if (missing.isEmpty())
            {
                missing.add("Verify practical gear, supplies and encounter mechanics");
            }

            result.put(encounter.getId(), new PvmReadiness(
                    encounter.getId(),
                    false,
                    RecommendationConfidence.CHECK_NEEDED,
                    missing));
        }
    }

    private static void req(List<String> missing, AccountSnapshot account,
            Skill skill, int required)
    {
        if (required <= 1) return;
        int current = account.getSkillLevel(skill);
        if (current < required)
        {
            missing.add(skill.getName() + " " + required + " (current " + current + ")");
        }
    }
}

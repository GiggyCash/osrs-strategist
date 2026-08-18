package com.udderlywet.osrsstrategist;

import net.runelite.api.Skill;

/**
 * Hard guardrails for player-imposed account builds.
 *
 * <p>This layer answers a different question from Ironman/Main/UIM policy. A
 * Hardcore Ironman can also be a 1 Defence pure, a Main can be a level-3
 * skiller, and a GIM can be a Defence pure. Both policy layers must agree before
 * Compass recommends an action.</p>
 */
public final class AccountBuildPolicy
{
    private AccountBuildPolicy() {}

    public static RestrictedBuildSuggestion detect(AccountSnapshot account)
    {
        return new RestrictedBuildDetector().suggest(account);
    }

    /**
     * Returns the build that is safe to enforce automatically. Ambiguous
     * CHECK_NEEDED suggestions remain STANDARD until the player confirms them in
     * a future explicit build setting.
     */
    public static RestrictedBuildType effectiveBuild(AccountSnapshot account)
    {
        RestrictedBuildSuggestion suggestion = detect(account);
        if (suggestion.getConfidence() != RecommendationConfidence.VERIFIED)
        {
            return RestrictedBuildType.STANDARD;
        }
        return suggestion.getType();
    }

    public static boolean allowsSkill(AccountSnapshot account, Skill skill)
    {
        if (account == null || skill == null) return true;
        RestrictedBuildType build = effectiveBuild(account);

        switch (build)
        {
            case SKILLER:
            case F2P_SKILLER:
                return !isCombatProgressionSkill(skill);

            case PRAYER_SKILLER:
                return skill == Skill.PRAYER || !isCombatProgressionSkill(skill);

            case ONE_DEFENCE_PURE:
                return skill != Skill.DEFENCE;

            case LOW_DEFENCE_PURE:
                return skill != Skill.DEFENCE
                        || account.getSkillLevel(Skill.DEFENCE) < 13;

            case INITIATE_PURE:
                return skill != Skill.DEFENCE
                        || account.getSkillLevel(Skill.DEFENCE) < 20;

            case RUNE_PURE:
                return skill != Skill.DEFENCE
                        || account.getSkillLevel(Skill.DEFENCE) < 40;

            case VOID_PURE:
                return skill != Skill.DEFENCE
                        || account.getSkillLevel(Skill.DEFENCE) < 42;

            case ZERKER:
                return skill != Skill.DEFENCE
                        || account.getSkillLevel(Skill.DEFENCE) < 45;

            case OBSIDIAN_MAULER:
                return skill != Skill.ATTACK && skill != Skill.DEFENCE;

            case DEFENCE_PURE:
                // Defence-pure combat progression is deliberately limited to
                // Defence and Prayer. Hitpoints rises incidentally from legal
                // Defence training and is never a direct recommendation anyway.
                return skill != Skill.ATTACK
                        && skill != Skill.STRENGTH
                        && skill != Skill.RANGED
                        && skill != Skill.MAGIC
                        && skill != Skill.SLAYER;

            case TEN_HITPOINTS:
                // Attack/Strength/Defence/Ranged training normally creates HP
                // experience. Magic can remain legal through splashing,
                // alching, teleports, curses, enchanting, etc., so method-level
                // policy decides which Magic methods are safe.
                return skill != Skill.ATTACK
                        && skill != Skill.STRENGTH
                        && skill != Skill.DEFENCE
                        && skill != Skill.RANGED
                        && skill != Skill.HITPOINTS
                        && skill != Skill.SLAYER;

            case COMBAT_ONLY:
                return isCombatProgressionSkill(skill);

            case RANGE_TANK:
            case MED_BUILD:
            case STANDARD:
            default:
                return true;
        }
    }

    public static boolean allowsMethod(
            AccountSnapshot account,
            TrainingMethod method)
    {
        if (account == null || method == null) return true;
        if (!allowsSkill(account, method.getSkill())) return false;

        RestrictedBuildType build = effectiveBuild(account);
        String id = method.getId() == null ? "" : method.getId().toLowerCase();

        if (build == RestrictedBuildType.TEN_HITPOINTS)
        {
            if (method.getSkill() == Skill.MAGIC)
            {
                // Only known non-damaging/zero-HP Magic routes are allowed by
                // default. Combat, Slayer, bursting, and barraging are blocked.
                return id.contains("alch")
                        || id.contains("curse")
                        || id.contains("teleport")
                        || id.contains("enchant")
                        || id.contains("utility");
            }
            return !isHpGeneratingCombatSkill(method.getSkill());
        }

        if ((build == RestrictedBuildType.SKILLER
                || build == RestrictedBuildType.F2P_SKILLER
                || build == RestrictedBuildType.PRAYER_SKILLER)
                && isHpGeneratingCombatSkill(method.getSkill()))
        {
            return false;
        }

        return true;
    }

    /**
     * Prevents broad gear ladders from suggesting armour that violates a known
     * Defence cap. Exact item-level requirement data can later replace these
     * conservative tier checks.
     */
    public static boolean allowsGearEntry(
            AccountSnapshot account,
            GearProgressionEntry entry)
    {
        if (account == null || entry == null) return true;
        RestrictedBuildType build = effectiveBuild(account);
        String id = entry.getId() == null ? "" : entry.getId().toLowerCase();

        switch (build)
        {
            case SKILLER:
            case F2P_SKILLER:
            case PRAYER_SKILLER:
            case TEN_HITPOINTS:
            case COMBAT_ONLY:
                // Combat-only can use gear normally. The fall-through is split
                // below to keep the restricted non-aggressor builds explicit.
                return build == RestrictedBuildType.COMBAT_ONLY;

            case ONE_DEFENCE_PURE:
            case OBSIDIAN_MAULER:
                return id.contains("1def") || id.contains("pure");

            case LOW_DEFENCE_PURE:
                return id.contains("low-def") || id.contains("pure");

            case INITIATE_PURE:
                return id.contains("20def") || id.contains("pure");

            case RUNE_PURE:
                return id.contains("40def") || id.contains("rune-pure")
                        || id.contains("pure");

            case VOID_PURE:
                return id.contains("42def") || id.contains("void-pure")
                        || id.contains("pure");

            case ZERKER:
                return id.contains("45def") || id.contains("zerker")
                        || id.contains("pure");

            case DEFENCE_PURE:
                return id.contains("defence-pure") || id.contains("tank");

            case RANGE_TANK:
                return entry.getStyle() == CombatStyle.RANGED
                        || entry.getStyle() == CombatStyle.MAGIC
                        || id.contains("tank");

            case MED_BUILD:
            case STANDARD:
            default:
                return true;
        }
    }

    public static String label(AccountSnapshot account)
    {
        return pretty(effectiveBuild(account));
    }

    private static boolean isCombatProgressionSkill(Skill skill)
    {
        switch (skill)
        {
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case PRAYER:
            case MAGIC:
            case SLAYER:
                return true;
            default:
                return false;
        }
    }

    private static boolean isHpGeneratingCombatSkill(Skill skill)
    {
        return skill == Skill.ATTACK
                || skill == Skill.STRENGTH
                || skill == Skill.DEFENCE
                || skill == Skill.RANGED
                || skill == Skill.MAGIC
                || skill == Skill.SLAYER;
    }

    private static String pretty(RestrictedBuildType type)
    {
        String text = type.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

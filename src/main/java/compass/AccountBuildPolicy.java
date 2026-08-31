package compass;
import static compass.Text.get;

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
        if (account == null)
            return build(RestrictedBuildType.STANDARD,
                    Confidence.CHECK_NEEDED, get(1206));

        var attack = account.level(Skill.ATTACK);
        var strength = account.level(Skill.STRENGTH);
        var defence = account.level(Skill.DEFENCE);
        var ranged = account.level(Skill.RANGED);
        var prayer = account.level(Skill.PRAYER);
        var magic = account.level(Skill.MAGIC);
        var hp = account.level(Skill.HITPOINTS);
        var highNonCombat = nonCombatExtreme(account, true);
        var lowNonCombat = nonCombatExtreme(account, false);
        int offence = Math.max(Math.max(attack, strength),
                Math.max(ranged, magic));
        var combat = Math.max(Math.max(offence, defence), prayer);
        boolean baselineOffence = attack <= 1 && strength <= 1
                && ranged <= 1 && magic <= 1;

        if (baselineOffence && defence <= 1 && prayer <= 1 && hp <= 10
                && highNonCombat >= 20)
            return verified(account.membership() != MembershipStatus.P2P
                    ? RestrictedBuildType.F2P_SKILLER
                    : RestrictedBuildType.SKILLER, get(583));
        if (baselineOffence && defence <= 1 && hp <= 10 && prayer >= 15
                && highNonCombat >= 20)
            return verified(RestrictedBuildType.PRAYER_SKILLER, get(590));
        if (lowNonCombat <= 1 && highNonCombat <= 1 && combat >= 40)
            return verified(RestrictedBuildType.COMBAT_ONLY, get(591));
        if (baselineOffence && defence >= 20)
            return verified(RestrictedBuildType.DEFENCE_PURE, get(592));
        if (hp <= 10 && (ranged >= 20 || magic >= 20 || prayer >= 20
                || highNonCombat >= 50))
            return verified(RestrictedBuildType.TEN_HITPOINTS, get(593));
        if (attack <= 1 && defence <= 1 && strength >= 50)
            return verified(RestrictedBuildType.OBSIDIAN_MAULER, get(594));
        if (defence <= 1 && offence >= 40)
            return verified(RestrictedBuildType.ONE_DEFENCE_PURE, get(595));

        if (offence >= 50)
        {
            if (defence >= 2 && defence <= 13)
                return verified(RestrictedBuildType.LOW_DEFENCE_PURE,
                        get(596));
            if (defence <= 20)
                return verified(RestrictedBuildType.INITIATE_PURE, get(597));
            if (defence >= 39 && defence <= 40)
                return verified(RestrictedBuildType.RUNE_PURE, get(584));
            if (defence <= 42 && defence >= 41)
                return verified(RestrictedBuildType.VOID_PURE, get(585));
            if (defence >= 43 && defence <= 45 && attack >= 50 && strength >= 50)
                return verified(RestrictedBuildType.ZERKER, get(586));
        }
        if (defence >= 70 && ranged >= 80 && magic >= 70
                && attack <= 60 && strength <= 70)
            return build(RestrictedBuildType.RANGE_TANK,
                    Confidence.CHECK_NEEDED, get(587));
        if (lowNonCombat <= 1 && highNonCombat <= 5 && combat >= 30)
            return build(RestrictedBuildType.COMBAT_ONLY,
                    Confidence.CHECK_NEEDED, get(588));
        return verified(RestrictedBuildType.STANDARD, get(589));
    }

    private static RestrictedBuildSuggestion verified(
            RestrictedBuildType type, String evidence)
    {
        return build(type, Confidence.VERIFIED, evidence);
    }

    private static RestrictedBuildSuggestion build(RestrictedBuildType type,
            Confidence confidence, String evidence)
    {
        return new RestrictedBuildSuggestion(type, confidence, evidence);
    }

    private static int nonCombatExtreme(AccountSnapshot account,
            boolean highest)
    {
        var value = highest ? 1 : Integer.MAX_VALUE;
        for (Skill skill : Skill.values())
            if (!isCombatProgressionSkill(skill))
                value = highest ? Math.max(value, account.level(skill))
                        : Math.min(value, account.level(skill));
        return value == Integer.MAX_VALUE ? 1 : value;
    }

    /**
     * Returns the build that is safe to enforce automatically. Ambiguous
     * CHECK_NEEDED suggestions remain STANDARD until the player confirms them in
     * a future explicit build setting.
     */
    public static RestrictedBuildType effectiveBuild(AccountSnapshot account)
    {
        var suggestion = detect(account);
        if (suggestion.getConfidence() != Confidence.VERIFIED)
        {
            return RestrictedBuildType.STANDARD;
        }
        return suggestion.getType();
    }

    public static boolean allowsSkill(AccountSnapshot account, Skill skill)
    {
        if (account == null || skill == null) return true;
        var build = effectiveBuild(account);

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
                        || account.level(Skill.DEFENCE) < 13;

            case INITIATE_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 20;

            case RUNE_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 40;

            case VOID_PURE:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 42;

            case ZERKER:
                return skill != Skill.DEFENCE
                        || account.level(Skill.DEFENCE) < 45;

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

        var build = effectiveBuild(account);
        var id = method.id == null ? "" : method.id.toLowerCase();

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
        var build = effectiveBuild(account);
        var id = entry.id == null ? "" : entry.id.toLowerCase();

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
        var text = type.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

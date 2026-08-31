package compass;

import java.util.Set;

/** Fail-closed quest safety for irreversible restricted-build XP. */
public final class RestrictedQuestPolicy
{
    private static final Set<String> ONE_DEFENCE = PolicyLists.normalizedSet(
            PolicyLists.DATA.one_defence_safe);
    private static final Set<String> LEVEL_THREE = PolicyLists.normalizedSet(
            PolicyLists.DATA.level_three_safe);
    private static final Set<String> PRAYER_EXTRA = PolicyLists.normalizedSet(
            PolicyLists.DATA.prayer_skiller_extra);

    private RestrictedQuestPolicy() {}

    public static boolean isSafe(AccountSnapshot account, String questName)
    {
        if (account == null || questName == null) return false;
        var build = AccountBuildPolicy.effectiveBuild(account);
        switch (build)
        {
            case STANDARD:
            case RANGE_TANK:
            case MED_BUILD:
            case COMBAT_ONLY:
                return true;
            case SKILLER:
            case F2P_SKILLER:
                return LEVEL_THREE.contains(PolicyLists.normalize(questName));
            case PRAYER_SKILLER:
            case DEFENCE_PURE:
            case TEN_HITPOINTS:
                return safeForPrayerOnly(questName);
            case ONE_DEFENCE_PURE:
            case LOW_DEFENCE_PURE:
            case INITIATE_PURE:
            case RUNE_PURE:
            case VOID_PURE:
            case ZERKER:
            case OBSIDIAN_MAULER:
                return ONE_DEFENCE.contains(PolicyLists.normalize(questName));
            default:
                return false;
        }
    }

    private static boolean safeForPrayerOnly(String quest)
    {
        var key = PolicyLists.normalize(quest);
        return LEVEL_THREE.contains(key) || PRAYER_EXTRA.contains(key);
    }
}

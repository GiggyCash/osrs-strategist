package compass;

import java.util.*;
import javax.inject.Singleton;

/**
 * Current standard Slayer-master requirements and point economics.
 *
 * <p>Sources: https://oldschool.runescape.wiki/w/Slayer_Master and
 * https://oldschool.runescape.wiki/w/Slayer_training (verified 2026-08-28).
 * Diary point boosts are intentionally not assumed without live diary state.</p>
 */
@Singleton
public class SlayerMasterCatalog
{
    private final List<SlayerMasterProfile> profiles =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    "/content/catalogs/slayer-masters.json",
                    SlayerMasterProfile[].class)));

    public List<SlayerMasterProfile> all()
    {
        return profiles;
    }

    public SlayerMasterProfile byId(String id)
    {
        var key = Names.words(id);
        for (SlayerMasterProfile profile : profiles)
            if (Names.words(profile.getId()).equals(key)) return profile;
        return null;
    }

    public SlayerMasterProfile match(String name)
    {
        var key = Names.words(name);
        if (key.isEmpty()) return null;
        for (SlayerMasterProfile profile : profiles)
        {
            if (Names.words(profile.getId()).equals(key)) return profile;
            for (String alias : profile.getNames())
                if (Names.words(alias).equals(key)) return profile;
        }
        return null;
    }

    public List<SlayerMasterProfile> eligible(StrategyContext context)
    {
        if (context == null || context.data() == null
                || context.data().account() == null) return Collections.emptyList();
        var account = context.data().account();
        if (account.membership() != MembershipStatus.P2P)
            return Collections.emptyList();
        var combat = SlayerGuidanceService.combatLevel(account);
        var slayer = account.level(net.runelite.api.Skill.SLAYER);
        var quests = context.data().quests();
        List<SlayerMasterProfile> result = new ArrayList<>();
        for (SlayerMasterProfile profile : profiles)
        {
            // Spria has Turael's zero-point pool plus Sourhogs but cannot
            // replace another master's task. Without a proximity goal Turael's
            // replacement flexibility strictly dominates her for a new task.
            if ("spria".equals(profile.getId())) continue;
            if (profile.isWilderness() && !context.allowsWilderness()) continue;
            if ("mortimer".equals(profile.getId()))
            {
                var live = context.data().slayer();
                boolean capeIntroduction = slayer >= 99 && live != null
                        && Boolean.TRUE.equals(live.isMortimerIntroduced());
                if (!capeIntroduction && (combat < profile.getMinimumCombat()
                        || slayer < profile.getMinimumSlayer())) continue;
            }
            else if (combat < profile.getMinimumCombat()
                    || slayer < profile.getMinimumSlayer()) continue;
            if (profile.getRequiredQuest() != null
                    && !questRequirementMet(profile, quests)) continue;
            result.add(profile);
        }
        return result;
    }

    private static boolean questRequirementMet(SlayerMasterProfile profile,
            QuestSnapshot quests)
    {
        if (quests == null) return false;
        var status = quests.statusOf(profile.getRequiredQuest());
        return status == QuestStatus.COMPLETE
                || profile.isQuestStartSufficient()
                && status == QuestStatus.IN_PROGRESS;
    }

}

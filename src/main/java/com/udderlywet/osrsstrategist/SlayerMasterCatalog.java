package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final List<SlayerMasterProfile> profiles = Collections.unmodifiableList(
            Arrays.asList(
                    master("turael", names("Turael/Aya", "Turael", "Aya"),
                            "Burthorpe", 0, 1, null, 0, 40,
                            .20, .15, .15, .05, false),
                    master("mazchna", names("Mazchna/Achtryn", "Mazchna", "Achtryn"),
                            "Canifis", 20, 1, "Priest in Peril", 6, 50,
                            .32, .25, .28, .08, false),
                    master("vannaka", names("Vannaka"), "Edgeville Dungeon",
                            40, 1, null, 8, 60, .45, .32, .30, .08, false),
                    master("chaeldar", names("Chaeldar"), "Zanaris",
                            70, 1, "Lost City", 10, 70,
                            .66, .48, .48, .10, false),
                    master("konar", names("Konar quo Maten", "Konar"),
                            "Mount Karuulm", 75, 1, null, 18, 80,
                            .76, .78, .62, 1.0, false),
                    master("nieve", names("Nieve/Steve", "Nieve", "Steve"),
                            "Tree Gnome Stronghold", 85, 1, null, 12, 90,
                            .88, .58, .42, .08, false),
                    master("duradel", names("Duradel/Kuradal", "Duradel", "Kuradal"),
                            "Shilo Village", 100, 50, "Shilo Village", 15, 100,
                            1.0, .66, .58, .08, false),
                    master("krystilia", names("Krystilia"), "Edgeville",
                            1, 1, null, 25, 100,
                            .72, .86, .48, 1.0, true),
                    specialMaster("spria", names("Spria"), "Draynor Village",
                            0, 1, "A Porcine of Interest", false,
                            0, 30, 40, .20, .15, .15, .08, false),
                    specialMaster("mortimer", names("Mortimer"),
                            "Wyrmscraig Cavern", 100, 70,
                            "Fallen From Grace", true,
                            0, 100, 120, .96, .90, .55, .45, false)
            ));

    public List<SlayerMasterProfile> all()
    {
        return profiles;
    }

    public SlayerMasterProfile byId(String id)
    {
        String key = normalize(id);
        for (SlayerMasterProfile profile : profiles)
            if (normalize(profile.getId()).equals(key)) return profile;
        return null;
    }

    public SlayerMasterProfile match(String name)
    {
        String key = normalize(name);
        if (key.isEmpty()) return null;
        for (SlayerMasterProfile profile : profiles)
        {
            if (normalize(profile.getId()).equals(key)) return profile;
            for (String alias : profile.getNames())
                if (normalize(alias).equals(key)) return profile;
        }
        return null;
    }

    public List<SlayerMasterProfile> eligible(StrategyContext context)
    {
        if (context == null || context.getData() == null
                || context.getData().getAccount() == null) return Collections.emptyList();
        AccountSnapshot account = context.getData().getAccount();
        if (account.getMembershipStatus() != MembershipStatus.P2P)
            return Collections.emptyList();
        int combat = SlayerGuidanceService.combatLevel(account);
        int slayer = account.getSkillLevel(net.runelite.api.Skill.SLAYER);
        QuestSnapshot quests = context.getData().getQuests();
        List<SlayerMasterProfile> result = new ArrayList<>();
        for (SlayerMasterProfile profile : profiles)
        {
            // Spria has Turael's zero-point pool plus Sourhogs but cannot
            // replace another master's task. Without a proximity goal Turael's
            // replacement flexibility strictly dominates her for a new task.
            if ("spria".equals(profile.getId())) continue;
            if (profile.isWilderness() && !context.isAllowWildernessMethods()) continue;
            if ("mortimer".equals(profile.getId()))
            {
                SlayerSnapshot live = context.getData().getSlayer();
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

    private static SlayerMasterProfile master(String id, List<String> names,
            String location, int combat, int slayer, String quest,
            int points, int blockCost, double xp, double supplies,
            double setup, double constraint, boolean wilderness)
    {
        return specialMaster(id, names, location, combat, slayer, quest,
                false, points, SlayerPointEconomy.SKIP_COST, blockCost,
                xp, supplies, setup, constraint, wilderness);
    }

    private static SlayerMasterProfile specialMaster(String id,
            List<String> names, String location, int combat, int slayer,
            String quest, boolean questStartSuffices, int points,
            int cancelCost, int blockCost, double xp, double supplies,
            double setup, double constraint, boolean wilderness)
    {
        return new SlayerMasterProfile(id, names, location, combat, slayer,
                quest, questStartSuffices, points, cancelCost, blockCost, xp,
                supplies, setup, constraint, wilderness);
    }

    private static boolean questRequirementMet(SlayerMasterProfile profile,
            QuestSnapshot quests)
    {
        if (quests == null) return false;
        QuestStatus status = quests.statusOf(profile.getRequiredQuest());
        return status == QuestStatus.COMPLETE
                || profile.isQuestStartSufficient()
                && status == QuestStatus.IN_PROGRESS;
    }

    private static List<String> names(String... values)
    {
        return Arrays.asList(values);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }
}

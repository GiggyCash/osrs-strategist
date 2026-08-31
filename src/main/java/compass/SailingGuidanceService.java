package compass;
import static compass.Text.get;

import javax.inject.Singleton;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Sailing-specific progression planner.
 *
 * <p>Sailing contains one-off charting XP, variable port tasks, salvaging, and
 * fixed-completion Barracuda Trials. Only the fixed trial completion XP is
 * reduced to an exact repeat count. Other routes receive concrete unlock/tool
 * guidance instead of fake precision.</p>
 */
@Singleton
public class SailingGuidanceService
{
    public Guidance build(
            GameData data,
            int currentLevel,
            int targetLevel,
            TrainingPlan plan)
    {
        if (data == null || data.account() == null
                || plan == null || plan.method() == null)
        {
            return null;
        }
        var account = data.account();
        if (account.membership() != MembershipStatus.P2P)
        {
            return null;
        }

        String id = plan.method().getId() == null
                ? "" : plan.method().getId();
        var currentXp = account.xp(Skill.SAILING);
        if (currentXp <= 0) currentXp = Experience.getXpForLevel(currentLevel);
        var targetXp = Experience.getXpForLevel(targetLevel);
        var xpNeeded = Math.max(0, targetXp - currentXp);

        if (id.startsWith("sailing_barracuda_"))
        {
            return barracudaGuidance(data, id, targetLevel, xpNeeded);
        }
        if ("sailing_salvage_small".equals(id))
        {
            return salvageGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if ("sailing_courier".equals(id))
        {
            return courierGuidance(data, currentLevel, targetLevel, xpNeeded);
        }
        if ("sailing_deep_sea_trawling".equals(id))
        {
            return new Guidance(
                    get(1370) + format(xpNeeded)
                            + get(1371) + targetLevel + ".",
                    get(732),
                    get(743),
                    get(754)
            );
        }

        return chartingGuidance(data, currentLevel, targetLevel, xpNeeded);
    }

    private static Guidance barracudaGuidance(
            GameData data,
            String methodId,
            int targetLevel,
            int xpNeeded)
    {
        Trial trial;
        if (methodId.contains("gwenith"))
        {
            trial = new Trial(
                    "The Gwenith Glide",
                    16050,
                    get(765),
                    get(776));
        }
        else if (methodId.contains("jubbly"))
        {
            trial = new Trial(
                    "The Jubbly Jive",
                    6200,
                    get(787),
                    get(788));
        }
        else
        {
            trial = new Trial(
                    get(1372),
                    1250,
                    get(789),
                    get(790));
        }

        var marlinCompletions = divideRoundUp(xpNeeded, trial.marlinXp);
        var action = get(733)
                + trial.name + get(734)
                + marlinCompletions + get(1373)
                + (marlinCompletions == 1 ? "" : "s")
                + " at " + format(trial.marlinXp)
                + get(1374) + format(xpNeeded)
                + " XP to level " + targetLevel + ".";

        var supplies = trial.requirements;
        var note = get(735);
        return new Guidance(
                action, supplies, trial.location, note);
    }

    private static Guidance salvageGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var action = get(736)
                + format(xpNeeded) + " XP toward level " + targetLevel + ".";
        var supplies = get(737);
        var where = get(738);
        var note = get(739);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance courierGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var action = get(740)
                + format(xpNeeded) + get(1375) + targetLevel + ".";
        var supplies = get(741);
        var where = get(742);
        var note = get(744);
        return new Guidance(action, supplies, where, note);
    }

    private static Guidance chartingGuidance(
            GameData data,
            int currentLevel,
            int targetLevel,
            int xpNeeded)
    {
        var quests = data.quests();
        if (!complete(quests, "Pandemonium"))
        {
            return new Guidance(
                    get(745),
                    get(746),
                    get(747),
                    get(748)
            );
        }

        var action = new StringBuilder();
        action.append(get(749))
                .append(format(xpNeeded)).append(get(1375))
                .append(targetLevel).append(".");

        var supplies = new StringBuilder(get(750));
        if (currentLevel >= 12 && !complete(quests, "Prying Times"))
        {
            supplies.append(get(751));
        }
        if (currentLevel >= 22 && !complete(quests, "Current Affairs"))
        {
            supplies.append(get(752));
        }
        if (currentLevel >= 15)
        {
            var economy = data.economy();
            if (economy != null
                    && economy.getConfidence() == Confidence.VERIFIED)
            {
                if (economy.getCoins() >= 15000)
                {
                    supplies.append(get(753));
                }
                else
                {
                    supplies.append(" You are ")
                            .append(format(15000 - economy.getCoins()))
                            .append(get(755));
                }
            }
            else
            {
                supplies.append(get(756));
            }
        }

        var where = get(757);
        var note = get(758);
        return new Guidance(
                action.toString(), supplies.toString(), where, note);
    }

    private static boolean complete(QuestSnapshot quests, String quest)
    {
        return quests != null && quests.statusOf(quest) == QuestStatus.COMPLETE;
    }

    private static int divideRoundUp(int numerator, int denominator)
    {
        if (numerator <= 0) return 0;
        return (numerator + denominator - 1) / denominator;
    }

    private static String format(long value)
    {
        return String.format("%,d", value);
    }

    private static final class Trial
    {
        private final String name;
        private final int marlinXp;
        private final String requirements;
        private final String location;

        private Trial(String name, int marlinXp, String requirements, String location)
        {
            this.name = name;
            this.marlinXp = marlinXp;
            this.requirements = requirements;
            this.location = location;
        }
    }
}

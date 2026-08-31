package compass;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class F2pCandidateIsolationTest
{
    @Test
    public void membersQuestNeverAppearsForF2pAccount()
    {
        Map<String, QuestStatus> quests = new HashMap<>();
        quests.put("Pandemonium", QuestStatus.NOT_STARTED);
        quests.put("The Ides of Milk", QuestStatus.NOT_STARTED);
        GameData data = GameData.builder(account(MembershipStatus.F2P))
                .quests(new QuestSnapshot(quests))
                .build();
        StrategyContext context = context(data);

        List<Recommendation> candidates =
                new QuestCandidateProvider(new QuestPriorityCatalog()).candidates(context);

        assertFalse(containsTitle(candidates, "Pandemonium"));
        assertTrue(containsTitle(candidates, "The Ides of Milk"));
    }

    @Test
    public void combatAchievementRewardTierDoesNotAppearForF2p()
    {
        GameData data = GameData.builder(account(MembershipStatus.F2P))
                .combatAchievements(new CombatAchievementSnapshot(10, 20))
                .build();

        assertTrue(new CombatAchievementCandidateProvider()
                .candidates(context(data)).isEmpty());
    }

    @Test
    public void combatAchievementTierCanRemainP2pAlternative()
    {
        GameData data = GameData.builder(account(MembershipStatus.P2P))
                .combatAchievements(new CombatAchievementSnapshot(10, 20))
                .build();

        assertFalse(new CombatAchievementCandidateProvider()
                .candidates(context(data)).isEmpty());
    }

    private static boolean containsTitle(List<Recommendation> candidates, String text)
    {
        for (Recommendation candidate : candidates)
        {
            if (candidate.getTitle() != null && candidate.getTitle().contains(text)) return true;
        }
        return false;
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(
                data,
                StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME,
                QuestTolerance.NORMAL,
                GoalType.MAX,
                true,
                false,
                false,
                new PreferenceProfile());
    }

    private static AccountSnapshot account(MembershipStatus membership)
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 60);
            xp.put(skill, Experience.getXpForLevel(60));
        }
        return new AccountSnapshot(
                "F2P Test",
                0,
                "Main",
                membership,
                80,
                1500,
                0L,
                levels,
                xp);
    }
}

package compass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActivityStrategyKnowledgeServiceTest
{
    private final ActivityStrategyKnowledgeService service =
            new ActivityStrategyKnowledgeService();

    @Test
    public void fullUimInventoryChangesMultipleDomainCandidates()
    {
        StrategyContext context = context(fullInventory());

        assertNull(service.attach(candidate("quest:waterfall-quest"), context));
        assertNull(service.attach(candidate("clue:pending"), context));
        assertNull(service.attach(candidate("pvm:tztok_jad"), context));
        assertNull(service.attach(candidate("minigame:tempoross"), context));
        assertNull(service.attach(candidate("upgrade:fighter-torso"), context));
        assertNull(service.attach(candidate(
                "prepare:infrastructure:poh-costume-room"), context));

        // These activities genuinely use the current/inside-instance setup.
        assertNotNull(service.attach(candidate("slayer:do-task"), context));
        assertNotNull(service.attach(candidate("minigame:gauntlet"), context));
        assertNotNull(service.attach(candidate("pvm:the_gauntlet"), context));
        assertNotNull(service.attach(candidate("pvm:the_corrupted_gauntlet"),
                context));
        assertNotNull(service.attach(candidate("verify:poh-build-mode"), context));
    }

    @Test
    public void unknownSlotsFailClosedWithoutPretendingInventoryIsFull()
    {
        GameData data = GameData.builder(uim())
                .inventory(new ItemsState(Collections.emptyList()))
                .build();
        assertNull(service.attach(candidate("quest:waterfall-quest"),
                context(data)));
        assertNotNull(service.attach(candidate("pvm:the_gauntlet"),
                context(data)));
    }

    @Test
    public void sourcedEvidenceReachesTheCommonRecommendation()
    {
        Recommendation result = service.attach(candidate("slayer:do-task"),
                context(fullInventory()));
        assertTrue(result.getStrategicValue().getEvidenceIds().contains(
                "strategy-source:SLAYER_TRAINING"));
    }

    private static Recommendation candidate(String id)
    {
        return new Recommendation(id, "Candidate", "Reason", 10.0, null,
                Confidence.VERIFIED, 0, 0,
                new Guidance("Do it.", "Observed setup",
                        "Verified location", "Note"),
                SafetyEvidence.harmless(false));
    }

    private static GameData fullInventory()
    {
        List<ItemState> items = new ArrayList<>();
        for (int slot = 0; slot < 28; slot++)
            items.add(new ItemState(1000 + slot,
                    "Item " + slot, 1, slot));
        return GameData.builder(uim())
                .inventory(new ItemsState(items, true))
                .build();
    }

    private static StrategyContext context(GameData data)
    {
        return new StrategyContext(data, StrategyMode.BALANCED,
                SessionIntent.PICK_FOR_ME, QuestTolerance.NORMAL,
                GoalType.AUTOMATIC, false, false,
                new PreferenceProfile());
    }

    private static AccountSnapshot uim()
    {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values())
        {
            levels.put(skill, 50);
            xp.put(skill, 101_333);
        }
        return new AccountSnapshot("Uim", 2, "Ultimate Ironman",
                MembershipStatus.P2P, 50, 50, 101_333L, levels, xp);
    }
}

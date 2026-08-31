package compass;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Release-facing copy checks across the recommendation families players see. */
public class PublicationUiStressTest
{
    @Test
    public void representativePlayerStatesStayCompactActionableAndClean()
    {
        List<Recommendation> cases = Arrays.asList(
                recommendation("quest", "Complete The Ribbiting Tale of a Lily Pad Labour Dispute",
                        "Unlocks the next verified quest prerequisite.", "Bring the required quest items.",
                        "Start the quest and follow Quest Helper."),
                recommendation("gear", "Acquire the Bow of faerdhinen",
                        "Advances the selected Ranged gear goal.", "Crystal weapon seed and crystal shards.",
                        "Complete the first verified acquisition prerequisite."),
                recommendation("pvm", "Prepare for the Corrupted Gauntlet",
                        "Removes the first known PvM preparation blocker.", "Song of the Elves access.",
                        "Complete the access prerequisite before practicing the encounter."),
                recommendation("slayer", "Prepare for an aberrant spectres task",
                        "Makes the current Slayer task safely actionable.", "Nose peg or Slayer helmet.",
                        "Equip mandatory protection before travelling."),
                recommendation("diary", "Advance the Western Provinces Elite Diary",
                        "Completes a shared diary prerequisite.", "The first unfinished skill or quest requirement.",
                        "Complete the first verified diary prerequisite."),
                recommendation("clue", "Prepare the current master clue step",
                        "Makes the observed clue step actionable.", "Required emote equipment.",
                        "Acquire the missing clue equipment."),
                recommendation("stash", "Confirm the Watson teleport STASH unit",
                        "Avoids rebuilding a clue loadout unnecessarily.", null,
                        "Confirm whether this STASH is already built."),
                recommendation("uim", "Retrieve the looting bag resource setup",
                        "Restores an immediately usable UIM setup.", null,
                        "UIM: retrieve the item before treating it as available."),
                recommendation("wilderness", "Prepare the Wilderness clue route",
                        "Makes the clue step safe to assess.", null,
                        "Wilderness: bank risked items before travelling."),
                recommendation("restricted", "Check the quest reward before starting",
                        "Protects the selected restricted build.", null,
                        "Restricted build: confirm irreversible XP rewards first."),
                recommendation("resource", "Acquire 2,147,483 cannonballs safely",
                        "Supplies the selected long-term resource goal.", "A verified account-aware source route.",
                        "Acquire only the proven shortfall."),
                recommendation("transport", "Unlock the Tree Gnome Village spirit tree",
                        "Adds reusable transport fan-out.", "Tree Gnome Village completion.",
                        "Complete the first verified access prerequisite."));

        for (Recommendation recommendation : cases)
        {
            String compact = Presentation.compactText(recommendation);
            String details = Presentation.detailedText(recommendation);
            assertFalse(recommendation.getId(), recommendation.getTitle().trim().isEmpty());
            assertFalse(recommendation.getId(), compact.trim().isEmpty());
            assertTrue(recommendation.getId() + " compact=" + compact.length(),
                    compact.length() < 520);
            assertTrue(recommendation.getId() + " details=" + details.length(),
                    details.length() < 500);
            assertFalse(compact.contains("policy class"));
            assertFalse(compact.contains("graph node"));
            assertFalse(details.contains("candidate score"));
            assertFalse(details.contains("Strategist will verify"));
            assertTrue(details.contains("WHY"));
            assertTrue(details.contains("CURRENT STEP"));
        }
    }

    @Test
    public void unavailableSelectedGoalExplainsSafeFallback()
    {
        Recommendation fallback = recommendation("f2p", "Train Woodcutting to 30",
                "Unlocks willow trees.", null, "6,760 XP remaining — about 181 oak chops to level 30.");
        GoalRecommendationContext context = GoalRecommendationContext.assess(
                GoalType.BOWFA, fallback, MembershipStatus.F2P);

        String compact = Presentation.compactText(fallback, context);
        String details = Presentation.detailedText(fallback, context);
        assertFalse(compact.contains("GOAL"));
        assertFalse(details.contains("GOAL"));
        assertTrue(details.contains("WHY\nUnlocks willow trees"));
        assertFalse(details.contains("Bowfa — Bowfa"));
    }

    private static Recommendation recommendation(String id, String title,
            String reason, String supplies, String action)
    {
        return new Recommendation(id, title, reason, 20.0, null,
                Confidence.CHECK_NEEDED, 0, 0,
                new Guidance(action, supplies,
                        "The nearest verified suitable location.", null));
    }
}

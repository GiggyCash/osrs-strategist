package com.udderlywet.osrsstrategist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.plugins.cluescrolls.ClueScrollService;
import net.runelite.client.plugins.cluescrolls.clues.AnagramClue;
import net.runelite.client.plugins.cluescrolls.clues.CipherClue;
import net.runelite.client.plugins.cluescrolls.clues.ClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.CoordinateClue;
import net.runelite.client.plugins.cluescrolls.clues.CrypticClue;
import net.runelite.client.plugins.cluescrolls.clues.EmoteClue;
import net.runelite.client.plugins.cluescrolls.clues.FairyRingClue;
import net.runelite.client.plugins.cluescrolls.clues.FaloTheBardClue;
import net.runelite.client.plugins.cluescrolls.clues.HotColdClue;
import net.runelite.client.plugins.cluescrolls.clues.LocationClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.MapClue;
import net.runelite.client.plugins.cluescrolls.clues.MusicClue;
import net.runelite.client.plugins.cluescrolls.clues.NpcClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.SkillChallengeClue;
import net.runelite.client.plugins.cluescrolls.clues.ThreeStepCrypticClue;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;

/**
 * Detects owned clue scrolls from containers Compass has actually observed.
 *
 * <p>A stale/unopened bank is never treated as proof that a clue disappeared.
 * UIM ignores normal bank state entirely. Challenge-scroll style intermediate
 * clue items preserve the previous clue observation rather than resetting it.</p>
 */
@Singleton
public class LiveClueStateReader
{
    private final ClueScrollService clueService;
    private final ClueScrollPlugin cluePlugin;
    private final Client client;

    @Inject
    public LiveClueStateReader(ClueScrollService clueService,
            ClueScrollPlugin cluePlugin, Client client)
    {
        this.clueService = clueService;
        this.cluePlugin = cluePlugin;
        this.client = client;
    }

    /** Compatibility constructor for evidence-only unit tests. */
    public LiveClueStateReader()
    {
        this(null, null, null);
    }

    public ClueSnapshot read(
            AccountMode mode,
            InventorySnapshot inventory,
            BankSnapshot bank,
            ClueSnapshot previous)
    {
        List<ItemStackSnapshot> visible = new ArrayList<>();
        if (inventory != null) visible.addAll(inventory.getItems());
        if (mode != AccountMode.ULTIMATE_IRONMAN && bank != null)
        {
            visible.addAll(bank.getItems());
        }

        ClueTier bestTier = ClueTier.UNKNOWN;
        boolean clueIntermediateObserved = false;
        for (ItemStackSnapshot item : visible)
        {
            String name = normalize(item.getName());
            if (name.isEmpty()) continue;

            if (isActualClueScroll(name))
            {
                ClueTier tier = ClueTier.fromText(name);
                if (tierPriority(tier) > tierPriority(bestTier))
                {
                    bestTier = tier;
                }
            }
            else if (isIntermediateClueItem(name))
            {
                clueIntermediateObserved = true;
            }
        }

        if (bestTier != ClueTier.UNKNOWN)
        {
            long firstSeen = System.currentTimeMillis();
            if (previous != null
                    && previous.isCluePresent()
                    && ClueTier.fromText(previous.getClueType()) == bestTier)
            {
                firstSeen = previous.getFirstSeenAtMillis();
            }
            return new ClueSnapshot(
                    true,
                    bestTier.name().toLowerCase(Locale.ROOT),
                    firstSeen,
                    RecommendationConfidence.VERIFIED,
                    readCurrentStep()
            );
        }

        if (clueIntermediateObserved && previous != null && previous.isCluePresent())
        {
            return previous;
        }

        // If the bank has not been observed, absence from inventory alone does
        // not prove a non-UIM clue was completed/dropped rather than banked.
        if (mode != AccountMode.ULTIMATE_IRONMAN
                && bank == null
                && previous != null
                && previous.isCluePresent())
        {
            return previous;
        }

        // UIM has no normal bank route. Once neither a clue scroll nor a known
        // intermediate clue item is present in inventory, this observation can
        // be cleared instead of waiting for impossible bank evidence.
        return null;
    }

    private ClueStepSnapshot readCurrentStep()
    {
        if (clueService == null) return null;
        ClueScroll clue;
        try
        {
            clue = clueService.getClue();
        }
        catch (RuntimeException ex)
        {
            return null;
        }
        if (clue == null) return null;

        String kind = clueKind(clue);
        String action;
        String location;
        try { action = actionOf(clue); }
        catch (RuntimeException ex) { action = null; }
        try { location = locationOf(clue); }
        catch (RuntimeException ex) { location = null; }
        if (action == null || action.trim().isEmpty())
            action = "Follow RuneLite's highlighted " + kind + " solution.";
        WorldPoint point = worldPointOf(clue);
        List<String> requirements = itemRequirements(clue);
        String enemy = clue.getEnemy() == null
                ? null : clue.getEnemy().getText();
        String stash = clue instanceof EmoteClue
                && ((EmoteClue) clue).getStashUnit() != null
                ? ((EmoteClue) clue).getStashUnit().name()
                        .toLowerCase(Locale.ROOT).replace('_', ' ')
                : null;
        return new ClueStepSnapshot(
                kind, action, location, requirements,
                clue.isRequiresSpade(), clue.isRequiresLight(), enemy,
                isWilderness(point, action, location), stash);
    }

    private String actionOf(ClueScroll clue)
    {
        if (clue instanceof EmoteClue)
            return ((EmoteClue) clue).getText();
        if (clue instanceof CrypticClue)
            return ((CrypticClue) clue).getSolution(cluePlugin);
        if (clue instanceof AnagramClue)
            return talkTo(((AnagramClue) clue).getNpcs(cluePlugin));
        if (clue instanceof CipherClue)
            return talkTo(((CipherClue) clue).getNpcs(cluePlugin));
        if (clue instanceof CoordinateClue)
            return "Dig on RuneLite's marked coordinate tile.";
        if (clue instanceof MapClue)
            return ((MapClue) clue).getDescription();
        if (clue instanceof MusicClue)
        {
            String npc = firstNpc((MusicClue) clue);
            return "Play " + ((MusicClue) clue).getSong()
                    + (npc == null ? " for the highlighted clue NPC."
                            : " for " + npc + ".");
        }
        if (clue instanceof FaloTheBardClue)
            return "Bring the requested item to Falo the Bard.";
        if (clue instanceof SkillChallengeClue)
            return ((SkillChallengeClue) clue).getChallenge();
        if (clue instanceof HotColdClue)
        {
            String solution = ((HotColdClue) clue).getSolution();
            return solution == null || solution.trim().isEmpty()
                    ? "Use the strange device and follow RuneLite's narrowing hot/cold area."
                    : solution;
        }
        if (clue instanceof FairyRingClue)
            return ((FairyRingClue) clue).getText();
        if (clue instanceof ThreeStepCrypticClue)
            return currentThreeStepAction((ThreeStepCrypticClue) clue);
        return "Follow RuneLite's highlighted " + clueKind(clue)
                + " solution.";
    }

    private String locationOf(ClueScroll clue)
    {
        if (clue instanceof EmoteClue)
            return ((EmoteClue) clue).getLocationName();
        if (clue instanceof AnagramClue)
            return ((AnagramClue) clue).getArea();
        if (clue instanceof MapClue)
            return ((MapClue) clue).getDescription();
        if (clue instanceof NpcClueScroll)
        {
            String npc = firstNpc((NpcClueScroll) clue);
            if (npc != null) return npc;
        }
        return clue instanceof LocationClueScroll
                ? "RuneLite's marked tile" : null;
    }

    private WorldPoint worldPointOf(ClueScroll clue)
    {
        if (!(clue instanceof LocationClueScroll) || cluePlugin == null)
            return null;
        try
        {
            return ((LocationClueScroll) clue).getLocation(cluePlugin);
        }
        catch (RuntimeException ex)
        {
            // Some dynamic clues do not resolve a single tile until more live
            // evidence exists. That uncertainty must not erase the step.
            return null;
        }
    }

    private List<String> itemRequirements(ClueScroll clue)
    {
        ItemRequirement[] values = null;
        if (clue instanceof EmoteClue)
            values = ((EmoteClue) clue).getItemRequirements();
        else if (clue instanceof FaloTheBardClue)
            values = ((FaloTheBardClue) clue).getItemRequirements();
        else if (clue instanceof SkillChallengeClue)
            values = ((SkillChallengeClue) clue).getItemRequirements();
        if (values == null || client == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (ItemRequirement value : values)
        {
            if (value == null) continue;
            String name;
            try { name = value.getCollectiveName(client); }
            catch (RuntimeException ex) { continue; }
            if (name != null && !name.trim().isEmpty()
                    && !result.contains(name.trim())) result.add(name.trim());
        }
        return result;
    }

    private String currentThreeStepAction(ThreeStepCrypticClue clue)
    {
        for (java.util.Map.Entry<CrypticClue, Boolean> step
                : clue.getClueSteps())
            if (!Boolean.TRUE.equals(step.getValue()))
            {
                String solution = step.getKey().getSolution(cluePlugin);
                if (solution != null && !solution.trim().isEmpty())
                    return solution;
            }
        return "Complete the remaining RuneLite-highlighted cryptic step.";
    }

    private static String clueKind(ClueScroll clue)
    {
        String value = clue.getClass().getSimpleName()
                .replace("Clue", "").replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT).trim();
        return value.isEmpty() ? "clue step" : value + " step";
    }

    private String firstNpc(NpcClueScroll clue)
    {
        if (clue == null || cluePlugin == null) return null;
        String[] values;
        try { values = clue.getNpcs(cluePlugin); }
        catch (RuntimeException ex) { return null; }
        return values == null || values.length == 0 ? null : values[0];
    }

    private String talkTo(String[] npcs)
    {
        return npcs == null || npcs.length == 0
                ? "Talk to RuneLite's highlighted clue NPC."
                : "Talk to " + npcs[0] + ".";
    }

    private static boolean isWilderness(WorldPoint point,
            String action, String location)
    {
        if (point != null)
        {
            int x = point.getX();
            int y = point.getY();
            if (x >= 2944 && x < 3392 && y >= 3523 && y < 3971)
                return true;
            if (x >= 2944 && x < 3264 && y >= 9918 && y < 10360)
                return true;
        }
        String text = normalize((action == null ? "" : action) + " "
                + (location == null ? "" : location));
        return text.contains("wilderness");
    }

    private static boolean isActualClueScroll(String name)
    {
        return name.startsWith("clue scroll (")
                || name.startsWith("clue scroll -")
                || name.equals("clue scroll");
    }

    private static boolean isIntermediateClueItem(String name)
    {
        return name.contains("challenge scroll")
                || name.contains("puzzle box")
                || name.contains("light box")
                || name.contains("strange device")
                || name.contains("hot/cold device");
    }

    private static int tierPriority(ClueTier tier)
    {
        switch (tier)
        {
            case MASTER: return 6;
            case ELITE: return 5;
            case HARD: return 4;
            case MEDIUM: return 3;
            case EASY: return 2;
            case BEGINNER: return 1;
            case UNKNOWN:
            default: return 0;
        }
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

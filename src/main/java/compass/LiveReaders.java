package compass;
import lombok.*;
import static java.lang.Math.*;
import static java.util.Collections.*;

import java.util.*;
import javax.inject.*;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.cluescrolls.*;
import net.runelite.client.plugins.cluescrolls.clues.*;
import net.runelite.client.plugins.cluescrolls.clues.item.ItemRequirement;
import net.runelite.client.plugins.poh.PohIcons;
import static compass.Text.get;

/**
 * Detects owned clue scrolls from containers Compass has actually observed.
 *
 * <p>A stale/unopened bank is never treated as proof that a clue disappeared.
 * UIM ignores normal bank state entirely. Challenge-scroll style intermediate
 * clue items preserve the previous clue observation rather than resetting it.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveClueStateReader
{
    private final ClueScrollService clueService;
    private final ClueScrollPlugin cluePlugin;
    private final Client client;

    public ClueSnapshot read(
            AccountMode mode,
            ItemsState inventory,
            ItemsState bank,
            ClueSnapshot previous)
    {
        List<ItemState> visible = new ArrayList<>();
        if (inventory != null) visible.addAll(inventory.getItems());
        if (mode != AccountMode.ULTIMATE_IRONMAN && bank != null)
        {
            visible.addAll(bank.getItems());
        }

        var bestTier = ClueTier.UNKNOWN;
        var clueIntermediateObserved = false;
        for (ItemState item : visible)
        {
            var name = Names.lower(item.getName());
            if (name.isEmpty()) continue;

            if (isActualClueScroll(name))
            {
                var tier = ClueTier.fromText(name);
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
            var firstSeen = System.currentTimeMillis();
            if (previous != null
                    && previous.cluePresent
                    && ClueTier.fromText(previous.clueType) == bestTier)
            {
                firstSeen = previous.getFirstSeenAtMillis();
            }
            return new ClueSnapshot(
                    true,
                    bestTier.name().toLowerCase(Locale.ROOT),
                    firstSeen,
                    Confidence.VERIFIED,
                    readCurrentStep()
            );
        }

        if (clueIntermediateObserved && previous != null && previous.cluePresent)
        {
            return previous;
        }

        // If the bank has not been observed, absence from inventory alone does
        // not prove a non-UIM clue was completed/dropped rather than banked.
        if (mode != AccountMode.ULTIMATE_IRONMAN
                && bank == null
                && previous != null
                && previous.cluePresent)
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

        var kind = clueKind(clue);
        String action;
        String location;
        try { action = actionOf(clue); }
        catch (RuntimeException ex) { action = null; }
        try { location = locationOf(clue); }
        catch (RuntimeException ex) { location = null; }
        if (action == null || action.trim().isEmpty())
            action = get(1562) + kind + " solution.";
        var point = worldPointOf(clue);
        var requirements = itemRequirements(clue);
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
            return get(339);
        if (clue instanceof MapClue)
            return ((MapClue) clue).getDescription();
        if (clue instanceof MusicClue)
        {
            var npc = firstNpc((MusicClue) clue);
            return "Play " + ((MusicClue) clue).getSong()
                    + (npc == null ? get(1563)
                            : " for " + npc + ".");
        }
        if (clue instanceof FaloTheBardClue)
            return get(340);
        if (clue instanceof SkillChallengeClue)
            return ((SkillChallengeClue) clue).getChallenge();
        if (clue instanceof HotColdClue)
        {
            var solution = ((HotColdClue) clue).getSolution();
            return solution == null || solution.trim().isEmpty()
                    ? get(341)
                    : solution;
        }
        if (clue instanceof FairyRingClue)
            return ((FairyRingClue) clue).getText();
        if (clue instanceof ThreeStepCrypticClue)
            return currentThreeStepAction((ThreeStepCrypticClue) clue);
        return get(1562) + clueKind(clue)
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
            var npc = firstNpc((NpcClueScroll) clue);
            if (npc != null) return npc;
        }
        return clue instanceof LocationClueScroll
                ? get(1564) : null;
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
        if (values == null || client == null) return emptyList();
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
        for (Map.Entry<CrypticClue, Boolean> step
                : clue.getClueSteps())
            if (!Boolean.TRUE.equals(step.getValue()))
            {
                var solution = step.getKey().getSolution(cluePlugin);
                if (solution != null && !solution.trim().isEmpty())
                    return solution;
            }
        return get(342);
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
                ? get(343)
                : "Talk to " + npcs[0] + ".";
    }

    private static boolean isWilderness(WorldPoint point,
            String action, String location)
    {
        if (point != null)
        {
            var x = point.getX();
            var y = point.getY();
            if (x >= 2944 && x < 3392 && y >= 3523 && y < 3971)
                return true;
            if (x >= 2944 && x < 3264 && y >= 9918 && y < 10360)
                return true;
        }
        String text = Names.lower((action == null ? "" : action) + " "
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
        return name.contains(get(1733))
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

}

/** Reads the game's six Combat Achievement reward-tier completion varbits. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveCombatAchievementReader
{
    private final Client client;

    public CombatAchievementSnapshot read(CombatAchievementSnapshot observed)
    {
        if (client.getGameState() != GameState.LOGGED_IN) return observed;

        var tiers = EnumSet.noneOf(CombatAchievementTier.class);
        addIfComplete(tiers, CombatAchievementTier.EASY,
                VarbitID.CA_TIER_STATUS_EASY);
        addIfComplete(tiers, CombatAchievementTier.MEDIUM,
                VarbitID.CA_TIER_STATUS_MEDIUM);
        addIfComplete(tiers, CombatAchievementTier.HARD,
                VarbitID.CA_TIER_STATUS_HARD);
        addIfComplete(tiers, CombatAchievementTier.ELITE,
                VarbitID.CA_TIER_STATUS_ELITE);
        addIfComplete(tiers, CombatAchievementTier.MASTER,
                VarbitID.CA_TIER_STATUS_MASTER);
        addIfComplete(tiers, CombatAchievementTier.GRANDMASTER,
                VarbitID.CA_TIER_STATUS_GRANDMASTER);

        var minimumPoints = 0;
        for (CombatAchievementTier tier : tiers)
            minimumPoints = max(minimumPoints, tier.getRewardPoints());
        var observedPoints = observed == null ? 0 : observed.getEarnedPoints();
        var observedTasks = observed == null ? 0 : observed.getCompletedTasks();
        return new CombatAchievementSnapshot(
                observedTasks,
                max(minimumPoints, observedPoints),
                tiers
        );
    }

    private void addIfComplete(Set<CombatAchievementTier> tiers,
            CombatAchievementTier tier, int varbit)
    {
        // RuneLite documents value 2 as completed. Accept >= 2 so future
        // additional completion states do not regress an already-earned tier.
        if (client.getVarbitValue(varbit) >= 2) tiers.add(tier);
    }
}

/** Reads only official current RuneLite prayer/spellbook identifiers. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveCombatEvidenceReader
{
    private final Client client;

    public CombatEvidenceSnapshot read()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
            return null;
        try
        {
            var active = EnumSet.noneOf(Prayer.class);
            for (Prayer prayer : Prayer.values())
                if (client.getVarbitValue(prayer.getVarbit()) > 0) active.add(prayer);
            return new CombatEvidenceSnapshot(
                    client.getVarbitValue(VarbitID.SPELLBOOK), active,
                    client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) > 0,
                    client.getVarbitValue(VarbitID.PRAYER_AUGURY_UNLOCKED) > 0,
                    client.getVarbitValue(VarbitID.PRAYER_PRESERVE_UNLOCKED) > 0);
        }
        catch (RuntimeException transientClientState)
        {
            // Hops/account switches can briefly expose a logged-in state before
            // the backing var client is readable. Unknown is safer than stale data.
            return null;
        }
    }
}

/** Reads all 12 regions x 4 Achievement Diary tier states directly from RuneLite. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveDiaryStateReader
{
    private static final String[] REGIONS = {
            "Ardougne", "Desert", "Falador", "Fremennik", "Kandarin",
            "Karamja", "Kourend & Kebos", compass.Text.get(1152),
            "Morytania", "Varrock", compass.Text.get(1724), "Wilderness"
    };
    /** Tier-completion varbits followed by the four completed-task counts. */
    private static final int[][] VARBITS = {
            {VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE, VarbitID.ARDOUGNE_EASY_COUNT, VarbitID.ARDOUGNE_MED_COUNT, VarbitID.ARDOUGNE_HARD_COUNT, VarbitID.ARDOUGNE_ELITE_COUNT},
            {VarbitID.DESERT_DIARY_EASY_COMPLETE, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE, VarbitID.DESERT_DIARY_HARD_COMPLETE, VarbitID.DESERT_DIARY_ELITE_COMPLETE, VarbitID.DESERT_EASY_COUNT, VarbitID.DESERT_MED_COUNT, VarbitID.DESERT_HARD_COUNT, VarbitID.DESERT_ELITE_COUNT},
            {VarbitID.FALADOR_DIARY_EASY_COMPLETE, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE, VarbitID.FALADOR_DIARY_HARD_COMPLETE, VarbitID.FALADOR_DIARY_ELITE_COMPLETE, VarbitID.FALADOR_EASY_COUNT, VarbitID.FALADOR_MED_COUNT, VarbitID.FALADOR_HARD_COUNT, VarbitID.FALADOR_ELITE_COUNT},
            {VarbitID.FREMENNIK_DIARY_EASY_COMPLETE, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE, VarbitID.FREMENNIK_EASY_COUNT, VarbitID.FREMENNIK_MED_COUNT, VarbitID.FREMENNIK_HARD_COUNT, VarbitID.FREMENNIK_ELITE_COUNT},
            {VarbitID.KANDARIN_DIARY_EASY_COMPLETE, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE, VarbitID.KANDARIN_DIARY_HARD_COMPLETE, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE, VarbitID.KANDARIN_EASY_COUNT, VarbitID.KANDARIN_MED_COUNT, VarbitID.KANDARIN_HARD_COUNT, VarbitID.KANDARIN_ELITE_COUNT},
            {VarbitID.ATJUN_EASY_DONE, VarbitID.ATJUN_MED_DONE, VarbitID.ATJUN_HARD_DONE, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE, VarbitID.KARAMJA_EASY_COUNT, VarbitID.KARAMJA_MED_COUNT, VarbitID.KARAMJA_HARD_COUNT, VarbitID.KARAMJA_ELITE_COUNT},
            {VarbitID.KOUREND_DIARY_EASY_COMPLETE, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE, VarbitID.KOUREND_DIARY_HARD_COMPLETE, VarbitID.KOUREND_DIARY_ELITE_COMPLETE, VarbitID.KOUREND_EASY_COUNT, VarbitID.KOUREND_MED_COUNT, VarbitID.KOUREND_HARD_COUNT, VarbitID.KOUREND_ELITE_COUNT},
            {VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE, VarbitID.LUMBRIDGE_EASY_COUNT, VarbitID.LUMBRIDGE_MED_COUNT, VarbitID.LUMBRIDGE_HARD_COUNT, VarbitID.LUMBRIDGE_ELITE_COUNT},
            {VarbitID.MORYTANIA_DIARY_EASY_COMPLETE, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE, VarbitID.MORYTANIA_EASY_COUNT, VarbitID.MORYTANIA_MED_COUNT, VarbitID.MORYTANIA_HARD_COUNT, VarbitID.MORYTANIA_ELITE_COUNT},
            {VarbitID.VARROCK_DIARY_EASY_COMPLETE, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE, VarbitID.VARROCK_DIARY_HARD_COMPLETE, VarbitID.VARROCK_DIARY_ELITE_COMPLETE, VarbitID.VARROCK_EASY_COUNT, VarbitID.VARROCK_MED_COUNT, VarbitID.VARROCK_HARD_COUNT, VarbitID.VARROCK_ELITE_COUNT},
            {VarbitID.WESTERN_DIARY_EASY_COMPLETE, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE, VarbitID.WESTERN_DIARY_HARD_COMPLETE, VarbitID.WESTERN_DIARY_ELITE_COMPLETE, VarbitID.WESTERN_EASY_COUNT, VarbitID.WESTERN_MED_COUNT, VarbitID.WESTERN_HARD_COUNT, VarbitID.WESTERN_ELITE_COUNT},
            {VarbitID.WILDERNESS_DIARY_EASY_COMPLETE, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE, VarbitID.WILDERNESS_EASY_COUNT, VarbitID.WILDERNESS_MED_COUNT, VarbitID.WILDERNESS_HARD_COUNT, VarbitID.WILDERNESS_ELITE_COUNT}
    };
    private final Client client;
    private final DiaryTaskCatalog taskCatalog = new DiaryTaskCatalog();
    private final Map<String, Boolean> observedTaskCompletion = new HashMap<>();

    public DiarySnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;

        Map<String, Integer> completed = new HashMap<>();
        Map<String, Integer> totals = new HashMap<>();
        Map<String, Map<DiaryTier, Boolean>> tiers = new HashMap<>();

        for (int i = 0; i < REGIONS.length; i++)
            add(completed, totals, tiers, REGIONS[i], VARBITS[i]);

        return new DiarySnapshot(completed, totals, tiers,
                observedTaskCompletion);
    }

    /**
     * Captures exact completed/incomplete rows while an Achievement Diary page
     * is visible. Rows not present on the open page remain unknown.
     */
    public boolean observeOpenDiary()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        var title = client.getWidget(InterfaceID.Journalscroll.TITLE);
        var layer = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
        if (title == null || layer == null) return false;
        var children = layer.getStaticChildren();
        if (children == null || children.length == 0) return false;
        var region = regionFor(title.getText());
        if (region == null && children[0] != null)
            region = regionFor(children[0].getText());
        if (region == null) return false;

        Map<String, Boolean> before = new HashMap<>(observedTaskCompletion);
        List<String> rows = new ArrayList<>();
        for (Widget child : children)
            if (child != null && child.getText() != null)
                rows.add(child.getText());
        observedTaskCompletion.putAll(observedTasksFromRows(
                region, rows, taskCatalog));
        return !before.equals(observedTaskCompletion);
    }

    static Map<String, Boolean> observedTasksFromRows(String region,
            List<String> rows, DiaryTaskCatalog catalog)
    {
        Map<String, Boolean> result = new HashMap<>();
        if (region == null || rows == null || catalog == null) return result;
        var regionTasks = catalog.all();
        var buffered = "";
        var bufferedComplete = false;
        for (String raw : rows)
        {
            if (raw == null) continue;
            var row = normalizeSpace(net.runelite.client.util.Text.removeTags(raw));
            if (row.isEmpty()) continue;
            var struck = raw.toLowerCase(Locale.ROOT).contains("<str>");

            var direct = match(regionTasks, region, row);
            if (direct != null)
            {
                result.put(direct.task.getId(), struck);
                buffered = "";
                bufferedComplete = false;
                continue;
            }

            var combined = buffered.isEmpty() ? row : buffered + " " + row;
            var wrapped = match(regionTasks, region, combined);
            if (wrapped != null)
            {
                result.put(wrapped.task.getId(),
                        bufferedComplete || struck);
                buffered = "";
                bufferedComplete = false;
                continue;
            }

            if (isTaskPrefix(regionTasks, region, combined))
            {
                buffered = combined;
                bufferedComplete = bufferedComplete || struck;
            }
            else if (isTaskPrefix(regionTasks, region, row))
            {
                buffered = row;
                bufferedComplete = struck;
            }
            else
            {
                buffered = "";
                bufferedComplete = false;
            }
        }
        return result;
    }

    public void clear()
    {
        observedTaskCompletion.clear();
    }

    private static Match match(List<DiaryTaskDefinition> tasks,
            String region, String row)
    {
        for (DiaryTaskDefinition task : tasks)
        {
            if (!task.getRegion().equals(region)) continue;
            var instruction = normalizeSpace(task.getTask());
            if (row.equals(instruction)
                    || row.startsWith(instruction + " ("))
                return new Match(task);
        }
        return null;
    }

    private static boolean isTaskPrefix(List<DiaryTaskDefinition> tasks,
            String region, String row)
    {
        for (DiaryTaskDefinition task : tasks)
            if (task.getRegion().equals(region)
                    && normalizeSpace(task.getTask()).startsWith(row))
                return true;
        return false;
    }

    private static String regionFor(String rawTitle)
    {
        String title = net.runelite.client.util.Text.removeTags(rawTitle == null ? "" : rawTitle)
                .toLowerCase(Locale.ROOT);
        for (String region : REGIONS)
            if (title.contains(region.split(" ")[0].toLowerCase(Locale.ROOT)))
                return region;
        return null;
    }

    private static String normalizeSpace(String value)
    {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)

    private static final class Match
    {
        private final DiaryTaskDefinition task;
    }

    private void add(Map<String, Integer> completed,
            Map<String, Integer> totals,
            Map<String, Map<DiaryTier, Boolean>> tiers,
            String region, int[] ids)
    {
        EnumMap<DiaryTier, Boolean> tierMap = new EnumMap<>(DiaryTier.class);
        var values = DiaryTier.values();
        for (int i = 0; i < values.length; i++)
            tierMap.put(values[i], client.getVarbitValue(ids[i]) >= 1);
        tiers.put(region, tierMap);

        var done = 0;
        for (int i = 4; i < ids.length; i++)
            done += max(0, client.getVarbitValue(ids[i]));
        completed.put(region, done);
        // Per-tier totals are maintained outside RuneLite's public count varbits.
        // Leave this unknown rather than freezing a copied third-party table.
        totals.put(region, 0);
    }
}

/** Builds a live economy snapshot from observed item containers and RuneLite prices. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveEconomyReader
{
    private final MarketPriceService marketPriceService;

    public AccountEconomySnapshot read(
            AccountSnapshot account,
            ItemsState inventory,
            ItemsState bank)
    {
        if (account == null) return null;
        var mode = AccountMode.fromTypeCode(account.modeCode());

        long coins = spendableCurrency(inventory == null
                ? null : inventory.getItems());
        if (mode != AccountMode.ULTIMATE_IRONMAN && bank != null)
        {
            coins = safeAdd(coins, spendableCurrency(bank.getItems()));
        }

        var bankValue = 0L;
        if (bank != null && mode != AccountMode.ULTIMATE_IRONMAN)
        {
            for (ItemState item : bank.getItems())
            {
                if (item == null || item.quantity <= 0) continue;
                int unitPrice = marketPriceService == null
                        ? 0
                        : marketPriceService.priceByItemId(item.itemId);
                if (unitPrice <= 0) continue;
                bankValue = safeAdd(
                        bankValue,
                        safeMultiply(unitPrice, item.quantity));
            }
        }

        Confidence confidence;
        if (mode == AccountMode.ULTIMATE_IRONMAN)
        {
            // Inventory coins are real, but coins held in specialized storage
            // are not universally exposed here yet.
            confidence = Confidence.CHECK_NEEDED;
        }
        else
        {
            confidence = bank == null
                    ? Confidence.CHECK_NEEDED
                    : Confidence.VERIFIED;
        }

        return new AccountEconomySnapshot(
                coins,
                bankValue,
                confidence);
    }

    private static long spendableCurrency(List<ItemState> items)
    {
        if (items == null) return 0L;
        var total = 0L;
        for (ItemState item : items)
        {
            if (item == null || item.getName() == null) continue;
            if ("Coins".equalsIgnoreCase(item.getName()))
            {
                total = safeAdd(total, item.quantity);
            }
            else if ("Platinum token".equalsIgnoreCase(item.getName())
                    || "Platinum tokens".equalsIgnoreCase(item.getName()))
            {
                total = safeAdd(total,
                        safeMultiply(1000L, item.quantity));
            }
        }
        return total;
    }

    private static long safeMultiply(long a, long b)
    {
        if (a <= 0 || b <= 0) return 0L;
        if (a > Long.MAX_VALUE / b) return Long.MAX_VALUE;
        return a * b;
    }

    private static long safeAdd(long a, long b)
    {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}

/**
 * Reads the item containers RuneLite can observe safely without automating any
 * gameplay.
 *
 * <p>The bank is cached only after RuneLite exposes the bank container. If the
 * account has not opened its bank during this client session, {@code readBank}
 * returns the last verified cache (or null). Compass therefore never treats
 * an unopened bank as an empty bank.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveItemStateReader
{
    private final Client client;
    private final ItemManager itemManager;
    private ItemsState lastBankSnapshot;
    private ItemsState lastGroupStorageSnapshot;

    public ItemsState readInventory()
    {
        List<ItemState> items =
                readContainer(InventoryID.INV);

        return items == null
                ? null
                : new ItemsState(items, true);
    }

    public ItemsState readEquipment()
    {
        List<ItemState> items =
                readContainer(InventoryID.WORN);

        return items == null
                ? null
                : new ItemsState(items);
    }

    public ItemsState readBank()
    {
        List<ItemState> items =
                readContainer(InventoryID.BANK);

        if (items != null)
        {
            lastBankSnapshot = new ItemsState(
                    items,
                    System.currentTimeMillis()
            );
        }

        return lastBankSnapshot;
    }

    /** Shared storage is usable only after this character actually opens it. */
    public ItemsState readGroupStorage()
    {
        return lastGroupStorageSnapshot;
    }

    public void observeGroupStorage(ItemContainer container)
    {
        var items = snapshot(container);
        if (items != null)
            lastGroupStorageSnapshot = new ItemsState(
                    true, items, System.currentTimeMillis());
    }

    public void clearAccountCaches()
    {
        lastBankSnapshot = null;
        lastGroupStorageSnapshot = null;
    }

    private List<ItemState> readContainer(
            int inventoryId)
    {
        ItemContainer container =
                client.getItemContainer(inventoryId);

        if (container == null)
        {
            return null;
        }

        return snapshot(container);
    }

    private List<ItemState> snapshot(ItemContainer container)
    {
        if (container == null) return null;
        List<ItemState> result = new ArrayList<>();
        var containerItems = container.getItems();
        for (int slotIndex = 0; slotIndex < containerItems.length; slotIndex++)
        {
            var item = containerItems[slotIndex];
            if (item == null
                    || item.getId() < 0
                    || item.getQuantity() <= 0)
            {
                continue;
            }

            String name = itemManager
                    .getItemComposition(item.getId())
                    .getName();

            result.add(
                    new ItemState(
                            item.getId(),
                            name,
                            item.getQuantity(),
                            slotIndex
                    )
            );
        }

        return result;
    }
}

/**
 * Observes durable furniture in the current character's own POH.
 *
 * <p>Scene objects alone cannot distinguish a personal house from a host's
 * house. RuneLite's building-mode varbit is therefore the ownership boundary:
 * guests cannot enable it. A complete build-mode scene scan can prove both
 * presence and absence, while every other scene returns no observation.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LivePohStateReader
{
    public static final String ARMOUR_CASE = "poh-armour-case";
    public static final String COSTUME_ROOM = get(1709);
    public static final String PERMANENT_PORTAL = get(1710);
    public static final String PORTAL_NEXUS = get(1711);
    public static final String RESTORATION_POOL = get(1712);
    public static final String SUPERIOR_GARDEN = get(1713);
    public static final String ORNATE_POOL = "poh-ornate-pool";
    public static final String JEWELLERY_BOX = get(1714);
    public static final String ORNATE_JEWELLERY_BOX = get(1715);
    public static final String SPELLBOOK_ALTAR = get(1716);
    public static final String OCCULT_ALTAR = get(1717);
    public static final String FAIRY_RING = "poh-fairy-ring";
    public static final String SPIRIT_TREE = "poh-spirit-tree";
    public static final String SPIRITUAL_FAIRY_TREE = get(1718);
    public static final String MOUNTED_GLORY = get(1719);
    public static final String ARMOUR_STAND = get(1720);

    private static final Set<String> TRACKED = new HashSet<>(Arrays.asList(
            COSTUME_ROOM, ARMOUR_CASE, PERMANENT_PORTAL, PORTAL_NEXUS,
            SUPERIOR_GARDEN, RESTORATION_POOL, ORNATE_POOL, JEWELLERY_BOX,
            ORNATE_JEWELLERY_BOX, SPELLBOOK_ALTAR, OCCULT_ALTAR,
            FAIRY_RING, SPIRIT_TREE, SPIRITUAL_FAIRY_TREE,
            MOUNTED_GLORY, ARMOUR_STAND));

    private final Client client;

    public PohSnapshot read()
    {
        if (client == null || client.getGameState() != GameState.LOGGED_IN
                || client.getVarbitValue(VarbitID.POH_BUILDING_MODE) != 1)
            return null;
        var worldView = client.getTopLevelWorldView();
        var scene = worldView == null ? null : worldView.getScene();
        if (scene == null || scene.getTiles() == null) return null;

        Set<Integer> objectIds = new HashSet<>();
        for (Tile[][] plane : scene.getTiles())
            if (plane != null)
                for (Tile[] column : plane)
                    if (column != null)
                        for (Tile tile : column) collect(tile, objectIds);
        return snapshotForObjectIds(objectIds);
    }

    static PohSnapshot snapshotForObjectIds(Set<Integer> objectIds)
    {
        Map<String, Capability> furniture = new LinkedHashMap<>();
        for (String key : TRACKED) furniture.put(key, Capability.BLOCKED);
        if (objectIds != null)
            for (Integer id : objectIds)
                if (id != null) classify(id, furniture);
        return new PohSnapshot(Capability.VERIFIED, furniture);
    }

    private static void collect(Tile tile, Set<Integer> ids)
    {
        if (tile == null) return;
        add(tile.getDecorativeObject(), ids);
        add(tile.getGroundObject(), ids);
        add(tile.getWallObject(), ids);
        var gameObjects = tile.getGameObjects();
        if (gameObjects != null)
            for (GameObject object : gameObjects) add(object, ids);
    }

    private static void add(TileObject object, Set<Integer> ids)
    {
        if (object != null && object.getId() >= 0) ids.add(object.getId());
    }

    private static void classify(int id, Map<String, Capability> values)
    {
        // Only completed armour cases are in this range; the build hotspot is
        // outside it and is deliberately not treated as storage.
        if (id >= ObjectID.POH_COS_ROOM_CAPE_RACK_OAK
                && id <= ObjectID.POH_COS_ROOM_ARMOUR_CASE_HOTSPOT)
            verify(values, COSTUME_ROOM);
        if (id >= ObjectID.POH_COS_ROOM_ARMOUR_CASE_OAK
                && id <= ObjectID.POH_COS_ROOM_ARMOUR_CASE_OPEN_MAHOGANY)
            verify(values, ARMOUR_CASE);

        var icon = PohIcons.getIcon(id);
        if (icon != null)
        {
            switch (icon)
            {
                case PORTALNEXUS:
                    verify(values, PORTAL_NEXUS);
                    break;
                case POOLS:
                    verify(values, RESTORATION_POOL);
                    break;
                case JEWELLERYBOX:
                    verify(values, JEWELLERY_BOX);
                    break;
                case SPELLBOOKALTAR:
                    verify(values, SPELLBOOK_ALTAR);
                    break;
                case MAGICTRAVEL:
                    if (id == ObjectID.POH_FAIRY_RING)
                        verify(values, FAIRY_RING);
                    else if (id == ObjectID.POH_SPIRIT_TREE)
                        verify(values, SPIRIT_TREE);
                    else if (id == ObjectID.POH_SPIRIT_RING)
                    {
                        verify(values, FAIRY_RING);
                        verify(values, SPIRIT_TREE);
                        verify(values, SPIRITUAL_FAIRY_TREE);
                    }
                    break;
                case GLORY:
                    verify(values, MOUNTED_GLORY);
                    break;
                case REPAIR:
                    verify(values, ARMOUR_STAND);
                    break;
                case EXITPORTAL:
                case ALTAR:
                case XERICSTALISMAN:
                case DIGSITEPENDANT:
                case MYTHICALCAPE:
                    break;
                default:
                    // Every remaining PohIcons entry is a configured portal
                    // destination, not an empty frame or hotspot.
                    verify(values, PERMANENT_PORTAL);
                    break;
            }
        }

        if (id == ObjectID.POH_POOL_REGENERATION)
            verify(values, ORNATE_POOL);
        if ((id >= ObjectID.POH_SUPERIOR_GARDEN_HOTSPOT_TREERING
                && id <= ObjectID.POH_SUPERIOR_GARDEN_HOTSPOT_SEATING_B_RIGHT)
                || (id >= ObjectID.POH_SPIRIT_TREE
                && id <= ObjectID.POH_POOL_REGENERATION))
            verify(values, SUPERIOR_GARDEN);
        if (id == ObjectID.POH_JEWELLERY_BOX_3)
            verify(values, ORNATE_JEWELLERY_BOX);
        if (id == ObjectID.POH_ALTAR_OCCULT
                || id == ObjectID.POH_ALTAR_OCCULT_STANDARD
                || id == ObjectID.POH_ALTAR_OCCULT_ANCIENT
                || id == ObjectID.POH_ALTAR_OCCULT_LUNAR
                || id == ObjectID.POH_ALTAR_OCCULT_ARCEUUS)
            verify(values, OCCULT_ALTAR);
    }

    private static void verify(Map<String, Capability> values, String key)
    {
        values.put(key, Capability.VERIFIED);
    }
}

/**
 * Converts RuneLite's live quest state into Compass's immutable snapshot.
 *
 * <p>Quest state is direct evidence. Multiple container/stat events can fire in
 * one game tick, so the complete quest scan is cached for that tick instead of
 * repeating the same reads unnecessarily.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveQuestStateReader
{
    private final Client client;
    private int cachedTick = -1;
    private QuestSnapshot cachedSnapshot;

    public QuestSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cachedSnapshot = null;
            return null;
        }

        var tick = client.getTickCount();
        if (cachedSnapshot != null && cachedTick == tick)
        {
            return cachedSnapshot;
        }

        Map<String, QuestStatus> states = new HashMap<>();

        for (Quest quest : Quest.values())
        {
            var state = quest.getState(client);
            states.put(quest.getName(), convert(state));
        }

        cachedSnapshot = new QuestSnapshot(states);
        cachedTick = tick;
        return cachedSnapshot;
    }

    private QuestStatus convert(QuestState state)
    {
        if (state == QuestState.FINISHED)
        {
            return QuestStatus.COMPLETE;
        }
        if (state == QuestState.IN_PROGRESS)
        {
            return QuestStatus.IN_PROGRESS;
        }
        if (state == QuestState.NOT_STARTED)
        {
            return QuestStatus.NOT_STARTED;
        }
        return QuestStatus.UNKNOWN;
    }
}

/**
 * Reads currently usable Rune pouch contents from the same varbits and enum
 * mapping used by RuneLite's Clue Scroll plugin.
 *
 * <p>The pouch is live storage, not persistent ownership. If no recognized
 * usable pouch is observed in inventory, stale remembered pouch contents are
 * removed while every unrelated storage capability is preserved.</p>
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveRunePouchStateReader
{
    private static final int[] AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1,
            VarbitID.RUNE_POUCH_QUANTITY_2,
            VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4
    };

    private static final int[] RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1,
            VarbitID.RUNE_POUCH_TYPE_2,
            VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4
    };

    private final Client client;
    private final ItemManager itemManager;

    public StorageSnapshot merge(
            StorageSnapshot base,
            ItemsState inventory)
    {
        boolean usable = hasUsableRunePouch(inventory)
                && client.getGameState() == GameState.LOGGED_IN;
        List<ItemState> liveContents = usable
                ? readContents() : null;
        return mergeObserved(base, usable, liveContents);
    }

    /** Pure merge seam used by tests and protects unrelated remembered state. */
    static StorageSnapshot mergeObserved(
            StorageSnapshot base,
            boolean usablePouchObserved,
            List<ItemState> liveContents)
    {
        StorageSnapshot source = base == null
                ? StorageSnapshot.unknown() : base;
        EnumMap<StorageKind, Capability> states =
                new EnumMap<>(StorageKind.class);
        states.putAll(source.getStates());
        EnumMap<StorageKind, List<ItemState>> contents =
                new EnumMap<>(StorageKind.class);
        for (Map.Entry<StorageKind, List<ItemState>> entry
                : source.getObservedContents().entrySet())
        {
            contents.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        if (!usablePouchObserved)
        {
            states.remove(StorageKind.RUNE_POUCH);
            contents.remove(StorageKind.RUNE_POUCH);
            return new StorageSnapshot(states, contents);
        }

        states.put(StorageKind.RUNE_POUCH, Capability.VERIFIED);
        contents.put(StorageKind.RUNE_POUCH,
                liveContents == null
                        ? new ArrayList<>()
                        : new ArrayList<>(liveContents));
        return new StorageSnapshot(states, contents);
    }

    List<ItemState> readContents()
    {
        List<ItemState> result = new ArrayList<>(AMOUNT_VARBITS.length);
        var runeEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        if (runeEnum == null) return result;

        for (int i = 0; i < AMOUNT_VARBITS.length; i++)
        {
            var amount = client.getVarbitValue(AMOUNT_VARBITS[i]);
            if (amount <= 0) continue;

            var runeType = client.getVarbitValue(RUNE_VARBITS[i]);
            if (runeType == 0) continue;

            var itemId = runeEnum.getIntValue(runeType);
            if (itemId <= 0) continue;
            var name = itemManager.getItemComposition(itemId).getName();
            if (name == null || name.trim().isEmpty()) continue;
            result.add(new ItemState(itemId, name, amount));
        }
        return result;
    }

    static boolean hasUsableRunePouch(ItemsState inventory)
    {
        if (inventory == null || inventory.getItems() == null) return false;
        for (ItemState item : inventory.getItems())
        {
            if (item == null || item.quantity <= 0 || item.getName() == null)
                continue;
            var name = item.getName().trim().toLowerCase(Locale.ROOT);

            // Fail closed on identity. Generic substring matching could treat a
            // future note/token/placeholder containing "rune pouch" as an
            // actually usable pouch and leak stale varbit runes into planning.
            if (name.equals("rune pouch")
                    || name.equals(get(1703)))
            {
                return true;
            }
        }
        return false;
    }
}

/** Reads only Sailing progression that RuneLite exposes as stable live state. */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveSailingStateReader
{
    private static final int[] PORT_TASK_VARPS = {
        VarPlayerID.PORT_TASKS_0, VarPlayerID.PORT_TASKS_1,
        VarPlayerID.PORT_TASKS_2, VarPlayerID.PORT_TASKS_3,
        VarPlayerID.PORT_TASKS_4
    };
    private static final int[] BOAT_DATA_VARPS = {
        VarPlayerID.SAILING_BOAT_1_DATA, VarPlayerID.SAILING_BOAT_2_DATA,
        VarPlayerID.SAILING_BOAT_3_DATA, VarPlayerID.SAILING_BOAT_4_DATA,
        VarPlayerID.SAILING_BOAT_5_DATA
    };

    private final Client client;
    private int cachedTick = -1;
    private SailingSnapshot cached;

    public SailingSnapshot read(QuestSnapshot quests)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cached = null;
            return null;
        }
        var tick = client.getTickCount();
        if (cached != null && cachedTick == tick) return cached;

        Set<String> ports = new HashSet<>();
        Set<String> activities = new HashSet<>();
        if (quests != null
                && quests.statusOf("Pandemonium") == QuestStatus.COMPLETE)
        {
            // Pandemonium deterministically grants the starter raft, Captain's
            // log access, and the route between its island and Port Sarim.
            ports.add(SailingSnapshot.PORT_SARIM);
            ports.add(SailingSnapshot.PORT_PANDEMONIUM);
            activities.add(SailingSnapshot.ACTIVITY_COURIER);
            activities.add(SailingSnapshot.ACTIVITY_SEA_CHARTING);
        }
        if (anyPositive(BOAT_DATA_VARPS))
            activities.add(SailingSnapshot.ACTIVITY_BOAT_OWNED);
        if (anyPositive(PORT_TASK_VARPS))
            activities.add(SailingSnapshot.ACTIVITY_ACTIVE_PORT_TASK);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_TEMPOR_TANTRUM_COMPLETED,
                SailingSnapshot.TRIAL_TEMPOR_COMPLETE);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_JUBBLY_JIVE_COMPLETED,
                SailingSnapshot.TRIAL_JUBBLY_COMPLETE);
        addIfPositive(activities,
                VarPlayerID.SAILING_BT_TRIAL_GWENITH_GLIDE_COMPLETED,
                SailingSnapshot.TRIAL_GWENITH_COMPLETE);

        cached = new SailingSnapshot(ports, activities,
                Confidence.VERIFIED);
        cachedTick = tick;
        return cached;
    }

    private boolean anyPositive(int[] varps)
    {
        for (int varp : varps)
            if (client.getVarpValue(varp) > 0) return true;
        return false;
    }

    private void addIfPositive(Set<String> activities, int varp, String id)
    {
        if (client.getVarpValue(varp) > 0) activities.add(id);
    }
}

/**
 * Reads the same authoritative Slayer assignment state exposed to RuneLite.
 *
 * <p>This avoids scraping chat text or relying on the user to type the current
 * task. Task count, task row, area, and points all come from live client state.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
class LiveSlayerStateReader
{
    private static final int BOSS_TASK_ID = 98;
    // RuneLite's own Slayer plugin uses value 7 to select the separate
    // Krystilia streak. No other numeric master mapping is inferred here.
    private static final int KRYSTILIA_MASTER_ID = 7;
    private static final int MORTIMER_MASTER_ID = 10;
    private static final String[] MASTER_NAMES = {null, "Turael/Aya",
            "Mazchna/Achtryn", "Vannaka", "Chaeldar", "Duradel/Kuradal",
            "Nieve/Steve", "Krystilia", "Konar quo Maten", "Spria",
            "Mortimer"};
    /** Block-list varbits indexed by the live Slayer master id. */
    private static final int[][] BLOCKS = {
            null,
            {VarbitID.SLAYER_BLOCKED_TURAEL_1, VarbitID.SLAYER_BLOCKED_TURAEL_2, VarbitID.SLAYER_BLOCKED_TURAEL_3, VarbitID.SLAYER_BLOCKED_TURAEL_4, VarbitID.SLAYER_BLOCKED_TURAEL_5, VarbitID.SLAYER_BLOCKED_TURAEL_6, VarbitID.SLAYER_BLOCKED_TURAEL_DIARY},
            {VarbitID.SLAYER_BLOCKED_MAZCHNA_1, VarbitID.SLAYER_BLOCKED_MAZCHNA_2, VarbitID.SLAYER_BLOCKED_MAZCHNA_3, VarbitID.SLAYER_BLOCKED_MAZCHNA_4, VarbitID.SLAYER_BLOCKED_MAZCHNA_5, VarbitID.SLAYER_BLOCKED_MAZCHNA_6, VarbitID.SLAYER_BLOCKED_MAZCHNA_DIARY},
            {VarbitID.SLAYER_BLOCKED_VANNAKA_1, VarbitID.SLAYER_BLOCKED_VANNAKA_2, VarbitID.SLAYER_BLOCKED_VANNAKA_3, VarbitID.SLAYER_BLOCKED_VANNAKA_4, VarbitID.SLAYER_BLOCKED_VANNAKA_5, VarbitID.SLAYER_BLOCKED_VANNAKA_6, VarbitID.SLAYER_BLOCKED_VANNAKA_DIARY},
            {VarbitID.SLAYER_BLOCKED_CHAELDAR_1, VarbitID.SLAYER_BLOCKED_CHAELDAR_2, VarbitID.SLAYER_BLOCKED_CHAELDAR_3, VarbitID.SLAYER_BLOCKED_CHAELDAR_4, VarbitID.SLAYER_BLOCKED_CHAELDAR_5, VarbitID.SLAYER_BLOCKED_CHAELDAR_6, VarbitID.SLAYER_BLOCKED_CHAELDAR_DIARY},
            {VarbitID.SLAYER_BLOCKED_DURADEL_1, VarbitID.SLAYER_BLOCKED_DURADEL_2, VarbitID.SLAYER_BLOCKED_DURADEL_3, VarbitID.SLAYER_BLOCKED_DURADEL_4, VarbitID.SLAYER_BLOCKED_DURADEL_5, VarbitID.SLAYER_BLOCKED_DURADEL_6, VarbitID.SLAYER_BLOCKED_DURADEL_DIARY},
            {VarbitID.SLAYER_BLOCKED_NIEVE_1, VarbitID.SLAYER_BLOCKED_NIEVE_2, VarbitID.SLAYER_BLOCKED_NIEVE_3, VarbitID.SLAYER_BLOCKED_NIEVE_4, VarbitID.SLAYER_BLOCKED_NIEVE_5, VarbitID.SLAYER_BLOCKED_NIEVE_6, VarbitID.SLAYER_BLOCKED_NIEVE_DIARY},
            {VarbitID.SLAYER_BLOCKED_KRYSTILIA_1, VarbitID.SLAYER_BLOCKED_KRYSTILIA_2, VarbitID.SLAYER_BLOCKED_KRYSTILIA_3, VarbitID.SLAYER_BLOCKED_KRYSTILIA_4, VarbitID.SLAYER_BLOCKED_KRYSTILIA_5, VarbitID.SLAYER_BLOCKED_KRYSTILIA_6, VarbitID.SLAYER_BLOCKED_KRYSTILIA_DIARY},
            {VarbitID.SLAYER_BLOCKED_KONAR_1, VarbitID.SLAYER_BLOCKED_KONAR_2, VarbitID.SLAYER_BLOCKED_KONAR_3, VarbitID.SLAYER_BLOCKED_KONAR_4, VarbitID.SLAYER_BLOCKED_KONAR_5, VarbitID.SLAYER_BLOCKED_KONAR_6, VarbitID.SLAYER_BLOCKED_KONAR_DIARY},
            {VarbitID.SLAYER_BLOCKED_TURAEL_1, VarbitID.SLAYER_BLOCKED_TURAEL_2, VarbitID.SLAYER_BLOCKED_TURAEL_3, VarbitID.SLAYER_BLOCKED_TURAEL_4, VarbitID.SLAYER_BLOCKED_TURAEL_5, VarbitID.SLAYER_BLOCKED_TURAEL_6, VarbitID.SLAYER_BLOCKED_TURAEL_DIARY},
            {VarbitID.SLAYER_BLOCKED_MORTIMER_1, VarbitID.SLAYER_BLOCKED_MORTIMER_2}
    };

    private final Client client;
    private int cachedTick = -1;
    private SlayerSnapshot cached;

    public SlayerSnapshot read()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            cachedTick = -1;
            cached = null;
            return null;
        }

        var tick = client.getTickCount();
        if (cached != null && cachedTick == tick)
        {
            return cached;
        }

        var amount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
        var points = max(0, client.getVarbitValue(VarbitID.SLAYER_POINTS));
        var masterId = client.getVarbitValue(VarbitID.SLAYER_MASTER);
        List<SlayerTaskOffer> offers = amount <= 0
                ? readMortimerOffers() : emptyList();
        if (!offers.isEmpty()) masterId = MORTIMER_MASTER_ID;
        var masterName = masterName(masterId);
        var rewards = readRewards();
        var streak = streak(masterId);
        var questPoints = max(0, client.getVarpValue(VarPlayerID.QP));
        boolean lumbridgeElite = client.getVarbitValue(
                VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0;
        int blockCapacity = masterId == MORTIMER_MASTER_ID ? 2
                : SlayerPointEconomy.blockCapacity(questPoints, lumbridgeElite);
        var occupiedBlockSlots = occupiedBlockSlots(masterId, blockCapacity);
        boolean mortimerIntroduced = client.getVarbitValue(
                VarbitID.MORTIMER_INTRODUCTION) > 0;
        if (amount <= 0)
        {
            cached = new SlayerSnapshot(
                    null, 0, masterName, null, points, streak, questPoints,
                    blockCapacity, occupiedBlockSlots, rewards, offers,
                    mortimerIntroduced,
                    Confidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }

        try
        {
            String taskName = taskName(
                    client.getVarpValue(VarPlayerID.SLAYER_TARGET),
                    client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
            if (taskName == null) return unresolved(amount, masterName,
                    points, streak, questPoints, blockCapacity, rewards,
                    mortimerIntroduced, tick);

            String taskLocation = null;
            var areaId = client.getVarpValue(VarPlayerID.SLAYER_AREA);
            if (areaId > 0)
            {
                var rows = client.getDBRowsByValue(
                        DBTableID.SlayerArea.ID,
                        DBTableID.SlayerArea.COL_AREA_ID,
                        0,
                        areaId);
                if (!rows.isEmpty())
                {
                    taskLocation = (String) client.getDBTableField(
                            rows.get(0),
                            DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER,
                            0)[0];
                }
            }

            cached = new SlayerSnapshot(
                    taskName,
                    amount,
                    masterName,
                    taskLocation,
                    points,
                    streak,
                    questPoints,
                    blockCapacity,
                    occupiedBlockSlots,
                    rewards,
                    emptyList(),
                    mortimerIntroduced,
                    Confidence.VERIFIED);
            cachedTick = tick;
            return cached;
        }
        catch (RuntimeException ex)
        {
            return unresolved(amount, masterName, points, streak, questPoints,
                    blockCapacity, rewards, mortimerIntroduced, tick);
        }
    }

    public void clear()
    {
        cachedTick = -1;
        cached = null;
    }

    private SlayerSnapshot unresolved(int amount, String masterName,
            int points, Integer streak, int questPoints, int blockCapacity,
            SlayerRewardSnapshot rewards, boolean mortimerIntroduced, int tick)
    {
        cached = new SlayerSnapshot(
                null,
                amount,
                masterName,
                null,
                points,
                streak,
                questPoints,
                blockCapacity,
                null,
                rewards,
                emptyList(),
                mortimerIntroduced,
                Confidence.CHECK_NEEDED);
        cachedTick = tick;
        return cached;
    }

    private Integer streak(int masterId)
    {
        if (masterId == MORTIMER_MASTER_ID)
        {
            // RuneLite exposes Mortimer's live choices/modifiers but no public
            // separate completed-task counter. Never substitute normal streak.
            return null;
        }
        return max(0, client.getVarbitValue(
                masterId == KRYSTILIA_MASTER_ID
                        ? VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED
                        : VarbitID.SLAYER_TASKS_COMPLETED));
    }

    private List<SlayerTaskOffer> readMortimerOffers()
    {
        List<SlayerTaskOffer> offers = new ArrayList<>();
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_1,
                VarbitID.SLAYER_CHOOSE_TASK_1_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_1_MODIFIER_NEGATIVE);
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_2,
                VarbitID.SLAYER_CHOOSE_TASK_2_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_2_MODIFIER_NEGATIVE);
        addOffer(offers, VarbitID.SLAYER_CHOOSE_TASK_3,
                VarbitID.SLAYER_CHOOSE_TASK_3_BOSS_ID,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_ID,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_VALUE,
                VarbitID.SLAYER_CHOOSE_TASK_3_MODIFIER_NEGATIVE);
        return offers;
    }

    private void addOffer(List<SlayerTaskOffer> offers, int taskVarbit,
            int bossVarbit, int modifierVarbit, int valueVarbit,
            int negativeVarbit)
    {
        var taskId = client.getVarbitValue(taskVarbit);
        if (taskId <= 0) return;
        String task = null;
        String modifier = null;
        var value = client.getVarbitValue(valueVarbit);
        var negative = client.getVarbitValue(negativeVarbit) > 0;
        try
        {
            task = taskName(taskId, client.getVarbitValue(bossVarbit));
            modifier = modifierName(
                    client.getVarbitValue(modifierVarbit));
        }
        catch (RuntimeException ignored)
        {
            // Preserve the option as unresolved. Omitting it could make a
            // decoded alternative look best when the hidden option is better.
        }
        offers.add(new SlayerTaskOffer(task, modifier, value, negative));
    }

    private String taskName(int taskId, int bossId)
    {
        int taskRow;
        if (taskId == BOSS_TASK_ID)
        {
            var rows = client.getDBRowsByValue(
                    DBTableID.SlayerTaskSublist.ID,
                    DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
                    0, bossId);
            if (rows.isEmpty()) return null;
            taskRow = (Integer) client.getDBTableField(rows.get(0),
                    DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
        }
        else
        {
            var rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
                    DBTableID.SlayerTask.COL_ID, 0, taskId);
            if (rows.isEmpty()) return null;
            taskRow = rows.get(0);
        }
        Object[] values = client.getDBTableField(taskRow,
                DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
        return values == null || values.length == 0
                ? null : (String) values[0];
    }

    private String modifierName(int modifierId)
    {
        var rows = client.getDBRowsByValue(DBTableID.SlayerModifiers.ID,
                DBTableID.SlayerModifiers.COL_ID, 0, modifierId);
        if (rows.isEmpty()) return null;
        Object[] values = client.getDBTableField(rows.get(0),
                DBTableID.SlayerModifiers.COL_NAME, 0);
        return values == null || values.length == 0
                ? null : (String) values[0];
    }

    private SlayerRewardSnapshot readRewards()
    {
        Map<SlayerReward, Capability> states =
                new EnumMap<>(SlayerReward.class);
        for (SlayerReward reward : SlayerReward.values())
            states.put(reward, client.getVarbitValue(reward.getVarbitId()) > 0
                    ? Capability.VERIFIED : Capability.BLOCKED);
        return new SlayerRewardSnapshot(states);
    }

    static String masterName(int masterId)
    {
        return masterId > 0 && masterId < MASTER_NAMES.length
                ? MASTER_NAMES[masterId] : null;
    }

    private Integer occupiedBlockSlots(int masterId, int capacity)
    {
        var slots = blockVarbits(masterId);
        if (slots == null) return null;
        var occupied = 0;
        var visible = min(capacity, slots.length);
        for (int i = 0; i < visible; i++)
            if (client.getVarbitValue(slots[i]) > 0) occupied++;
        return occupied;
    }

    private static int[] blockVarbits(int masterId)
    {
        return masterId > 0 && masterId < BLOCKS.length
                ? BLOCKS[masterId] : null;
    }
}

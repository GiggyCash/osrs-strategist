package compass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.client.plugins.skillcalculator.skills.AgilityAction;
import net.runelite.client.plugins.skillcalculator.skills.ConstructionAction;
import net.runelite.client.plugins.skillcalculator.skills.CookingAction;
import net.runelite.client.plugins.skillcalculator.skills.CraftingAction;
import net.runelite.client.plugins.skillcalculator.skills.FarmingAction;
import net.runelite.client.plugins.skillcalculator.skills.FiremakingAction;
import net.runelite.client.plugins.skillcalculator.skills.FishingAction;
import net.runelite.client.plugins.skillcalculator.skills.FletchingAction;
import net.runelite.client.plugins.skillcalculator.skills.HerbloreAction;
import net.runelite.client.plugins.skillcalculator.skills.HunterAction;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.client.plugins.skillcalculator.skills.MiningAction;
import net.runelite.client.plugins.skillcalculator.skills.PrayerAction;
import net.runelite.client.plugins.skillcalculator.skills.RunecraftAction;
import net.runelite.client.plugins.skillcalculator.skills.SkillAction;
import net.runelite.client.plugins.skillcalculator.skills.SmithingAction;
import net.runelite.client.plugins.skillcalculator.skills.ThievingAction;
import net.runelite.client.plugins.skillcalculator.skills.WoodcuttingAction;
import static compass.Text.get;

/** High-value abilities loaded from the bundled catalog. */
@Singleton
final class AbilityUnlockCatalog extends IndexedCatalog<AbilityUnlockDefinition>
{
    public static final String PROVENANCE =
            Text.get(129);
    public AbilityUnlockCatalog()
    {
        super(Text.get(130), AbilityUnlockDefinition[].class,
                value -> value.id, Names::slug);
    }
    public AbilityUnlockDefinition get(String id)
    {
        return indexed(Names.slug(id));
    }
    public List<AbilityUnlockDefinition> all() { return values; }
}

/** Sourced activity strategy profiles loaded from the bundled catalog. */
@Singleton
final class ActivityStrategyKnowledgeCatalog
{
    private final List<ActivityStrategyProfile> profiles = Collections.unmodifiableList(Arrays.asList(
            BundledCatalogLoader.array(Text.get(88),
                    ActivityStrategyProfile[].class)));

    public ActivityStrategyProfile profileFor(String candidateId, AccountMode mode)
    {
        if (candidateId == null || mode == null) return null;
        ActivityStrategyProfile best = null;
        for (ActivityStrategyProfile profile : profiles)
        {
            if (!profile.supports(mode)
                    || !candidateId.startsWith(profile.getCandidatePrefix())) continue;
            if (best == null || profile.getCandidatePrefix().length()
                    > best.getCandidatePrefix().length()) best = profile;
        }
        return best;
    }
    public List<ActivityStrategyProfile> all() { return profiles; }
}

/** Verified course access data loaded from the bundled catalog. */
@Singleton
class AgilityCourseCatalog extends CatalogStore<AgilityCourseDefinition>
{
    public AgilityCourseCatalog() { super(Text.get(1605), AgilityCourseDefinition[].class); }

    public AgilityCourseDefinition wildernessCourse()
    {
        return find(AgilityCourseDefinition::isWilderness);
    }

}

/** Pinned Wiki quest-detail evidence. Runtime access is strictly local. */
final class AuthoritativeQuestEnrichmentCatalog
{
    public static final String PROVENANCE = get(39);
    private static final String RESOURCE = get(1586);
    private final Map<String, Record> records;

    public AuthoritativeQuestEnrichmentCatalog()
    {
        Map<String, Record> values = new LinkedHashMap<>();
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            record.validate();
            if (values.put(Names.words(record.name), record) != null)
                throw new IllegalStateException(get(1111) + record.name);
        }
        records = Collections.unmodifiableMap(values);
    }

    public Record recordFor(String name)
    {
        var wikiName = aliases().get(Names.words(name));
        return records.get(Names.words(wikiName == null ? name : wikiName));
    }
    public Map<String, Record> all() { return records; }
    public boolean hasStrictFieldEvidence()
    {
        for (Record record : records.values())
            if (record.hasLegacyEvidence()) return false;
        return true;
    }

    private static Map<String, String> aliases()
    {
        Map<String, String> result = new HashMap<>();
        String[][] values = {
                {get(51), get(52)},
                {get(1112), get(53)},
                {get(54), get(40)},
                {get(1113), get(41)},
                {get(1114), get(42)},
                {get(1115), get(1116)},
                {get(1117), get(43)},
                {get(1118), get(44)},
                {get(1119), get(45)},
                {get(1120), get(46)},
                {"Vale Totems", get(1121)}
        };
        for (String[] alias : values) result.put(Names.words(alias[0]), alias[1]);
        return result;
    }

    public enum EvidenceState
    {
        VALUE, NONE, NOT_APPLICABLE, SOURCE_MISSING, MISSING, PARSE_FAILURE,
        UNSUPPORTED_STRUCTURE, UNKNOWN, LEGACY_NONE;
        public boolean isEvidence()
        {
            return this == VALUE || this == NONE || this == NOT_APPLICABLE
                    || this == LEGACY_NONE;
        }
        public boolean isStrictEvidence()
        {
            return this == VALUE || this == NONE || this == NOT_APPLICABLE;
        }
    }

    @Getter
    public static final class Record
    {
        private String name;
        private String start;
        private EvidenceState startState;
        private String requirements;
        private EvidenceState requirementsState;
        private String items;
        private EvidenceState itemsState;
        private String enemies;
        private EvidenceState combatState;
        private String rewards;
        private EvidenceState rewardsState;

        public EvidenceState getRequirementState() { return requirementsState; }
        public EvidenceState getItemState() { return itemsState; }
        public EvidenceState getRewardState() { return rewardsState; }
        public boolean hasStartEvidence() { return startState.isEvidence(); }
        public boolean hasRequirementEvidence() { return requirementsState.isEvidence(); }
        public boolean hasItemEvidence() { return itemsState.isEvidence(); }
        public boolean hasCombatEvidence() { return combatState.isEvidence(); }
        public boolean hasRewardEvidence() { return rewardsState.isEvidence(); }
        public boolean hasStrictItemEvidence() { return itemsState.isStrictEvidence(); }
        public boolean hasStrictRequirementEvidence() { return requirementsState.isStrictEvidence(); }
        public boolean hasStrictCombatEvidence() { return combatState.isStrictEvidence(); }
        public boolean hasStrictRewardEvidence() { return rewardsState.isStrictEvidence(); }
        public boolean hasLegacyEvidence()
        {
            return startState == EvidenceState.LEGACY_NONE
                    || requirementsState == EvidenceState.LEGACY_NONE
                    || itemsState == EvidenceState.LEGACY_NONE
                    || combatState == EvidenceState.LEGACY_NONE
                    || rewardsState == EvidenceState.LEGACY_NONE;
        }
        private void validate()
        {
            if (name == null || name.trim().isEmpty())
                throw new IllegalStateException(get(1122));
            validate(start, startState, "start");
            validate(requirements, requirementsState, "requirements");
            validate(items, itemsState, "items");
            validate(enemies, combatState, "combat");
            validate(rewards, rewardsState, "rewards");
        }
        private static void validate(String value, EvidenceState state, String field)
        {
            if (state == null || state == EvidenceState.LEGACY_NONE)
                throw new IllegalStateException("Invalid " + field + " evidence state");
            var blank = value == null || value.trim().isEmpty();
            if ((state == EvidenceState.VALUE) == blank
                    || ((state == EvidenceState.NONE || state == EvidenceState.NOT_APPLICABLE
                    || state == EvidenceState.SOURCE_MISSING || state == EvidenceState.PARSE_FAILURE
                    || state == EvidenceState.UNSUPPORTED_STRUCTURE) && !blank))
                throw new IllegalStateException(get(1123) + field);
        }
    }
}

/** Canonical direct quest and skill requirements from the pinned Wiki import. */
final class AuthoritativeQuestRequirementCatalog
{
    public static final String PROVENANCE = Text.get(55);
    private static final String RESOURCE = Text.get(1641);
    private final Map<String, Record> records;

    public AuthoritativeQuestRequirementCatalog()
    {
        Map<String, Record> values = new LinkedHashMap<>();
        for (Record record : BundledCatalogLoader.array(RESOURCE, Record[].class))
        {
            record.freeze();
            var key = Names.words(record.name);
            if (key.isEmpty() || values.put(key, record) != null)
                throw new IllegalStateException(Text.get(1124) + record.name);
        }
        records = Collections.unmodifiableMap(values);
    }

    public Map<String, Record> all() { return records; }

    @Getter
    public static final class Record
    {
        private String name;
        private List<String> prerequisites;
        private Map<Skill, Integer> skills;
        private int questPoints;
        private List<String> otherChecks;
        private String startLocation;

        private void freeze()
        {
            prerequisites = immutable(prerequisites);
            otherChecks = immutable(otherChecks);
            EnumMap<Skill, Integer> skillCopy = new EnumMap<>(Skill.class);
            if (skills != null) skillCopy.putAll(skills);
            skills = Collections.unmodifiableMap(skillCopy);
            startLocation = startLocation == null ? "" : startLocation;
        }
        private static List<String> immutable(List<String> values)
        {
            return values == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}

/** Complete pinned RuneLite diary task/prerequisite catalogue. */
@Singleton
final class DiaryTaskCatalog
{
    public static final int EXPECTED_TASKS = 378;
    public static final String PROVENANCE =
            Text.get(213);
    private static final Pattern SKILL = Pattern.compile(
            Text.get(1616));
    private static final Pattern QUEST = Pattern.compile(
            Text.get(1617));
    private static final Pattern COMBAT = Pattern.compile(
            Text.get(1618));
    private static final Pattern QUEST_POINTS = Pattern.compile(
            Text.get(1619));

    private final List<DiaryTaskDefinition> tasks;

    public DiaryTaskCatalog()
    {
        tasks = Collections.unmodifiableList(load());
        if (tasks.size() != EXPECTED_TASKS)
            throw new IllegalStateException("Expected " + EXPECTED_TASKS
                    + Text.get(1128) + tasks.size());
    }

    public List<DiaryTaskDefinition> all() { return tasks; }

    public List<DiaryTaskDefinition> forTier(String region, DiaryTier tier)
    {
        List<DiaryTaskDefinition> result = new ArrayList<>();
        for (DiaryTaskDefinition task : tasks)
            if (task.getRegion().equalsIgnoreCase(region)
                    && task.getTier() == tier) result.add(task);
        return Collections.unmodifiableList(result);
    }

    public Map<String, Map<DiaryTier, Integer>> census()
    {
        Map<String, Map<DiaryTier, Integer>> result = new LinkedHashMap<>();
        for (DiaryTaskDefinition task : tasks)
        {
            Map<DiaryTier, Integer> tiers = result.computeIfAbsent(
                    task.getRegion(), key -> new LinkedHashMap<>());
            tiers.put(task.getTier(), tiers.getOrDefault(task.getTier(), 0) + 1);
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<DiaryTaskDefinition> load()
    {
        InputStream stream = DiaryTaskCatalog.class.getResourceAsStream(
                Text.get(1620));
        if (stream == null)
            throw new IllegalStateException(Text.get(1129));
        List<DiaryTaskDefinition> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)))
        {
            String line;
            var number = 0;
            while ((line = reader.readLine()) != null)
            {
                number++;
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                var fields = line.split("\\t", 4);
                if (fields.length != 4)
                    throw new IllegalStateException(Text.get(1130)
                            + number);
                DiaryTier tier = DiaryTier.valueOf(
                        fields[1].toUpperCase(Locale.ROOT));
                result.add(new DiaryTaskDefinition(fields[0], tier, fields[2],
                        requirements(fields[3])));
            }
        }
        catch (IOException | IllegalArgumentException ex)
        {
            throw new IllegalStateException(Text.get(1131), ex);
        }
        return result;
    }

    private static List<DiaryTaskRequirement> requirements(String raw)
    {
        List<DiaryTaskRequirement> result = new ArrayList<>();
        if (raw.contains(Text.get(1621)))
        {
            result.add(DiaryTaskRequirement.alternative(
                    Text.get(214) + raw));
            return result;
        }
        var skill = SKILL.matcher(raw);
        while (skill.find())
            result.add(DiaryTaskRequirement.skill(
                    Skill.valueOf(skill.group(1)),
                    Integer.parseInt(skill.group(2))));
        var quest = QUEST.matcher(raw);
        while (quest.find())
        {
            var identity = Quest.valueOf(quest.group(1));
            result.add(DiaryTaskRequirement.quest(identity.getName(),
                    Boolean.parseBoolean(quest.group(2))));
        }
        var combat = COMBAT.matcher(raw);
        while (combat.find()) result.add(DiaryTaskRequirement.combat(
                Integer.parseInt(combat.group(1))));
        var points = QUEST_POINTS.matcher(raw);
        while (points.find()) result.add(DiaryTaskRequirement.questPoints(
                Integer.parseInt(points.group(1))));
        return result;
    }
}

/** Verified Farming access evidence loaded from the bundled catalog. */
@Singleton
class FarmingAccessCatalog extends CatalogStore<FarmingAccessDefinition>
{
    public FarmingAccessCatalog() { super(Text.get(1704), FarmingAccessDefinition[].class); }

    public FarmingAccessDefinition forRegion(int regionId)
    {
        return find(value -> value.getRegionIds().contains(regionId));
    }

}

/** Farming run patches loaded from the bundled catalog. */
@Singleton
class FarmingRunCatalog extends CatalogStore<FarmingRunPatchDefinition>
{
    public FarmingRunCatalog() { super(Text.get(218), FarmingRunPatchDefinition[].class); }
    public List<FarmingRunPatchDefinition> forRegion(int regionId)
    {
        return filter(patch -> patch.matchesRegion(regionId));
    }
}

/** Level-aware Farming resource definitions. */
@Singleton
class FarmingSupplyCatalog
{
    private static final SupplyOption[] HERB_SEEDS = {
            option(9, ItemID.GUAM_SEED),
            option(14, ItemID.MARRENTILL_SEED),
            option(19, ItemID.TARROMIN_SEED),
            option(26, ItemID.HARRALANDER_SEED),
            option(32, ItemID.RANARR_SEED),
            option(38, ItemID.TOADFLAX_SEED),
            option(44, ItemID.IRIT_SEED),
            option(50, ItemID.AVANTOE_SEED),
            option(56, ItemID.KWUARM_SEED),
            option(62, ItemID.SNAPDRAGON_SEED),
            option(67, ItemID.CADANTINE_SEED),
            option(73, ItemID.LANTADYME_SEED),
            option(79, ItemID.DWARF_WEED_SEED),
            option(85, ItemID.TORSTOL_SEED)
    };

    private static final SupplyOption[] TREE_SAPLINGS = {
            option(15, ItemID.PLANTPOT_OAK_SAPLING),
            option(30, ItemID.PLANTPOT_WILLOW_SAPLING),
            option(45, ItemID.PLANTPOT_MAPLE_SAPLING),
            option(60, ItemID.PLANTPOT_YEW_SAPLING),
            option(75, ItemID.PLANTPOT_MAGIC_TREE_SAPLING)
    };

    public ResourceRequirement rake()
    {
        return new ResourceRequirement("resource:rake", "Rake", 1, ItemID.RAKE);
    }

    public ResourceRequirement dibber()
    {
        return new ResourceRequirement("resource:dibber", "Seed dibber", 1, ItemID.DIBBER);
    }

    public ResourceRequirement spade()
    {
        return new ResourceRequirement("resource:spade", "Spade", 1, ItemID.SPADE);
    }

    public ResourceRequirement herbSeedsForLevel(int level)
    {
        return new ResourceRequirement(
                Text.get(1698), Text.get(1699), 1,
                unlockedItemIds(HERB_SEEDS, level));
    }

    public ResourceRequirement potatoSeeds()
    {
        return new ResourceRequirement(
                Text.get(1700), Text.get(1133), 3,
                ItemID.POTATO_SEED);
    }

    public ResourceRequirement watermelonSeeds()
    {
        return new ResourceRequirement(
                Text.get(1701), Text.get(1134), 3,
                ItemID.WATERMELON_SEED);
    }

    public ResourceRequirement treeSaplingsForLevel(int level)
    {
        return new ResourceRequirement(
                Text.get(1702), Text.get(1135), 1,
                unlockedItemIds(TREE_SAPLINGS, level));
    }

    private static int[] unlockedItemIds(SupplyOption[] options, int level)
    {
        List<Integer> ids = new ArrayList<>();
        for (SupplyOption option : options)
        {
            if (level >= option.level) ids.add(option.itemId);
        }
        var result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) result[i] = ids.get(i);
        return result;
    }

    private static SupplyOption option(int level, int itemId)
    {
        return new SupplyOption(level, itemId);
    }

    private static final class SupplyOption
    {
        private final int level;
        private final int itemId;
        private SupplyOption(int level, int itemId)
        {
            this.level = level;
            this.itemId = itemId;
        }
    }
}

/** Gear acquisition chains loaded from the bundled catalog. */
@Singleton
class GearAcquisitionCatalog extends IndexedCatalog<GearAcquisitionRoute>
{
    public static final String PROVENANCE = Text.get(249);
    public GearAcquisitionCatalog()
    {
        super(Text.get(250), GearAcquisitionRoute[].class,
                GearAcquisitionRoute::getItemName, Names::words);
    }
    public GearAcquisitionRoute forItem(String itemName) { return indexed(Names.words(itemName)); }
    public List<GearAcquisitionRoute> all() { return values; }

}

/** Encounter-context gear progression loaded from the bundled catalog. */
@Singleton
class GearProgressionCatalog extends CatalogStore<GearProgressionEntry>
{
    public GearProgressionCatalog() { super(Text.get(295), GearProgressionEntry[].class); }
    public List<GearProgressionEntry> forStyle(CombatStyle style)
    {
        return filter(entry -> entry.getStyle() == style);
    }
    public List<GearProgressionEntry> forContext(String contextId)
    {
        return filter(entry -> entry.getContextId().equals(contextId));
    }
}

/**
 * Executable subset of the pinned authoritative quest item evidence.
 * Complex prose remains visible as explicit verification rather than guessed.
 */
final class ImportedQuestItemRequirementCatalog
{
    private static final String RESOURCE = Text.get(1725);
    private final Map<String, Result> requirements;

    public ImportedQuestItemRequirementCatalog()
    {
        Map<String, Result> result = new LinkedHashMap<>();
        for (Entry entry : BundledCatalogLoader.array(RESOURCE, Entry[].class))
        {
            if (entry.quest == null || entry.result == null)
                throw new IllegalStateException(Text.get(1137) + RESOURCE);
            entry.result.freeze();
            if (result.put(Names.words(entry.quest), entry.result) != null)
                throw new IllegalStateException(Text.get(1138) + entry.quest);
        }
        requirements = Collections.unmodifiableMap(result);
    }

    public Result resultFor(String questName)
    {
        return requirements.get(Names.words(questName));
    }

    public int questCount() { return requirements.size(); }

    public long fullyExecutableCount()
    {
        return requirements.values().stream()
                .filter(Result::isFullyExecutable).count();
    }

    public long partiallyExecutableCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() != null
                        && !result.isFullyExecutable()).count();
    }

    public long rawOnlyCount()
    {
        return requirements.values().stream()
                .filter(result -> result.getExpression() == null
                        && !result.getUnresolved().isEmpty()).count();
    }


    private static final class Entry
    {
        private String quest;
        private Result result;
    }

    /** Immutable executable evidence generated from the pinned source snapshot. */
    public static final class Result
    {
        private ItemRequirementExpression expression;
        private List<String> unresolved;
        private int parsedLineCount;

        private void freeze()
        {
            expression = expression == null ? null : expression.freeze();
            unresolved = unresolved == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(unresolved));
        }

        public ItemRequirementExpression getExpression() { return expression; }
        public List<String> getUnresolved() { return unresolved; }
        public int getParsedLineCount() { return parsedLineCount; }
        public boolean isFullyExecutable() { return unresolved.isEmpty(); }
        public boolean isDeterministicallyExecutable()
        {
            return unresolved.isEmpty() && countChecks(expression) == 0;
        }
        public int getCheckNeededExpressionCount() { return countChecks(expression); }
        private static int countChecks(ItemRequirementExpression value)
        {
            if (value == null) return 0;
            var count = value.getKind() == ItemRequirementExpression.Kind.CHECK_NEEDED ? 1 : 0;
            for (ItemRequirementExpression child : value.getChildren())
                count += countChecks(child);
            return count;
        }
    }
}

/** Audited infrastructure milestones loaded from the bundled catalog. */
@Singleton
final class InfrastructureMilestoneCatalog
{
    public static final String AUDITED_AT = "2026-08-29";
    public static final List<String> PROVENANCE_URLS = Collections.unmodifiableList(Arrays.asList(
            Text.get(317),
            Text.get(319),
            Text.get(320),
            Text.get(321),
            Text.get(322),
            Text.get(323),
            Text.get(324),
            Text.get(325),
            Text.get(326)));
    private final Map<String, InfrastructureMilestone> milestones;

    public InfrastructureMilestoneCatalog()
    {
        Map<String, InfrastructureMilestone> values = new LinkedHashMap<>();
        for (InfrastructureMilestone value : BundledCatalogLoader.array(
                Text.get(318),
                InfrastructureMilestone[].class))
            if (values.put(value.id, value) != null)
                throw new IllegalStateException(Text.get(1258) + value.id);
        milestones = Collections.unmodifiableMap(values);
    }

    public InfrastructureMilestone get(String id)
    {
        return id == null ? null : milestones.get(id);
    }

    public List<InfrastructureMilestone> all()
    {
        return Collections.unmodifiableList(new ArrayList<>(milestones.values()));
    }
}

/** Deterministic execution profiles loaded from the required bundled catalog. */
@Singleton
class MethodExecutionProfileCatalog extends IndexedCatalog<MethodProfile>
{
    private static final String RESOURCE = Text.get(371);
    public MethodExecutionProfileCatalog()
    {
        super(RESOURCE, MethodProfile[].class, MethodProfile::getMethodId);
        for (MethodProfile profile : values) if (profile.getActionTerms() == null)
            throw new IllegalStateException(Text.get(1224) + RESOURCE);
    }

    public MethodProfile forMethod(String methodId) { return indexed(methodId); }
    public Map<String, MethodProfile> all() { return index; }
}

/** Audited route locations loaded from the bundled catalog. */
@Singleton
final class MethodLocationCatalog extends IndexedCatalog<MethodLocationProfile>
{
    public static final String ECTOFUNTUS_SOURCE = Text.get(373);
    public static final String FRUIT_TREE_SOURCE = Text.get(374);
    public static final String TREE_PATCH_SOURCE = Text.get(375);
    public static final String FLY_FISHING_SOURCE = Text.get(376);
    public MethodLocationCatalog()
    {
        super(Text.get(377), MethodLocationProfile[].class,
                MethodLocationProfile::getMethodId);
    }
    public MethodLocationProfile forMethod(String methodId)
    {
        return indexed(methodId);
    }
    public Map<String, MethodLocationProfile> all() { return index; }
}

/**
 * Indexed sourced method strategy. Shared methods stay shared; material
 * account differences are represented before ranking.
 */
@Singleton
final class MethodStrategyKnowledgeCatalog
{
    private static final Set<AccountMode> ALL_KNOWN =
            EnumSet.allOf(AccountMode.class);
    private static final Set<String> CONVENTIONAL_BANK_LOOPS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    Text.get(1863),
                    Text.get(1735), Text.get(1864),
                    Text.get(1577), "cooking_wines",
                    Text.get(1771), "mining_mlm",
                    Text.get(1636),
                    Text.get(1865),
                    Text.get(1866), Text.get(1867),
                    Text.get(1868), Text.get(1631),
                    Text.get(1869),
                    Text.get(1870), Text.get(1871),
                    "crafting_gems", "crafting_dhide", "fletching_bows",
                    Text.get(1872), Text.get(1873),
                    Text.get(1874),
                    Text.get(1579), Text.get(1875),
                    Text.get(1876), Text.get(1877),
                    Text.get(1878), Text.get(1879),
                    Text.get(1880), Text.get(1633),
                    Text.get(1634), "thieving_vyres")));

    private final Map<String, java.util.List<MethodStrategyProfile>> exact =
            new HashMap<>();
    private final Map<String, MethodStrategyProfile> generated =
            new ConcurrentHashMap<>();
    private final MethodExecutionProfileCatalog executionProfiles =
            new MethodExecutionProfileCatalog();

    public MethodStrategyKnowledgeCatalog()
    {
        for (MethodStrategyProfile profile : BundledCatalogLoader.array(
                Text.get(384),
                MethodStrategyProfile[].class))
        {
            if (profile.getMethodId() == null || profile.getTier() == null)
                throw new IllegalStateException(Text.get(385));
            addExact(profile);
        }
    }

    public MethodStrategyProfile profileFor(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode)
    {
        if (method == null || metadata == null || mode == null) return null;
        java.util.List<MethodStrategyProfile> specific = exact.get(
                method.id);
        if (specific != null)
        {
            MethodStrategyProfile selected = null;
            for (MethodStrategyProfile profile : specific)
                if (profile.supports(mode)
                        && (selected == null || profile.getTier().ordinal()
                                < selected.getTier().ordinal()))
                    selected = profile;
            return selected;
        }

        var bankLoop = CONVENTIONAL_BANK_LOOPS.contains(method.id);
        if (mode == AccountMode.UNKNOWN && bankLoop) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN
                && (bankLoop || !metadata.isUimFriendly())) return null;

        var modes = EnumSet.copyOf(ALL_KNOWN);
        if (!metadata.isUimFriendly() || bankLoop)
            modes.remove(AccountMode.ULTIMATE_IRONMAN);
        if (!modes.contains(mode)) return null;

        var key = mode.name() + ':' + method.id;
        return generated.computeIfAbsent(key, ignored -> genericProfile(
                method, metadata, mode, bankLoop, modes,
                executionProfiles.forMethod(method.id)));
    }

    private static MethodStrategyProfile genericProfile(TrainingMethod method,
            TrainingMethodMetadata metadata, AccountMode mode,
            boolean bankLoop, Set<AccountMode> modes,
            MethodProfile executionProfile)
    {
        StrategySourceId source = accountSkillSource(
                method.getSkill(), mode, metadata.isFreeToPlayAllowed());
        String reason = metadata.isSelfSourceFriendly() && mode.isIronLike()
                ? Text.get(386)
                : Text.get(387);
        return new MethodStrategyProfile(method.id,
                StrategyKnowledgeTier.VERIFIED_SHARED, modes,
                bankLoop ? MethodBankingBehavior.CONVENTIONAL_BANK_LOOP
                        : MethodBankingBehavior.NONE,
                typedFootprint(method, metadata, executionProfile),
                metadata.isSelfSourceFriendly() && mode.isIronLike() ? 0.55 : 0.35,
                reason, Collections.singletonList(source));
    }

    /**
     * Conservative family defaults use typed skill/input/setup properties.
     * Account-specific routes with materially different behavior stay in the
     * exact sourced records above; method names and IDs never change a
     * footprint.
     */
    private static MethodInventoryFootprint typedFootprint(
            TrainingMethod method, TrainingMethodMetadata metadata,
            MethodProfile executionProfile)
    {
        var tearsDown = method.getSetupMinutes() >= 8;
        switch (method.getSkill())
        {
            case AGILITY:
            case ATTACK:
            case STRENGTH:
            case DEFENCE:
            case HITPOINTS:
            case RANGED:
            case MAGIC:
            case SLAYER:
            case THIEVING:
                return new MethodInventoryFootprint(0, 0, 0,
                        InventoryFlow.NEUTRAL, tearsDown);
            case MINING:
            case FISHING:
            case WOODCUTTING:
                return new MethodInventoryFootprint(1, 1, 0,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            case HUNTER:
                return new MethodInventoryFootprint(3, 2, 1,
                        InventoryFlow.GROWS_NONSTACKABLE_OUTPUTS, tearsDown);
            default:
                break;
        }
        boolean consumesInputs = executionProfile != null
                && !executionProfile.getInputs().isEmpty();
        return new MethodInventoryFootprint(2, 1, 1,
                consumesInputs ? InventoryFlow.REPLACES_INPUTS_WITH_OUTPUTS
                        : InventoryFlow.NEUTRAL,
                tearsDown);
    }

    private static StrategySourceId accountSkillSource(
            net.runelite.api.Skill skill, AccountMode mode, boolean f2p)
    {
        if (skill == net.runelite.api.Skill.SAILING)
            return StrategySourceId.SAILING_TRAINING;
        if (skill == null || mode == null || !mode.isIronLike())
            return f2p ? StrategySourceId.F2P_SKILL_TRAINING
                    : StrategySourceId.GENERAL_SKILL_TRAINING;
        var prefix = mode == AccountMode.ULTIMATE_IRONMAN
                ? "UIM_" : "IRONMAN_";
        try
        {
            return StrategySourceId.valueOf(prefix + skill.name());
        }
        catch (IllegalArgumentException absentSpecializedGuide)
        {
            return mode == AccountMode.ULTIMATE_IRONMAN
                    ? StrategySourceId.UIM_SKILL_GUIDES
                    : StrategySourceId.IRONMAN_SKILL_GUIDES;
        }
    }

    private void addExact(MethodStrategyProfile profile)
    {
        exact.computeIfAbsent(profile.getMethodId(),
                ignored -> new java.util.ArrayList<>()).add(profile);
    }
}

/** Minigame definitions loaded from the bundled catalog. */
@Singleton
class MinigameCatalog extends CatalogStore<MinigameDefinition>
{
    public MinigameCatalog() { super(Text.get(1808), MinigameDefinition[].class); }
    public MinigameDefinition byId(String id)
    {
        return id == null ? null : find(value -> id.equals(value.id));
    }
}

/** Exact minigame setup profiles loaded from the bundled catalog. */
final class MinigameSetupCatalog extends IndexedCatalog<MinigameSetupProfile>
{
    public MinigameSetupCatalog()
    {
        super(Text.get(383), MinigameSetupProfile[].class,
                MinigameSetupProfile::getActivityId);
    }

    public MinigameSetupProfile forActivity(String id) { return indexed(id); }
    public int size() { return index.size(); }
}

/** Qualitative money-making methods loaded from the bundled catalog. */
@Singleton
class MoneyMakingCatalog extends CatalogStore<MoneyMakingDefinition>
{
    public MoneyMakingCatalog() { super(Text.get(1734), MoneyMakingDefinition[].class); }
    public List<MoneyMakingDefinition> forAccount(AccountMode mode)
    {
        return filter(method -> method.supports(mode));
    }
}

/** Persistent method objectives loaded from the bundled catalog. */
@Singleton
class ProgressionObjectiveCatalog extends CatalogStore<ProgressionObjectiveDefinition>
{
    public ProgressionObjectiveCatalog() { super(Text.get(440), ProgressionObjectiveDefinition[].class); }
    public ProgressionObjectiveDefinition forMethod(String methodId)
    {
        return methodId == null ? null
                : find(value -> methodId.equals(value.getMethodId()));
    }
}

/** RuneLite boss identities enriched by the audited bundled safety catalog. */
@Singleton
class PvmActivityCatalog
{
    private static final Set<String> PROFILED = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "pvm:brutus", "pvm:obor", "pvm:bryophyta", Text.get(1815),
                    "pvm:scurrius", "pvm:giant_mole", "pvm:sarachnis", "pvm:hespori",
                    "pvm:zulrah", "pvm:vorkath", Text.get(1816),
                    Text.get(1817), Text.get(1818),
                    Text.get(1819), Text.get(1820),
                    Text.get(1821), "pvm:cerberus", "pvm:araxxor", "pvm:kraken",
                    "pvm:tztok_jad", "pvm:tzkal_zuk", "pvm:sol_heredit", "pvm:nex",
                    Text.get(1822), Text.get(1823), "pvm:kreearra",
                    Text.get(1824), Text.get(1825), Text.get(1826),
                    "pvm:vardorvis", Text.get(1827),
                    Text.get(1828),
                    Text.get(1829), Text.get(1830))));
    private final List<PvmActivityDefinition> activities;
    private final Map<String, PvmActivityDefinition> byId;

    public PvmActivityCatalog()
    {
        Map<String, PvmActivityDefinition> values = new LinkedHashMap<>();
        for (PvmActivityDefinition value : BundledCatalogLoader.array(
                Text.get(1831), PvmActivityDefinition[].class))
            if (value.id == null || values.put(value.id, value) != null)
                throw new IllegalStateException(Text.get(1199));
        var bosses = 0;
        for (HiscoreSkill skill : HiscoreSkill.values())
            if (skill.getType() == HiscoreSkillType.BOSS)
            {
                bosses++;
                var id = "pvm:" + skill.name().toLowerCase(Locale.ROOT);
                if (!values.containsKey(id))
                    throw new IllegalStateException(Text.get(1200) + id);
            }
        if (values.size() != bosses)
            throw new IllegalStateException(Text.get(1201));
        byId = Collections.unmodifiableMap(values);
        activities = Collections.unmodifiableList(new ArrayList<>(values.values()));
    }

    public List<PvmActivityDefinition> all() { return activities; }
    public PvmActivityDefinition byId(String id) { return byId.get(id); }
    public PvmActivityDefinition match(String raw)
    {
        if (raw == null) return null;
        var key = normalize(raw);
        for (PvmActivityDefinition value : activities)
            if (normalize(value.id).equals(key)
                    || normalize(value.getName()).equals(key)) return value;
        return null;
    }
    public boolean hasCuratedReadinessProfile(String id)
    {
        return id != null && PROFILED.contains(id.toLowerCase(Locale.ROOT));
    }
    public int curatedReadinessProfileCount() { return PROFILED.size(); }
    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ROOT).replace("pvm:", "")
                .replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}

/** Fully locally-verifiable subset; other encounters retain readiness floors. */
@Singleton
class PvmEvidenceProfileCatalog
{
    private final Map<String, PvmEvidenceProfile> profiles = new LinkedHashMap<>();

    public PvmEvidenceProfileCatalog()
    {
        add(new PvmEvidenceProfile("pvm:brutus", "melee",
                Collections.emptyList(), 5, 0));
        add(new PvmEvidenceProfile("pvm:obor", "melee",
                Collections.singletonList("Giant key"), 5, 0));
        add(new PvmEvidenceProfile("pvm:bryophyta", "melee",
                Collections.singletonList("Mossy key"), 5, 0));
        add(new PvmEvidenceProfile("pvm:scurrius", "melee",
                Collections.emptyList(), 5, 1));
    }

    public PvmEvidenceProfile forActivity(String id)
    {
        return id == null ? null : profiles.get(id.toLowerCase());
    }

    public int size() { return profiles.size(); }

    private void add(PvmEvidenceProfile profile)
    {
        profiles.put(profile.getActivityId(), profile);
    }
}

/** Reviewable PvM preparation evidence loaded from the bundled catalog. */
@Singleton
class PvmPreparationProfileCatalog extends IndexedCatalog<PvmPreparationProfile>
{
    public static final String PROVENANCE =
            Text.get(441);
    private static final String RESOURCE = Text.get(442);
    public PvmPreparationProfileCatalog()
    {
        super(RESOURCE, PvmPreparationProfile[].class,
                PvmPreparationProfile::getActivityId);
        for (PvmPreparationProfile profile : values) if (profile.getChecks() == null)
            throw new IllegalStateException(Text.get(1164) + RESOURCE);
    }

    public PvmPreparationProfile forActivity(String id) { return indexed(id); }
    public Map<String, PvmPreparationProfile> all() { return index; }
}

/** Curated quest knowledge plus fail-closed authoritative enrichment. */
@Singleton
class QuestKnowledgeCatalog
{
    private static final String RESOURCE = Text.get(558);
    private static final Pattern REWARD_XP = Pattern.compile(
            Text.get(1919), Pattern.CASE_INSENSITIVE);
    private final Map<String, QuestDefinition> definitions = new LinkedHashMap<>();

    public QuestKnowledgeCatalog()
    {
        for (QuestDefinition definition
                : BundledCatalogLoader.array(RESOURCE, QuestDefinition[].class))
            add(definition);
        seedImportedRequirements();
    }

    private void seedImportedRequirements()
    {
        AuthoritativeQuestEnrichmentCatalog enrichment =
                new AuthoritativeQuestEnrichmentCatalog();
        for (AuthoritativeQuestRequirementCatalog.Record record
                : new AuthoritativeQuestRequirementCatalog().all().values())
        {
            if (definitionFor(record.getName()) != null) continue;
            AuthoritativeQuestEnrichmentCatalog.Record details =
                    enrichment.recordFor(record.getName());
            List<String> checks = new ArrayList<>(record.getOtherChecks());
            List<String> uncertainties = new ArrayList<>();
            if (details == null)
                uncertainties.addAll(Arrays.asList("items", "access/combat",
                        "rewards/unlocks", "start location"));
            else
            {
                addEvidenceCheck(checks, "Required items", details.getItems());
                addEvidenceCheck(checks, Text.get(1166), details.getRequirements());
                addEvidenceCheck(checks, Text.get(1920), details.getEnemies());
                if (!details.hasItemEvidence()) uncertainties.add("items");
                if (!details.hasRequirementEvidence() || !details.hasCombatEvidence())
                    uncertainties.add("access/combat");
                if (!details.hasRewardEvidence()) uncertainties.add("rewards/unlocks");
                else if (hasUnparsedCombatXp(details.getRewards(), rewardXp(details.getRewards())))
                    uncertainties.add("irreversible xp");
            }
            var start = record.getStartLocation();
            if (start.trim().isEmpty() && details != null && details.hasStartEvidence())
                start = plain(details.getStart());
            if (start.trim().isEmpty() && !uncertainties.contains("start location"))
                uncertainties.add("start location");
            List<String> unlocks = details != null && details.hasRewardEvidence()
                    ? Collections.singletonList("Quest rewards: "
                            + abbreviate(plain(details.getRewards()), 500))
                    : Collections.emptyList();
            add(new QuestDefinition(record.getName(),
                    QuestMembershipPolicy.isFreeToPlayQuest(record.getName()),
                    record.getPrerequisites(), record.getSkills(), Collections.emptyList(), null,
                    record.getQuestPoints(), checks, start, unlocks,
                    details == null ? Collections.emptyMap() : rewardXp(details.getRewards()),
                    uncertainties));
        }
    }

    private static void addEvidenceCheck(List<String> checks, String label, String value)
    {
        if (value == null || value.trim().isEmpty() || "none".equalsIgnoreCase(value.trim()))
            return;
        checks.add(label + ": " + abbreviate(plain(value), 500));
    }

    private static Map<Skill, Integer> rewardXp(String rewards)
    {
        EnumMap<Skill, Integer> result = new EnumMap<>(Skill.class);
        var matcher = REWARD_XP.matcher(rewards == null ? "" : rewards);
        while (matcher.find())
        {
            Skill skill;
            try
            {
                skill = Skill.valueOf(matcher.group(1).trim()
                        .toUpperCase(Locale.ROOT).replace(' ', '_'));
            }
            catch (IllegalArgumentException ex) { continue; }
            result.merge(skill, Integer.parseInt(matcher.group(2).replace(",", "")), Integer::sum);
        }
        return result;
    }

    private static boolean hasUnparsedCombatXp(String rewards, Map<Skill, Integer> parsed)
    {
        var text = plain(rewards).toLowerCase(Locale.ROOT);
        for (Skill skill : Arrays.asList(Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE,
                Skill.HITPOINTS, Skill.PRAYER, Skill.RANGED, Skill.MAGIC))
            if (text.matches("(?s).*\\b" + skill.getName().toLowerCase(Locale.ROOT)
                    + Text.get(1921)) && !parsed.containsKey(skill))
                return true;
        return false;
    }

    private static String plain(String wiki)
    {
        if (wiki == null) return "";
        return wiki.replaceAll("(?s)<!--.*?-->", " ")
                .replaceAll(Text.get(1922), "$1")
                .replaceAll(Text.get(1923), "$2 $1")
                .replaceAll("\\{\\{[^}]+}}", " ").replaceAll("'{2,}", "")
                .replaceAll("[\\r\\n*#]+", " ").replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static String abbreviate(String value, int length)
    {
        return value.length() <= length ? value : value.substring(0, length - 1).trim() + "…";
    }

    public QuestDefinition definitionFor(String name) { return definitions.get(Names.words(name)); }
    public Map<String, QuestDefinition> all() { return Collections.unmodifiableMap(definitions); }

    private void add(QuestDefinition definition)
    {
        if (definition == null || definition.getName() == null)
            throw new IllegalStateException(Text.get(1167) + RESOURCE);
        var id = Names.words(definition.getName());
        if (definitions.put(id, definition) != null)
            throw new IllegalStateException(Text.get(1168) + definition.getName());
    }

}

/** High-value quest weighting loaded from the bundled catalog. */
@Singleton
class QuestPriorityCatalog
{
    private final Map<String, QuestPriority> priorities = new HashMap<>();

    public QuestPriorityCatalog()
    {
        for (QuestPriority priority : BundledCatalogLoader.array(
                Text.get(559), QuestPriority[].class))
            if (priorities.put(Names.words(priority.getName()), priority) != null)
                throw new IllegalStateException(Text.get(1169) + priority.getName());
    }

    public QuestPriority priorityFor(String questName) { return priorities.get(Names.words(questName)); }
    public Map<String, QuestPriority> snapshot() { return Collections.unmodifiableMap(priorities); }


    public static final class QuestPriority
    {
        private String name;
        private double scoreBonus;
        private String reason;
        public String getName() { return name; }
        public double getScoreBonus() { return scoreBonus; }
        public String getReason() { return reason; }
    }
}

/** Deterministic recipe graph loaded from the bundled catalog. */
@Singleton
class ResourceDependencyCatalog
{
    private final Map<Integer, ResourceDependencyDefinition> definitions;
    private final Map<String, ResourceDependencyDefinition> definitionsByName;

    public ResourceDependencyCatalog()
    {
        this(java.util.Arrays.asList(BundledCatalogLoader.array(
                Text.get(598),
                ResourceDependencyDefinition[].class)));
    }

    ResourceDependencyCatalog(List<ResourceDependencyDefinition> values)
    {
        Map<Integer, ResourceDependencyDefinition> byId = new LinkedHashMap<>();
        Map<String, ResourceDependencyDefinition> byName = new LinkedHashMap<>();
        if (values != null)
            for (ResourceDependencyDefinition value : values)
            {
                if (value == null) continue;
                if (byId.put(value.getItemId(), value) != null)
                    throw new IllegalStateException(Text.get(1170) + value.getItemId());
                var name = Names.words(value.getItemName());
                if (!name.isEmpty()) byName.put(name, value);
            }
        definitions = Collections.unmodifiableMap(byId);
        definitionsByName = Collections.unmodifiableMap(byName);
    }

    public ResourceDependencyDefinition forItem(int itemId) { return definitions.get(itemId); }
    public ResourceDependencyDefinition forItemName(String itemName)
    {
        return definitionsByName.get(Names.words(itemName));
    }
    public int size() { return definitions.size(); }

}

/** Common progression resource routes loaded from the bundled catalog. */
@Singleton
class ResourceSourceCatalog
{
    private static final String RESOURCE = Text.get(708);
    private final List<ResourceSourceDefinition> sources;

    public ResourceSourceCatalog()
    {
        sources = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, ResourceSourceDefinition[].class)));
        for (ResourceSourceDefinition source : sources)
            if (source.id == null || source.getNameTokens() == null)
                throw new IllegalStateException(Text.get(1171) + RESOURCE);
    }

    public List<ResourceSourceDefinition> all() { return sources; }

    public List<ResourceSourceDefinition> match(String itemName)
    {
        var normalized = normalize(itemName);
        if (normalized.isEmpty()) return Collections.emptyList();
        List<ResourceSourceDefinition> result = new ArrayList<>();
        for (ResourceSourceDefinition source : sources)
        {
            if ("raw-fish".equals(source.id) && !normalized.startsWith("raw ")) continue;
            if ("cooked-food".equals(source.id) && normalized.startsWith("raw ")) continue;
            for (String token : source.getNameTokens())
                if (containsPhrase(normalized, normalize(token)))
                {
                    result.add(source);
                    break;
                }
        }
        return Collections.unmodifiableList(result);
    }

    public List<String> suggestions(String itemName, AccountMode mode, boolean allowWilderness)
    {
        return suggestions(itemName, mode, MembershipStatus.P2P, allowWilderness);
    }

    public List<String> suggestions(String itemName, AccountMode mode,
            MembershipStatus membership, boolean allowWilderness)
    {
        List<String> result = new ArrayList<>();
        for (ResourceSourceDefinition source : match(itemName))
        {
            if (source.isWilderness() && !allowWilderness) continue;
            if ((mode == AccountMode.HARDCORE_IRONMAN
                    || mode == AccountMode.HARDCORE_GROUP_IRONMAN)
                    && source.getRiskLevel() == RiskLevel.HIGH) continue;
            String route = membership == MembershipStatus.P2P
                    ? memberRoute(source, mode) : freeToPlayRoute(source, itemName, mode);
            if (route != null && !route.trim().isEmpty() && !result.contains(route))
                result.add(route);
            if (result.size() >= 4) break;
        }
        return Collections.unmodifiableList(result);
    }

    private static String memberRoute(ResourceSourceDefinition source, AccountMode mode)
    {
        if (mode == AccountMode.ULTIMATE_IRONMAN) return source.getUimRoute();
        if (mode != null && mode.isIronLike()) return source.getIronRoute();
        return source.getMainRoute();
    }

    private static String freeToPlayRoute(ResourceSourceDefinition source,
            String itemName, AccountMode mode)
    {
        var normalized = normalize(itemName);
        var explicitlySafe = false;
        for (String safeName : source.getFreeToPlayItemNames())
            if (normalized.equals(normalize(safeName)))
            {
                explicitlySafe = true;
                break;
            }
        if (!explicitlySafe) return null;
        if (mode == AccountMode.ULTIMATE_IRONMAN) return source.getFreeToPlayUimRoute();
        if (mode != null && mode.isIronLike()) return source.getFreeToPlayIronRoute();
        return source.getFreeToPlayMainRoute();
    }

    private static boolean containsPhrase(String value, String phrase)
    {
        if (phrase.isEmpty()) return false;
        return (" " + value + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9']+", " ").trim();
    }
}

/**
 * Adapts RuneLite's maintained skill-calculator action enums into Compass.
 * The enum types are wired explicitly because Plugin Hub review forbids Java
 * reflection; their maintained {@code values()} still provide the full catalog.
 */
@Singleton
class RuneLiteSkillActionCatalog
{
    private final ItemManager itemManager;
    private final Map<Skill, SkillAction[]> actionsBySkill = new LinkedHashMap<>();

    @Inject
    public RuneLiteSkillActionCatalog(ItemManager itemManager)
    {
        this.itemManager = itemManager;
        seedClassMap();
    }

    /** Test/diagnostic constructor. Membership remains UNKNOWN without ItemManager. */
    public RuneLiteSkillActionCatalog()
    {
        this.itemManager = null;
        seedClassMap();
    }

    public List<ActionDef> actionsFor(Skill skill)
    {
        var constants = actionsBySkill.get(skill);
        if (constants == null) return Collections.emptyList();
        List<ActionDef> actions = new ArrayList<>();
        for (SkillAction action : constants)
        {
            var enumValue = (Enum<?>) action;
            String id = "runelite:" + skill.name().toLowerCase(Locale.ROOT)
                    + ":" + enumValue.name().toLowerCase(Locale.ROOT);
            String name = itemManager == null ? pretty(enumValue.name())
                    : action.getName(itemManager);
            MembershipStatus membership = itemManager == null
                    ? MembershipStatus.UNKNOWN
                    : action.isMembers(itemManager)
                            ? MembershipStatus.P2P : MembershipStatus.F2P;
            actions.add(new ActionDef(
                    skill,
                    id,
                    name,
                    CurrentLiveSkillActionOverrides.level(id, action.getLevel()),
                    CurrentLiveSkillActionOverrides.xp(id, action.getXp()),
                    null,
                    membership,
                    action.getIcon()));
        }
        return Collections.unmodifiableList(actions);
    }

    public Map<Skill, Integer> coverageCounts()
    {
        Map<Skill, Integer> result = new EnumMap<>(Skill.class);
        for (Skill skill : actionsBySkill.keySet())
        {
            result.put(skill, actionsFor(skill).size());
        }
        return Collections.unmodifiableMap(result);
    }

    private void seedClassMap()
    {
        actionsBySkill.put(Skill.AGILITY, AgilityAction.values());
        actionsBySkill.put(Skill.COOKING, CookingAction.values());
        actionsBySkill.put(Skill.CONSTRUCTION, ConstructionAction.values());
        actionsBySkill.put(Skill.CRAFTING, CraftingAction.values());
        actionsBySkill.put(Skill.FARMING, FarmingAction.values());
        actionsBySkill.put(Skill.FIREMAKING, FiremakingAction.values());
        actionsBySkill.put(Skill.FISHING, FishingAction.values());
        actionsBySkill.put(Skill.FLETCHING, FletchingAction.values());
        actionsBySkill.put(Skill.HERBLORE, HerbloreAction.values());
        actionsBySkill.put(Skill.HUNTER, HunterAction.values());
        actionsBySkill.put(Skill.MAGIC, MagicAction.values());
        actionsBySkill.put(Skill.MINING, MiningAction.values());
        actionsBySkill.put(Skill.PRAYER, PrayerAction.values());
        actionsBySkill.put(Skill.RUNECRAFT, RunecraftAction.values());
        actionsBySkill.put(Skill.SMITHING, SmithingAction.values());
        actionsBySkill.put(Skill.THIEVING, ThievingAction.values());
        actionsBySkill.put(Skill.WOODCUTTING, WoodcuttingAction.values());
    }

    private static String pretty(String value)
    {
        var text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

/**
 * Concrete resource definitions for conventional F2P Runecraft routes.
 *
 * <p>Keeping item IDs here lets the generic readiness engine prove supplies
 * from equipment, inventory, an observed bank, or safe verified UIM storage
 * instead of leaving Runecraft requirements as permanent free-text questions.</p>
 */
@Singleton
class RunecraftSupplyCatalog
{
    public ResourceRequirement runeEssence()
    {
        return new ResourceRequirement(
                Text.get(1941),
                Text.get(1172),
                1,
                ItemID.BLANKRUNE,
                ItemID.BLANKRUNE_HIGH
        );
    }

    public ResourceRequirement altarEntryFor(String methodId)
    {
        if (Text.get(1875).equals(methodId))
            return entry("air", Text.get(1173),
                    ItemID.AIR_TALISMAN, ItemID.TIARA_AIR);
        if (Text.get(1876).equals(methodId))
            return entry("mind", Text.get(1174),
                    ItemID.MIND_TALISMAN, ItemID.TIARA_MIND);
        if (Text.get(1877).equals(methodId))
            return entry("water", Text.get(1175),
                    ItemID.WATER_TALISMAN, ItemID.TIARA_WATER);
        if (Text.get(1878).equals(methodId))
            return entry("earth", Text.get(1176),
                    ItemID.EARTH_TALISMAN, ItemID.TIARA_EARTH);
        if (Text.get(1879).equals(methodId))
            return entry("fire", Text.get(1177),
                    ItemID.FIRE_TALISMAN, ItemID.TIARA_FIRE);
        if (Text.get(1880).equals(methodId))
            return entry("body", Text.get(1178),
                    ItemID.BODY_TALISMAN, ItemID.TIARA_BODY);
        return null;
    }

    public boolean supports(String methodId)
    {
        return altarEntryFor(methodId) != null;
    }

    private static ResourceRequirement entry(
            String rune,
            String label,
            int talismanId,
            int tiaraId)
    {
        return new ResourceRequirement(
                Text.get(1942) + rune + "_entry",
                label,
                1,
                talismanId,
                tiaraId
        );
    }
}

/**
 * Current standard Slayer-master requirements and point economics.
 *
 * <p>Sources: https://oldschool.runescape.wiki/w/Slayer_Master and
 * https://oldschool.runescape.wiki/w/Slayer_training (verified 2026-08-28).
 * Diary point boosts are intentionally not assumed without live diary state.</p>
 */
@Singleton
class SlayerMasterCatalog
{
    private final List<SlayerMasterProfile> profiles =
            Collections.unmodifiableList(Arrays.asList(BundledCatalogLoader.array(
                    Text.get(1948),
                    SlayerMasterProfile[].class)));

    public List<SlayerMasterProfile> all()
    {
        return profiles;
    }

    public SlayerMasterProfile byId(String id)
    {
        var key = Names.words(id);
        for (SlayerMasterProfile profile : profiles)
            if (Names.words(profile.id).equals(key)) return profile;
        return null;
    }

    public SlayerMasterProfile match(String name)
    {
        var key = Names.words(name);
        if (key.isEmpty()) return null;
        for (SlayerMasterProfile profile : profiles)
        {
            if (Names.words(profile.id).equals(key)) return profile;
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
            if ("spria".equals(profile.id)) continue;
            if (profile.isWilderness() && !context.allowsWilderness()) continue;
            if ("mortimer".equals(profile.id))
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

/** Stable task mechanics loaded from the required bundled catalog. */
@Singleton
class SlayerTaskProfileCatalog
{
    private static final String RESOURCE = Text.get(892);
    private final List<SlayerTaskProfile> profiles;

    public SlayerTaskProfileCatalog()
    {
        profiles = Collections.unmodifiableList(Arrays.asList(
                BundledCatalogLoader.array(RESOURCE, SlayerTaskProfile[].class)));
        for (SlayerTaskProfile profile : profiles)
            if (profile.id == null || profile.getAliases() == null
                    || profile.getAliases().isEmpty())
                throw new IllegalStateException(Text.get(1184) + RESOURCE);
    }

    public SlayerTaskProfile profileFor(String taskName)
    {
        var normalized = Names.lower(taskName);
        if (normalized.isEmpty()) return null;
        for (SlayerTaskProfile profile : profiles)
            for (String alias : profile.getAliases())
            {
                var candidate = Names.lower(alias);
                if (normalized.equals(candidate) || normalized.contains(candidate)
                        || candidate.contains(normalized)) return profile;
            }
        return null;
    }

    public List<SlayerTaskProfile> all() { return profiles; }

}

/** Reviewed Slayer task strategy loaded from the bundled catalog. */
@Singleton
class SlayerTaskStrategicCatalog
{
    private static final String RESOURCE = Text.get(893);
    private static final Set<String> INTRINSIC_WILDERNESS = ids(
            "black-knights", "dark-warriors", "earth-warriors", "ents", "green-dragons",
            "lava-dragons", "magic-axes", "mammoths", "revenants", "rogues");
    private final SlayerTaskProfileCatalog taskProfiles;
    private final Map<String, SlayerTaskStrategicProfile> byProfileId;

    @Inject
    public SlayerTaskStrategicCatalog(SlayerTaskProfileCatalog taskProfiles)
    {
        this.taskProfiles = taskProfiles == null ? new SlayerTaskProfileCatalog() : taskProfiles;
        Map<String, SlayerTaskStrategicProfile> values = new HashMap<>();
        for (SlayerTaskStrategicProfile profile
                : BundledCatalogLoader.array(RESOURCE, SlayerTaskStrategicProfile[].class))
        {
            if (profile.getTaskProfileId() == null)
                throw new IllegalStateException(Text.get(1185) + RESOURCE);
            if (values.put(profile.getTaskProfileId(), profile) != null)
                throw new IllegalStateException(Text.get(1186)
                        + profile.getTaskProfileId());
        }
        byProfileId = Collections.unmodifiableMap(values);
    }

    public SlayerTaskStrategicCatalog() { this(new SlayerTaskProfileCatalog()); }

    public SlayerTaskStrategicProfile profileFor(String taskName)
    {
        var mechanics = taskProfiles.profileFor(taskName);
        return mechanics == null ? null : byProfileId.get(mechanics.id);
    }

    public int size() { return byProfileId.size(); }
    public Collection<SlayerTaskStrategicProfile> all() { return byProfileId.values(); }

    public boolean isWildernessBound(String taskName)
    {
        var mechanics = taskProfiles.profileFor(taskName);
        if (mechanics == null) return false;
        var profile = byProfileId.get(mechanics.id);
        return INTRINSIC_WILDERNESS.contains(mechanics.id)
                || (profile != null && profile.isDirectEncounter()
                        && profile.getInherentRisk() == RiskLevel.HIGH);
    }

    private static Set<String> ids(String... values)
    {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, values);
        return Collections.unmodifiableSet(result);
    }
}

/** Single indexed source for legacy, curated, and explicitly F2P training routes. */
@Singleton
class TrainingMethodCatalog
{
    public static final String PROVENANCE = Text.get(215);
    public static final String AUDITED_THROUGH = "2026-08-25";

    private final Map<Skill, List<TrainingMethod>> legacy = new EnumMap<>(Skill.class);
    private final Map<Skill, List<CuratedTrainingMethod>> curated = new EnumMap<>(Skill.class);
    private final Map<Skill, List<CuratedTrainingMethod>> f2p = new EnumMap<>(Skill.class);

    public TrainingMethodCatalog()
    {
        for (TrainingMethod method : BundledCatalogLoader.array(
                Text.get(897), TrainingMethod[].class))
        {
            if (method.id == null || method.getSkill() == null)
                throw invalid(Text.get(897));
            legacy.computeIfAbsent(method.getSkill(), ignored -> new ArrayList<>()).add(method);
        }
        loadCurated(Text.get(216), curated, true);
        loadCurated(Text.get(217), f2p, false);
        freeze(legacy);
        freeze(curated);
        freeze(f2p);
    }

    public List<TrainingMethod> legacyFor(Skill skill)
    {
        return legacy.getOrDefault(skill, Collections.emptyList());
    }

    public List<CuratedTrainingMethod> curatedFor(Skill skill)
    {
        return curated.getOrDefault(skill, Collections.emptyList());
    }

    public List<CuratedTrainingMethod> f2pFor(Skill skill)
    {
        return f2p.getOrDefault(skill, Collections.emptyList());
    }

    boolean legacyOnly() { return false; }

    private static void loadCurated(String resource,
            Map<Skill, List<CuratedTrainingMethod>> target, boolean metadataRequired)
    {
        for (CuratedTrainingMethod value : BundledCatalogLoader.array(
                resource, CuratedTrainingMethod[].class))
        {
            TrainingMethod method = value == null ? null : value.method();
            if (method == null || method.getSkill() == null || method.id == null
                    || metadataRequired && value.getMetadata() == null)
                throw invalid(resource);
            target.computeIfAbsent(method.getSkill(), ignored -> new ArrayList<>()).add(value);
        }
    }

    private static <T> void freeze(Map<Skill, List<T>> values)
    {
        for (Map.Entry<Skill, List<T>> entry : values.entrySet())
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    private static IllegalStateException invalid(String resource)
    {
        return new IllegalStateException(Text.get(1132) + resource);
    }
}

/** Audited exact routes that can be derived from ordinary live account state. */
@Singleton
final class TravelRouteEvidenceCatalog
{
    private final Map<String, TravelRouteEvidenceDefinition> definitions =
            new LinkedHashMap<>();

    public TravelRouteEvidenceCatalog()
    {
        add(new TravelRouteEvidenceDefinition(
                Text.get(1587), "The Grand Tree",
                Collections.emptyList()));
        add(new TravelRouteEvidenceDefinition("ectophial", "Ghosts Ahoy",
                Arrays.asList("Ectophial")));
    }

    public TravelRouteEvidenceDefinition get(String id)
    {
        return id == null ? null : definitions.get(id);
    }

    public Map<String, TravelRouteEvidenceDefinition> all()
    {
        return Collections.unmodifiableMap(definitions);
    }

    private void add(TravelRouteEvidenceDefinition definition)
    {
        if (definitions.put(definition.getRouteId(), definition) != null)
            throw new IllegalStateException(Text.get(1188)
                    + definition.getRouteId());
    }
}

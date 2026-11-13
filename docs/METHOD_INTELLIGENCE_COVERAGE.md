# Method intelligence coverage audit

Snapshot: 2026-10-01, local development tree after checkpoint `bc082bd`.
This is a measured implementation inventory, not whole-product completion.

## Evidence layers

| Layer | Current coverage | What it proves |
|---|---:|---|
| Reviewed action ingredients | 169 records: Cooking 30, Fletching 42, Smithing 36, Crafting 61 | Reviewed input identity, quantities, membership and calculator batch units where supplied. |
| Execution profiles | 74 profiles; 11 have reviewed references, including one intentionally empty list | Method-specific action filtering and progress/input calculations. Ten profiles have nonempty reviewed references. |
| Legacy profile input rules | 40 FIXED, 3 ACTION_ITEM, 2 SAPLING_FOR_TREE rules | Remaining rule-based inputs; this count does not certify their mechanics. |
| Full method contracts | 9 records covering 11 method IDs | Explicit readiness evaluation for those IDs; two records intentionally remain unresolved. |
| Full recipe flow | 30 recipes: 10 Cooking, 14 Fletching, 6 Smithing | Working/efficient batch targets, reusable tools, retained outputs and capacity models for these recipes. |

Counts come from `reviewed-action-recipes.json`, `method-execution-profiles.json`
and `method-intelligence.json` under `src/main/resources/content/catalogs`.
They are different units of coverage and must not be added together.

## Full contract boundaries

The modeled method IDs are cooking_f2p_fish, cooking_f2p_fish_baseline,
cooking_wines, cooking_hosidius, cooking_f2p_uim_carried_fish,
fletching_arrow_shafts, fletching_bows, smithing_f2p_platebodies and
smithing_f2p_platebody_baseline. cooking_karambwan_1t and
cooking_gnome_restaurant have explicit unresolved contracts; neither is counted
as supported execution.

`MethodReadinessService.evaluate` returns null for an absent contract.
`RecommendationGuidanceService` then uses existing specialized, adaptive,
variable or universal guidance. Those paths retain their own safety checks, but
an ingredient recipe does not automatically gain a full capacity, setup or
preparation contract. Broad skill coverage is therefore still incomplete.

## Foundation boundary update

Bow cutting and the existing F2P platebody routes now exercise the same full
candidate pipeline. Candidate-specific preparation, capacity, guidance and
optional economics remain attached through strategy/session ranking. Reviewed
banking behavior can override the older generated family classification, which
allows carried-log UIM bow cutting while conventional Smithing bank loops remain
closed to UIM. Smithing adds no skill-specific Java policy.

Production economics remain deliberately unrated. The next content expansion
needs reviewed, normalized economic facts for a complete comparable cohort; live
prices and GE infrastructure are outside this boundary. Gem full contracts remain
disabled until that evidence exists. Other Smithing routes, including smelting,
Blast Furnace and boosted or catalysed variants, require separate setup facts.

## Whole-product and release limits

This audit does not establish completion for quests, Slayer, Farming, PvM,
minigames, gear, recurring activities, UI, performance or privacy. Their existing
implementations need domain-specific acceptance evidence; passing recipe tests
cannot stand in for that audit. The three historical quest coverage failures
were resolved as baseline drift and remain documented in
`QUEST_BASELINE_REVIEW_2026-11-09.md`.

The isolated release restoration remains a separate approval-boundary task.
It must not be inferred released from development branch pushes. The older
product implementation map describes historical checkpoints rather than the
completion status of this broader mission.

## Bow-cutting migration update

At the bow-migration checkpoint, the existing route gained one method record/ID
and twelve flow recipes. That checkpoint had eight method records, nine IDs and
24 flow recipes (ten Cooking, fourteen Fletching), with two unresolved records.
No new training method or production Java was added by that migration. Current
tree totals are recorded in the table above.

Tests exercise retained output capacity, knife space, Main purchase affordability,
Iron purchase exclusion, UIM bank exclusion, membership fail-closed behavior and
recipe changes with carried log types. This is evidence for this route only;
the broader domain gaps above remain open.

### Bow transition verification

Additional snapshot tests cover levels 4/5/9/10, a full inventory of retained
unstrung bows, moving the knife into a UIM conventional bank, and unknown
inventory observation. These transitions invalidate readiness or change the
selected recipe as appropriate. Production event routing was inspected:
OsrsStrategistPlugin.onItemContainerChanged marks inventory, equipment, bank and
Group Storage changes for account refresh; onStatChanged marks level changes.
Existing plugin tests cover strategic-container filtering and refresh coalescing.
This establishes snapshot and event-routing coverage, not an in-client playtest.

### Gem migration preflight

[Gem flow research](GEM_METHOD_MIGRATION_REVIEW.md) verifies the six existing
recipe outputs/tools/levels and identifies an unresolved recipe-value ranking
issue. Affordability plus a higher level is insufficient to choose expensive
training inputs. No gem full contract is enabled by that research.

### Jewelry ingredient migration

The existing universal jewelry name inference is replaced by 44 reviewed casting
recipes: rings, necklaces, bracelets and unstrung amulets represented in current
RuneLite CraftingAction. Reviewed ingredients now total 169 (Crafting 61); full
method/flow contracts and execution profiles are unchanged. Each added row has
its current Wiki page revision, exact consumed item IDs, membership and reusable
mould setup. Wiki recipe levels/XP were compared with current RuneLite source;
this migration does not override those calculator values.

The former matcher recognized `red topaz` inside output names, so `Topaz ring`
incorrectly became a gold-bar-only recipe. Reviewed topaz casting consumes silver
and cut red topaz. Exact names also distinguish casting an unstrung amulet from
stringing/enchanting, and prevent invented jewelry names inheriting ingredients.
The canonical Dragon necklace page is followed from the Dragonstone necklace
redirect. No diagnostic-name alias or enchanted recipe is inferred.

Tests cover all 44 ingredient/membership combinations, reusable mould separation,
the topaz regression, and unsupported names. This proves consumed ingredients,
not affordability, efficient training value, full preparation or output capacity.
The pending gem-value policy remains a separate decision.

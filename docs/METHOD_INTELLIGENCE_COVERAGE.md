# Method intelligence coverage audit

Snapshot: 2026-09-30, development commit
`1bf2c7b9c0fec2f18cde2975dff38031722a645a`.
This is a measured implementation inventory, not whole-product completion.

## Evidence layers

| Layer | Current coverage | What it proves |
|---|---:|---|
| Reviewed action ingredients | 125 records: Cooking 30, Fletching 42, Smithing 36, Crafting 17 | Reviewed input identity, quantities, membership and calculator batch units where supplied. |
| Execution profiles | 74 profiles; 11 have reviewed references, including one intentionally empty list | Method-specific action filtering and progress/input calculations. Ten profiles have nonempty reviewed references. |
| Legacy profile input rules | 40 FIXED, 3 ACTION_ITEM, 2 SAPLING_FOR_TREE rules | Remaining rule-based inputs; this count does not certify their mechanics. |
| Full method contracts | 7 records covering 8 method IDs | Explicit readiness evaluation for those IDs; two records intentionally remain unresolved. |
| Full recipe flow | 12 recipes: 10 Cooking, 2 Fletching | Working/efficient batch targets, reusable tools, retained outputs and capacity models for these recipes. |

Counts come from `reviewed-action-recipes.json`, `method-execution-profiles.json`
and `method-intelligence.json` under `src/main/resources/content/catalogs`.
They are different units of coverage and must not be added together.

## Full contract boundaries

The modeled method IDs are cooking_f2p_fish, cooking_f2p_fish_baseline,
cooking_wines, cooking_hosidius, cooking_f2p_uim_carried_fish and
fletching_arrow_shafts. cooking_karambwan_1t and cooking_gnome_restaurant have
explicit unresolved contracts; neither is counted as supported execution.

`MethodReadinessService.evaluate` returns null for an absent contract.
`RecommendationGuidanceService` then uses existing specialized, adaptive,
variable or universal guidance. Those paths retain their own safety checks, but
an ingredient recipe does not automatically gain a full capacity, setup or
preparation contract. Broad skill coverage is therefore still incomplete.

## Next substantial migration proposal

Migrate the existing fletching_bows cutting route to full method intelligence.
It already has twelve explicit reviewed cutting references; ordinary log inputs,
a reusable knife and retained nonstackable outputs exercise a different flow
from stackable arrow shafts. Verify output IDs, levels, batch limits and account
storage handling before adding contracts. Keep bow stringing and crossbows out
of this route unless separately reviewed; do not infer them from name suffixes.

Acceptance evidence should include selection changes with carried/banked supplies,
Main affordability versus Iron self-sourcing, UIM retrieval boundaries, occupied
inventory and tool slots, retained outputs, and equipment/container changes.
Remove any superseded method-specific Java only after finding every caller.
This proposal adds no new training method and should need no schema extension.

Subsequent candidates are existing Smithing anvil routes and gem cutting. Their
reviewed input rows are useful prerequisites, not proof that reusable tools,
outputs, unlocks and working capacity have been modeled. Guaranteed iron smelting
and boosted/catalysed/Blast Furnace variants require separate setup evidence.

## Whole-product and release limits

This audit does not establish completion for quests, Slayer, Farming, PvM,
minigames, gear, recurring activities, UI, performance or privacy. Their existing
implementations need domain-specific acceptance evidence; passing recipe tests
cannot stand in for that audit. The three known quest coverage failures remain.

The isolated release restoration remains a separate approval-boundary task.
It must not be inferred released from development branch pushes. The older
product implementation map describes historical checkpoints rather than the
completion status of this broader mission.

## Bow-cutting migration update

The proposed existing bow-cutting route now has a full contract. This adds one
method record/ID and twelve flow recipes: totals are eight method records, nine
IDs and 24 flow recipes (ten Cooking, fourteen Fletching). Two unresolved method
records remain. The 125 reviewed ingredient records and 74 execution profiles
are unchanged. No new training method or production Java was added.

Tests exercise retained output capacity, knife space, Main purchase affordability,
Iron purchase exclusion, UIM bank exclusion, membership fail-closed behavior and
recipe changes with carried log types. This is evidence for this route only;
the broader domain gaps above remain open.

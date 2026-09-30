# Method intelligence

Cooking and early arrow-shaft Fletching consume a shared method contract. Java evaluates the
contract; method IDs, recipes, ingredients, access gates, locations and retained
outputs live in `content/catalogs/method-intelligence.json`.

`MethodReadinessService` evaluates access before selecting a legal recipe and
batch. `MethodPreparationService` merges duplicate item requests before spending
observed stock, describes retrieval separately from carried supplies, and uses
`AccountResourcePlanner.purchase` for one combined affordability decision.
Unknown storage is not empty. Main purchases need observed ownership, usable
cash and exact matching price quotes. Iron modes cannot purchase. Group stock
requires an enabled, fresh observation. UIM conventional bank routes are blocked;
looting bag, death storage and deathpiles cannot satisfy a recipe without a
separate verified retrieval contract.

`MethodCapacity` simulates each action's consumed inputs and retained outputs.
Hard capacity means one action fits; working capacity means the chosen batch
fits. Efficient capacity refers only to the data's larger batch threshold. It
does not prove throughput, burn rates, available inputs, sustained banking or
XP per hour. Unprepared efficient quantities stay unknown. Reusable tools are
reserved once and never consumed. Equipped tools satisfy capacity only when the
contract explicitly permits equipped use; bank ownership is never equipment.
Input/output recycling under the same item ID is rejected until modeled.

Selection sorts feasible candidates ahead of unknown hard requirements before
comparing scores. The same readiness result supplies guidance and the final
actionability gate; it cannot override another unresolved access requirement.
Guidance bounds a batch and asks for reassessment after processing or reaching
the next recipe level. It does not predict exact successful cooks or burns.
One carried input can be a valid small UIM batch.

Karambwan execution and Gnome Restaurant orders remain unresolved because their
full live execution requirements are not modeled. Missing Iron supplies likewise
remain unresolved until an acquisition route is proved. These are intentional
safety limits, not substitute recommendations to buy or assume access.

## Source review

Reviewed through the live OSRS Wiki Action API on 2026-09-30. Method contracts
record their source revision. Ingredient identity, F2P status, stackability and
Cooking levels were checked against these revisions:

| Page | Revision |
| --- | --- |
| Raw shrimps | 15331265 |
| Raw sardine | 15331276 |
| Raw herring | 15331274 |
| Raw trout | 15331267 |
| Raw pike | 15331271 |
| Raw salmon | 15331268 |
| Raw tuna | 15331278 |
| Raw lobster | 15331269 |
| Raw swordfish | 15337086 |
| Grapes | 15353159 |
| Jug of water | 15229681 |

The four-action preparation batch is a planner policy, not a game requirement.
The larger 28-fish/14-wine thresholds describe modeled inventory batches, not
claims that those routes maximize account progression.

## Early Fletching

The arrow-shaft contract covers regular logs from level 1 and oak logs from
level 15 within the existing early-method level range. Both reserve one reusable
knife and retain stackable shafts. Optional observed bank supplies are separate
from requiring a bank loop, so UIM can process carried logs without receiving
bank withdrawal instructions. Missing tools or inaccessible sources remain gated.
The capacity simulator checks the resulting stack against the live quantity
representation limit instead of silently assuming unlimited stack growth.

Verified on 2026-09-30 through current Wiki revisions: Arrow shaft 15332152,
Logs 15322883, Oak logs 15182629 and Knife 15350068. The 27-log threshold is
inventory batch capacity with a knife, not a throughput claim. An existing output
stack or retained setup can reduce it. Java contains no Fletching recipe IDs or
special cases.

## Verified pickup preparation

`MethodAcquisitionCatalog` currently supports only reviewed ground-item pickups
with explicitly unrestricted access, no risk, and no skill XP. It does not turn
the descriptive resource-source catalog into evidence. The initial contracts are
a knife in Lumbridge Castle's kitchen and logs on the castle bank floor, reviewed
against the Knife and Logs revisions above. Other source types require explicit
access, build, input and outcome evaluation before they can be added.

Preparation first uses observed usable stock, then a verified affordable purchase
when allowed. If purchasing is unavailable or unproved, every missing item must
have a reviewed pickup route before a self-source plan becomes actionable. Unknown
ordinary ownership or opted-in unobserved Group Storage still blocks that proof.
UIM bank and retrieval-only storage are not counted. Pickups remain acquisition
steps until live inventory confirms possession; guidance leads with the pickup
instructions and handles absent spawns without promising a respawn time.

Pickup preparation carries a higher ranking burden than observed retrieval and
increases with requested quantity. This is a planner preference, not a measured
travel time. The method capacity check reserves all resulting inputs and tools
before any processing or disposal can be assumed.

## Reviewed legacy action inputs

`reviewed-action-recipes.json` replaces Cooking's legacy name-derived input
fallback. It contains 29 ordinary fish recipes, jug of wine, and the two existing
arrow-shaft ingredient contracts. Each
row records exact consumed item IDs and quantities per attempt, explicit
membership, setup assumptions, a Wiki page/revision and review date. Runtime
validation in `ReviewedActionRecipeCatalog` rejects incomplete provenance,
substring matching, duplicate keys, missing membership or stackability, and
invalid ingredient identities/quantities. The installed
plugin reads bundled data only.

This table is an ingredient contract for the existing universal guidance path,
not a replacement readiness engine. It does not prove level, access, usable
ownership, purchase affordability, inventory capacity, burn rates or successful
outputs. Those remain separate planning responsibilities. Unknown membership can
use only explicitly F2P rows. Supported fish are cooked on a range/fire; wine
uses grapes and water and requires fermentation. Quantities describe attempts,
not a promise that every output succeeds.

The 30 normalized output names were checked against all 148 Cooking actions in
the current public RuneLite CookingAction source on 2026-09-30. The remaining
118 actions are not covered by this ingredient table. Previous Java guesses for
composite, alternative-input, local and quest-dependent foods are deliberately
removed rather than certified by migration. This includes cooked meat, sinew,
karambwan and unknown future foods. Unsupported names return unresolved inputs
and cannot win universal Cooking action selection. They need reviewed explicit
recipes and any special access/setup model before becoming supported.

The existing `action-recipes.json` table remains a legacy contract for other
skills. New reviewed rows are checked first and retain item IDs; legacy rows do
not silently acquire provenance. Method readiness continues to use
`method-intelligence.json`; adding a reviewed ingredient row alone does not add a
new actionable training method.

Twelve method recipes (ten Cooking and two Fletching) now use `inputRecipe` to reference the shared reviewed
row by exact output name within their skill. Their repeated ingredient IDs,
names, quantities and stackability have been removed from method data. Inline
inputs and a reference cannot coexist. Missing references fail catalog loading,
and a method marked F2P cannot reference a members-only input recipe. Referenced
ingredients are copied into the evaluated method contract; batch limits, reusable
tools, retained outputs and access requirements remain method-specific.

Stackability is explicitly recorded per shared ingredient (verified against the
same individual Wiki pages/revisions as the input IDs). It cannot default from a
missing field. Action quantity multiplication also fails unresolved if its total
cannot be represented, rather than saturating a supposedly exact requirement.
The existing regular-log and oak-log arrow-shaft methods now use shared inputs.
`actionUnitsPerBatch` separates calculator counts from processing batches: regular
logs produce 15 calculator shaft units per log, while readiness still consumes
one log per processing action. A null conversion explicitly leaves a recipe
available only to method batch planning. The oak recipe uses that state because
RuneLite's generic Arrow shaft action does not identify its log tier. Its existing
method contract retains the 30-shaft output and level gate. Neither recipe is
available to F2P/unknown membership. Knife tools remain in the method contract.

The arrow-shaft Java special case and its integer ceiling helper have been
removed. Generic conversion rounds positive counts up without addition overflow.
The existing Arrow shaft Wiki revision 15332152 provides the batch yields; Logs
15322883 and Oak logs 15182629 supply item/stackability facts. The current public
RuneLite FletchingAction source was checked on 2026-09-30 for its individual-shaft
calculator unit. Other Fletching name-inference branches remain separate audit
work; this change does not broaden method coverage.

## Existing battlestaff assembly family

The four elemental battlestaff assembly recipes now use the shared reviewed
catalog. Each consumes one battlestaff and its charged elemental orb per crafted
staff. Ingredient IDs and non-stackability were checked against the current
Battlestaff/orb Wiki pages; assembly provenance is recorded on each output row.
RuneLite's current CraftingAction entries were checked for the same four outputs.

This replaces the Java substring/element-name inference; it adds no training
method, orb-charging plan or transmutation route. F2P and unknown membership
cannot resolve these members recipes, even if an action is mislabeled. Unknown
staff names remain unresolved instead of acquiring guessed ingredients. Existing
level/access/resource/readiness checks retain their separate responsibilities.

## Existing glassblowing family

Seven existing RuneLite calculator actions now resolve through reviewed rows:
beer glass, empty candle lantern, empty oil lamp, vial, fishbowl, unpowered orb
and lantern lens. Each consumes one molten glass per attempt and retains the
instruction to use a glassblowing pipe. The calculator's Fishbowl label describes
making the empty bowl; its setup text says so explicitly. Filled lamps/bowls,
vials of water and arbitrary names containing those words do not inherit this
recipe. Light orbs are outside this migration's previously supported family.

Crafting membership is taken from each Wiki creation recipe, not the output
item's infobox. Beer glass and vial are free items with members-only glassblowing
recipes. Their reviewed rows therefore reject F2P and unknown membership even
if a caller labels the output item free. Per-output source revisions are stored
in the data; molten glass identity/non-stackability comes from revision 15288555.
This removes the Java substring rule and does not add a new training method or
assert full tool/access/capacity readiness from ingredient resolution alone.

## Existing ordinary dart assembly family

The eight ordinary dart types in RuneLite (bronze, iron, steel, mithril, adamant,
rune, amethyst and dragon) now resolve to exact reviewed tip IDs plus ordinary
feathers. Wiki recipes show ten tips and ten feathers producing ten darts;
normalized calculator quantities are one of each per individual dart. Both
inputs are stackable. The recipes remain members-only. Colored-feather variants,
poisoning and tip production are separate routes, not inferred alternatives.

The suffix-based dart-input guess is removed. Unknown dart names and atlatl
variants now remain unresolved in universal recipe guidance instead of receiving
invented tip names. This review found that Atlatl dart Wiki revision 15343589
models 30 Fletching XP per 20 outputs, whereas the inspected public RuneLite
FletchingAction entry supplies 9.5 per dart. Atlatl assembly also uses headless
atlatl darts and atlatl dart tips, not the ordinary tip/feather pair. Resolve that
XP/input discrepancy in a separate reviewed slice before enabling its recipe.
No new training method or full readiness contract is introduced here.

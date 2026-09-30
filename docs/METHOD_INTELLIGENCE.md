# Method intelligence

Cooking is the first consumer of a shared method contract. Java evaluates the
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

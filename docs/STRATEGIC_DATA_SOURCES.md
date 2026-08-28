# Strategic data source registry

This registry records external facts used by local structured strategy data.
It is deliberately a re-verification index rather than copied guide prose.
Runtime recommendation rendering does not fetch these pages.

Last audited: 2026-08-29.

## Account infrastructure

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| UIM POH storage and travel breakpoints | [UIM Construction guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Construction), [UIM item management](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management) | The Costume Room shell is available at 42 Construction, but the modeled first useful equipment storage milestone is 46 for the oak armour case/magic wardrobe. Portal Chamber access is modeled at 50. Furniture or a configured portal still requires direct capability evidence; level alone never proves completion. |

## Travel and locations

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| Ectofuntus method location and Ectophial route | [Ectofuntus](https://oldschool.runescape.wiki/w/Ectofuntus), [Ectophial](https://oldschool.runescape.wiki/w/Ectophial) | The Ectofuntus is north of Port Phasmatys; the Ghosts Ahoy reward teleports directly there and refills. The model still requires an observed exact route key before reducing travel burden. |
| Fruit-tree location alternatives | [Fruit tree patch locations](https://oldschool.runescape.wiki/w/Fruit_tree_patch/Patches), [Farming runs](https://oldschool.runescape.wiki/w/Farming_runs) | Catherby and Tree Gnome Stronghold have fruit-tree patches; an exact verified Gnome Stronghold spirit-tree route can make the latter lower-travel. A generic spirit-tree unlock is not treated as destination proof. |
| Tree-patch location alternatives | [Tree patch](https://oldschool.runescape.wiki/w/Tree_patch) | Falador Park and Tree Gnome Stronghold have tree patches; only the exact observed destination route receives travel credit. |
| F2P fly-fishing location | [Free-to-play Fishing training](https://oldschool.runescape.wiki/w/Free-to-play_Fishing_training) | Barbarian Village is a concrete fly-fishing loop. The catalog does not copy variable XP-rate estimates. |
| Fairy-ring access checks | [Fairy rings](https://oldschool.runescape.wiki/w/Fairy_rings), [Dramen staff](https://oldschool.runescape.wiki/w/Dramen_staff) | The network becomes available during Fairytale II after the relevant permission; staff-free access needs separate diary evidence. This remains in `TransportCatalog`/dependency evidence, not a location bonus. |

## Resource and account-mode value

| Structured rule | Source | Fact retained locally |
| --- | --- | --- |
| Iron Slayer consumable opportunity cost | [Ironman Slayer guide](https://oldschool.runescape.wiki/w/Ironman_Guide/Slayer) | Cannonball time can make cannon use unattractive for an Iron even when the same executable method is convenient for a Main. The evaluator represents replacement burden; it does not force a named task or method winner. |
| UIM consumable/storage pressure | [Ultimate Ironman guide](https://oldschool.runescape.wiki/w/Ultimate_ironman_guide), [UIM item management](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management) | Conventional bank contents are not usable UIM supply; containers and retrieval systems have distinct setup/risk consequences. Exact future consumption is never invented. |

General acquisition wording continues to come from `ResourceSourceCatalog`.
Specific quantities and tradeability must be supplied by typed item/activity
data; `SustainableResourceValueService` does not infer either from prose.

## Contextual gear value

| Structured rule | Source | Fact retained locally |
| --- | --- | --- |
| UIM storage/replacement value | [UIM equipment guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Equipment), [UIM item management](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management) | A slightly weaker storable or easily re-obtainable item can be strategically preferable to a marginal upgrade occupying a persistent slot. Storability must be explicit evidence; it is never inferred from the item name. |
| Acquisition routes | The item, quest, minigame and boss pages referenced by `GearAcquisitionCatalog.PROVENANCE` | An acquisition route is not a universal BIS claim. Encounter benefit, replacement horizon, account legality, resources, and storage consequences remain separate typed inputs. |

## Quest paths and rewards

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| Recipe for Disaster Goblin generals | [Freeing the Goblin generals](https://oldschool.runescape.wiki/w/Recipe_for_Disaster/Freeing_the_Goblin_generals), [Recipe for Disaster](https://oldschool.runescape.wiki/w/Recipe_for_Disaster) | The subquest requires Another Cook's Quest/Cook's Assistant and Goblin Diplomacy and awards 1,000 Farming, Cooking, and Crafting XP. It has no Farming-level requirement. The reward can reduce a near-term manual Farming grind, but Farming is never labeled a Barrows-gloves prerequisite. |

## Live Slayer decisions

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| Observable assignment/point state | [RuneLite SlayerPlugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java) | RuneLite updates task identity/count/location from Slayer varps/DB rows and points/streak from their varbits. Assigned-master identity and occupied block slots are not inferred when the client does not prove them. |
| Skip, block, and streak economics | [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training), [Slayer Rewards](https://oldschool.runescape.wiki/w/Slayer_Rewards), [Slayer reward points](https://oldschool.runescape.wiki/w/Slayer_reward_point) | Cancelling costs 30 points; block costs vary by master; ordinary slots require 50 quest points each and the seventh requires Lumbridge & Draynor elite. Bonus streaks occur at 10/50/100/250/1,000. Block advice requires observed point, master, weight, and free-slot evidence. |

When any retained mechanic changes, update the relevant typed catalog/service,
its semantic tests, the audit date above, and this row together.

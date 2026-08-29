# Strategic data source registry

This registry records external facts used by local structured strategy data.
It is deliberately a re-verification index rather than copied guide prose.
Runtime recommendation rendering does not fetch these pages.

Last audited: 2026-08-29.

## Account infrastructure

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| UIM POH storage and travel breakpoints | [UIM Construction guide](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Construction), [UIM item management](https://oldschool.runescape.wiki/w/Ultimate_Ironman_Guide/Item_Management) | The Costume room shell is available at 42 Construction and the modeled first equipment-storage furniture is the oak armour case at 46. Personal reusable storage and transport receive higher UIM value, but only observed furniture counts. |
| Core POH rooms and furniture | [Construction](https://oldschool.runescape.wiki/w/Construction), [Costume room](https://oldschool.runescape.wiki/w/Costume_room), [Oak armour case](https://oldschool.runescape.wiki/w/Oak_armour_case), [Portal chamber](https://oldschool.runescape.wiki/w/Portal_chamber), [Portal nexus](https://oldschool.runescape.wiki/w/Portal_nexus), [Pool space](https://oldschool.runescape.wiki/w/Pool_space), [Achievement gallery](https://oldschool.runescape.wiki/w/Achievement_gallery) | Retained exact milestones include Costume room 42/50,000 coins; oak armour case 46/3 oak planks; configured starter portal 50 with its frame, focus, spell level, and 100-cast rune cost; Superior Garden/restoration pool 65; marble nexus 72; and basic jewellery box 81. Higher-cost infrastructure remains preparation work until its materials are actually obtained. |
| POH natural transport | [Spirit tree](https://oldschool.runescape.wiki/w/Spirit_tree), [Fairy ring (Construction)](https://oldschool.runescape.wiki/w/Fairy_ring_(Construction)), [Fairy rings](https://oldschool.runescape.wiki/w/Fairy_rings) | A POH spirit tree requires 75 Construction, 83 Farming, a spirit sapling, and Tree Gnome Village network access. A POH fairy ring requires 85 Construction, full Fairytale II completion for the enchantment, 10 unnoted mushrooms, and a filled watering can. |
| Live personal-house evidence | RuneLite [`VarbitID.POH_BUILDING_MODE`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/VarbitID.java), [`PohIcons`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/poh/PohIcons.java) | A scene scan is attributed to the character only in own-house building mode. RuneLite-maintained object identities classify configured portals, storage, pools, jewellery, altars, and natural transport; a public POH is never treated as personal infrastructure. |

## Travel and locations

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| Ectofuntus method location and Ectophial route | [Ectofuntus](https://oldschool.runescape.wiki/w/Ectofuntus), [Ectophial](https://oldschool.runescape.wiki/w/Ectophial) | The Ectofuntus is north of Port Phasmatys; the Ghosts Ahoy reward teleports directly there and refills. The model still requires an observed exact route key before reducing travel burden. |
| Fruit-tree location alternatives | [Fruit tree patch locations](https://oldschool.runescape.wiki/w/Fruit_tree_patch/Patches), [Farming runs](https://oldschool.runescape.wiki/w/Farming_runs) | Catherby and Tree Gnome Stronghold have fruit-tree patches; an exact verified Gnome Stronghold spirit-tree route can make the latter lower-travel. A generic spirit-tree unlock is not treated as destination proof. |
| Tree-patch location alternatives | [Tree patch](https://oldschool.runescape.wiki/w/Tree_patch) | Falador Park and Tree Gnome Stronghold have tree patches; only the exact observed destination route receives travel credit. |
| F2P fly-fishing location | [Free-to-play Fishing training](https://oldschool.runescape.wiki/w/Free-to-play_Fishing_training) | Barbarian Village is a concrete fly-fishing loop. The catalog does not copy variable XP-rate estimates. |
| Fairy-ring access checks | [Fairy rings](https://oldschool.runescape.wiki/w/Fairy_rings), [Dramen staff](https://oldschool.runescape.wiki/w/Dramen_staff) | The network becomes available during Fairytale II after the relevant permission; staff-free access needs separate diary evidence. This remains in `TransportCatalog`/dependency evidence, not a location bonus. |

## Training method properties

| Structured data | Source | Fact retained locally |
| --- | --- | --- |
| High Level Alchemy attention | [High Level Alchemy](https://oldschool.runescape.wiki/w/High_Level_Alchemy), [game ticks](https://oldschool.runescape.wiki/w/Ticks) | One cast consumes one item and can be repeated every five ticks (3 seconds). Repeated training therefore needs regular input and is classified ACTIVE, not AFK/LOW attention. This changes session fit through method properties rather than a named-method winner rule. |
| Curse splashing | [Curse](https://oldschool.runescape.wiki/w/Curse), [Splashing](https://oldschool.runescape.wiki/w/Splashing), [Monk of Zamorak](https://oldschool.runescape.wiki/w/Monk_of_Zamorak) | Curse is F2P at 19 Magic, grants 29 XP on either hit or miss, and consumes one body, three earth, and two water runes without staff substitution. A Magic attack bonus of -64 or lower guarantees a splash; the caged level-17 Monk of Zamorak under the Varrock Palace stairs is a verified F2P target. |
| Variable activity execution | [Giants' Foundry](https://oldschool.runescape.wiki/w/Giants%27_Foundry), [Mahogany Homes](https://oldschool.runescape.wiki/w/Mahogany_Homes), [Farming contracts](https://oldschool.runescape.wiki/w/Contract_farming), [Hunters' Rumours](https://oldschool.runescape.wiki/w/Hunters%27_Rumours), [Forestry training](https://oldschool.runescape.wiki/w/Pay-to-play_Woodcutting_training), [Stealing valuables](https://oldschool.runescape.wiki/w/Stealing_valuables) | Foundry commissions consume 28 bars and default to an observed 14/14 adjacent-metal alloy; Mahogany Homes tiers are Beginner 1, Novice 20, Adept 50, Expert 70; Farming contracts are easy 45, medium 65, hard 85; Rumours are Novice 46, Adept 57, Expert 72, Master 91 plus At First Light; Forestry routes resolve a named tree/location; the Varlamore loop resolves wealthy citizens, house keys, south-west houses, and valuables. Variable outcomes remain qualitative. |
| Warmth and contribution setup | [Wintertodt](https://oldschool.runescape.wiki/w/Wintertodt), [Wintertodt warm clothing](https://oldschool.runescape.wiki/w/Wintertodt/Warm_clothing), [Fishing Trawler](https://oldschool.runescape.wiki/w/Fishing_Trawler) | Four warm items give maximum Wintertodt clothing protection; food healing at least 4 Hitpoints restores 35% warmth, so cakes are a concrete default. Fishing Trawler requires at least 50 contribution for the Angler roll; ten swamp-paste leak repairs supply that threshold, while 300 paste, rope, and a bailing bucket form the reviewed group setup. |
| Farming seed breakpoints | [Allotment patch](https://oldschool.runescape.wiki/w/Allotment_patch), [Herb patch](https://oldschool.runescape.wiki/w/Herb_patch) | Allotment guidance selects only an observed six-seed planting supply at a level-legal tier (potato 1 through snape grass 61). Herb guidance selects only an observed level-legal seed (guam 9 through torstol 85); it does not ask the player to decide which Herblore resource to spend. |

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
| Observable assignment/point state | [RuneLite SlayerPlugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java), [RuneScape varbit 4067](https://oldschool.runescape.wiki/w/RuneScape:Varbit/4067) | RuneLite updates task identity/count/location from Slayer varps/DB rows and points/streak from their varbits. Varbit 4067 identifies the assigning master (1 Turael/Aya through 9 Spria); the value persists after task completion, so task count remains the assignment-state authority. |
| Master-specific blocks | [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training), RuneLite `VarbitID` block-slot constants | The current game exposes six ordinary and one diary block slot per master (Turael/Aya/Spria share their list). Compass counts occupied slots only for the observed assigning master and never claims a block without weight, points, and a live free-slot proof. |
| Skip, block, and streak economics | [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training), [Slayer Rewards](https://oldschool.runescape.wiki/w/Slayer_Rewards), [Slayer reward points](https://oldschool.runescape.wiki/w/Slayer_reward_point) | Cancelling costs 30 points; block costs vary by master; ordinary slots require 50 quest points each and the seventh requires Lumbridge & Draynor elite. Points begin after the first four standard tasks. Bonus streaks occur at 10/50/100/250/1,000. |
| Reward ownership and current costs | [Slayer Rewards](https://oldschool.runescape.wiki/w/Slayer_Rewards), RuneLite `VarbitID` reward constants | Reviewed live varbits prove ownership for permanent/toggle rewards and extensions. Current retained costs include Bigger and Badder 50, Malevolent Masquerade 400, Broader Fletching 300, Ring Bling 150, Like a Boss 200, Hot Stuff 100, and Task Storage 500. Cosmetic rewards do not enter strategy. |
| Reviewed task economics | [Slayer training](https://oldschool.runescape.wiki/w/Slayer_training), [Duradel](https://oldschool.runescape.wiki/w/Duradel), [Nieve](https://oldschool.runescape.wiki/w/Nieve), [Chaeldar](https://oldschool.runescape.wiki/w/Chaeldar), [Konar quo Maten](https://oldschool.runescape.wiki/w/Konar_quo_Maten), and matching Slayer-task pages | Fifty-four detailed families have qualitative XP, resource, duration, setup, attention, required-item, risk, current master-specific assignment weight, and alternative properties. The high-level Duradel, Nieve, Chaeldar, and Konar tables were rechecked directly on 2026-08-29. Exact kill rates and supply consumption are deliberately not retained. |

When any retained mechanic changes, update the relevant typed catalog/service,
its semantic tests, the audit date above, and this row together.

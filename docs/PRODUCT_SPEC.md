# OSRS Strategist Product Spec

## Mission
OSRS Strategist answers one question: **What is the best useful thing I can do next?**

The player should normally see only:
1. Recommended
2. Relaxed alternative
3. Progression alternative
4. Ready opportunities such as herb runs, birdhouses, or clues

The engine may be complex. The panel should not be.

## Supported account families
- Main
- Ironman
- Hardcore Ironman
- Group Ironman
- Hardcore Group Ironman
- Ultimate Ironman

## Core decision path
Goal -> valid methods -> requirements -> observed resources -> account restrictions -> player preferences -> session context -> score -> recommendation.

No recommendation may skip a required capability check.

## Main account GP planning
Before recommending a GE purchase, compare:
- cash on hand
- banked materials
- protected items
- safely disposable tradeable items
- likely sale value
- best unlocked money-making routes
- time to earn missing GP
- time to gather materials directly
- long-term cost of selling an item needed by another goal

Never sell or buy automatically.

## Ironman
Prefer self-sufficient resource and unlock paths. No GE assumptions.

## Group Ironman
Behave like an Ironman by default. If **Use Group Storage** is enabled, count only items the plugin has actually observed in Group Storage. Never make "ask your teammate" a normal recommendation.

## Ultimate Ironman
Use a capability graph. A strategy such as POH storage is valid only after the plugin verifies the required house, room/furniture where applicable, and item compatibility.

Storage/capability families to model:
- inventory
- looting bag
- POH costume/storage furniture
- STASH units
- Tool Leprechaun tool storage
- seed box and other containers
- minigame/activity storage where relevant
- death storage
- deathpile

Unknown must remain unknown. Never pretend a capability exists.

## Farming / Tool Leprechaun
Farm-run preparation must account for Tool Leprechaun storage, especially for UIM. A tool can count as available only if the plugin has observed or the player has confirmed it is stored there. The engine should distinguish:
- in inventory
- in bank
- in Group Storage, if enabled
- stored with Tool Leprechaun
- unavailable

## Birdhouse run preparation
Before calling the player ready, verify the current run's requirements. The UI should show green checks for items already available and a missing-item line for anything absent. Typical categories include:
- clockworks / birdhouses as appropriate
- correct logs or completed birdhouses for the account's Hunter level
- suitable seeds
- hammer/chisel when the chosen preparation method needs them
- transport/access requirements

The exact list must come from game data rather than hard-coded UI text once the data layer is built.

## Herb run preparation
Only show patches the player can actually use/reach. Check:
- suitable herb seeds
- seed dibber
- spade
- rake only when the patch state actually requires it
- compost strategy if used
- teleports/access
- Tool Leprechaun storage where observed

## Clues
Clues are opportunities, not constant alarms.

Track:
- clue tier
- where the clue is visible: inventory or bank snapshot
- age / time deferred
- repeated player skips
- whether supplies are ready
- STASH built/fill state where observable
- required emote/combat items
- common clue supplies such as spade and teleports

Clue score should slowly decrease when repeatedly skipped, but may rise again when the account becomes fully prepared or when a strategic reason makes doing the clue useful.

## Preference learning
Feedback actions:
- Do this: small positive weight
- Later: tiny negative weight
- Not today: moderate temporary suppression
- Dislike: stronger negative weight

Preferences move slowly. Important unlocks can still override dislike.

## Strategy styles
- Efficient: time and unlock value dominate
- Balanced: efficiency plus variety
- Relaxed: low-attention and comfortable methods score higher

## Quest tolerance
- Low
- Normal
- High

Quest tolerance changes frequency, not whether critical quests exist.

## Session intent
Planned inputs:
- 20 minutes
- 1 hour
- long session
- AFK / working
- surprise me

## Confidence
Every recommendation receives:
- Verified
- Check needed
- Blocked

## Safety
Never automate clicks, movement, purchases, sales, combat, skilling, banking, death actions, or other game inputs.
Irreversible or risky UIM/death/sale advice must display a warning and the assumptions used.

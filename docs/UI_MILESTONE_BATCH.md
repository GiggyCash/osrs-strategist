# UI 2.0 and Milestone Tracking

This note records the design rules introduced by the `feature/ui-milestone-batch` work so future changes do not accidentally undo the intended feel of Compass.

## Visual direction

Compass should look like a polished RuneLite tool with a small amount of Old School RuneScape flavor. It should not look like a parchment website squeezed into the sidebar.

The default visual language is:

- RuneLite charcoal/dark-gray surfaces.
- Muted OSRS-like gold for emphasis rather than bright yellow everywhere.
- RuneScape's own skill sprites loaded through RuneLite's `SpriteManager`.
- Compact cards with restrained borders.
- Small progress bars for skill checkpoints.
- Green only for genuine success/completion states.
- No constant animations, glowing panels, or decorative clutter.

Colors belong in `StrategistTheme`. Skill icon loading belongs in `SkillIconLoader`. Avoid scattering replacement RGB values and sprite lookup logic throughout the panel.

## Information hierarchy

The default panel should answer these questions quickly:

1. Who/what account am I planning for?
2. What big goal and strategy profile are active?
3. What should I do next?
4. What checkpoint am I working toward?
5. What is the best currently-supported method?
6. Is the method verified or does something still need checking?
7. What are my two best alternatives and current opportunities?

Deep instructions remain behind the `Details` button. The strategy engine may become very sophisticated without turning the default sidebar into a wall of text.

## Natural milestone completion

A player never has to press `Do This` for Compass to recognize progress.

The top skill recommendation becomes a `TrackedMilestone`. RuneLite `StatChanged` events cause the account to be reread. When the observed skill level reaches the tracked target, `MilestoneTracker` emits a `MilestoneCompletion`.

Example:

```text
DO NEXT
Train Farming to 10

Player trains normally without touching Compass.

Farming reaches 10.

Compass detects completion, reranks the account, and shows a brief completion banner.
```

The tracked milestone is stored per RuneScape profile through `AccountMilestoneStore`, so restarting RuneLite does not intentionally erase the checkpoint being watched.

## Variety is soft, not forced

Completing a milestone does **not** put that skill on cooldown and does **not** teach the preference model that the player dislikes it.

Instead, `PreferenceProfile` receives a temporary soft score adjustment. The initial implementation is a `-10` score adjustment for 30 minutes.

That usually encourages a fresh recommendation after something like Farming 1 -> 10, but the same skill can remain #1 when continuing it is clearly the stronger strategic decision. This preserves the principle that player variety should influence planning without overruling important progression.

Long-term preference, explicit cooldowns, and temporary score adjustments must remain separate concepts.

## Completion banner

The completion banner is intentionally brief and non-modal. It appears for roughly eight seconds, reports the completed level range, and then gets out of the way. It should never require dismissal or interrupt gameplay.

## Native skill icons

`SkillIconLoader` maps RuneLite API `Skill` values to RuneLite's current `HiscoreSkill` enum by matching enum names, then retrieves the official skill sprite through `SpriteManager`.

Do not bundle copied Wiki skill icons or custom approximations while RuneLite already exposes the current game sprites. If RuneLite changes this API, verify the replacement against the current official RuneLite repository before modifying the loader.

## Future UI work

Good future additions include compact opportunity countdowns, small verified/check-needed badges, current mission display, and tasteful goal-completion history. Any addition should preserve the scan-first rule and should be tested at the actual RuneLite sidebar width.

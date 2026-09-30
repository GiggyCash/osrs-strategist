# Existing gem method: flow evidence and ranking decision

Research date: 2026-09-30. Audited development commit:
`cce6379940d104bfa4d8f7dde6611b9d8a3daa36`.
No gem method contract is enabled by this document.

## Verified existing-route facts

Current Wiki recipe templates agree with the six existing reviewed input rows.
Each consumes one uncut gem and retains one nonstackable cut gem. A reusable
[chisel](https://oldschool.runescape.wiki/w/Chisel) is required (item1755,
revision15341754). No disposal or output stacking should be assumed.

| Recipe/source | Level | Output ID | Wiki revision |
|---|---:|---:|---:|
| [Sapphire](https://oldschool.runescape.wiki/w/Sapphire) | 20 | 1607 | 15357748 |
| [Emerald](https://oldschool.runescape.wiki/w/Emerald) | 27 | 1605 | 15357750 |
| [Ruby](https://oldschool.runescape.wiki/w/Ruby) | 34 | 1603 | 15357755 |
| [Diamond](https://oldschool.runescape.wiki/w/Diamond) | 43 | 1601 | 15357761 |
| [Dragonstone](https://oldschool.runescape.wiki/w/Dragonstone) | 55 | 1615 | 15357763 |
| [Onyx](https://oldschool.runescape.wiki/w/Onyx) | 67 | 6573 | 15357764 |

The first four recipes are F2P; dragonstone/onyx are members. The existing
crafting_gems training method itself is members-only and this migration should
not silently broaden that method's membership. Soft gems and zenyte remain
outside its reviewed references. The earlier zenyte XP disagreement is not
resolved by this research.

## Architecture issue before enabling the full contract

MethodReadiness.adjustment charges a fixed burden per BUY step, regardless of
purchaseCost. MethodReadinessService breaks equal-adjustment ties by recipe
level. Purchase cost currently proves affordability but does not rank recipe
value. For two recipes with equal preparation shape and verified affordable
quotes, the higher-level recipe can win even if it costs far more.

The current Onyx page explicitly discourages using it for training because of
potential coin losses and acquisition difficulty. Therefore mechanically valid
inputs, outputs and sufficient cash do not establish a good recommendation.
The same policy gap may affect other contracted recipes; it is not a reason to
add an onyx-specific Java exception.

Before enabling crafting_gems, choose a generic recipe-value policy that can
consider purchase cost, retained output value, verified XP/action, account goals
and ownership opportunity cost. Candidate options for reviewer discussion:

1. Reuse the existing strategy-value boundary with recipe-level evidence, keeping
   unresolved value from manufacturing a purchase preference.
2. Introduce an explicit reviewed per-recipe intent/acquisition restriction while
   the general value model is built. This is a product restriction, not a claim
   that cutting the item is mechanically impossible.

Do not substitute a blanket lower-price or higher-level rule for this decision:
both can conflict with XP efficiency, player goals and owned-resource value.
Do not equate Iron self-sourcing with zero opportunity cost. No schema or runtime
behavior has changed while the policy question is pending.

# Feature Matrix

| Feature | Main | Iron | GIM | UIM |
|---|---|---|---|---|
| Adaptive goals | Yes | Yes | Yes | Yes |
| F2P/P2P hard gating | Yes | Yes | Yes | Yes |
| Strategy/session/quest preferences | Yes | Yes | Yes | Yes |
| Learned recommendation preferences | Yes | Yes | Yes | Yes |
| Later / Not Today cooldowns | Yes | Yes | Yes | Yes |
| Wilderness methods opt-in | Yes | Yes | Yes | Yes |
| In-game method guidance | Yes | Yes | Yes | Yes |
| GE candidate logic | Yes | No | No | No |
| Verified affordability/time comparison | Yes | N/A | N/A | N/A |
| Protected-item sale guard | Yes | Yes | Yes | Yes |
| Normal bank-aware readiness | Yes | Yes | Yes | No |
| Self-sufficient resource paths | Optional | Yes | Yes | Yes |
| Group Storage | No | No | Optional + observed | No |
| UIM observed storage contents | N/A | N/A | N/A | Capability-gated |
| POH storage checks | Useful | Useful | Useful | Critical/capability-gated |
| STASH checks | Useful | Useful | Useful | Critical/capability-gated |
| Tool Leprechaun storage | Helpful | Helpful | Helpful | Critical/capability-gated |
| Looting bag | N/A | N/A | N/A | Capability/item/precondition gated |
| Death storage / deathpile | N/A | N/A | N/A | High/irreversible risk gate |
| Birdhouse readiness | Yes | Yes | Yes | Yes |
| Herb/tree run readiness | Yes | Yes | Yes | Yes |
| Farming live patch checklist | Yes | Yes | Yes | Yes |
| Clue reminders / age signal | Yes | Yes | Yes | Yes |
| Clue/STASH prep model | Yes | Yes | Yes | Yes |
| Long CLOG/outfit objective protection | Yes | Yes | Yes | Yes |
| Typed goal paths | Yes | Yes | Yes | Yes |
| PvM readiness seam | Yes | Yes | Yes | Yes |
| Gear / CA / minigame / transport / POH domains | Typed | Typed | Typed | Typed |
| Local core works offline | Yes | Yes | Yes | Yes |
| Future Plus hosted capabilities | Optional | Optional | Optional | Optional |

## Notes

`Typed` means the domain has a structured home and shared strategy-pipeline integration, not that every OSRS record in that domain is already encoded.

UIM never falls back to a normal bank. A storage system must be verified, and item-specific routes additionally require compatibility and current preconditions/capacity. Observed storage contents can count toward readiness only when the corresponding capability is verified.

Future Plus services are intentionally separate from the local planner. No remote service is active in the current build.

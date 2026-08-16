# Healthy Engagement Design

OSRS Strategist should help a player enjoy RuneScape on their own terms. It must not become a retention machine that pressures the player to keep playing.

## Product rule

Strategic correctness comes first. Account state, requirements, risk policy, the player's explicit goal, account mode, and explicit feedback all outrank engagement heuristics. Healthy-engagement scoring is a small final adjustment used only to distinguish otherwise-similar recommendations.

## Motivation principles

The design borrows cautiously from Self-Determination Theory (SDT), especially the needs for autonomy and competence. In game research, need satisfaction has been associated with stronger intrinsic motivation and more positive player experience. Strategist applies that idea in a narrow way:

- **Autonomy:** the player chooses strategy style, session intent, quest tolerance, Wilderness policy, collectionist mode, and activity-variety preference. `Later`, `Not Today`, and `Dislike` are treated as information rather than failure.
- **Competence:** recommendations should be concrete, achievable, and account-aware. Readiness checks explain what is known and what still has to be verified.
- **Variety without disruption:** repeated avoidance or a long run of completed activities in one broad family can slightly favor a fresh alternative. One skip is not enough to infer category fatigue.
- **Momentum protection:** progression-protected objectives such as an outfit or important untradeable grind are exempt from completion-based variety penalties.

These are product heuristics, not a diagnosis of boredom, fatigue, addiction, ADHD, burnout, or any other psychological state.

## Explicitly prohibited engagement patterns

Strategist should not implement:

- daily-login streaks or penalties for breaking a streak;
- artificial scarcity or countdown pressure unrelated to real OSRS mechanics;
- variable/random rewards designed only to drive repeated plugin interaction;
- guilt language for leaving, skipping, or changing goals;
- fake urgency;
- hidden penalties for not playing;
- forced break prompts based solely on elapsed time;
- attempts to infer a mental-health condition from gameplay telemetry.

Real OSRS cooldowns, farming timers, clues, and similar game-state facts are allowed because they describe the game rather than manufacturing urgency.

## Current implementation

`HealthyEngagementPolicy` groups activities into broad families and reads the bounded per-character `RecommendationHistory`.

- A single `Later`, `Not Today`, or `Dislike` does not penalize the entire family.
- Two or more recent avoidance events may create a time-decayed family penalty.
- Three or more recent completions in one family may create a small freshness penalty.
- Progression-protected methods ignore the completion repetition penalty.
- The final family adjustment is capped at 5 recommendation-score points before the user's `Activity variety` multiplier is allowed to influence a near-tie.
- Exact activity preference and cooldown behavior remains in `PreferenceProfile`.

## Research references

- Ryan, R. M., & Deci, E. L. (2000). *The “what” and “why” of goal pursuits: Human needs and the self-determination of behavior*. Psychological Inquiry, 11(4), 227-268.
- Ryan, R. M., Rigby, C. S., & Przybylski, A. (2006). *The motivational pull of video games: A self-determination theory approach*. Motivation and Emotion, 30, 344-360.
- Mills, D. J., Milyavskaya, M., Mettler, J., Heath, N. L., & Derevensky, J. L. (2018). *How do passion for video games and needs frustration explain time spent gaming?* British Journal of Social Psychology, 57(2), 461-481. DOI: 10.1111/bjso.12239.
- Pyszkowska, A., Nowacki, A., & Dziura, N. (2026). *Game on but pay the price: Hyperfocus, flow, escapism, self-efficacy, and burnout among video gamers with ADHD traits*. Research in Developmental Disabilities, 170, 105241. DOI: 10.1016/j.ridd.2026.105241.

The references justify keeping motivation player-centered. They do not justify treating Strategist's behavioral history as a psychological assessment.

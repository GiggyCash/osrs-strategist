# Maintenance Plan for a Non-Coder Owner

## Rule 1: the plugin must not rewrite itself
Self-modifying production code would make review, debugging, and RuneLite Plugin Hub distribution harder.

## Rule 2: make game knowledge easy to update
Long-term game content should live in structured data where possible. The Java engine remains stable while activities, requirements, XP estimates, clue items, and new content are updated separately.

## Rule 3: automate detection, not blind publication
Future GitHub Actions should:
- build on every push
- run strategy tests
- detect stale RuneLite compatibility
- flag changed game data
- open/update a report for review

Do not let an automated scraper silently publish strategy changes.

## Your normal update workflow
1. Jagex updates OSRS.
2. Automated checks or community reports flag likely affected content.
3. You send the repo/error/update note to ChatGPT.
4. We update game data or code.
5. Tests run.
6. You test in the development client.
7. Push the reviewed change.
8. Update Plugin Hub commit when needed.

## Regression tests we must build
At minimum:
- rich Main should prefer sensible GE purchases
- poor Main should compare money making, liquidation, and gathering
- Iron should never recommend GE
- GIM storage ON should use observed Group Storage
- GIM storage OFF should behave like Iron
- UIM without POH storage must not be told to use it
- UIM with unknown Tool Leprechaun contents must not assume tools are there
- UIM with confirmed Tool Leprechaun tools may count them for farm-run prep
- clue dislike should lower clue frequency gradually, not permanently
- critical quest requirement may still surface despite low quest tolerance
- "Not today" should suppress immediate repeat recommendations

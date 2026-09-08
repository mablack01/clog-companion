# Clog Companion

A RuneLite plugin that helps you decide which Collection Log slots to go for next. Every slot
gets a realistic time estimate — not just its drop rate, but how long each attempt actually
takes (a clue casket is quick to open, but the clue behind it is not) — and a tier:
**Easy**, **Medium**, **Long**, or **Grind**. Filter by tier, category, requirements and what
you already own, sort by fastest, or roll a random target.

## How estimates work

```
attempts = 1 / rate                         (Expected mode, default)
         = ln(0.5) / ln(1 - rate)           (Likely mode: the median player)
minutes  = setupMinutes + attempts × minutesPerAttempt
```

`minutesPerAttempt` covers everything it takes to roll the drop table once — travel, the boss
kill, the minigame round, or acquiring *and* solving the clue. Tier thresholds default to
1 h / 5 h / 25 h and are configurable.

## Data

| File | Origin |
|---|---|
| `src/main/resources/com/clogcompanion/clog-items.json` | Generated from the OSRS Wiki (entry structure + each item's drop sources). Do not hand-edit. |
| `src/main/resources/com/clogcompanion/clog-sources.json` | Hand-curated: per-attempt time, setup time, and requirements for every log entry. |

## Development

```bash
./gradlew test            # unit tests, including a data-integrity check
./gradlew run             # launch a RuneLite dev client with the plugin loaded
python3 .github/token-budget.py   # Plugin Hub review-budget estimate (src/main/java only)
```

Data and tests are free for the Plugin Hub's review budget; only `src/main/java` counts, so
logic stays in Java and bulk data stays in JSON.

## License

BSD 2-Clause. See `LICENSE`.

# Clog Companion

A RuneLite plugin that helps you decide which Collection Log slots to go for next. Collection log tiers are determined by a few factors such as challenge to obtain, time length, requirements, etc. to fit into the following categories: 
**Easy**, **Medium**, **Long**, or **Grind**. In the side panel you can filter by tier, category, requirements and what
you already own, sort by fastest, or roll a random target. For example while it is "quick" to loot a clue scroll they take time to complete, therefore hard clue rewards are weighed a bit higher despite being an easy task to do. Additionally things like charged ice or coagulated venom are harder to do since they require speed killing bosses, so despite it being < 5 minutes to get the kill it is weighed more due to the challenge.

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

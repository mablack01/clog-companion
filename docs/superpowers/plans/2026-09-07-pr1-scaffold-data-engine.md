# Clog Companion PR1 — Scaffold, Data, Engine — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A buildable, CI-gated RuneLite plugin skeleton with the full collection-log dataset and a unit-tested difficulty/requirements/filter engine — no UI yet.

**Architecture:** Bulk data is JSON under `src/main/resources` (free for the Hub review budget); Java under `src/main/java` holds only logic. `model` and `engine` never import `net.runelite.api.Client`; they take `AccountState` values. A Python generator in `tools/` produces `clog-items.json` from the OSRS Wiki; humans curate `clog-sources.json`.

**Tech Stack:** Java 11, Gradle 8.10, RuneLite `latest.release`, Lombok 1.18.30, Gson (from RuneLite classpath), JUnit 4.12, Mockito, Python 3 (tools only).

**Spec:** `docs/superpowers/specs/2026-09-07-clog-companion-design.md`

## Global Constraints

- Java 11 (`options.release.set(11)`); no new Gradle dependencies.
- Follow `AGENTS.md`: `@Inject Gson`, `gameval` constants, no reflection, no `Thread.sleep`, cleanup in `shutDown()`.
- No `com.example` / `Example*` remnants anywhere.
- Package root `com.clogcompanion`; config group `"clog-companion"`.
- `src/main/java` token count must stay under `.github/token-ceiling`.
- Commit messages: plain conventional commits, no AI attribution lines.
- Resources are read with `Class.getResourceAsStream()`.

---

## File map

| Path | Responsibility |
|---|---|
| `runelite-plugin.properties`, `settings.gradle`, `build.gradle`, `README.md`, `LICENSE`, `icon.png` | Hub metadata and build |
| `.github/workflows/ci.yml`, `.github/token-ceiling`, `tools/token-budget.py` | Build + test + review-budget gate |
| `src/main/java/com/clogcompanion/ClogCompanionPlugin.java` | Wiring only |
| `src/main/java/com/clogcompanion/ClogCompanionConfig.java` | Config |
| `src/main/java/com/clogcompanion/model/{Tier,Category,Requirements,ClogSource,ClogItem,AccountState}.java` | Plain data |
| `src/main/java/com/clogcompanion/data/ClogDataset.java` | Load + join + index the two JSON resources |
| `src/main/java/com/clogcompanion/engine/{DifficultyEngine,EstimateMode,RequirementChecker,ClogFilter,ClogSorter}.java` | Pure logic |
| `src/main/resources/com/clogcompanion/clog-items.json` | Generated slots |
| `src/main/resources/com/clogcompanion/clog-sources.json` | Curated entries |
| `tools/generate_clog_data.py`, `tools/rate_overrides.json`, `tools/entry_pages.json` | Generator + its hand-maintained inputs |
| `src/test/java/com/clogcompanion/**` | Tests |

---

### Task 1: Rename the template into Clog Companion

**Files:**
- Modify: `runelite-plugin.properties`, `settings.gradle`, `build.gradle`, `README.md`
- Create: `LICENSE` (BSD-2-Clause), `icon.png` (32×32)
- Move: `src/main/java/com/example/*` → `src/main/java/com/clogcompanion/`, `src/test/java/com/example/*` → `src/test/java/com/clogcompanion/`

**Produces:** `ClogCompanionPlugin`, `ClogCompanionConfig` (`@ConfigGroup("clog-companion")`), `ClogCompanionPluginTest.main`.

- [ ] **Step 1: Move and rename sources**

```bash
git mv src/main/java/com/example src/main/java/com/clogcompanion
git mv src/test/java/com/example src/test/java/com/clogcompanion
git mv src/main/java/com/clogcompanion/ExamplePlugin.java src/main/java/com/clogcompanion/ClogCompanionPlugin.java
git mv src/main/java/com/clogcompanion/ExampleConfig.java src/main/java/com/clogcompanion/ClogCompanionConfig.java
git mv src/test/java/com/clogcompanion/ExamplePluginTest.java src/test/java/com/clogcompanion/ClogCompanionPluginTest.java
```

- [ ] **Step 2: Rewrite `ClogCompanionPlugin.java`**

```java
package com.clogcompanion;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Clog Companion",
	description = "Ranks Collection Log slots by how quick they are to get",
	tags = {"collection log", "clog", "completionist", "drop rate"}
)
public class ClogCompanionPlugin extends Plugin
{
	@Inject
	private ClogCompanionConfig config;

	@Override
	protected void startUp()
	{
		log.debug("Clog Companion started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Clog Companion stopped");
	}

	@Provides
	ClogCompanionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClogCompanionConfig.class);
	}
}
```

- [ ] **Step 3: Rewrite `ClogCompanionConfig.java`** (config items are added in Task 8; keep the interface empty-bodied for now)

```java
package com.clogcompanion;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup("clog-companion")
public interface ClogCompanionConfig extends Config
{
}
```

- [ ] **Step 4: Rewrite the test launcher**

```java
package com.clogcompanion;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClogCompanionPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClogCompanionPlugin.class);
		RuneLite.main(args);
	}
}
```

- [ ] **Step 5: Metadata**

`runelite-plugin.properties`:
```
displayName=Clog Companion
author=mablack01
description=Ranks Collection Log slots by how quick they are to get, with filters and a random roll
tags=collection log,clog,completionist,drop rate,pets
plugins=com.clogcompanion.ClogCompanionPlugin
build=standard
```

`settings.gradle`: `rootProject.name = 'clog-companion'`.
`build.gradle`: `def pluginMainClass = 'com.clogcompanion.ClogCompanionPluginTest'`, `group = 'com.clogcompanion'`, and add `testImplementation 'org.mockito:mockito-core:5.7.0'`.
`README.md`: one paragraph on purpose, a "Development" section (`./gradlew run`, `./gradlew test`, `python3 tools/generate_clog_data.py`), and a "Data" section pointing at the two JSON files and how tiers are computed.
`LICENSE`: BSD-2-Clause, copyright 2026 mablack01.
`icon.png`: generate a 32×32 PNG with Pillow (a book glyph on transparent background) — must be a real PNG.

- [ ] **Step 6: Verify no template remnants and the build passes**

Run: `grep -rn "example\|Example" --include=*.java --include=*.gradle --include=*.properties . ; ./gradlew build -q`
Expected: no matches; BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "chore: rename template to Clog Companion, add license and icon"
```

---

### Task 2: CI with build, tests, and the review-budget gate

**Files:**
- Create: `tools/token-budget.py` (copy of flip-smart's; change the `replace("com/flipsmart/", "")` to `"com/clogcompanion/"`), `.github/token-ceiling`, `.github/workflows/ci.yml`

- [ ] **Step 1: Copy the estimator** from `~/Projects/flip-smart-runelite-plugin/tools/token-budget.py`, adjust the package prefix, `chmod +x`.

- [ ] **Step 2: Write `.github/workflows/ci.yml`**

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
permissions:
  contents: read
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '11' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew build --no-daemon
      - name: Review token budget
        run: python3 tools/token-budget.py --ceiling "$(tr -dc '0-9' < .github/token-ceiling)"
```

- [ ] **Step 3: Set the ceiling**: run `python3 tools/token-budget.py`, write `ceiling = measured + 30` into `.github/token-ceiling` — but not before Task 8 finishes; for now write `20000` and re-tighten in Task 8.

- [ ] **Step 4: Commit** `ci: build, test, and review-budget gate`.

---

### Task 3: Model types

**Files:**
- Create: `src/main/java/com/clogcompanion/model/Tier.java`, `Category.java`, `Requirements.java`, `ClogSource.java`, `ClogItem.java`, `AccountState.java`
- Test: `src/test/java/com/clogcompanion/model/RequirementsTest.java`

**Produces:**
```java
enum Tier { EASY, MEDIUM, LONG, GRIND, UNRATED }
enum Category { BOSSES, RAIDS, CLUES, MINIGAMES, OTHER }
@Value class Requirements { Map<String,Integer> skills; List<String> quests; List<String> diaries; static Requirements none(); }
@Value class ClogSource { String id; String name; Category category; Integer minutesPerAttempt; Integer setupMinutes; Requirements requirements; String notes; boolean isRated(); }
@Value class ClogItem { String id; String sourceId; int itemId; String name; Double rate; String rateText; String wikiUrl; }
@Value class AccountState { Map<String,Integer> skillLevels; Set<String> completedQuests; Set<String> completedDiaries; static AccountState empty(); }
```
Gson deserialises `@Value` classes directly (final fields are set by Gson's reflective adapter — this is RuneLite-standard and not "plugin reflection"). Null collections must be normalised: `Requirements` getters return empty collections when the JSON omitted them.

- [ ] **Step 1: Failing test**

```java
package com.clogcompanion.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class RequirementsTest
{
	@Test
	public void nullCollectionsReadAsEmpty()
	{
		Requirements r = new Requirements(null, null, null);
		assertTrue(r.getSkills().isEmpty());
		assertTrue(r.getQuests().isEmpty());
		assertTrue(r.getDiaries().isEmpty());
	}

	@Test
	public void sourceWithoutTimesIsUnrated()
	{
		ClogSource s = new ClogSource("x", "X", Category.OTHER, null, 0, Requirements.none(), null);
		assertFalse(s.isRated());
		assertTrue(new ClogSource("x", "X", Category.OTHER, 5, 0, Requirements.none(), null).isRated());
	}
}
```

- [ ] **Step 2: Run** `./gradlew test --tests 'com.clogcompanion.model.*'` → compile failure.
- [ ] **Step 3: Implement** the six types. `Requirements`:

```java
package com.clogcompanion.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Requirements
{
	Map<String, Integer> skills;
	List<String> quests;
	List<String> diaries;

	public static Requirements none()
	{
		return new Requirements(Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
	}

	public Map<String, Integer> getSkills()
	{
		return skills == null ? Collections.emptyMap() : skills;
	}

	public List<String> getQuests()
	{
		return quests == null ? Collections.emptyList() : quests;
	}

	public List<String> getDiaries()
	{
		return diaries == null ? Collections.emptyList() : diaries;
	}
}
```

`ClogSource.isRated()` = `minutesPerAttempt != null && setupMinutes != null`; `getRequirements()` returns `Requirements.none()` when null. `AccountState.empty()` returns empty collections.

- [ ] **Step 4: Run tests** → PASS. **Step 5: Commit** `feat(model): collection log data types`.

---

### Task 4: DifficultyEngine

**Files:**
- Create: `engine/EstimateMode.java`, `engine/DifficultyEngine.java`
- Test: `src/test/java/com/clogcompanion/engine/DifficultyEngineTest.java`

**Produces:**
```java
enum EstimateMode { EXPECTED, LIKELY }
class DifficultyEngine {
  DifficultyEngine(EstimateMode mode, int easyMax, int mediumMax, int longMax);
  OptionalDouble expectedMinutes(ClogItem item, ClogSource source);  // empty when unrated
  Tier tier(ClogItem item, ClogSource source);
  static String formatMinutes(double m);   // "45m", "2h 10m", "3d 4h"
}
```

- [ ] **Step 1: Failing tests**

```java
package com.clogcompanion.engine;

import static org.junit.Assert.*;
import com.clogcompanion.model.*;
import org.junit.Test;

public class DifficultyEngineTest
{
	private static final DifficultyEngine EXPECTED = new DifficultyEngine(EstimateMode.EXPECTED, 60, 300, 1500);
	private static final DifficultyEngine LIKELY = new DifficultyEngine(EstimateMode.LIKELY, 60, 300, 1500);

	private static ClogSource source(Integer perAttempt, Integer setup)
	{
		return new ClogSource("s", "S", Category.BOSSES, perAttempt, setup, Requirements.none(), null);
	}

	private static ClogItem item(Double rate)
	{
		return new ClogItem("s:i", "s", 1, "I", rate, "1/x", null);
	}

	@Test
	public void expectedModeIsOneOverRate()
	{
		assertEquals(2.0 * 512, EXPECTED.expectedMinutes(item(1.0 / 512), source(2, 0)).getAsDouble(), 1e-6);
	}

	@Test
	public void likelyModeUsesMedianAttempts()
	{
		// ln(0.5)/ln(1-1/512) ≈ 354.5 attempts
		assertEquals(354.5, LIKELY.expectedMinutes(item(1.0 / 512), source(1, 0)).getAsDouble(), 0.5);
	}

	@Test
	public void setupIsAddedOnce()
	{
		assertEquals(100 + 10, EXPECTED.expectedMinutes(item(1.0), source(10, 100)).getAsDouble(), 1e-9);
	}

	@Test
	public void guaranteedDropIsOneAttempt()
	{
		assertEquals(5.0, EXPECTED.expectedMinutes(item(1.0), source(5, 0)).getAsDouble(), 1e-9);
	}

	@Test
	public void missingRateOrTimeIsUnrated()
	{
		assertFalse(EXPECTED.expectedMinutes(item(null), source(5, 0)).isPresent());
		assertFalse(EXPECTED.expectedMinutes(item(0.5), source(null, 0)).isPresent());
		assertEquals(Tier.UNRATED, EXPECTED.tier(item(null), source(5, 0)));
	}

	@Test
	public void tierThresholds()
	{
		assertEquals(Tier.EASY, EXPECTED.tier(item(1.0), source(60, 0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(61, 0)));
		assertEquals(Tier.MEDIUM, EXPECTED.tier(item(1.0), source(300, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(301, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0), source(1500, 0)));
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0), source(1501, 0)));
	}

	@Test
	public void clueCasketUniqueIsNotEasy()
	{
		// Looting is instant, but each attempt is a full master clue (~25 min) at 1/28,988 → GRIND,
		// while a 1/50 casket item at the same per-attempt cost lands LONG.
		assertEquals(Tier.GRIND, EXPECTED.tier(item(1.0 / 28988), source(25, 0)));
		assertEquals(Tier.LONG, EXPECTED.tier(item(1.0 / 50), source(25, 0)));
	}

	@Test
	public void formatMinutes()
	{
		assertEquals("45m", DifficultyEngine.formatMinutes(45));
		assertEquals("2h 10m", DifficultyEngine.formatMinutes(130));
		assertEquals("3d 4h", DifficultyEngine.formatMinutes(3 * 1440 + 4 * 60 + 12));
	}
}
```

- [ ] **Step 2: Run** → compile failure. **Step 3: Implement**

```java
package com.clogcompanion.engine;

import com.clogcompanion.model.ClogItem;
import com.clogcompanion.model.ClogSource;
import com.clogcompanion.model.Tier;
import java.util.OptionalDouble;

public class DifficultyEngine
{
	private static final double LIKELY_PROBABILITY = 0.5;

	private final EstimateMode mode;
	private final int easyMax;
	private final int mediumMax;
	private final int longMax;

	public DifficultyEngine(EstimateMode mode, int easyMax, int mediumMax, int longMax)
	{
		this.mode = mode;
		this.easyMax = easyMax;
		this.mediumMax = mediumMax;
		this.longMax = longMax;
	}

	public OptionalDouble expectedMinutes(ClogItem item, ClogSource source)
	{
		Double rate = item.getRate();
		if (rate == null || rate <= 0 || source == null || !source.isRated())
		{
			return OptionalDouble.empty();
		}
		return OptionalDouble.of(source.getSetupMinutes() + attempts(rate) * source.getMinutesPerAttempt());
	}

	public Tier tier(ClogItem item, ClogSource source)
	{
		OptionalDouble minutes = expectedMinutes(item, source);
		if (!minutes.isPresent())
		{
			return Tier.UNRATED;
		}
		double m = minutes.getAsDouble();
		if (m <= easyMax) return Tier.EASY;
		if (m <= mediumMax) return Tier.MEDIUM;
		if (m <= longMax) return Tier.LONG;
		return Tier.GRIND;
	}

	private double attempts(double rate)
	{
		if (rate >= 1.0)
		{
			return 1;
		}
		if (mode == EstimateMode.LIKELY)
		{
			return Math.log(1 - LIKELY_PROBABILITY) / Math.log(1 - rate);
		}
		return 1 / rate;
	}

	public static String formatMinutes(double minutes)
	{
		long total = Math.round(minutes);
		long days = total / 1440;
		long hours = (total % 1440) / 60;
		long mins = total % 60;
		if (days > 0) return days + "d " + hours + "h";
		if (hours > 0) return hours + "h " + mins + "m";
		return mins + "m";
	}
}
```

- [ ] **Step 4: Run** → PASS. **Step 5: Commit** `feat(engine): difficulty engine with expected/likely modes and tiers`.

---

### Task 5: RequirementChecker

**Files:**
- Create: `engine/RequirementChecker.java`
- Test: `src/test/java/com/clogcompanion/engine/RequirementCheckerTest.java`

**Produces:** `static List<String> unmet(Requirements req, AccountState state)` — empty list means met. Reasons: `"Slayer 93 (have 87)"`, `"Regicide not completed"`, `"Desert Hard diary not completed"`. Skill/quest/diary names are stored in the JSON as enum/constant names (`SLAYER`, `REGICIDE`, `DIARY_DESERT_HARD`); the checker formats them with `titleCase` (`REGICIDE` → `Regicide`, `DIARY_DESERT_HARD` → `Desert Hard diary`).

- [ ] **Step 1: Failing tests**

```java
package com.clogcompanion.engine;

import static org.junit.Assert.*;
import com.clogcompanion.model.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RequirementCheckerTest
{
	private static final Requirements REQ = new Requirements(
		ImmutableMap.of("SLAYER", 93),
		Collections.singletonList("REGICIDE"),
		Collections.singletonList("DIARY_DESERT_HARD"));

	@Test
	public void allMet()
	{
		AccountState state = new AccountState(ImmutableMap.of("SLAYER", 95),
			ImmutableSet.of("REGICIDE"), ImmutableSet.of("DIARY_DESERT_HARD"));
		assertTrue(RequirementChecker.unmet(REQ, state).isEmpty());
	}

	@Test
	public void reportsEveryUnmetReason()
	{
		AccountState state = new AccountState(ImmutableMap.of("SLAYER", 87), ImmutableSet.of(), ImmutableSet.of());
		assertEquals(Arrays.asList("Slayer 93 (have 87)", "Regicide not completed", "Desert Hard diary not completed"),
			RequirementChecker.unmet(REQ, state));
	}

	@Test
	public void missingSkillCountsAsLevelOne()
	{
		AccountState state = AccountState.empty();
		assertEquals("Slayer 93 (have 1)", RequirementChecker.unmet(REQ, state).get(0));
	}

	@Test
	public void noRequirementsIsAlwaysMet()
	{
		assertTrue(RequirementChecker.unmet(Requirements.none(), AccountState.empty()).isEmpty());
	}
}
```

- [ ] **Step 2: Run** → fails. **Step 3: Implement** (Guava is on RuneLite's classpath; tests may use it, main code uses plain `java.util`):

```java
package com.clogcompanion.engine;

import com.clogcompanion.model.AccountState;
import com.clogcompanion.model.Requirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RequirementChecker
{
	private RequirementChecker()
	{
	}

	public static List<String> unmet(Requirements req, AccountState state)
	{
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, Integer> e : req.getSkills().entrySet())
		{
			int have = state.getSkillLevels().getOrDefault(e.getKey(), 1);
			if (have < e.getValue())
			{
				out.add(titleCase(e.getKey()) + " " + e.getValue() + " (have " + have + ")");
			}
		}
		for (String quest : req.getQuests())
		{
			if (!state.getCompletedQuests().contains(quest))
			{
				out.add(titleCase(quest) + " not completed");
			}
		}
		for (String diary : req.getDiaries())
		{
			if (!state.getCompletedDiaries().contains(diary))
			{
				out.add(titleCase(diary.replaceFirst("^DIARY_", "")) + " diary not completed");
			}
		}
		return out;
	}

	static String titleCase(String constant)
	{
		StringBuilder sb = new StringBuilder();
		for (String word : constant.toLowerCase().split("_"))
		{
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}
		return sb.toString();
	}
}
```

- [ ] **Step 4: Run** → PASS. **Step 5: Commit** `feat(engine): requirement checker`.

---

### Task 6: ClogFilter and ClogSorter

**Files:**
- Create: `engine/ClogFilter.java`, `engine/ClogSorter.java`, `engine/RatedSlot.java`
- Test: `engine/ClogFilterTest.java`, `engine/ClogSorterTest.java`

**Produces:**
```java
@Value class RatedSlot { ClogItem item; ClogSource source; Tier tier; OptionalDouble minutes; List<String> unmet; boolean obtained; }
class ClogFilter {  // mutable criteria object; UI binds to it in PR2
  Set<Tier> tiers (default all); Category category (null = all); boolean hideObtained; boolean onlyMeetsRequirements; String search;
  boolean test(RatedSlot s);
}
enum ClogSorter implements Comparator<RatedSlot> { FASTEST, RAREST, NAME }
```
`RatedSlot` is what the UI lists; PR2 builds them by combining dataset + engine + obtained set.

- [ ] **Step 1: Failing tests**

```java
package com.clogcompanion.engine;

import static org.junit.Assert.*;
import com.clogcompanion.model.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.junit.Test;

public class ClogFilterTest
{
	private static RatedSlot slot(String name, Category cat, Tier tier, double minutes, boolean obtained, List<String> unmet)
	{
		ClogSource src = new ClogSource("s", "Src", cat, 1, 0, Requirements.none(), null);
		ClogItem it = new ClogItem("s:" + name, "s", 1, name, 0.5, "1/2", null);
		return new RatedSlot(it, src, tier, OptionalDouble.of(minutes), unmet, obtained);
	}

	private static final RatedSlot EASY_BOSS = slot("Whip", Category.BOSSES, Tier.EASY, 10, false, Collections.emptyList());
	private static final RatedSlot GRIND_CLUE = slot("3rd age", Category.CLUES, Tier.GRIND, 9999, false, Collections.emptyList());
	private static final RatedSlot OWNED = slot("Pet", Category.BOSSES, Tier.EASY, 10, true, Collections.emptyList());
	private static final RatedSlot LOCKED = slot("Fang", Category.RAIDS, Tier.MEDIUM, 100, false, Collections.singletonList("Slayer 93 (have 1)"));
	private static final List<RatedSlot> ALL = Arrays.asList(EASY_BOSS, GRIND_CLUE, OWNED, LOCKED);

	private static List<String> names(ClogFilter f)
	{
		return ALL.stream().filter(f::test).map(s -> s.getItem().getName()).collect(Collectors.toList());
	}

	@Test
	public void defaultFilterHidesObtainedOnly()
	{
		assertEquals(Arrays.asList("Whip", "3rd age", "Fang"), names(new ClogFilter()));
	}

	@Test
	public void tierAndCategory()
	{
		ClogFilter f = new ClogFilter();
		f.setTiers(EnumSet.of(Tier.EASY));
		f.setHideObtained(false);
		assertEquals(Arrays.asList("Whip", "Pet"), names(f));
		f.setCategory(Category.CLUES);
		assertTrue(names(f).isEmpty());
	}

	@Test
	public void requirementsAndSearch()
	{
		ClogFilter f = new ClogFilter();
		f.setOnlyMeetsRequirements(true);
		assertEquals(Arrays.asList("Whip", "3rd age"), names(f));
		f.setSearch("AGE");
		assertEquals(Collections.singletonList("3rd age"), names(f));
	}

	@Test
	public void searchMatchesSourceNameToo()
	{
		ClogFilter f = new ClogFilter();
		f.setSearch("src");
		assertEquals(3, names(f).size());
	}
}
```

```java
package com.clogcompanion.engine;

import static org.junit.Assert.*;
import com.clogcompanion.model.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;
import org.junit.Test;

public class ClogSorterTest
{
	private static RatedSlot slot(String name, Double rate, OptionalDouble minutes)
	{
		ClogSource src = new ClogSource("s", "Src", Category.BOSSES, 1, 0, Requirements.none(), null);
		return new RatedSlot(new ClogItem("s:" + name, "s", 1, name, rate, "", null), src,
			minutes.isPresent() ? Tier.EASY : Tier.UNRATED, minutes, Collections.emptyList(), false);
	}

	private static final RatedSlot B = slot("b", 0.5, OptionalDouble.of(20));
	private static final RatedSlot A = slot("a", 0.1, OptionalDouble.of(5));
	private static final RatedSlot C = slot("c", null, OptionalDouble.empty());

	private static List<String> order(ClogSorter s)
	{
		return Arrays.asList(C, B, A).stream().sorted(s).map(r -> r.getItem().getName()).collect(Collectors.toList());
	}

	@Test
	public void fastestPutsUnratedLast()
	{
		assertEquals(Arrays.asList("a", "b", "c"), order(ClogSorter.FASTEST));
	}

	@Test
	public void rarestIsAscendingRateUnknownLast()
	{
		assertEquals(Arrays.asList("a", "b", "c"), order(ClogSorter.RAREST));
	}

	@Test
	public void nameIsCaseInsensitiveAlphabetical()
	{
		assertEquals(Arrays.asList("a", "b", "c"), order(ClogSorter.NAME));
	}
}
```

- [ ] **Step 2: Run** → fails. **Step 3: Implement**

`RatedSlot` — `@Value` with the six fields above.

```java
package com.clogcompanion.engine;

import com.clogcompanion.model.Category;
import com.clogcompanion.model.Tier;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClogFilter
{
	private Set<Tier> tiers = EnumSet.allOf(Tier.class);
	private Category category;
	private boolean hideObtained = true;
	private boolean onlyMeetsRequirements;
	private String search = "";

	public boolean test(RatedSlot s)
	{
		if (hideObtained && s.isObtained()) return false;
		if (!tiers.contains(s.getTier())) return false;
		if (category != null && s.getSource().getCategory() != category) return false;
		if (onlyMeetsRequirements && !s.getUnmet().isEmpty()) return false;
		String q = search.trim().toLowerCase(Locale.ROOT);
		return q.isEmpty()
			|| s.getItem().getName().toLowerCase(Locale.ROOT).contains(q)
			|| s.getSource().getName().toLowerCase(Locale.ROOT).contains(q);
	}
}
```

```java
package com.clogcompanion.engine;

import java.util.Comparator;

public enum ClogSorter implements Comparator<RatedSlot>
{
	FASTEST
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return Double.compare(a.getMinutes().orElse(Double.MAX_VALUE), b.getMinutes().orElse(Double.MAX_VALUE));
		}
	},
	RAREST
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return Double.compare(rate(a), rate(b));
		}
	},
	NAME
	{
		@Override
		public int compare(RatedSlot a, RatedSlot b)
		{
			return String.CASE_INSENSITIVE_ORDER.compare(a.getItem().getName(), b.getItem().getName());
		}
	};

	private static double rate(RatedSlot s)
	{
		Double r = s.getItem().getRate();
		return r == null ? Double.MAX_VALUE : r;
	}
}
```

- [ ] **Step 4: Run** → PASS. **Step 5: Commit** `feat(engine): filter criteria and sort orders`.

---

### Task 7: Generator, data files, and ClogDataset

**Files:**
- Create: `tools/generate_clog_data.py`, `tools/entry_pages.json`, `tools/rate_overrides.json`
- Create: `src/main/resources/com/clogcompanion/clog-items.json` (generated), `clog-sources.json` (curated, all 124 entries)
- Create: `src/main/java/com/clogcompanion/data/ClogDataset.java`
- Test: `src/test/java/com/clogcompanion/data/ClogDatasetTest.java`

**Produces:**
```java
class ClogDataset {
  static ClogDataset load(Gson gson);   // reads both resources via ClogDataset.class.getResourceAsStream
  List<ClogItem> getItems(); Map<String, ClogSource> getSources();
  ClogSource sourceOf(ClogItem item);     // never null after validation
  Optional<ClogItem> byId(String id);
  List<ClogItem> byName(String itemName); // all slots sharing an item name (for chat-message marking)
  List<String> validate();                // problems: dangling sourceId, unknown quest/diary/skill names
}
```
`validate()` resolves names against `net.runelite.api.Skill`, `Quest`, and `Varbits` field names via `Enum.valueOf` / a static `Set` of `DIARY_*` names built from the `Varbits` constants declared in a static list (no reflection) — keep the diary list in `model/Diaries.java` as `static final Map<String,Integer> BY_NAME` mapping `"DIARY_DESERT_HARD"` → `Varbits.DIARY_DESERT_HARD`, covering all 48 (12 regions × 4 tiers).

**Generator behaviour (`tools/generate_clog_data.py`):**
1. `GET api.php?action=parse&page=Collection_log&prop=wikitext`. Walk lines: `==Tab==` sets category (only Bosses/Raids/Clues/Minigames/Other; stop at `==Duplicate entries==`); `===Entry===` starts a source; `{{plink|Name...}}` inside the following table adds a slot. Source id = `slugify(entry)`; slot id = `sourceId + ":" + slugify(name)`.
2. Resolve item ids: batch `action=query&prop=revisions&rvslots=main&rvprop=content&redirects=1&titles=A|B|…` (50 per call); take the first integer after `|id` inside the `{{Infobox Item` block; fall back to `{{Infobox Pet`/`Infobox Item` `id1`. Cache raw page text in `tools/.cache/` so re-runs are offline.
3. Resolve rates: for each source, page = `entry_pages.json[entry]` or the entry heading; parse `{{DropsLine|name=X|…|rarity=R|…rolls=N}}`: `R` as `a/b` → `a/b*N`; `Always` → 1.0; other words → null. Then apply `rate_overrides.json` (`{"sourceId:slug": 0.0123}`) on top. Missing → `null`.
4. Write `clog-items.json` as `{"_generated": {"wikiRevision": …, "date": …}, "items": [...]}` sorted by id.
5. Print: slot count, rated %, and any entry missing from `clog-sources.json` with a stub block.
Use `urllib` only (no third-party deps); `User-Agent: clog-companion-generator (github.com/mablack01/clog-companion)`; 200 ms sleep between calls.

**Curation (`clog-sources.json`):** one record per entry with realistic `minutesPerAttempt` (a full kill/round/clue including travel and banking), `setupMinutes`, and requirements as enum names. Every entry gets a number — `null` only if truly unknowable. Add `notes` where the per-attempt figure needs justification (clues: "includes getting the clue").

- [ ] **Step 1: Failing test**

```java
package com.clogcompanion.data;

import static org.junit.Assert.*;
import com.clogcompanion.model.ClogItem;
import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;

public class ClogDatasetTest
{
	private static final ClogDataset DATA = ClogDataset.load(new Gson());

	@Test
	public void loadsAFullLog()
	{
		assertTrue("expected >1000 slots, got " + DATA.getItems().size(), DATA.getItems().size() > 1000);
		assertTrue(DATA.getSources().size() >= 100);
	}

	@Test
	public void everyReferenceResolves()
	{
		List<String> problems = DATA.validate();
		assertTrue(String.join("\n", problems), problems.isEmpty());
	}

	@Test
	public void reportsUnratedCoverage()
	{
		long unrated = DATA.getItems().stream()
			.filter(i -> i.getRate() == null || !DATA.sourceOf(i).isRated()).count();
		System.out.println("UNRATED slots: " + unrated + " / " + DATA.getItems().size());
	}

	@Test
	public void lookupsWork()
	{
		ClogItem whip = DATA.byId("abyssal_sire:abyssal_whip").orElseThrow(AssertionError::new);
		assertEquals("Abyssal Sire", DATA.sourceOf(whip).getName());
		assertTrue(DATA.byName("Dragon pickaxe").size() > 1);
	}
}
```

- [ ] **Step 2: Run** → fails (no class / no data).
- [ ] **Step 3: Write the generator, `entry_pages.json`, run it, curate `clog-sources.json`** (all 124 entries).
- [ ] **Step 4: Implement `ClogDataset` and `model/Diaries`.** Loader:

```java
public static ClogDataset load(Gson gson)
{
	try (Reader items = open("clog-items.json"); Reader sources = open("clog-sources.json"))
	{
		ItemsFile f = gson.fromJson(items, ItemsFile.class);
		List<ClogSource> s = gson.fromJson(sources, new TypeToken<List<ClogSource>>() {}.getType());
		return new ClogDataset(f.items, s);
	}
	catch (IOException | RuntimeException e)
	{
		throw new IllegalStateException("Clog Companion data failed to load", e);
	}
}

private static Reader open(String name) throws IOException
{
	InputStream in = ClogDataset.class.getResourceAsStream(name);
	if (in == null) throw new IOException("missing resource " + name);
	return new InputStreamReader(in, StandardCharsets.UTF_8);
}
```
`ItemsFile` is a private static class `{ List<ClogItem> items; }`.

- [ ] **Step 5: Run** → PASS, note the printed UNRATED count. **Step 6: Commit** in two commits: `feat(data): wiki generator and initial collection log dataset`, then `feat(data): dataset loader with validation`.

---

### Task 8: Plugin wiring, config, ceiling

**Files:**
- Modify: `ClogCompanionPlugin.java`, `ClogCompanionConfig.java`, `.github/token-ceiling`, `README.md`

- [ ] **Step 1: Config items** — `estimateMode` (enum `EstimateMode`, default `EXPECTED`), `easyMaxMinutes` 60, `mediumMaxMinutes` 300, `longMaxMinutes` 1500, each with `name`/`description`, positions 0–3.
- [ ] **Step 2: Plugin** — `@Inject Gson gson;` in `startUp()` load `ClogDataset` off the client thread is unnecessary (resource read, ~200 KB, done once; acceptable on startUp which runs on the client thread? No — do it synchronously in `startUp`, it's a jar resource read taking milliseconds), keep it in a field, `log.debug("Loaded {} slots across {} entries", …)`. Expose `@Getter ClogDataset dataset` and a `DifficultyEngine engine()` factory reading config. `shutDown()` nulls the dataset.
- [ ] **Step 3: Verify** `./gradlew build` and `python3 tools/token-budget.py`; write `measured + 30` into `.github/token-ceiling`.
- [ ] **Step 4: Commit** `feat: load dataset on startup; config for estimate mode and tiers` and `ci: set token ceiling`.

---

### Task 9: Open PR1

- [ ] Push branch `feature/pr1-scaffold-data-engine`, open a **draft** PR against `main` with: summary, what's in / what's not (no UI yet), how to verify (`./gradlew test`, generator run), the UNRATED count, and the follow-up PR list from the spec.

---

## Self-review

- **Spec coverage:** scaffold ✔ (T1), CI + budget ✔ (T2), model ✔ (T3), difficulty ✔ (T4), requirements ✔ (T5), filter/sort ✔ (T6), data + generator + loader ✔ (T7), config ✔ (T8). UI, account reader, obtained tracker, roll → PR2–4 by design.
- **Type consistency:** `ClogSource(id, name, category, minutesPerAttempt, setupMinutes, requirements, notes)` and `ClogItem(id, sourceId, itemId, name, rate, rateText, wikiUrl)` are used identically in T3–T7; `RatedSlot(item, source, tier, minutes, unmet, obtained)` in T6 only.
- **Placeholders:** none.

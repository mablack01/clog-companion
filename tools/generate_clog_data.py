#!/usr/bin/env python3
"""Generate src/main/resources/com/clogcompanion/clog-items.json from the OSRS Wiki.

Reads the wiki's "Collection log" page for the tab -> entry -> item structure, resolves
item ids from each item's Infobox, and pulls per-attempt drop rates from each entry's
drop tables ({{DropsLine}}), then layers tools/rate_overrides.json on top for entries
whose rewards the wiki does not express as drop lines (minigames, clue uniques).

Everything is fetched through the MediaWiki API with urllib and cached under
tools/.cache/ so re-runs are offline and fast. Delete the cache to refresh.

Usage:
    python3 tools/generate_clog_data.py            # regenerate + coverage report
    python3 tools/generate_clog_data.py --report   # coverage report only, no write
"""

import argparse
import datetime
import hashlib
import html
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "clog-companion-generator (github.com/mablack01/clog-companion)"
TOOLS = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(TOOLS)
CACHE = os.path.join(TOOLS, ".cache")
RESOURCES = os.path.join(ROOT, "src", "main", "resources", "com", "clogcompanion")
ITEMS_OUT = os.path.join(RESOURCES, "clog-items.json")
SOURCES_FILE = os.path.join(RESOURCES, "clog-sources.json")
ENTRY_PAGES = os.path.join(TOOLS, "entry_pages.json")
RATE_OVERRIDES = os.path.join(TOOLS, "rate_overrides.json")

TABS = {"Bosses": "BOSSES", "Raids": "RAIDS", "Clues": "CLUES", "Minigames": "MINIGAMES", "Other": "OTHER"}
# Entries that collect items from many unrelated sources; any source's best rate applies.
CATCH_ALL_ENTRIES = {"All Pets", "Skilling Pets", "Slayer", "Miscellaneous", "Cyclopes", "Revenants",
                     "Elder Chaos Druids", "Tormented Demons", "TzHaar", "Random Events", "Champion's Challenge",
                     "Forestry", "Gnome Restaurant", "Boat Paints", "Sea Treasures", "Creature Creation",
                     "Glough's Experiments"}
PLINK = re.compile(r"\{\{plink\|([^}|]+)")
HEADING2 = re.compile(r"^==([^=].*?)==\s*$")
HEADING3 = re.compile(r"^===(.+?)===\s*$")
FRACTION = re.compile(r"^(\d[\d,]*(?:\.\d+)?)\s*/\s*(\d[\d,]*(?:\.\d+)?)$")
INFOBOX_ITEM = re.compile(r"\{\{Infobox (?:Item|Pet)\b", re.I)


# ---------------------------------------------------------------- wiki access

def _cached(key: str, fetch):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, hashlib.sha1(key.encode()).hexdigest() + ".json")
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            return json.load(fh)
    data = fetch()
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(data, fh)
    time.sleep(0.2)
    return data


def api(params: dict) -> dict:
    params = dict(params, format="json", formatversion="2")
    url = API + "?" + urllib.parse.urlencode(params)

    def fetch():
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.load(resp)

    return _cached(url, fetch)


def page_wikitext(title: str) -> tuple[str, int]:
    """Return (wikitext, revid) following redirects; ('', 0) if the page is missing."""
    data = api({"action": "parse", "page": title, "prop": "wikitext|revid", "redirects": 1})
    parse = data.get("parse")
    if not parse:
        return "", 0
    return parse["wikitext"], parse.get("revid", 0)


def pages_wikitext(titles: list[str]) -> dict[str, str]:
    """Batch-fetch wikitext for up to 50 titles; keys are the *requested* titles."""
    out = {}
    for i in range(0, len(titles), 50):
        chunk = titles[i:i + 50]
        data = api({"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
                    "redirects": 1, "titles": "|".join(chunk)})
        query = data.get("query", {})
        # Map normalised/redirected titles back to what we asked for.
        back = {}
        for n in query.get("normalized", []):
            back[n["to"]] = n["from"]
        for r in query.get("redirects", []):
            back[r["to"]] = back.get(r["from"], r["from"])
        for page in query.get("pages", []):
            title = page["title"]
            requested = back.get(title, title)
            revs = page.get("revisions")
            out[requested] = revs[0]["slots"]["main"]["content"] if revs else ""
    return out


# ---------------------------------------------------------------- parsing

def slugify(name: str) -> str:
    s = name.lower().replace("'", "")
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s


def parse_collection_log(wikitext: str) -> list[dict]:
    """[{category, entry, items:[names]}] in page order, for the five real tabs only."""
    entries, category, current = [], None, None
    for line in wikitext.splitlines():
        m2 = HEADING2.match(line)
        if m2:
            category = TABS.get(m2.group(1).strip())
            current = None
            continue
        if category is None:
            continue
        m3 = HEADING3.match(line)
        if m3:
            current = {"category": category, "entry": m3.group(1).strip(), "items": []}
            entries.append(current)
            continue
        if current is not None:
            for name in PLINK.findall(line):
                name = name.strip()
                if name and name not in current["items"]:
                    current["items"].append(name)
    return [e for e in entries if e["items"]]


def infobox_item_id(wikitext: str):
    """First item id declared in the page's Infobox Item / Infobox Pet block."""
    m = INFOBOX_ITEM.search(wikitext)
    if not m:
        return None
    depth, i, start = 0, m.start(), m.start()
    while i < len(wikitext):
        if wikitext.startswith("{{", i):
            depth += 1
            i += 2
        elif wikitext.startswith("}}", i):
            depth -= 1
            i += 2
            if depth == 0:
                break
        else:
            i += 1
    block = wikitext[start:i]
    ids = re.findall(r"\|\s*id\d*\s*=\s*(\d+)", block)
    return int(ids[0]) if ids else None


def parse_rarity(text: str):
    """'1/512', '2/60', '3 × 1/512', '1/13,333.5; 1/7,333.5' (first variant), 'Always' -> float."""
    text = re.sub(r"<!--.*?-->", "", text).split(";")[0].strip().replace(",", "")
    if text.lower() == "always":
        return 1.0
    mult = 1.0
    m = re.match(r"^(\d+(?:\.\d+)?)\s*[×x]\s*(.+)$", text)
    if m:
        mult, text = float(m.group(1)), m.group(2).strip()
    m = FRACTION.match(text)
    if m:
        num, den = float(m.group(1)), float(m.group(2))
        return min(1.0, mult * num / den) if den else None
    try:
        return min(1.0, mult * float(text))
    except ValueError:
        return None


def item_sources(name: str) -> list[tuple[str, float]]:
    """[(source name, rate)] from the item page's rendered 'Item sources' table, in page order."""
    data = api({"action": "parse", "page": name, "prop": "text", "redirects": 1})
    text = data.get("parse", {}).get("text", "")
    if isinstance(text, dict):
        text = text.get("*", "")
    table = re.search(r'<table class="[^"]*item-drops[^"]*".*?</table>', text, re.S)
    if not table:
        return []
    rows = []
    for tr in re.findall(r"<tr>(.*?)</tr>", table.group(0), re.S):
        cells = [html.unescape(re.sub(r"<[^>]+>", "", c)).strip() for c in re.findall(r"<td[^>]*>(.*?)</td>", tr, re.S)]
        if len(cells) >= 4:
            rate = parse_rarity(cells[3])
            if rate is not None:
                rows.append((cells[0], rate))
    return rows


def entry_config(entry_pages: dict, entry: str) -> tuple[list[str], float]:
    """entry_pages.json value -> (source-name prefixes, rate multiplier). Defaults to the heading and 1."""
    value = entry_pages.get(entry, [entry])
    if isinstance(value, str):
        return [value], 1.0
    if isinstance(value, dict):
        pages = value.get("pages", [entry])
        return ([pages] if isinstance(pages, str) else pages), float(value.get("multiplier", 1.0))
    return value, 1.0


def rate_for(entry: str, pages: list[str], multiplier: float, rows: list[tuple[str, float]]):
    """First source row that belongs to this entry (prefix match on the entry's page names)."""
    for k in (p.lower() for p in pages):
        for source, rate in rows:
            s = source.lower()
            if s == k or s.startswith(k + " ") or s.startswith(k + " ("):
                return min(1.0, rate * multiplier)
    if entry in CATCH_ALL_ENTRIES and rows:
        return max(r for _, r in rows)
    return None


def rate_text(rate) -> str:
    if rate is None:
        return "?"
    if rate >= 1:
        return "Always"
    return f"1/{round(1 / rate):,}"


# ---------------------------------------------------------------- main

def load_json(path, default):
    if not os.path.exists(path):
        return default
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--report", action="store_true", help="print coverage only; do not write")
    parser.add_argument("--sources", action="store_true",
                        help="for unrated slots, list the source names the wiki offers (to build entry_pages.json)")
    args = parser.parse_args()

    entry_pages = load_json(ENTRY_PAGES, {})
    overrides = load_json(RATE_OVERRIDES, {})
    sources = {s["id"]: s for s in load_json(SOURCES_FILE, [])}

    log_text, log_rev = page_wikitext("Collection log")
    entries = parse_collection_log(log_text)

    all_names = sorted({n for e in entries for n in e["items"]})
    item_pages = pages_wikitext(all_names)
    item_ids = {n: infobox_item_id(item_pages.get(n, "")) for n in all_names}
    rows_by_item = {n: item_sources(n) for n in all_names}

    # Pass 1: rate each slot from its own entry's sources.
    items, coverage, missing_ids, unmatched = [], [], [], {}
    for e in entries:
        source_id = slugify(e["entry"])
        pages, multiplier = entry_config(entry_pages, e["entry"])
        for name in e["items"]:
            slot_id = f"{source_id}:{slugify(name)}"
            rows = rows_by_item.get(name, [])
            rate = None if e["entry"] in CATCH_ALL_ENTRIES else rate_for(e["entry"], pages, multiplier, rows)
            if slot_id in overrides:
                rate = parse_rarity(str(overrides[slot_id]))
            item_id = item_ids.get(name)
            if item_id is None:
                missing_ids.append(name)
            items.append({
                "id": slot_id,
                "sourceId": source_id,
                "timeSourceId": None,
                "itemId": item_id if item_id is not None else -1,
                "name": name,
                "rate": rate,
                "rateText": rate_text(rate),
                "wikiUrl": "https://oldschool.runescape.wiki/w/" + urllib.parse.quote(name.replace(" ", "_")),
                "_entry": e["entry"],
            })

    # Pass 2: catch-all entries (All Pets, Slayer, ...) borrow the best-rated slot of the same item
    # from a real entry, including that entry's per-attempt time; otherwise fall back to any source.
    best_by_name: dict[str, dict] = {}
    for it in items:
        if it["rate"] is not None and it["_entry"] not in CATCH_ALL_ENTRIES:
            cur = best_by_name.get(it["name"])
            if cur is None or it["rate"] > cur["rate"]:
                best_by_name[it["name"]] = it
    for it in items:
        if it["_entry"] not in CATCH_ALL_ENTRIES or it["rate"] is not None:
            continue
        donor = best_by_name.get(it["name"])
        if donor is not None:
            it["rate"], it["rateText"], it["timeSourceId"] = donor["rate"], donor["rateText"], donor["sourceId"]
        else:
            pages, multiplier = entry_config(entry_pages, it["_entry"])
            rate = rate_for(it["_entry"], pages, multiplier, rows_by_item.get(it["name"], []))
            it["rate"], it["rateText"] = rate, rate_text(rate)

    for it in items:
        if it["rate"] is None and rows_by_item.get(it["name"]):
            unmatched.setdefault(it["_entry"], {}).update({s: None for s, _ in rows_by_item[it["name"]]})
    for e in entries:
        source_id = slugify(e["entry"])
        rated = sum(1 for it in items if it["sourceId"] == source_id and it["rate"] is not None)
        coverage.append((e["entry"], source_id, e["category"], rated, len(e["items"])))
    for it in items:
        del it["_entry"]

    total = len(items)
    rated_total = sum(1 for i in items if i["rate"] is not None)
    print(f"entries: {len(entries)}   slots: {total}   rated: {rated_total} ({100 * rated_total / total:.0f}%)")
    print("\nrate coverage by entry (lowest first):")
    for entry, sid, cat, rated, n in sorted(coverage, key=lambda c: (c[3] / c[4], c[0])):
        flag = "" if rated == n else "  <-- add entry_pages/rate_overrides"
        print(f"  {rated:3}/{n:<3} {cat:<9} {entry}{flag}")
    if missing_ids:
        print(f"\n{len(missing_ids)} items without an Infobox id: {', '.join(sorted(set(missing_ids)))}")
    if args.sources and unmatched:
        print("\nsource names offered by the wiki for unrated slots (map them in entry_pages.json):")
        for entry in sorted(unmatched):
            print(f"  {entry}: {', '.join(sorted(unmatched[entry]))[:300]}")

    uncurated = [(e, s, c) for e, s, c, _, _ in coverage if s not in sources]
    if uncurated:
        print(f"\n{len(uncurated)} entries missing from clog-sources.json — paste and fill in:")
        for entry, sid, cat in uncurated:
            print(json.dumps({"id": sid, "name": entry, "category": cat, "minutesPerAttempt": None,
                              "setupMinutes": 0, "requirements": {}, "notes": ""}) + ",")

    if args.report:
        return
    os.makedirs(RESOURCES, exist_ok=True)
    with open(ITEMS_OUT, "w", encoding="utf-8") as fh:
        json.dump({
            "_generated": {"wikiRevision": log_rev, "date": datetime.date.today().isoformat(),
                           "generator": "tools/generate_clog_data.py"},
            "items": sorted(items, key=lambda i: i["id"]),
        }, fh, indent=1, ensure_ascii=False)
        fh.write("\n")
    print(f"\nwrote {ITEMS_OUT}")


if __name__ == "__main__":
    main()

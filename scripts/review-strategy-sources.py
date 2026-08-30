#!/usr/bin/env python3
"""Validate or development-time check pinned strategic source revisions."""

import argparse
import json
import pathlib
import sys
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / "src/main/resources/content/strategy-source-snapshot.json"
API = "https://oldschool.runescape.wiki/api.php"


def fail(message):
    print("strategy source error: " + message, file=sys.stderr)
    raise SystemExit(1)


def load_and_validate():
    data = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    if data.get("runtimeNetworking") is not False:
        fail("runtimeNetworking must remain false")
    sources = data.get("sources", [])
    ids = [source.get("id") for source in sources]
    if len(ids) != len(set(ids)):
        fail("duplicate source id")
    if len(sources) != 16:
        fail("expected 16 registered sources, found %d" % len(sources))
    for source in sources:
        if not source.get("id") or not source.get("reviewedRevision"):
            fail("source is missing identity or reviewed revision")
        if not source.get("derivedFamilies"):
            fail(source["id"] + " has no derived strategy family")
        if source.get("apiTitle") and not isinstance(
                source.get("reviewedRevision"), int):
            fail(source["id"] + " needs a numeric MediaWiki revision")
    return data


def latest_revisions(titles):
    params = urllib.parse.urlencode({
        "action": "query", "format": "json", "formatversion": 2,
        "prop": "revisions", "rvprop": "ids|timestamp",
        "titles": "|".join(sorted(set(titles))),
    })
    request = urllib.request.Request(API + "?" + params,
            headers={"User-Agent": "GielinorCompass-source-review/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.load(response)
    result = {}
    for page in payload.get("query", {}).get("pages", []):
        revisions = page.get("revisions", [])
        if page.get("missing") or not revisions:
            fail("MediaWiki page is missing: " + page.get("title", "?"))
        result[page["title"]] = revisions[0]
    return result


def check_live(data):
    wiki = [source for source in data["sources"] if source.get("apiTitle")]
    latest = latest_revisions([source["apiTitle"] for source in wiki])
    changed = []
    for source in wiki:
        current = latest.get(source["apiTitle"])
        if current is None:
            fail("API omitted " + source["apiTitle"])
        if current["revid"] != source["reviewedRevision"]:
            changed.append({
                "sourceId": source["id"],
                "title": source["apiTitle"],
                "reviewedRevision": source["reviewedRevision"],
                "currentRevision": current["revid"],
                "currentTimestamp": current["timestamp"],
                "reviewFamilies": source["derivedFamilies"],
            })
    print(json.dumps({"changedSources": changed}, indent=2,
            sort_keys=True))
    if changed:
        raise SystemExit(2)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validate", action="store_true",
            help="validate the committed offline snapshot only")
    parser.add_argument("--check-live", action="store_true",
            help="query the MediaWiki Action API during development")
    args = parser.parse_args()
    if not args.validate and not args.check_live:
        parser.error("choose --validate or --check-live")
    data = load_and_validate()
    if args.check_live:
        check_live(data)
    else:
        print("Strategy source snapshot valid (%d sources)." %
                len(data["sources"]))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        fail(str(error))

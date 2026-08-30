#!/usr/bin/env python3
"""Validate the committed development-time freshness manifest without networking."""

import datetime as dt
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "src/main/resources/content/content-freshness.json"
STRATEGY_SNAPSHOT = ROOT / "src/main/resources/content/strategy-source-snapshot.json"
REQUIRED = {
    "quests", "miniquests", "training-methods", "pvm", "slayer",
    "minigames", "diaries", "transportation", "clues", "stash",
    "gear", "resource-dependencies", "resource-sources",
    "strategy-sources",
}


def fail(message):
    print("freshness error: " + message, file=sys.stderr)
    raise SystemExit(1)


data = json.loads(MANIFEST.read_text(encoding="utf-8"))
validation_date = dt.date.fromisoformat(data["validationDate"])
if data.get("currentLiveStatus") not in {
        "LIVE_CURRENT", "ANNOUNCED_NOT_LIVE", "UNKNOWN",
        "REMOVED_SUPERSEDED"}:
    fail("invalid currentLiveStatus")
families = data.get("families", [])
ids = [entry.get("id") for entry in families]
if len(ids) != len(set(ids)):
    fail("duplicate family id")
missing = REQUIRED.difference(ids)
if missing:
    fail("missing families: " + ", ".join(sorted(missing)))
if data.get("runtimeNetworking") is not False:
    fail("runtimeNetworking must remain false")
for entry in families:
    dt.date.fromisoformat(entry["snapshotDate"])
    if dt.date.fromisoformat(entry["snapshotDate"]) > validation_date:
        fail(entry["id"] + " snapshot is after validationDate")
    if entry.get("recordCount", -2) < -1:
        fail(entry["id"] + " has an invalid recordCount")
    if entry.get("status") not in {"STRUCTURED", "PARTIAL", "SCAFFOLDED"}:
        fail(entry["id"] + " has an invalid status")
strategy_entry = next(entry for entry in families
        if entry.get("id") == "strategy-sources")
strategy_snapshot = json.loads(STRATEGY_SNAPSHOT.read_text(encoding="utf-8"))
if strategy_entry.get("recordCount") != len(strategy_snapshot.get("sources", [])):
    fail("strategy-sources recordCount does not match the pinned source snapshot")
if strategy_entry.get("snapshotDate") != strategy_snapshot.get("reviewedDate"):
    fail("strategy-sources snapshotDate does not match the pinned review date")
for change in data.get("announcedNotLive", []):
    dt.date.fromisoformat(change["effectiveDate"])
    if change.get("planningEnabled") is not False:
        fail(change.get("id", "announced change") + " must not affect planning")
print("Freshness manifest valid for " + validation_date.isoformat()
      + " (" + str(len(families)) + " families).")

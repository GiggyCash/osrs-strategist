#!/usr/bin/env python3
"""Validate the committed development-time freshness manifest without networking."""

import datetime as dt
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "src/main/resources/content/content-freshness.json"
REQUIRED = {
    "quests", "miniquests", "training-methods", "pvm", "slayer",
    "minigames", "diaries", "transportation", "clues", "stash",
    "gear", "resource-dependencies", "resource-sources",
}


def fail(message):
    print("freshness error: " + message, file=sys.stderr)
    raise SystemExit(1)


data = json.loads(MANIFEST.read_text(encoding="utf-8"))
validation_date = dt.date.fromisoformat(data["validationDate"])
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
for change in data.get("announcedNotLive", []):
    if change.get("planningEnabled") is not False:
        fail(change.get("id", "announced change") + " must not affect planning")
print("Freshness manifest valid for " + validation_date.isoformat()
      + " (" + str(len(families)) + " families).")

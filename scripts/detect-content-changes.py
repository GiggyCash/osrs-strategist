#!/usr/bin/env python3
"""Compare reviewed content identity snapshots without runtime networking."""

import argparse
import datetime as dt
import json
import pathlib
import sys

STATUSES = {"NEW", "REMOVED", "RENAMED", "CHANGED", "POSSIBLY_STALE"}


def load(path):
    with pathlib.Path(path).open(encoding="utf-8") as handle:
        return json.load(handle)


def records(snapshot):
    result = {}
    for family in snapshot.get("families", []):
        family_id = family["id"]
        for record in family.get("records", []):
            key = (family_id, record["id"])
            if key in result:
                raise ValueError("duplicate identity %s/%s" % key)
            result[key] = record
    return result


def compare(baseline, current, validation_date, max_age_days):
    before = records(baseline)
    after = records(current)
    events = []
    for key in sorted(after.keys() - before.keys()):
        events.append(event("NEW", key, after[key].get("name")))
    for key in sorted(before.keys() - after.keys()):
        events.append(event("REMOVED", key, before[key].get("name")))
    for key in sorted(before.keys() & after.keys()):
        old = before[key]
        new = after[key]
        if old.get("name") != new.get("name"):
            value = event("RENAMED", key, new.get("name"))
            value["previousName"] = old.get("name")
            events.append(value)
        if old.get("fingerprint") != new.get("fingerprint"):
            events.append(event("CHANGED", key, new.get("name")))

    for family in sorted(current.get("families", []), key=lambda value: value["id"]):
        snapshot_date = dt.date.fromisoformat(family["snapshotDate"])
        age = (validation_date - snapshot_date).days
        if age > max_age_days:
            value = event("POSSIBLY_STALE", (family["id"], "*"), None)
            value["snapshotDate"] = snapshot_date.isoformat()
            value["ageDays"] = age
            events.append(value)
    return events


def event(status, key, name):
    if status not in STATUSES:
        raise ValueError("unsupported status " + status)
    result = {"status": status, "family": key[0], "id": key[1]}
    if name is not None:
        result["name"] = name
    return result


def self_test():
    baseline = {"families": [{"id": "quests", "snapshotDate": "2026-08-01",
        "records": [
            {"id": "stable", "name": "Old name", "fingerprint": "a"},
            {"id": "changed", "name": "Changed", "fingerprint": "a"},
            {"id": "removed", "name": "Removed", "fingerprint": "a"}]}]}
    current = {"families": [{"id": "quests", "snapshotDate": "2026-08-01",
        "records": [
            {"id": "stable", "name": "New name", "fingerprint": "a"},
            {"id": "changed", "name": "Changed", "fingerprint": "b"},
            {"id": "new", "name": "New", "fingerprint": "a"}]}]}
    events = compare(baseline, current, dt.date(2026, 8, 25), 14)
    found = {value["status"] for value in events}
    if found != STATUSES:
        raise AssertionError("change detector did not exercise every status: "
                             + repr(found))
    print("Content change detector self-test passed (5 statuses).")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline")
    parser.add_argument("--current")
    parser.add_argument("--validation-date")
    parser.add_argument("--max-age-days", type=int, default=30)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    if not args.baseline or not args.current:
        parser.error("--baseline and --current are required unless --self-test is used")
    current = load(args.current)
    validation = dt.date.fromisoformat(args.validation_date
            or current["validationDate"])
    output = compare(load(args.baseline), current, validation,
            max(0, args.max_age_days))
    print(json.dumps({"events": output}, indent=2, sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (KeyError, ValueError, json.JSONDecodeError) as error:
        print("content change detector error: " + str(error), file=sys.stderr)
        raise SystemExit(1)

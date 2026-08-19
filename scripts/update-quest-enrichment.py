#!/usr/bin/env python3
"""Generate the pinned quest-detail snapshot consumed by the local planner.

This is a development-time tool. The plugin never contacts the Wiki at runtime.
"""

import argparse
import json
import re
import urllib.parse
import urllib.request


API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = (
    "GielinorCompass quest-enrichment generator "
    "(contact: GitHub GiggyCash/osrs-strategist)"
)


def request(parameters):
    url = API + "?" + urllib.parse.urlencode(parameters)
    call = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(call, timeout=60) as response:
        return json.load(response)


def quest_bucket():
    query = (
        "bucket('quest').select('page_name','start_point','requirements',"
        "'items_required','enemies_to_defeat').orderBy('page_name','asc')"
        ".limit(500).run()"
    )
    return request({"action": "bucket", "format": "json",
                    "formatversion": 2, "query": query})["bucket"]


def reward_sections(names):
    result = {}
    for offset in range(0, len(names), 40):
        response = request({
            "action": "query", "format": "json", "formatversion": 2,
            "prop": "revisions", "rvprop": "content", "rvslots": "main",
            "titles": "|".join(names[offset:offset + 40]),
        })
        for page in response["query"]["pages"]:
            revisions = page.get("revisions", [])
            if not revisions:
                continue
            source = revisions[0]["slots"]["main"]["content"]
            match = re.search(
                r"(?ims)^==+\s*Rewards?\s*==+\s*(.*?)(?=^==[^=]|\Z)",
                source,
            )
            if not match:
                # A small number of miniquests document deterministic rewards
                # in the walkthrough instead of a dedicated Rewards heading.
                match = re.search(
                    r"(?ims)(The rewarded XP .*?)(?=^==[^=]|\Z)", source
                )
            if match:
                result[page["title"]] = match.group(1).strip()
    return result


def escape(value):
    return (value or "").replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    raw_rows = quest_bucket()
    rows_by_name = {}
    for row in raw_rows:
        # Some guide pages transclude several Quest details templates into the
        # same page bucket. They are not canonical RuneLite quest identities.
        rows_by_name.setdefault(row["page_name"], row)
    rows = [rows_by_name[name] for name in sorted(rows_by_name)]
    rewards = reward_sections([row["page_name"] for row in rows])
    with open(args.output, "w", encoding="utf-8", newline="\n") as output:
        output.write("# OSRS Wiki quest bucket and quest reward sections.\n")
        output.write("# Generated development-time; runtime performs no network requests.\n")
        output.write("# name<TAB>start<TAB>requirements<TAB>items<TAB>enemies<TAB>rewards\n")
        for row in rows:
            values = [row["page_name"], row.get("start_point", ""),
                      row.get("requirements", ""), row.get("items_required", ""),
                      row.get("enemies_to_defeat", ""),
                      rewards.get(row["page_name"], "")]
            output.write("\t".join(escape(value) for value in values) + "\n")


if __name__ == "__main__":
    main()

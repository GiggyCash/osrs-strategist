#!/usr/bin/env python3
"""Generate the pinned quest-detail snapshot consumed by the local planner.

This is a development-time tool. The plugin never contacts the Wiki at runtime.
The generated schema records evidence state separately from field value so a
missing source field or parser failure cannot silently become verified NONE.
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

VALUE = "VALUE"
NONE = "NONE"
NOT_APPLICABLE = "NOT_APPLICABLE"
SOURCE_MISSING = "SOURCE_MISSING"
PARSE_FAILURE = "PARSE_FAILURE"
UNSUPPORTED_STRUCTURE = "UNSUPPORTED_STRUCTURE"

# These miniquests intentionally distribute durable unlocks through their
# walkthrough sections instead of publishing a conventional Rewards section.
# QuestKnowledgeCatalog owns their typed unlocks; this manifest proves the
# source structure was recognised rather than incorrectly inferring no reward.
DISTRIBUTED_REWARD_SECTIONS = {
    "The Frozen Door": ("The Evil Within",),
    "Barbarian Training": (
        "Farming", "Smithing", "Herblore", "Required for completing"
    ),
}


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


def bucket_field(row, key, blank_is_none):
    if key not in row:
        return "", SOURCE_MISSING
    value = row.get(key)
    if value is None:
        value = ""
    value = str(value)
    if blank_is_none:
        semantic = re.sub(r"<[^>]+>", "", value).strip().lower()
        if semantic == "none" or semantic.startswith(
                "you don't need to bring any items yourself"):
            return "", NONE
    if value.strip():
        return value, VALUE
    return "", NONE if blank_is_none else SOURCE_MISSING


def reward_sections(names):
    result = {}
    for offset in range(0, len(names), 40):
        batch = names[offset:offset + 40]
        response = request({
            "action": "query", "format": "json", "formatversion": 2,
            "prop": "revisions", "rvprop": "content", "rvslots": "main",
            "titles": "|".join(batch),
        })
        seen = set()
        for page in response.get("query", {}).get("pages", []):
            title = page.get("title")
            if not title:
                continue
            seen.add(title)
            revisions = page.get("revisions", [])
            if not revisions:
                result[title] = ("", SOURCE_MISSING)
                continue
            slots = revisions[0].get("slots", {})
            source = slots.get("main", {}).get("content")
            if source is None:
                result[title] = ("", SOURCE_MISSING)
                continue
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
            if not match or not match.group(1).strip():
                expected = DISTRIBUTED_REWARD_SECTIONS.get(title)
                if expected and all(has_heading(source, heading)
                                    for heading in expected):
                    result[title] = ("", NOT_APPLICABLE)
                    continue
                # The source was read successfully but does not use a supported
                # rewards section. This is uncertainty, not a parser crash and
                # not proof that the activity has no rewards.
                result[title] = ("", UNSUPPORTED_STRUCTURE)
            else:
                result[title] = (match.group(1).strip(), VALUE)
        for title in batch:
            result.setdefault(title, ("", SOURCE_MISSING))
    return result


def has_heading(source, heading):
    return re.search(
        r"(?im)^==+\s*" + re.escape(heading) + r"\s*==+\s*$", source
    ) is not None


def escape(value):
    return (value or "").replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    raw_rows = quest_bucket()
    rows_by_name = {}
    for row in raw_rows:
        page_name = row.get("page_name")
        if not page_name:
            raise RuntimeError("Quest bucket returned a row without page_name")
        # Some guide pages transclude several Quest details templates into the
        # same page bucket. They are not canonical RuneLite quest identities.
        rows_by_name.setdefault(page_name, row)
    rows = [rows_by_name[name] for name in sorted(rows_by_name)]
    rewards = reward_sections([row["page_name"] for row in rows])
    with open(args.output, "w", encoding="utf-8", newline="\n") as output:
        output.write("# OSRS Wiki quest bucket and quest reward sections.\n")
        output.write("# Generated development-time; runtime performs no network requests.\n")
        output.write("# schema=2; blank values are meaningful only with their evidence state.\n")
        output.write("# name<TAB>start<TAB>start_state<TAB>requirements<TAB>requirements_state<TAB>items<TAB>items_state<TAB>enemies<TAB>enemies_state<TAB>rewards<TAB>rewards_state\n")
        for row in rows:
            start, start_state = bucket_field(row, "start_point", False)
            requirements, requirements_state = bucket_field(
                row, "requirements", True)
            items, items_state = bucket_field(row, "items_required", True)
            enemies, enemies_state = bucket_field(
                row, "enemies_to_defeat", True)
            reward_text, reward_state = rewards[row["page_name"]]
            values = [row["page_name"], start, start_state,
                      requirements, requirements_state, items, items_state,
                      enemies, enemies_state, reward_text, reward_state]
            output.write("\t".join(escape(value) for value in values) + "\n")


if __name__ == "__main__":
    main()

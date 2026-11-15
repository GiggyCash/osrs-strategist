#!/usr/bin/env python3
"""Acquire, validate, and project one reviewed OSRS Wiki knowledge family."""

import argparse
import copy
import datetime as dt
import decimal
import hashlib
import json
import pathlib
import re
import sys
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "knowledge/manifests/smithing-platebodies.json"
SNAPSHOT = ROOT / "knowledge/raw/smithing-platebodies.json"
ACTION_CATALOG = ROOT / "src/main/resources/content/catalogs/reviewed-action-recipes.json"
METHOD_CATALOG = ROOT / "src/main/resources/content/catalogs/method-intelligence.json"


class KnowledgeError(ValueError):
    pass


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise KnowledgeError("cannot read %s: %s" % (path, error)) from error


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def template(wikitext, name):
    start = wikitext.lower().find("{{" + name.lower())
    if start < 0:
        raise KnowledgeError("missing %s template" % name)
    depth = 0
    cursor = start
    while cursor < len(wikitext) - 1:
        token = wikitext[cursor:cursor + 2]
        if token == "{{":
            depth += 1
            cursor += 2
            continue
        if token == "}}":
            depth -= 1
            cursor += 2
            if depth == 0:
                return wikitext[start:cursor]
            continue
        cursor += 1
    raise KnowledgeError("unterminated %s template" % name)


def parameters(block):
    result = {}
    for match in re.finditer(r"(?m)^\|\s*([^=\n]+?)\s*=\s*(.*?)\s*$", block):
        key, value = match.group(1).strip().lower(), match.group(2).strip()
        if key in result:
            raise KnowledgeError("duplicate template parameter: " + key)
        result[key] = value
    return result


def required(values, key, page):
    value = values.get(key)
    if value is None or not value.strip():
        raise KnowledgeError("%s is missing %s" % (page, key))
    return value.strip()


def yes_no(value, label):
    normalized = value.strip().lower()
    if normalized not in ("yes", "no"):
        raise KnowledgeError("%s must be Yes or No" % label)
    return normalized == "yes"


def positive_int(value, label):
    try:
        parsed = int(value)
    except (TypeError, ValueError) as error:
        raise KnowledgeError("%s must be an integer" % label) from error
    if parsed <= 0:
        raise KnowledgeError("%s must be positive" % label)
    return parsed


def positive_decimal(value, label):
    try:
        parsed = decimal.Decimal(value)
    except (decimal.InvalidOperation, TypeError, ValueError) as error:
        raise KnowledgeError("%s must be numeric" % label) from error
    if not parsed.is_finite() or parsed <= 0:
        raise KnowledgeError("%s must be positive and finite" % label)
    return parsed


def page_fact(title, revision):
    wikitext = revision["slots"]["main"]["content"]
    item = parameters(template(wikitext, "Infobox Item"))
    fact = {
        "title": title,
        "url": "https://oldschool.runescape.wiki/w/" + title.replace(" ", "_"),
        "revision": positive_int(revision["revid"], title + " revision"),
        "timestamp": revision["timestamp"],
        "wikitextSha256": hashlib.sha256(wikitext.encode("utf-8")).hexdigest(),
        "item": {
            "name": required(item, "name", title),
            "id": positive_int(required(item, "id", title), title + " item id"),
            "members": yes_no(required(item, "members", title), title + " members"),
            "stackable": yes_no(required(item, "stackable", title), title + " stackable"),
        },
    }
    if "platebody" in title.lower():
        recipe = parameters(template(wikitext, "Recipe"))
        unsupported = [key for key in ("skill2", "mat2", "output2") if recipe.get(key)]
        if unsupported:
            raise KnowledgeError("%s has unsupported recipe fields: %s" % (title, unsupported))
        fact["recipe"] = {
            "skill": required(recipe, "skill1", title),
            "level": positive_int(required(recipe, "skill1lvl", title), title + " level"),
            "xp": required(recipe, "skill1exp", title),
            "members": yes_no(required(recipe, "members", title), title + " recipe members"),
            "ticks": positive_int(required(recipe, "ticks", title), title + " ticks"),
            "tool": required(recipe, "tools", title),
            "facility": required(recipe, "facilities", title),
            "material": required(recipe, "mat1", title),
            "materialQuantity": positive_int(required(recipe, "mat1quantity", title), title + " material quantity"),
            "output": required(recipe, "output1", title),
            "outputQuantity": positive_int(recipe.get("output1quantity", "1"), title + " output quantity"),
        }
    return fact


def acquire(manifest):
    validate_manifest(manifest)
    titles = {manifest["toolPage"]}
    for record in manifest["records"]:
        titles.update((record["outputPage"], record["materialPage"]))
    params = urllib.parse.urlencode({
        "action": "query", "format": "json", "formatversion": 2,
        "prop": "revisions", "rvprop": "ids|timestamp|content", "rvslots": "main",
        "titles": "|".join(sorted(titles)),
    })
    request = urllib.request.Request(manifest["wikiApi"] + "?" + params,
            headers={"User-Agent": "GielinorCompass-knowledge-review/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.load(response)
    pages = {}
    for page in payload.get("query", {}).get("pages", []):
        revisions = page.get("revisions", [])
        if page.get("missing") or len(revisions) != 1:
            raise KnowledgeError("Wiki page is missing or revision is ambiguous: " + page.get("title", "?"))
        pages[page["title"]] = page_fact(page["title"], revisions[0])
    if set(pages) != titles:
        raise KnowledgeError("Wiki response did not contain the exact requested pages")
    snapshot = {
        "schemaVersion": 1,
        "domain": manifest["domain"],
        "reviewedDate": manifest["reviewedDate"],
        "license": manifest["license"],
        "acquiredFrom": manifest["wikiApi"],
        "runtimeNetworking": False,
        "records": copy.deepcopy(manifest["records"]),
        "toolPage": manifest["toolPage"],
        "pages": [pages[title] for title in sorted(pages)],
    }
    validate_snapshot(snapshot)
    return snapshot


def validate_manifest(value):
    if value.get("schemaVersion") != 1 or value.get("runtimeNetworking") is not False:
        raise KnowledgeError("manifest requires schemaVersion 1 and runtimeNetworking false")
    if value.get("wikiApi") != "https://oldschool.runescape.wiki/api.php":
        raise KnowledgeError("manifest must use the OSRS Wiki Action API")
    validate_identity(value)


def validate_identity(value):
    if value.get("domain") != "smithing-platebodies":
        raise KnowledgeError("unsupported knowledge domain")
    try:
        reviewed = dt.date.fromisoformat(value["reviewedDate"])
    except (KeyError, TypeError, ValueError) as error:
        raise KnowledgeError("invalid reviewedDate") from error
    if reviewed > dt.date.today() or value.get("license") != "OSRS Wiki text: CC BY-NC-SA 3.0":
        raise KnowledgeError("invalid review date or source license")
    records = value.get("records")
    if not isinstance(records, list) or not records:
        raise KnowledgeError("knowledge records are missing")
    ids = [record.get("recipeId") for record in records]
    matches = [record.get("match") for record in records]
    outputs = [record.get("outputPage") for record in records]
    if None in ids + matches + outputs or len(ids) != len(set(ids)) or len(matches) != len(set(matches)) \
            or len(outputs) != len(set(outputs)):
        raise KnowledgeError("knowledge records have missing or duplicate identities")


def validate_snapshot(value):
    if value.get("schemaVersion") != 1 or value.get("runtimeNetworking") is not False:
        raise KnowledgeError("snapshot requires schemaVersion 1 and runtimeNetworking false")
    if value.get("acquiredFrom") != "https://oldschool.runescape.wiki/api.php":
        raise KnowledgeError("snapshot must identify the OSRS Wiki Action API")
    validate_identity(value)
    pages_list = value.get("pages")
    if not isinstance(pages_list, list):
        raise KnowledgeError("snapshot pages are missing")
    pages = {page.get("title"): page for page in pages_list if isinstance(page, dict)}
    if len(pages) != len(pages_list) or None in pages:
        raise KnowledgeError("snapshot has duplicate or malformed pages")
    expected_pages = {value.get("toolPage")}
    for record in value["records"]:
        expected_pages.update((record.get("outputPage"), record.get("materialPage")))
    if set(pages) != expected_pages:
        raise KnowledgeError("snapshot must contain exactly the reviewed source pages")
    tool = pages.get(value.get("toolPage"))
    if tool is None:
        raise KnowledgeError("tool source page is missing")
    validate_page(tool)
    tool_item = tool["item"]
    if tool_item["name"] != "Hammer" or tool_item["stackable"] or tool_item["members"]:
        raise KnowledgeError("unsupported tool facts")
    output_ids = set()
    for record in value["records"]:
        output, material = pages.get(record.get("outputPage")), pages.get(record.get("materialPage"))
        if output is None or material is None:
            raise KnowledgeError("record source page is missing")
        validate_page(output, recipe=True)
        validate_page(material)
        recipe, output_item, material_item = output["recipe"], output["item"], material["item"]
        positive_int(recipe.get("level"), record["recipeId"] + " level")
        positive_int(recipe.get("ticks"), record["recipeId"] + " ticks")
        positive_int(recipe.get("materialQuantity"), record["recipeId"] + " material quantity")
        positive_int(recipe.get("outputQuantity"), record["recipeId"] + " output quantity")
        positive_decimal(recipe.get("xp"), record["recipeId"] + " xp")
        if recipe["skill"] != "Smithing" or recipe["facility"] != "Anvil" \
                or recipe["tool"] != tool_item["name"] or recipe["output"] != output_item["name"] \
                or recipe["material"] != material_item["name"] or recipe["members"] != output_item["members"] \
                or recipe["outputQuantity"] != 1 or output_item["stackable"] or material_item["stackable"] \
                or output_item["members"] or material_item["members"] \
                or record["match"] != output_item["name"].lower():
            raise KnowledgeError("contradictory or unsupported Smithing facts for " + record["recipeId"])
        if output_item["id"] in output_ids:
            raise KnowledgeError("duplicate output item id")
        output_ids.add(output_item["id"])
    return value


def assert_manifest_snapshot(manifest, snapshot):
    validate_manifest(manifest)
    for key in ("domain", "reviewedDate", "license", "runtimeNetworking", "toolPage", "records"):
        if snapshot.get(key) != manifest.get(key):
            raise KnowledgeError("reviewed snapshot diverges from manifest: " + key)
    if snapshot.get("acquiredFrom") != manifest.get("wikiApi"):
        raise KnowledgeError("reviewed snapshot diverges from manifest: wikiApi")


def validate_page(page, recipe=False):
    if not isinstance(page.get("revision"), int) or page["revision"] <= 0 \
            or not re.fullmatch(r"[0-9a-f]{64}", page.get("wikitextSha256", "")) \
            or page.get("url") != "https://oldschool.runescape.wiki/w/" + page["title"].replace(" ", "_"):
        raise KnowledgeError("invalid page provenance: " + page.get("title", "?"))
    try:
        dt.datetime.fromisoformat(page["timestamp"].replace("Z", "+00:00"))
    except (KeyError, TypeError, ValueError) as error:
        raise KnowledgeError("invalid page timestamp") from error
    item = page.get("item", {})
    if item.get("name") != page["title"] or not isinstance(item.get("id"), int) or item["id"] <= 0 \
            or not isinstance(item.get("members"), bool) or not isinstance(item.get("stackable"), bool):
        raise KnowledgeError("invalid item facts: " + page["title"])
    if recipe and not isinstance(page.get("recipe"), dict):
        raise KnowledgeError("recipe facts are missing: " + page["title"])


def normalized(snapshot):
    validate_snapshot(snapshot)
    pages = {page["title"]: page for page in snapshot["pages"]}
    tool = pages[snapshot["toolPage"]]["item"]
    result = []
    for record in snapshot["records"]:
        output, material = pages[record["outputPage"]], pages[record["materialPage"]]
        recipe = output["recipe"]
        result.append({
            "recipeId": record["recipeId"],
            "actionRecipe": {
                "skill": "SMITHING", "match": record["match"],
                "inputs": [material["item"]["name"]], "units": [recipe["materialQuantity"]],
                "itemIds": [material["item"]["id"]], "stackable": [material["item"]["stackable"]],
                "membersOnly": recipe["members"], "source": output["url"],
                "sourceRevision": output["revision"], "reviewed": snapshot["reviewedDate"],
            },
            "methodRecipe": {
                "id": record["recipeId"], "skill": "SMITHING", "level": recipe["level"],
                "tools": [{"itemId": tool["id"], "name": tool["name"], "quantity": 1,
                           "stackable": tool["stackable"], "equippedAllowed": False}],
                "outputsPerAction": recipe["outputQuantity"], "outputStackable": output["item"]["stackable"],
                "outputItemId": output["item"]["id"], "inputRecipe": record["match"],
            },
        })
    return result


def assert_projection(snapshot, actions, methods):
    expected = normalized(snapshot)
    action_by_key = {(row.get("skill"), row.get("match")): row for row in actions}
    method_bundle = methods[0] if isinstance(methods, list) and len(methods) == 1 else None
    if not isinstance(method_bundle, dict):
        raise KnowledgeError("method catalog must contain one bundle")
    method_by_id = {row.get("id"): row for row in method_bundle.get("recipes", [])}
    for projection in expected:
        action_expected, method_expected = projection["actionRecipe"], projection["methodRecipe"]
        action = action_by_key.get(("SMITHING", action_expected["match"]))
        method = method_by_id.get(projection["recipeId"])
        if action is None or method is None:
            raise KnowledgeError("catalog target is missing: " + projection["recipeId"])
        for key, value in action_expected.items():
            if action.get(key) != value:
                raise KnowledgeError("action catalog contradicts %s.%s" % (projection["recipeId"], key))
        for key, value in method_expected.items():
            if method.get(key) != value:
                raise KnowledgeError("method catalog contradicts %s.%s" % (projection["recipeId"], key))
    return expected


def sync(snapshot, actions, methods):
    expected = normalized(snapshot)
    action_by_key = {(row.get("skill"), row.get("match")): row for row in actions}
    method_by_id = {row.get("id"): row for row in methods[0].get("recipes", [])}
    for projection in expected:
        action = action_by_key.get(("SMITHING", projection["actionRecipe"]["match"]))
        method = method_by_id.get(projection["recipeId"])
        if action is None or method is None:
            raise KnowledgeError("sync will not invent an unsupported Compass target: " + projection["recipeId"])
        action.update(projection["actionRecipe"])
        method.update(projection["methodRecipe"])
    assert_projection(snapshot, actions, methods)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    fetch = sub.add_parser("fetch", help="fetch a review candidate from the OSRS Wiki")
    fetch.add_argument("--output", type=pathlib.Path, required=True)
    sub.add_parser("validate", help="validate the checked-in reviewed raw snapshot")
    sub.add_parser("check", help="validate and compare the reviewed snapshot with runtime catalogs")
    sub.add_parser("sync", help="copy reviewed factual fields into existing runtime catalogs")
    args = parser.parse_args()
    manifest = read_json(MANIFEST)
    if args.command == "fetch":
        write_json(args.output, acquire(manifest))
        print("Fetched reviewed candidate to %s" % args.output)
        return
    snapshot = read_json(SNAPSHOT)
    assert_manifest_snapshot(manifest, snapshot)
    if args.command == "validate":
        validate_snapshot(snapshot)
        print("Knowledge snapshot valid (%d reviewed recipes)." % len(snapshot["records"]))
        return
    actions, methods = read_json(ACTION_CATALOG), read_json(METHOD_CATALOG)
    if args.command == "sync":
        sync(snapshot, actions, methods)
        write_json(ACTION_CATALOG, actions)
        write_json(METHOD_CATALOG, methods)
        print("Synchronized %d reviewed recipes." % len(snapshot["records"]))
        return
    assert_projection(snapshot, actions, methods)
    print("Knowledge projection matches runtime catalogs (%d reviewed recipes)." % len(snapshot["records"]))


if __name__ == "__main__":
    try:
        main()
    except (KnowledgeError, OSError, KeyError, TypeError) as error:
        print("knowledge pipeline error: " + str(error), file=sys.stderr)
        raise SystemExit(1)

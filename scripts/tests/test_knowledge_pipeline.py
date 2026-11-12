#!/usr/bin/env python3

import copy
import pathlib
import sys
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))
import knowledge_pipeline as pipeline


class KnowledgePipelineTest(unittest.TestCase):
    def setUp(self):
        self.snapshot = pipeline.read_json(pipeline.SNAPSHOT)

    def test_reviewed_snapshot_projects_exactly_into_runtime_catalogs(self):
        actions = pipeline.read_json(pipeline.ACTION_CATALOG)
        methods = pipeline.read_json(pipeline.METHOD_CATALOG)
        projected = pipeline.assert_projection(self.snapshot, actions, methods)
        self.assertEqual(6, len(projected))
        self.assertEqual(48, projected[2]["methodRecipe"]["level"])
        self.assertEqual([5], projected[2]["actionRecipe"]["units"])

    def test_missing_page_is_rejected(self):
        self.snapshot["pages"] = self.snapshot["pages"][1:]
        with self.assertRaisesRegex(pipeline.KnowledgeError, "source pages"):
            pipeline.validate_snapshot(self.snapshot)

    def test_duplicate_identity_is_rejected(self):
        self.snapshot["records"].append(copy.deepcopy(self.snapshot["records"][0]))
        with self.assertRaisesRegex(pipeline.KnowledgeError, "duplicate identities"):
            pipeline.validate_snapshot(self.snapshot)

    def test_malformed_provenance_is_rejected(self):
        self.snapshot["pages"][0]["wikitextSha256"] = "not-a-hash"
        with self.assertRaisesRegex(pipeline.KnowledgeError, "page provenance"):
            pipeline.validate_snapshot(self.snapshot)

    def test_manifest_drift_is_rejected(self):
        manifest = pipeline.read_json(pipeline.MANIFEST)
        manifest["records"][0]["match"] = "invented platebody"
        with self.assertRaisesRegex(pipeline.KnowledgeError, "diverges from manifest"):
            pipeline.assert_manifest_snapshot(manifest, self.snapshot)

    def test_contradictory_source_facts_are_rejected(self):
        output = next(page for page in self.snapshot["pages"]
                      if page["title"] == "Steel platebody")
        output["recipe"]["material"] = "Iron bar"
        with self.assertRaisesRegex(pipeline.KnowledgeError, "contradictory"):
            pipeline.validate_snapshot(self.snapshot)

    def test_unsupported_recipe_shape_is_rejected(self):
        output = next(page for page in self.snapshot["pages"]
                      if page["title"] == "Steel platebody")
        output["recipe"]["facility"] = "Future forge"
        with self.assertRaisesRegex(pipeline.KnowledgeError, "unsupported"):
            pipeline.validate_snapshot(self.snapshot)

    def test_catalog_drift_is_rejected(self):
        actions = pipeline.read_json(pipeline.ACTION_CATALOG)
        methods = pipeline.read_json(pipeline.METHOD_CATALOG)
        steel = next(row for row in methods[0]["recipes"]
                     if row["id"] == "steel_platebody_smithing")
        steel["level"] = 47
        with self.assertRaisesRegex(pipeline.KnowledgeError, "contradicts.*level"):
            pipeline.assert_projection(self.snapshot, actions, methods)


if __name__ == "__main__":
    unittest.main()

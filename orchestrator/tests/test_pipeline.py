import unittest
from app import collect_rank_group, generate_magazine

class PipelineTest(unittest.TestCase):
    def test_demo_collection_builds_six_unique_channels(self):
        result = collect_rank_group({"regionCode": "KR", "categoryId": "24", "maxResults": 18})
        self.assertEqual(18, result["collectedCount"])
        self.assertEqual(18, len(result["videos"]))
        selected = result["group"]["items"]
        self.assertEqual(6, len(selected))
        self.assertEqual(6, len({item["channelTitle"] for item in selected}))
        self.assertEqual(list(range(1, 7)), [item["rankNo"] for item in selected])

    def test_generation_is_commentary_only_and_quality_gated(self):
        collected = collect_rank_group({"maxResults": 18})
        plan = generate_magazine({"jobId": "test", "format": "SHORTS", "group": collected["group"], "items": collected["group"]["items"]})
        self.assertEqual([6, 5, 4, 3, 2, 1], [entry["rankNo"] for entry in plan["entries"]])
        self.assertTrue(plan["quality"]["checks"]["sixEntries"])
        self.assertFalse(plan["risk"]["checks"]["sourceClipUsed"])
        self.assertLessEqual(plan["risk"]["score"], 30)

if __name__ == "__main__": unittest.main()

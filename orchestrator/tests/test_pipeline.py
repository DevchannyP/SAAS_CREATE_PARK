import unittest
from app import collect_rank_group

class PipelineTest(unittest.TestCase):
    def test_demo_collection_builds_six_unique_channels(self):
        result = collect_rank_group({"regionCode": "KR", "categoryId": "24", "maxResults": 18})
        self.assertEqual(18, result["collectedCount"])
        self.assertEqual(18, len(result["videos"]))
        selected = result["group"]["items"]
        self.assertEqual(6, len(selected))
        self.assertEqual(6, len({item["channelTitle"] for item in selected}))
        self.assertEqual(list(range(1, 7)), [item["rankNo"] for item in selected])

if __name__ == "__main__": unittest.main()

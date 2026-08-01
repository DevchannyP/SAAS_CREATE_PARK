import unittest
from engines.youtube_rank import calc_hot_score

class RankingTest(unittest.TestCase):
    def test_newer_video_wins_with_same_engagement(self):
        self.assertGreater(calc_hot_score(100_000, 5_000, 500, 2), calc_hot_score(100_000, 5_000, 500, 48))

    def test_negative_counts_are_safely_clamped(self):
        self.assertGreaterEqual(calc_hot_score(-1, -1, -1, 0), 0)

if __name__ == "__main__": unittest.main()

import unittest

from engine.generator import generate_song
from engine.profiles import PROFILES


class GeneratorTests(unittest.TestCase):
    def test_all_profiles_generate_events(self):
        for profile in PROFILES:
            song = generate_song(profile, bars=4, seed=123)
            self.assertGreater(len(song["events"]), 0)
            self.assertEqual(song["profile"], profile)

    def test_seed_is_deterministic(self):
        first = generate_song("cybernetic", bars=8, seed=421)
        second = generate_song("cybernetic", bars=8, seed=421)
        self.assertEqual(first, second)

    def test_different_seeds_change_score(self):
        first = generate_song("space", bars=8, seed=1)
        second = generate_song("space", bars=8, seed=2)
        self.assertNotEqual(first, second)

    def test_tempo_override(self):
        song = generate_song("epic", bars=4, seed=9, bpm=120)
        self.assertEqual(song["bpm"], 120)


if __name__ == "__main__":
    unittest.main()

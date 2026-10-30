import unittest

from engine.generator import generate_song
from engine.profiles import PROFILES
from engine.radio import generate_transmission
from engine.theory import scale_note, triad


class GeneratorTests(unittest.TestCase):
    def test_all_profiles_generate_events(self):
        for profile in PROFILES:
            song = generate_song(profile, bars=4, seed=123)
            self.assertGreater(len(song["events"]), 0)
            self.assertEqual(song["profile"], profile)

    def test_seed_is_deterministic(self):
        self.assertEqual(
            generate_song("cybernetic", bars=8, seed=421),
            generate_song("cybernetic", bars=8, seed=421),
        )

    def test_different_seeds_change_score(self):
        self.assertNotEqual(
            generate_song("space", bars=8, seed=1),
            generate_song("space", bars=8, seed=2),
        )

    def test_tempo_override(self):
        self.assertEqual(generate_song("epic", bars=4, seed=9, bpm=120)["bpm"], 120)

    def test_final_bar_resolves_to_tonic(self):
        song = generate_song("epic", bars=8, seed=421)
        self.assertEqual(song["harmony"][-1]["degree"], 0)

    def test_harmony_has_one_chord_per_bar(self):
        song = generate_song("space", bars=12, seed=77)
        self.assertEqual(len(song["harmony"]), 12)
        for chord in song["harmony"]:
            self.assertEqual(len(chord["notes"]), 3)

    def test_scale_helper_crosses_octaves(self):
        self.assertEqual(scale_note(60, "major", 7), 72)

    def test_triad_is_ascending(self):
        self.assertEqual(triad(60, "major", 0), (60, 64, 67))


class RadioTests(unittest.TestCase):
    def test_radio_is_deterministic(self):
        first = generate_transmission(("space", "sci-fi", "epic"), 4, 421)
        second = generate_transmission(("space", "sci-fi", "epic"), 4, 421)
        self.assertEqual(first, second)

    def test_sections_overlap_by_one_bar(self):
        song = generate_transmission(("space", "epic", "ambient"), 4, 421)
        self.assertEqual(song["bars"], 10)
        self.assertEqual(
            [section["start_bar"] for section in song["sections"]],
            [0, 3, 6],
        )
        self.assertEqual(len(song["harmony"]), 12)

    def test_radio_keeps_tempo_and_tonal_root(self):
        song = generate_transmission(("space", "cybernetic", "epic"), 4, 123)
        self.assertEqual(len({section["seed"] for section in song["sections"]}), 3)
        self.assertEqual(len({chord["notes"][0] for chord in song["harmony"][:4]}), 1)
        self.assertTrue(all(event["step"] >= 0 for event in song["events"]))

    def test_invalid_radio_sequence_is_rejected(self):
        with self.assertRaises(ValueError):
            generate_transmission(("space", "not-a-profile"), 4, 1)

    def test_single_profile_radio(self):
        song = generate_transmission(("ambient",), 4, 55)
        self.assertEqual(song["bars"], 4)
        self.assertEqual(song["profiles"], ["ambient"])


if __name__ == "__main__":
    unittest.main()

import unittest

from engine.generator import PHRASE, generate_song
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

    def test_four_bar_phrase_has_call_response_and_cadence(self):
        self.assertEqual(len(PHRASE), 4)
        self.assertEqual(PHRASE[0][0], 0)
        self.assertEqual(PHRASE[2][0], 4)
        self.assertEqual(PHRASE[3][2], 0)
        self.assertIsNone(PHRASE[3][3])

    def test_space_default_is_wide_leap_and_pulse(self):
        song = generate_song("space", bars=4, seed=421, bpm=110)
        expected = PROFILES["space"]["phrase_variants"]["wide-leap"]
        self.assertEqual(song["phrase_variant"], "wide-leap")
        self.assertEqual(song["motif"], list(expected[0]))
        self.assertEqual(len(expected), 4)
        self.assertEqual(expected[0], (0, 4, 7, 5, 9, 7, 5, 4))
        noise = [
            event for event in song["events"]
            if event["voice"] == "noise"
        ]
        self.assertEqual(
            [event["step"] for event in noise],
            [4, 12, 20, 28, 36, 44, 52, 60],
        )

    def test_space_minor_hook_is_selectable_and_deterministic(self):
        first = generate_song(
            "space", bars=16, seed=421, bpm=110,
            phrase_variant="minor-hook",
        )
        second = generate_song(
            "space", bars=16, seed=421, bpm=110,
            phrase_variant="minor-hook",
        )
        self.assertEqual(first, second)
        self.assertEqual(first["phrase_variant"], "minor-hook")
        self.assertEqual(
            first["motif"],
            list(PROFILES["space"]["phrase_variants"]["minor-hook"][0]),
        )
        self.assertNotEqual(
            first["motif"],
            generate_song("space", bars=16, seed=421, bpm=110)["motif"],
        )

    def test_unknown_phrase_variant_is_rejected(self):
        with self.assertRaises(ValueError):
            generate_song("space", bars=8, seed=421, phrase_variant="unknown")

    def test_profile_without_variants_rejects_variant_argument(self):
        with self.assertRaises(ValueError):
            generate_song("retro-arcade", bars=8, seed=421, phrase_variant="minor-hook")

    def test_melody_uses_eighth_note_grid(self):
        song = generate_song("retro-arcade", bars=4, seed=421, bpm=140)
        melody = [
            event for event in song["events"]
            if event["voice"] == "pulse"
        ]
        self.assertTrue(melody)
        self.assertTrue(all(event["step"] % 2 == 0 for event in melody))
        self.assertTrue(all(event["length"] in (2, 3) for event in melody))

    def test_bass_has_root_fifth_root_movement_in_audible_register(self):
        song = generate_song("space", bars=1, seed=421, bpm=90)
        bass = [
            event for event in song["events"]
            if event["voice"] == "triangle" and event["length"] == 3
        ]
        self.assertEqual(len(bass), 4)
        chord = song["harmony"][0]["notes"]
        self.assertEqual(
            [event["note"] for event in bass],
            [chord[0] - 12, chord[0] - 12, chord[2] - 12, chord[0] - 12],
        )

    def test_arpeggio_uses_regular_up_down_pattern(self):
        song = generate_song("retro-arcade", bars=1, seed=421, bpm=140)
        arp = [
            event for event in song["events"]
            if event["voice"] == "square"
        ]
        self.assertEqual([event["step"] for event in arp], [0, 4, 8, 12])
        chord = song["harmony"][0]["notes"]
        self.assertEqual(
            [event["note"] for event in arp],
            [chord[0] + 12, chord[2] + 12, chord[0] + 12, chord[2] + 12],
        )


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
        section_roots = [song["harmony"][index]["notes"][0] for index in (0, 4, 8)]
        self.assertEqual(len(set(section_roots)), 1)
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

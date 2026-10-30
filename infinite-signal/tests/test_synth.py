import unittest

from engine.generator import generate_song
from engine.synth import render_pcm


class SynthTests(unittest.TestCase):
    def test_pcm_is_16_bit_mono_sized(self):
        song = generate_song("space", bars=1, seed=42, bpm=120)
        pcm = render_pcm(song, sample_rate=8000)
        expected_frames = int(1 * 16 * (60.0 / 120 / 4) * 8000)
        self.assertEqual(len(pcm), expected_frames * 2)

    def test_pcm_contains_non_silent_samples(self):
        song = generate_song("retro-arcade", bars=1, seed=7, bpm=140)
        pcm = render_pcm(song, sample_rate=8000)
        self.assertTrue(any(value != 0 for value in pcm))


if __name__ == "__main__":
    unittest.main()

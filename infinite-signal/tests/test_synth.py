import os
import tempfile
import unittest
import wave

from engine.generator import generate_song
from engine.radio import generate_transmission
from engine.synth import _wave_value, render_pcm
from engine.wav_export import write_wav


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

    def test_pulse_wave_has_twenty_five_percent_duty(self):
        self.assertEqual(_wave_value(0.10, "pulse"), 1.0)
        self.assertEqual(_wave_value(0.40, "pulse"), -1.0)

    def test_triangle_wave_has_expected_turning_points(self):
        self.assertAlmostEqual(_wave_value(0.0, "triangle"), -1.0)
        self.assertAlmostEqual(_wave_value(0.5, "triangle"), 1.0)

    def test_wav_export_has_expected_format_and_duration(self):
        song = generate_song("space", bars=2, seed=42, bpm=120)
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "test.wav")
            write_wav(song, path, sample_rate=8000)
            with wave.open(path, "rb") as audio:
                self.assertEqual(audio.getnchannels(), 1)
                self.assertEqual(audio.getsampwidth(), 2)
                self.assertEqual(audio.getframerate(), 8000)
                self.assertEqual(audio.getnframes(), 2 * 16 * 8000 // 8)

    def test_radio_score_renders_non_silent_pcm(self):
        song = generate_transmission(("space", "sci-fi"), 3, 123)
        pcm = render_pcm(song, sample_rate=8000)
        self.assertTrue(any(value != 0 for value in pcm))
        expected_frames = int(song["bars"] * 16 * (60.0 / song["bpm"] / 4) * 8000)
        self.assertEqual(len(pcm), expected_frames * 2)


if __name__ == "__main__":
    unittest.main()

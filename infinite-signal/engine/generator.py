"""Seeded, structured chiptune composition generator."""

import random

from .profiles import PROFILES
from .theory import scale_note, triad

STEPS_PER_BAR = 16
ROOT_MIDI = 48
MELODY_SLOTS = 8


class _RNG:
    """Small PRNG with the subset of methods needed by this module."""
    def __init__(self, seed):
        self.state = int(seed) & 0x7fffffff
        if self.state == 0:
            self.state = 1

    def _next(self):
        self.state = (1103515245 * self.state + 12345) & 0x7fffffff
        return self.state

    def random(self):
        return self._next() / 2147483648.0

    def randint(self, low, high):
        return low + (self._next() % (high - low + 1))

    def choice(self, values):
        return values[self._next() % len(values)]


def _new_seed():
    try:
        return random.getrandbits(31)
    except AttributeError:
        return random.randint(0, 2147483647)


def _make_motif(rng, profile, progression):
    """Create an eight-slot two-bar motif using chord tones as anchors."""
    motif = []
    for slot in range(MELODY_SLOTS):
        if rng.random() > profile["melody_density"]:
            motif.append(None)
            continue
        chord_degree = progression[0 if slot < 4 else 1]
        anchor = rng.choice((chord_degree, chord_degree + 2, chord_degree + 4))
        if slot in (0, 4):
            anchor = chord_degree
        if motif and motif[-1] is not None and rng.random() < 0.55:
            anchor = motif[-1] + rng.choice((-2, -1, 1, 2))
        motif.append(max(0, min(anchor, 13)))
    if all(note is None for note in motif):
        motif[0] = progression[0]
    return motif


def generate_song(profile_name="space", bars=8, seed=None, bpm=None):
    """Generate a score with phrase repetition, chord-aware melody and cadence."""
    if profile_name not in PROFILES:
        raise ValueError("Unknown profile: %s" % profile_name)
    if bars < 1 or bars > 64:
        raise ValueError("bars must be between 1 and 64")

    if seed is None:
        seed = _new_seed()
    rng = _RNG(seed)
    profile = PROFILES[profile_name]
    low_bpm, high_bpm = profile["bpm"]
    tempo = int(bpm if bpm is not None else rng.randint(low_bpm, high_bpm))
    if tempo < 40 or tempo > 220:
        raise ValueError("bpm must be between 40 and 220")

    scale = profile["scale"]
    root = ROOT_MIDI + rng.choice((0, 2, 3, 5, 7, 9, 10))
    progression = profile["progression"]
    motif = _make_motif(rng, profile, progression)
    events = []
    harmony = []

    for bar in range(bars):
        chord_degree = progression[bar % len(progression)]
        if bar == bars - 1 and bars >= 4:
            chord_degree = 0
        chord = triad(root, scale, chord_degree)
        harmony.append({"bar": bar, "degree": chord_degree, "notes": chord})
        bar_start = bar * STEPS_PER_BAR

        for beat in range(4):
            bass_note = chord[0] - 24
            if beat == 2 and rng.random() < 0.30:
                bass_note = chord[2] - 24
            events.append({
                "step": bar_start + beat * 4, "length": 3,
                "note": bass_note, "voice": "triangle", "volume": 0.32,
            })

        phrase_bar = bar % 2
        for slot in range(MELODY_SLOTS):
            degree = motif[slot]
            if degree is None:
                continue
            local_slot = (slot + 1) % MELODY_SLOTS if phrase_bar else slot
            if bar >= 2 and rng.random() < 0.16:
                degree += rng.choice((-2, -1, 1, 2))
            degree = max(0, min(degree, 13))
            if local_slot in (0, 4):
                degree = rng.choice((chord_degree, chord_degree + 2, chord_degree + 4))
            note = scale_note(root, scale, degree, octave=1)
            events.append({
                "step": bar_start + local_slot * 2,
                "length": 2 if profile["melody_density"] < 0.5 else rng.choice((1, 2, 2, 3)),
                "note": note,
                "voice": profile["wave"],
                "volume": 0.22 + profile["brightness"] * 0.12,
            })

        for step in range(0, STEPS_PER_BAR, 2):
            if rng.random() < profile["arp_density"]:
                tone = chord[(step // 2 + bar) % 3] + 12
                events.append({
                    "step": bar_start + step, "length": 1,
                    "note": tone, "voice": "square", "volume": 0.095,
                })

        pattern = profile["drum_pattern"]
        if pattern != "none":
            for beat in range(4):
                hit = pattern == "machine" or (pattern == "march" and beat in (0, 2))
                hit = hit or (pattern == "pulse" and beat in (1, 3))
                hit = hit or (pattern == "sparse" and beat == 0 and bar % 2 == 0)
                if hit:
                    events.append({
                        "step": bar_start + beat * 4, "length": 1,
                        "note": 0, "voice": "noise",
                        "volume": 0.12 if pattern != "machine" else 0.17,
                    })

    events.sort(key=lambda item: (item["step"], item["voice"]))
    return {
        "title": "%s / %s" % (profile["label"], seed),
        "profile": profile_name, "seed": seed, "bpm": tempo,
        "bars": bars, "steps_per_bar": STEPS_PER_BAR, "root_midi": root,
        "motif": motif, "harmony": harmony, "events": events,
    }

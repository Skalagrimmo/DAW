"""Seeded, phrase-based chiptune composition generator."""

import random

from .profiles import PROFILES
from .theory import scale_note, triad

STEPS_PER_BAR = 16
ROOT_MIDI = 48
MELODY_SLOTS = 8


class _RNG:
    """Small deterministic PRNG with the subset of methods needed here."""
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
    """Build a one-bar motif with chord tones on strong eighth-note slots."""
    motif = []
    previous = progression[0]
    for slot in range(MELODY_SLOTS):
        chord_degree = progression[0 if slot < 4 else 1]
        required = slot in (0, 4, 7)
        if not required and rng.random() > profile["melody_density"]:
            motif.append(None)
            continue

        if slot == 0:
            degree = progression[0]
        elif slot == 4:
            degree = progression[1]
        elif slot == 7:
            degree = 0
        elif slot % 2 == 0:
            degree = rng.choice((
                chord_degree, chord_degree + 2, chord_degree + 4,
            ))
        else:
            # Passing tones move by step instead of jumping randomly.
            degree = previous + rng.choice((-1, 1))
            if rng.random() < 0.25:
                degree = previous + rng.choice((-2, 2))

        degree = max(0, min(degree, 10))
        motif.append(degree)
        previous = degree

    return motif


def generate_song(profile_name="space", bars=8, seed=None, bpm=None):
    """Generate a score with repeated phrases, bass movement and cadence."""
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

        # NES-style triangle bass: root pulses with fifth/third movement.
        bass_pattern = (chord[0], chord[0], chord[2], chord[1])
        for beat, bass_pitch in enumerate(bass_pattern):
            events.append({
                "step": bar_start + beat * 4,
                "length": 4,
                "note": bass_pitch - 24,
                "voice": "triangle",
                "volume": 0.30,
            })

        # Repeated eighth-note motif; small changes arrive every four bars.
        for slot, base_degree in enumerate(motif):
            if base_degree is None:
                continue
            degree = base_degree
            if bar >= 4 and bar % 4 in (2, 3) and slot in (2, 6):
                if rng.random() < 0.30:
                    degree = max(0, min(10, degree + rng.choice((-1, 1))))

            # Land on the current chord at the start of each half-bar.
            if slot == 0:
                degree = chord_degree
            elif slot == 4:
                degree = chord_degree + 2

            note = scale_note(root, scale, degree, octave=2)
            note_length = 3 if slot in (0, 4, 7) else rng.choice((2, 2, 3))
            events.append({
                "step": bar_start + slot * 2,
                "length": note_length,
                "note": note,
                "voice": profile["wave"],
                "volume": 0.28 + profile["brightness"] * 0.10,
            })

        # Short eighth-note arpeggios reinforce the active triad.
        for slot in range(8):
            if rng.random() < profile["arp_density"]:
                tone = chord[slot % 3] + 12
                events.append({
                    "step": bar_start + slot * 2,
                    "length": 2,
                    "note": tone,
                    "voice": "square",
                    "volume": 0.075,
                })

        pattern = profile["drum_pattern"]
        if pattern != "none":
            for beat in range(4):
                hit = pattern == "machine" or (
                    pattern == "march" and beat in (0, 2)
                )
                hit = hit or (pattern == "pulse" and beat in (1, 3))
                hit = hit or (
                    pattern == "sparse" and beat == 0 and bar % 2 == 0
                )
                if hit:
                    events.append({
                        "step": bar_start + beat * 4,
                        "length": 1,
                        "note": 0,
                        "voice": "noise",
                        "volume": 0.10 if pattern != "machine" else 0.13,
                    })

    events.sort(key=lambda item: (item["step"], item["voice"]))
    return {
        "title": "%s / %s" % (profile["label"], seed),
        "profile": profile_name, "seed": seed, "bpm": tempo,
        "bars": bars, "steps_per_bar": STEPS_PER_BAR, "root_midi": root,
        "motif": motif, "harmony": harmony, "events": events,
    }

"""Seeded, phrase-based classic 8-bit composition generator."""

import random

from .profiles import PROFILES
from .theory import scale_note, triad

STEPS_PER_BAR = 16
ROOT_MIDI = 48
MELODY_SLOTS = 8

# Relative scale-degree contour for a four-bar call / response phrase.
# The final bar deliberately descends and leaves space around the tonic.
PHRASE = (
    (0, 2, 4, 2, 4, 2, 1, 2),
    (2, 4, 5, 4, 2, 4, 5, 4),
    (4, 5, 7, 5, 4, 2, 1, 2),
    (2, 1, 0, None, 0, None, 0, None),
)


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


def _arp_slots(density):
    """Choose a regular arpeggio rhythm instead of random note placement."""
    if density >= 0.55:
        return tuple(range(8))
    if density >= 0.35:
        return (0, 2, 4, 6)
    if density >= 0.20:
        return (0, 4)
    return (0,)


def generate_song(profile_name="space", bars=8, seed=None, bpm=None):
    """Generate a score with four-bar phrases, moving bass and cadence."""
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
    phrase = profile.get("phrase", PHRASE)
    events = []
    harmony = []
    motif = list(phrase[0])
    arp_slots = _arp_slots(profile["arp_density"])

    for bar in range(bars):
        chord_degree = progression[bar % len(progression)]
        if bar == bars - 1 and bars >= 4:
            chord_degree = 0
        chord = triad(root, scale, chord_degree)
        harmony.append({"bar": bar, "degree": chord_degree, "notes": chord})
        bar_start = bar * STEPS_PER_BAR
        phrase_bar = bar % 4
        contour = phrase[phrase_bar]

        # Triangle bass sits in a more audible register and outlines the chord.
        bass_pattern = (chord[0], chord[0], chord[2], chord[0])
        for beat, bass_pitch in enumerate(bass_pattern):
            events.append({
                "step": bar_start + beat * 4,
                "length": 3,
                "note": bass_pitch - 12,
                "voice": "triangle",
                "volume": 0.22,
            })

        # A repeated four-bar melody: strong beats carry chord tones;
        # weak slots may be omitted according to the profile's density.
        for slot, offset in enumerate(contour):
            if offset is None:
                continue
            anchor = slot in (0, 4, 7)
            if not anchor and rng.random() > profile["melody_density"]:
                continue

            degree = chord_degree + offset
            # Resolve the last phrase toward the tonic rather than looping
            # an unresolved contour at the end of a standalone composition.
            if bar == bars - 1 and bars >= 4 and slot in (0, 2, 4, 6):
                degree = 0 if slot >= 2 else 2

            note = scale_note(root, scale, degree, octave=2)
            note_length = 3 if slot in (0, 4) else 2
            events.append({
                "step": bar_start + slot * 2,
                "length": note_length,
                "note": note,
                "voice": profile["wave"],
                "volume": 0.24 + profile["brightness"] * 0.05,
            })

        # Regular up / down triad arpeggio; density changes the rhythm,
        # not the pitch order, so it stays musical and predictable.
        arp_order = (0, 1, 2, 1, 0, 1, 2, 1)
        for slot in arp_slots:
            events.append({
                "step": bar_start + slot * 2,
                "length": 2,
                "note": chord[arp_order[slot]] + 12,
                "voice": "square",
                "volume": 0.045,
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
                        "volume": 0.075 if pattern == "machine" else 0.055,
                    })

    events.sort(key=lambda item: (item["step"], item["voice"]))
    return {
        "title": "%s / %s" % (profile["label"], seed),
        "profile": profile_name, "seed": seed, "bpm": tempo,
        "bars": bars, "steps_per_bar": STEPS_PER_BAR, "root_midi": root,
        "motif": motif, "harmony": harmony, "events": events,
    }

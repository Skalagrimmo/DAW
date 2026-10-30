"""Seeded, structured chiptune composition generator."""

import random

from .profiles import PROFILES, SCALES

NOTE_NAMES = ("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
STEPS_PER_BAR = 16
ROOT_MIDI = 48


def midi_name(note):
    return "%s%d" % (NOTE_NAMES[note % 12], note // 12 - 1)


def _scale_note(root, scale, degree, octave=0):
    intervals = SCALES[scale]
    size = len(intervals)
    octave_shift, index = divmod(degree, size)
    return root + intervals[index] + 12 * (octave_shift + octave)


def generate_song(profile_name="space", bars=8, seed=None, bpm=None):
    """Return a deterministic score made of timed note events.

    Event fields: step, length, note (MIDI), voice, volume.
    The score is independent of the audio output backend.
    """
    if profile_name not in PROFILES:
        raise ValueError("Unknown profile: %s" % profile_name)
    if bars < 1 or bars > 64:
        raise ValueError("bars must be between 1 and 64")

    rng = random.Random(seed)
    profile = PROFILES[profile_name]
    low_bpm, high_bpm = profile["bpm"]
    tempo = int(bpm if bpm is not None else rng.randint(low_bpm, high_bpm))
    if tempo < 40 or tempo > 220:
        raise ValueError("bpm must be between 40 and 220")

    scale = profile["scale"]
    root = ROOT_MIDI + rng.choice((0, 2, 3, 5, 7, 9, 10))
    progression = profile["progression"]
    events = []
    motif = []
    previous_degree = 3

    for bar in range(bars):
        chord_degree = progression[bar % len(progression)]
        chord_root = _scale_note(root, scale, chord_degree)
        chord = (
            chord_root,
            _scale_note(root, scale, chord_degree + 2),
            _scale_note(root, scale, chord_degree + 4),
        )
        bar_start = bar * STEPS_PER_BAR

        # Bass anchors the harmony on each beat.
        for beat in range(4):
            bass_note = chord[0] - 24
            if beat == 2 and rng.random() < 0.35:
                bass_note = chord[2] - 24
            events.append({
                "step": bar_start + beat * 4,
                "length": 3,
                "note": bass_note,
                "voice": "triangle",
                "volume": 0.34,
            })

        # A small melodic motif is introduced, then varied on later bars.
        for step in range(0, STEPS_PER_BAR, 2):
            if rng.random() > profile["melody_density"]:
                continue
            if bar == 0 or not motif or rng.random() < 0.38:
                candidates = (chord_degree, chord_degree + 2, chord_degree + 4,
                              chord_degree + 5, chord_degree + 6)
                degree = rng.choice(candidates)
                if rng.random() < 0.22:
                    degree += 7
                motif.append(degree)
            else:
                source = motif[(step // 2 + bar) % len(motif)]
                degree = source
                if rng.random() < 0.24:
                    degree += rng.choice((-2, -1, 1, 2))
            degree = max(0, min(degree, 13))
            note = _scale_note(root, scale, degree, octave=1)
            length = rng.choice((1, 2, 3, 4))
            events.append({
                "step": bar_start + step,
                "length": length,
                "note": note,
                "voice": profile["wave"],
                "volume": 0.22 + profile["brightness"] * 0.12,
            })
            previous_degree = degree

        # Arpeggio layer: deliberately sparse on atmospheric profiles.
        for step in range(0, STEPS_PER_BAR, 2):
            if rng.random() < profile["arp_density"]:
                tone = rng.choice(chord)
                events.append({
                    "step": bar_start + step,
                    "length": 1,
                    "note": tone + 12,
                    "voice": "square",
                    "volume": 0.10,
                })

        # Lightweight rhythm patterns; noise is synthesized by the backend.
        pattern = profile["drum_pattern"]
        if pattern != "none":
            for beat in range(4):
                hit = pattern == "machine" or (pattern == "march" and beat in (0, 2))
                hit = hit or (pattern == "pulse" and beat in (1, 3))
                hit = hit or (pattern == "sparse" and beat == 0 and bar % 2 == 0)
                if hit:
                    events.append({
                        "step": bar_start + beat * 4,
                        "length": 1,
                        "note": 0,
                        "voice": "noise",
                        "volume": 0.12 if pattern != "machine" else 0.17,
                    })

    events.sort(key=lambda item: item["step"])
    return {
        "title": "%s / %s" % (profile["label"], seed if seed is not None else "random"),
        "profile": profile_name,
        "seed": seed,
        "bpm": tempo,
        "bars": bars,
        "steps_per_bar": STEPS_PER_BAR,
        "root_midi": root,
        "events": events,
    }

"""Continuous multi-profile transmission builder.

Sections overlap by one bar. The outgoing section fades down while the
incoming section fades up, keeping the pulse continuous at mood boundaries.
"""

import random

from .generator import generate_song
from .profiles import PROFILES


DEFAULT_SEQUENCE = ("space", "sci-fi", "epic", "space")
TRANSITION_BARS = 1
ROOT_MIDI = 48


def _section_seed(seed, index):
    """Derive stable, independent seeds without relying on Python hash()."""
    return (int(seed) + (index + 1) * 104729) & 0x7fffffff


def _transpose_event(event, semitones):
    copied = dict(event)
    if copied["voice"] != "noise":
        copied["note"] += semitones
    return copied


def generate_transmission(
    profiles=None, bars_per_section=8, seed=421, bpm=None
):
    """Build a deterministic score from a sequence of mood profiles.

    Adjacent sections share the same tempo and tonal root. Their boundary bars
    overlap: outgoing events fade out and incoming events fade in by step.
    """
    if profiles is None:
        profiles = DEFAULT_SEQUENCE
    if seed is None:
        seed = random.getrandbits(31)
    profiles = tuple(profiles)
    if not profiles:
        raise ValueError("at least one profile is required")
    unknown = [name for name in profiles if name not in PROFILES]
    if unknown:
        raise ValueError("Unknown profile: %s" % unknown[0])
    if bars_per_section < 2 or bars_per_section > 64:
        raise ValueError("bars_per_section must be between 2 and 64")

    first = generate_song(profiles[0], bars=bars_per_section,
                          seed=_section_seed(seed, 0), bpm=bpm)
    shared_bpm = first["bpm"]
    shared_root = first["root_midi"]
    steps_per_bar = first["steps_per_bar"]
    events = []
    harmony = []
    section_info = []

    for index, profile_name in enumerate(profiles):
        song = first if index == 0 else generate_song(
            profile_name,
            bars=bars_per_section,
            seed=_section_seed(seed, index),
            bpm=shared_bpm,
        )
        offset_bars = index * (bars_per_section - TRANSITION_BARS)
        offset_steps = offset_bars * steps_per_bar
        transpose = shared_root - song["root_midi"]

        for event in song["events"]:
            local_bar = event["step"] // steps_per_bar
            local_step = event["step"] % steps_per_bar
            volume = event["volume"]

            # Fade the outgoing section during its final bar.
            if index < len(profiles) - 1 and local_bar == bars_per_section - 1:
                volume *= max(0.0, 1.0 - local_step / float(steps_per_bar))

            # Fade the incoming section during its first bar.
            if index > 0 and local_bar == 0:
                volume *= local_step / float(steps_per_bar)

            adjusted = _transpose_event(event, transpose)
            adjusted["step"] += offset_steps
            adjusted["volume"] = volume
            events.append(adjusted)

        for chord in song["harmony"]:
            harmony.append({
                "bar": offset_bars + chord["bar"],
                "degree": chord["degree"],
                "notes": tuple(note + transpose for note in chord["notes"]),
                "profile": profile_name,
            })

        section_info.append({
            "profile": profile_name,
            "seed": song["seed"],
            "start_bar": offset_bars,
            "bars": bars_per_section,
        })

    events.sort(key=lambda item: (item["step"], item["voice"]))
    total_bars = len(profiles) * bars_per_section - (
        len(profiles) - 1
    ) * TRANSITION_BARS
    return {
        "title": "Infinite Signal / %s" % " -> ".join(profiles),
        "profile": "radio",
        "profiles": list(profiles),
        "seed": seed,
        "bpm": shared_bpm,
        "bars": total_bars,
        "steps_per_bar": steps_per_bar,
        "root_midi": shared_root,
        "sections": section_info,
        "harmony": harmony,
        "events": events,
    }

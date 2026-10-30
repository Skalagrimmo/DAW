"""Small chip-style PCM renderer.

The score generator is backend-independent. This CPython renderer produces
mono signed 16-bit PCM and keeps the working mix in a compact float array.
"""

import math
from array import array

SAMPLE_RATE = 22050


def midi_frequency(note):
    return 440.0 * (2.0 ** ((note - 69) / 12.0))


def _wave_value(phase, shape):
    phase = phase % 1.0
    if shape == "triangle":
        return 1.0 - 4.0 * abs(phase - 0.5)
    if shape == "saw":
        return 2.0 * phase - 1.0
    return 1.0 if phase < 0.5 else -1.0


def render_pcm(song, sample_rate=SAMPLE_RATE):
    """Render a score to mono 16-bit little-endian PCM bytes."""
    bpm = song["bpm"]
    total_steps = song["bars"] * song["steps_per_bar"]
    seconds_per_step = 60.0 / bpm / 4.0
    frame_count = int(total_steps * seconds_per_step * sample_rate)
    mix = array("f", [0.0]) * frame_count

    for event in song["events"]:
        start = int(event["step"] * seconds_per_step * sample_rate)
        length = int(event["length"] * seconds_per_step * sample_rate)
        end = min(frame_count, start + max(1, length))
        if start >= frame_count:
            continue

        volume = event["volume"]
        voice = event["voice"]
        frequency = midi_frequency(event["note"]) if voice != "noise" else 0.0
        phase_step = frequency / sample_rate if frequency else 0.0
        noise_state = (event["step"] * 1103515245 + event["note"] * 12345 + 7) & 0x7fffffff

        for index in range(start, end):
            local = index - start
            duration = max(1, end - start)
            attack = min(1.0, local / max(1.0, sample_rate * 0.006))
            release = min(1.0, (duration - local) / max(1.0, sample_rate * 0.025))
            envelope = min(attack, release)
            if voice == "noise":
                noise_state = (1103515245 * noise_state + 12345) & 0x7fffffff
                value = ((noise_state >> 15) / 16384.0) - 1.0
            else:
                value = _wave_value(local * phase_step, voice)
            mix[index] += value * volume * envelope

    output = bytearray(frame_count * 2)
    for index, sample in enumerate(mix):
        sample = max(-1.0, min(1.0, sample * 0.72))
        value = int(sample * 32767)
        if value < 0:
            value += 65536
        offset = index * 2
        output[offset] = value & 255
        output[offset + 1] = (value >> 8) & 255
    return bytes(output)

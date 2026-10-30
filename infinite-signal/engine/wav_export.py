"""CPython WAV export backend."""

import wave

from .synth import render_pcm, SAMPLE_RATE


def write_wav(song, path, sample_rate=SAMPLE_RATE):
    pcm = render_pcm(song, sample_rate)
    with wave.open(path, "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(sample_rate)
        output.writeframes(pcm)
    return path

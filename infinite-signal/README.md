# Infinite Signal

A procedural chiptune radio: generate original, repeatable music from narrative mood profiles.

This is an isolated Python project inside the DAW repository. It does not modify the existing Android application.

## v0.1 goals

- Narrative profiles: Cybernetic, Sci-Fi, Epic, Space, Dungeon, Industrial, Adventure, Horror, Retro Arcade, Ambient.
- Deterministic generation: the same profile + seed produces the same composition.
- Structured music: chord progression, melody, bass, arpeggio and percussion.
- Separate composition engine and sound renderer.
- WAV export on desktop Python; engine modules avoid third-party dependencies and are designed to stay portable to MicroPython.

## Run (CPython 3.10+)

From this directory:

```bash
python radio.py --profile space --bars 8 --seed 421 --wav space-421.wav
```

Try another mood:

```bash
python radio.py --profile cybernetic --bars 16 --seed 2026 --wav cybernetic.wav
python radio.py --list-profiles
```

On Android, run from Termux with Python installed. WAV export uses only Python's standard library.

## Architecture

- `engine/profiles.py` — mood profiles and musical constraints.
- `engine/generator.py` — deterministic composition generation; no audio-device dependency.
- `engine/synth.py` — simple chip-style waveform renderer.
- `engine/wav_export.py` — CPython WAV file output.
- `radio.py` — command-line interface.

## Design principle

The generator creates a small musical score first. The synthesizer interprets that score later. This allows the same composition engine to feed different sound backends in the future: NES-like, Game Boy-like, desktop WAV, or a MicroPython hardware output.

## v0.1 limitations

This is the first playable prototype, not a tracker or full DAW. It generates short structured loops. Live continuous playback, transitions between moods, richer instrument envelopes, stereo output and a graphical radio interface are planned for later iterations.

# Infinite Signal

A procedural chiptune radio: generate original, repeatable music from narrative mood profiles.

This is an isolated Python project inside the DAW repository. It does not modify the existing Android application.

## Current milestone: v0.2 — Music Intelligence

- Ten narrative profiles: Cybernetic, Sci-Fi, Epic, Space, Dungeon, Industrial, Adventure, Horror, Retro Arcade and Ambient.
- Deterministic generation: the same profile + seed produces the same score.
- Chord-aware bass, arpeggios and melody.
- Two-bar melodic phrase with controlled repetition and variation.
- Tonic resolution at the end of longer tracks.
- Separate score generator and chip-style PCM renderer.
- Standard-library WAV export.

## Run (CPython 3.10+)

```bash
python radio.py --profile space --bars 8 --seed 421 --wav space-421.wav
python radio.py --profile cybernetic --bars 16 --seed 2026 --wav cybernetic.wav
python radio.py --list-profiles
```

On Android, run from Termux with Python installed. WAV export uses only Python's standard library.

## Architecture

- `engine/profiles.py` — mood profiles and musical constraints.
- `engine/theory.py` — scale-degree and diatonic triad helpers.
- `engine/generator.py` — deterministic composition generation; no audio-device dependency.
- `engine/synth.py` — chip-style waveform renderer.
- `engine/wav_export.py` — CPython WAV file output.
- `radio.py` — command-line interface.

## Design principle

The generator creates a musical score first. The synthesizer interprets that score later. The same score format can feed different sound backends: NES-like, Game Boy-like, desktop WAV or a future MicroPython hardware output.

## Roadmap

- [x] v0.1 — seeded score generation and WAV export.
- [x] v0.2 — phrase structure, chord-aware layers and cadence.
- [ ] v0.3 — continuous radio stream with musical section transitions.
- [ ] v0.4 — live playback backend.
- [ ] v0.5 — visual radio interface.
- [ ] v1.0 — MicroPython hardware backend.

## Current limitations

This is an early procedural composition prototype, not a full DAW or tracker. The current renderer is intentionally simple mono chip-style PCM. Continuous playback, richer envelopes, stereo output and more expressive phrase development remain future work.

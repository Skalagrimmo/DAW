# Infinite Signal

A procedural chiptune radio and composition engine. It creates original, repeatable music from narrative mood profiles and can combine those profiles into a continuous transmission.

This is an isolated Python project inside the DAW repository. It does not modify the existing Android application.

## Current milestone: v0.3 — Continuous Radio (musical refinement in progress)

The v0.3 line is being refined toward a classic 8-bit sound before any live-playback work begins. The current refinement adds a more deliberate phrase contour, stronger chord anchors, moving triangle bass, pulse-wave leads and shorter, cleaner percussive envelopes.

- Ten narrative profiles: Cybernetic, Sci-Fi, Epic, Space, Dungeon, Industrial, Adventure, Horror, Retro Arcade and Ambient.
- Deterministic generation: the same profile sequence + seed produces the same score.
- Chord-aware bass, arpeggios and melody, with two-bar motifs and controlled variation.
- Multi-profile transmissions with a shared tempo and tonal center.
- One-bar section overlaps with outgoing fade-down and incoming fade-up.
- Two operating modes: standalone Composer and multi-profile Radio.
- Separate score generator and chip-style PCM renderer.
- Standard-library WAV export.

## Run (CPython 3.10+)

Standalone composition:

```bash
python radio.py --mode composer --profile retro-arcade --bars 8 --seed 421 --bpm 140 --wav retro-arcade.wav
python radio.py --mode composer --profile space --bars 8 --seed 421 --wav space-421.wav
```

Continuous multi-mood radio:

```bash
python radio.py --mode radio --sequence space,sci-fi,epic,space --bars 8 --seed 421 --wav transmission.wav
```

Choose a different mood sequence:

```bash
python radio.py --mode radio --sequence dungeon,horror,ambient --bars 8 --wav dark-signal.wav
```

List available moods:

```bash
python radio.py --list-profiles
```

In radio mode, `--bars` means bars per section. Adjacent sections overlap by one bar, so the total duration is shorter than simply adding every section's length. A shared tempo and tonal center keep transitions coherent in this first version.

On Android, run from Termux with Python installed. WAV export uses only Python's standard library.

## Architecture

- `engine/profiles.py` — mood profiles and musical constraints.
- `engine/theory.py` — scale-degree and diatonic triad helpers.
- `engine/generator.py` — deterministic single-composition score generation.
- `engine/radio.py` — deterministic section sequencing, tonal alignment and transitions.
- `engine/synth.py` — chip-style waveform renderer.
- `engine/wav_export.py` — CPython WAV file output.
- `radio.py` — command-line interface.

## Design principle

The generator creates a musical score first. The synthesizer interprets that score later. The same score format can feed different sound backends: NES-like, Game Boy-like, desktop WAV or a future MicroPython hardware output.

This makes the music engine reusable: a game can request a mood sequence for a level, while another project can generate a different soundtrack from the same engine and profile system.

## Roadmap

- [x] v0.1 — seeded score generation and WAV export.
- [x] v0.2 — phrase structure, chord-aware layers and cadence.
- [x] v0.3 — multi-profile continuous score with bar-boundary overlaps.
- [ ] v0.3.1 — musical refinement: classic 8-bit phrasing, bass movement and pulse synthesis.
- [ ] v0.4 — live playback backend and longer-running stream scheduler.
- [ ] v0.5 — visual radio interface.
- [ ] v1.0 — MicroPython hardware backend.

## Current limitations

The radio currently creates a finite transmission score and exports it as one WAV; it does not yet play indefinitely or stream live audio. Section transitions use a simple one-bar volume crossfade, with a shared tempo and tonal center. The renderer is intentionally simple mono chip-style PCM. Richer envelopes, stereo output, adaptive tempo changes and more expressive phrase development remain future work.

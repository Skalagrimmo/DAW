#!/usr/bin/env python3
"""Infinite Signal command-line composer and multi-profile radio."""

import argparse

from engine.generator import generate_song
from engine.profiles import PROFILES
from engine.radio import DEFAULT_SEQUENCE, generate_transmission
from engine.wav_export import write_wav


def main():
    parser = argparse.ArgumentParser(description="Generate original procedural chiptune.")
    parser.add_argument("--mode", choices=("composer", "radio"), default="composer")
    parser.add_argument("--profile", default="space", choices=sorted(PROFILES))
    parser.add_argument(
        "--sequence",
        default=",".join(DEFAULT_SEQUENCE),
        help="Comma-separated mood profiles for radio mode",
    )
    parser.add_argument(
        "--bars", type=int, default=8,
        help="Bars per section in radio mode; total bars are shorter due to overlaps",
    )
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--bpm", type=int, default=None)
    parser.add_argument(
        "--variant", default=None,
        help="Melody variant for profiles that provide alternatives (composer mode)",
    )
    parser.add_argument("--wav", default="infinite-signal.wav")
    parser.add_argument("--list-profiles", action="store_true")
    args = parser.parse_args()

    if args.list_profiles:
        for key in sorted(PROFILES):
            print("%-14s %s" % (key, PROFILES[key]["label"]))
        return

    if args.mode == "radio":
        if args.variant is not None:
            parser.error("--variant is only available in composer mode")
        sequence = tuple(item.strip() for item in args.sequence.split(",") if item.strip())
        song = generate_transmission(
            profiles=sequence, bars_per_section=args.bars,
            seed=args.seed, bpm=args.bpm,
        )
    else:
        song = generate_song(
            args.profile, args.bars, args.seed, args.bpm,
            phrase_variant=args.variant,
        )

    output = write_wav(song, args.wav)
    duration = song["bars"] * 4 * 60.0 / song["bpm"]
    print("INFINITE SIGNAL")
    print("Mode         : %s" % args.mode.upper())
    print("Transmission : %s" % song["title"])
    print("Profile      : %s" % song["profile"])
    if args.mode == "radio":
        print("Sections     : %s" % " -> ".join(song["profiles"]))
    print("Tempo        : %d BPM" % song["bpm"])
    print("Length       : %.1f seconds" % duration)
    print("Events       : %d" % len(song["events"]))
    print("WAV          : %s" % output)


if __name__ == "__main__":
    main()

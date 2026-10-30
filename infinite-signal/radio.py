#!/usr/bin/env python3
"""Infinite Signal command-line radio."""

import argparse

from engine.generator import generate_song
from engine.profiles import PROFILES
from engine.wav_export import write_wav


def main():
    parser = argparse.ArgumentParser(description="Generate original procedural chiptune.")
    parser.add_argument("--profile", default="space", choices=sorted(PROFILES))
    parser.add_argument("--bars", type=int, default=8)
    parser.add_argument("--seed", type=int, default=None)
    parser.add_argument("--bpm", type=int, default=None)
    parser.add_argument("--wav", default="infinite-signal.wav")
    parser.add_argument("--list-profiles", action="store_true")
    args = parser.parse_args()

    if args.list_profiles:
        for key in sorted(PROFILES):
            print("%-14s %s" % (key, PROFILES[key]["label"]))
        return

    song = generate_song(args.profile, args.bars, args.seed, args.bpm)
    output = write_wav(song, args.wav)
    duration = song["bars"] * 4 * 60.0 / song["bpm"]
    print("INFINITE SIGNAL")
    print("Transmission : %s" % song["title"])
    print("Profile      : %s" % song["profile"])
    print("Tempo        : %d BPM" % song["bpm"])
    print("Length       : %.1f seconds" % duration)
    print("Events       : %d" % len(song["events"]))
    print("WAV          : %s" % output)


if __name__ == "__main__":
    main()

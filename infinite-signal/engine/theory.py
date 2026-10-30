"""Small, dependency-free music theory helpers."""

from .profiles import SCALES


def scale_note(root_midi, scale_name, degree, octave=0):
    """Map a diatonic scale degree (which may cross octaves) to MIDI."""
    intervals = SCALES[scale_name]
    octave_shift, index = divmod(degree, len(intervals))
    return root_midi + intervals[index] + 12 * (octave_shift + octave)


def triad(root_midi, scale_name, degree):
    """Return a diatonic triad as three MIDI notes."""
    return (
        scale_note(root_midi, scale_name, degree),
        scale_note(root_midi, scale_name, degree + 2),
        scale_note(root_midi, scale_name, degree + 4),
    )


def chord_label(root_midi, chord):
    """Return the pitch-class name of a chord root."""
    names = ("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    return names[chord[0] % 12]

import tempfile
import wave
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import numpy.typing as npt
from media import ffmpeg

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "app/src/main/res/raw"
RATE = 44100
MAIN_PEAK_DBFS = -14.0
TICK_PEAK_DBFS = -28.0
FAN_PEAK_DBFS = -27.0
BUMP_PEAK_DBFS = -21.0
NOISE_SEED = 7
FADE_OUT_SECONDS = 0.02
VORBIS_ARGS = ["-ac", "1", "-c:a", "libvorbis", "-q:a", "2"]

type Wave = npt.NDArray[np.float64]


@dataclass(frozen=True)
class Note:
    start: float
    frequency: float
    decay: float
    level: float = 1.0
    glide_to: float | None = None
    overtones: tuple[float, ...] = (0.22, 0.06)


def timeline(seconds: float) -> Wave:
    return np.arange(int(seconds * RATE)) / RATE


def envelope(t: Wave, attack: float, decay: float) -> Wave:
    rise = np.clip(t / attack, 0.0, 1.0)
    return rise * rise * (3 - 2 * rise) * np.exp(-t / decay)


def phase(t: Wave, note: Note) -> Wave:
    if note.glide_to is None:
        return 2 * np.pi * note.frequency * t
    ratio = note.glide_to / note.frequency
    bend = note.frequency * ratio ** (t / t[-1])
    return 2 * np.pi * np.cumsum(bend) / RATE


def voice(note: Note, seconds: float, attack: float) -> Wave:
    t = timeline(seconds - note.start)
    angle = phase(t, note)
    tone = np.sin(angle)
    for harmonic, gain in enumerate(note.overtones, start=2):
        tone += gain * np.sin(harmonic * angle)
    return note.level * tone * envelope(t, attack, note.decay)


def render(notes: list[Note], seconds: float, peak_dbfs: float, attack: float = 0.004) -> Wave:
    mix = np.zeros(len(timeline(seconds)))
    for note in notes:
        offset = int(note.start * RATE)
        part = voice(note, seconds, attack)[: len(mix) - offset]
        mix[offset : offset + len(part)] += part
    fade = int(FADE_OUT_SECONDS * RATE)
    mix[-fade:] *= np.linspace(1.0, 0.0, fade)
    gain = 10 ** (peak_dbfs / 20) / float(np.max(np.abs(mix)))
    normalized: Wave = mix * gain
    return normalized


def correct() -> Wave:
    return render(
        [Note(0.0, 1046.5, 0.09, 0.8), Note(0.075, 1568.0, 0.16)],
        0.5,
        MAIN_PEAK_DBFS,
    )


def wrong() -> Wave:
    return render(
        [Note(0.0, 330.0, 0.08, glide_to=247.0, overtones=(0.35, 0.1))],
        0.32,
        MAIN_PEAK_DBFS,
        attack=0.008,
    )


def done() -> Wave:
    pitches = [784.0, 987.8, 1174.7, 1568.0]
    notes = [Note(i * 0.07, f, 0.22 if i == len(pitches) - 1 else 0.12, 0.8) for i, f in enumerate(pitches)]
    return render(notes, 0.8, MAIN_PEAK_DBFS)


def chapter() -> Wave:
    brassy = (0.3, 0.15, 0.08)
    run = [Note(i * 0.11, f, 0.1, 0.75, overtones=brassy) for i, f in enumerate([523.3, 659.3, 784.0])]
    chord = [Note(0.34, f, 0.42, 0.55, overtones=brassy) for f in [1046.5, 1318.5, 1568.0]]
    sparkle = [Note(0.34 + i * 0.09, f, 0.18, 0.25) for i, f in enumerate([2093.0, 2637.0, 3136.0])]
    return render(run + chord + sparkle, 1.3, MAIN_PEAK_DBFS)


def tick() -> Wave:
    return render([Note(0.0, 2200.0, 0.006, overtones=())], 0.04, TICK_PEAK_DBFS, attack=0.001)


def swept_noise(seconds: float, low: float, high: float) -> Wave:
    t = timeline(seconds)
    noise = np.random.default_rng(NOISE_SEED).standard_normal(len(t))
    cutoff = low * (high / low) ** (t / t[-1])
    smoothing = 1 - np.exp(-2 * np.pi * cutoff / RATE)
    filtered = np.zeros(len(t))
    level = 0.0
    for index, (sample, alpha) in enumerate(zip(noise, smoothing, strict=True)):
        level += alpha * (sample - level)
        filtered[index] = level
    return filtered


def peaked(samples: Wave) -> Wave:
    result: Wave = samples / float(np.max(np.abs(samples)))
    return result


def normalized(mix: Wave, peak_dbfs: float) -> Wave:
    fade = int(FADE_OUT_SECONDS * RATE)
    mix[-fade:] *= np.linspace(1.0, 0.0, fade)
    result: Wave = mix * (10 ** (peak_dbfs / 20) / float(np.max(np.abs(mix))))
    return result


def fan() -> Wave:
    seconds = 0.5
    t = timeline(seconds)
    swell = np.sin(np.pi * np.clip(t / seconds, 0.0, 1.0)) ** 1.5
    whoosh = swept_noise(seconds, 500.0, 4500.0) * swell
    return normalized(whoosh, FAN_PEAK_DBFS)


def bump() -> Wave:
    seconds = 0.16
    squish = render(
        [Note(0.0, 240.0, 0.06, glide_to=330.0, overtones=(0.06,)), Note(0.0, 243.0, 0.05, 0.5, glide_to=335.0)],
        seconds,
        -6.0,
        attack=0.012,
    )
    t = timeline(seconds)
    air = swept_noise(seconds, 400.0, 250.0) * envelope(t, 0.015, 0.03)
    return normalized(peaked(squish) + 0.15 * peaked(air), BUMP_PEAK_DBFS)


def write_ogg(name: str, samples: Wave) -> Path:
    target = RAW / f"sound_{name}.ogg"
    pcm = (samples * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as folder:
        source = Path(folder) / f"{name}.wav"
        with wave.open(str(source), "wb") as file:
            file.setnchannels(1)
            file.setsampwidth(2)
            file.setframerate(RATE)
            file.writeframes(pcm.tobytes())
        ffmpeg("-i", str(source), *VORBIS_ARGS, str(target))
    return target


def main() -> None:
    RAW.mkdir(parents=True, exist_ok=True)
    sounds = {
        "correct": correct(),
        "wrong": wrong(),
        "done": done(),
        "chapter": chapter(),
        "tick": tick(),
        "fan": fan(),
        "bump": bump(),
    }
    for name, samples in sounds.items():
        target = write_ogg(name, samples)
        print(f"{target.name}: {len(samples) / RATE:.2f} с, {target.stat().st_size / 1000:.1f} КБ")


if __name__ == "__main__":
    main()

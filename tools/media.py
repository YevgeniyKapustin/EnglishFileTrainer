import re
import stat
import subprocess
from pathlib import Path

import imageio_ffmpeg

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
AUDIO_ARGS = ["-vn", "-ac", "1", "-c:a", "libopus", "-b:a", "32k", "-application", "voip"]
VIDEO_SIZE = "512x288"
VIDEO_ARGS = [
    *["-vf", f"scale={VIDEO_SIZE.replace('x', ':')}:flags=lanczos,setsar=1"],
    *["-c:v", "libx264", "-preset", "slow", "-crf", "28", "-profile:v", "baseline", "-pix_fmt", "yuv420p"],
    *["-c:a", "aac", "-ac", "1", "-b:a", "64k", "-movflags", "+faststart"],
]


def ffmpeg(*args: str) -> str:
    command = [imageio_ffmpeg.get_ffmpeg_exe(), "-hide_banner", "-y", *args]
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", check=False)
    if result.returncode != 0:
        raise RuntimeError(result.stderr)
    return result.stderr


def is_compressed_video(path: Path) -> bool:
    info = ffmpeg("-i", str(path), "-f", "null", "-t", "0", "-")
    return f", {VIDEO_SIZE}" in info and re.search(r"Audio: aac.*\bmono\b", info) is not None


def compress_audio(path: Path) -> None:
    target = path.with_suffix(".ogg")
    ffmpeg("-i", str(path), *AUDIO_ARGS, str(target))
    path.chmod(stat.S_IWRITE)
    path.unlink()


def compress_video(path: Path) -> None:
    temporary = path.with_suffix(".tmp.mp4")
    ffmpeg("-i", str(path), *VIDEO_ARGS, str(temporary))
    path.chmod(stat.S_IWRITE)
    temporary.replace(path)


def megabytes(paths: list[Path]) -> float:
    return sum(p.stat().st_size for p in paths) / 1e6


def main() -> None:
    tracks = sorted((ASSETS / "audio").glob("*.mp3"))
    videos = [v for v in sorted((ASSETS / "video").glob("*.mp4")) if not is_compressed_video(v)]
    before = megabytes(tracks + videos)
    for track in tracks:
        compress_audio(track)
    for video in videos:
        compress_video(video)
    after = megabytes([t.with_suffix(".ogg") for t in tracks] + videos)
    print(f"аудио {len(tracks)}, видео {len(videos)}: {before:.1f} → {after:.1f} МБ")


if __name__ == "__main__":
    main()

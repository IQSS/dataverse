#!/usr/bin/env python3
"""
combine_videos.py
-----------------
Walks the Playwright test-results directory, collects every .webm video
whose parent folder starts with the given suite prefix, concatenates them
in sorted order, and re-encodes the result as an MPEG-4 file (H.264 + AAC).

Usage
-----
    # Unified suite (all tests)
    python scripts/combine_videos.py --suite suite

    # Custom output filename
    python scripts/combine_videos.py --suite suite --output my_demo.mp4

    # Absolute output path
    python scripts/combine_videos.py --suite suite --output ~/Desktop/demo.mp4

Requirements
------------
    - Python 3.8+
    - ffmpeg must be on $PATH  (brew install ffmpeg  /  apt install ffmpeg)
"""

import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

RESULTS_ROOT = Path(__file__).resolve().parent.parent / "test-results"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def find_webm_files(results_root: Path, suite: str) -> list[Path]:
    """
    Recursively walk *results_root* and return a sorted list of .webm files
    that live inside folders whose name starts with *suite* + '-'.

    Playwright names each test's output folder as:
        <suite>-<spec-slug>-<hash>-<test-title>-<project>/
    All of these sit directly under test-results/, NOT inside a suite
    sub-directory.  We therefore search the root and filter by prefix.
    """
    webm_files: list[Path] = []

    for root, _dirs, files in os.walk(results_root):
        root_path = Path(root)
        # Accept the folder if any ancestor (relative to results_root)
        # starts with the suite prefix, OR if the folder itself does.
        try:
            rel = root_path.relative_to(results_root)
        except ValueError:
            continue

        parts = rel.parts
        if not parts:
            continue  # the root itself — skip

        # Playwright appends the project name as the SUFFIX of each folder:
        #   <spec-slug>-<test-title>-<project>
        # We therefore filter by suffix, not prefix.
        if not parts[0].endswith(f"-{suite}"):
            continue

        for filename in files:
            if filename.lower().endswith(".webm"):
                webm_files.append(root_path / filename)

    if not webm_files:
        print(
            f"[ERROR] No .webm files found under: {results_root}\n"
            f"        (looked for folders ending with '-{suite}')",
            file=sys.stderr,
        )
        sys.exit(1)

    webm_files.sort()  # sort by full path → preserves spec/test ordering
    return webm_files


def check_ffmpeg() -> None:
    """Abort early with a helpful message if ffmpeg is not available."""
    try:
        subprocess.run(
            ["ffmpeg", "-version"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError):
        print(
            "[ERROR] ffmpeg not found on PATH.\n"
            "  macOS : brew install ffmpeg\n"
            "  Ubuntu: sudo apt install ffmpeg",
            file=sys.stderr,
        )
        sys.exit(1)


def write_concat_list(webm_files: list[Path], list_path: Path) -> None:
    """
    Write an ffmpeg concat-demuxer input file.

    Each line looks like:
        file '/absolute/path/to/clip.webm'
    Paths are written as absolute POSIX strings so ffmpeg can find them
    regardless of where the script is invoked from.
    """
    with list_path.open("w", encoding="utf-8") as fh:
        for p in webm_files:
            # ffmpeg requires single-quoted paths; escape any embedded quotes
            safe = str(p.resolve()).replace("'", "'\\''")
            fh.write(f"file '{safe}'\n")


def concat_and_encode(list_path: Path, output_path: Path) -> None:
    """
    Use ffmpeg's concat demuxer to stitch the clips together and
    re-encode as MPEG-4 / H.264 + AAC in a single pass.

    Key flags
    ---------
    -f concat           use the concat demuxer (works with .webm inputs)
    -safe 0             allow absolute paths in the list file
    -c:v libx264        H.264 video codec
    -crf 22             quality level (18=near-lossless, 28=smaller file)
    -preset fast        encoding speed/compression trade-off
    -c:a aac            AAC audio codec
    -b:a 128k           audio bitrate
    -movflags +faststart  move MOOV atom to the front for web streaming
    -y                  overwrite output without prompting
    """
    cmd = [
        "ffmpeg",
        "-f", "concat",
        "-safe", "0",
        "-i", str(list_path),
        "-c:v", "libx264",
        "-crf", "22",
        "-preset", "fast",
        "-c:a", "aac",
        "-b:a", "128k",
        "-movflags", "+faststart",
        "-y",
        str(output_path),
    ]

    print("\n[ffmpeg] Running:")
    print("  " + " ".join(cmd) + "\n")

    result = subprocess.run(cmd)

    if result.returncode != 0:
        print("\n[ERROR] ffmpeg exited with a non-zero status.", file=sys.stderr)
        sys.exit(result.returncode)


# ---------------------------------------------------------------------------
# Entry-point
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Combine Playwright .webm videos into a single MP4."
    )
    parser.add_argument(
        "--suite",
        required=True,
        help=(
            "Playwright project name prefix to collect videos for "
            "(e.g. 'suite' for the unified suite)."
        ),
    )
    parser.add_argument(
        "--output",
        default=None,
        help=(
            "Destination MP4 filename (default: test-results/<suite>_combined.mp4)."
        ),
    )
    parser.add_argument(
        "--results-root",
        default=str(RESULTS_ROOT),
        help=f"Override the test-results root directory (default: {RESULTS_ROOT}).",
    )

    args = parser.parse_args()

    results_root = Path(args.results_root)

    if not results_root.is_dir():
        print(
            f"[ERROR] test-results directory not found: {results_root}\n"
            "  Make sure you have run the Playwright tests first so that "
            "test-results/ is populated.",
            file=sys.stderr,
        )
        sys.exit(1)

    if args.output:
        raw = Path(args.output)
        # Keep absolute paths as-is; resolve relative paths into test-results/
        # so the file never lands in the repo root and gets accidentally committed.
        output_path = raw if raw.is_absolute() else results_root / raw
    else:
        output_path = results_root / f"{args.suite}_combined.mp4"

    # -----------------------------------------------------------------------
    check_ffmpeg()

    webm_files = find_webm_files(results_root, args.suite)

    print(f"[INFO] Suite      : {args.suite}")
    print(f"[INFO] Source dir : {results_root}  (suffix: -{args.suite})")
    print(f"[INFO] Output     : {output_path}")
    print(f"[INFO] Clips found: {len(webm_files)}")
    for i, f in enumerate(webm_files, 1):
        print(f"         {i:>3}. {f.relative_to(Path(args.results_root))}")

    # Write the temporary concat list inside the results root so that
    # relative paths (if any) resolve correctly.
    with tempfile.NamedTemporaryFile(
        mode="w",
        suffix="_concat_list.txt",
        dir=Path(args.results_root),
        delete=False,
    ) as tmp:
        tmp_path = Path(tmp.name)

    try:
        write_concat_list(webm_files, tmp_path)
        concat_and_encode(tmp_path, output_path)
    finally:
        tmp_path.unlink(missing_ok=True)

    print(f"\n[DONE] Combined video saved to:\n  {output_path.resolve()}")


if __name__ == "__main__":
    main()

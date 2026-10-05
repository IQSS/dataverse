# Combining Test-Run Videos into a Single MP4

Playwright records a separate `.webm` clip for every test it runs.  
The [`scripts/combine_videos.py`](../scripts/combine_videos.py) utility stitches all of those clips into one continuous MP4 file suitable for sharing with leadership or stakeholders.

---

## Prerequisites

| Requirement | Install |
|-------------|---------|
| Python 3.8+ | ships with macOS / available via `brew install python` |
| ffmpeg | `brew install ffmpeg` (macOS) · `sudo apt install ffmpeg` (Ubuntu) |

> The script uses only Python standard-library modules (`os`, `pathlib`, `subprocess`, `tempfile`, `argparse`) — no `pip install` required.

---

## Quick Start

Run the Playwright suite first so that `test-results/` is populated, then:

```bash
# Unified suite  →  test-results/suite_combined.mp4
python scripts/combine_videos.py --suite suite

# Custom output filename
python scripts/combine_videos.py --suite suite --output leadership_demo.mp4

# Absolute path
python scripts/combine_videos.py --suite suite --output ~/Desktop/demo.mp4
```

---

## CLI Reference

```
python scripts/combine_videos.py [OPTIONS]

Options:
  --suite NAME       REQUIRED. Playwright project name to collect videos for.
                     For the unified suite use 'suite'.
                     Default output: test-results/<suite>_combined.mp4
  --output FILE      Custom output filename.
                     Relative paths resolve inside test-results/.
                     Absolute paths save wherever you point.
  --results-root DIR Override the test-results root directory.
                     Default: <repo-root>/test-results
  -h, --help         Show this help message and exit.
```

### Examples

```bash
# Default output
python scripts/combine_videos.py --suite suite

# Custom relative output (saved inside test-results/)
python scripts/combine_videos.py --suite suite --output leadership_demo.mp4

# Absolute output path
python scripts/combine_videos.py --suite suite --output ~/Desktop/leadership_demo.mp4

# Point at a CI artifact download
python scripts/combine_videos.py --suite suite --results-root /tmp/downloaded-results
```

---

## How It Works

1. **Discovery** — `os.walk()` recursively scans `test-results/` and collects every `.webm` file whose parent folder **ends with** `-<suite>`. Playwright names result folders `<spec-slug>-<test-title>-<project>`, so the project name is always the suffix.
2. **Ordering** — Files are sorted by their full path, which preserves the lexicographic spec order (`01-preflight`, `02-account-management`, `10-assign-…`, etc.).
3. **Concat list** — A temporary [ffconcat](https://ffmpeg.org/ffmpeg-formats.html#concat-1) text file is written listing all clips in order. Using the concat *demuxer* (rather than the concat *filter*) keeps the command simple and handles clips with different durations gracefully.
4. **Encode** — ffmpeg runs a single-pass re-encode:

   | Setting | Value | Rationale |
   |---------|-------|-----------|
   | Video codec | `libx264` (H.264) | Universal compatibility |
   | Quality | CRF 22 | Good balance of quality vs. file size |
   | Preset | `fast` | Reasonable encode time on a laptop |
   | Audio codec | `aac` | Standard AAC at 128 kbps |
   | Container | MP4 with `+faststart` | MOOV atom moved to front for web streaming |

5. **Cleanup** — The temporary concat list is deleted whether the encode succeeds or fails.

---

## Output Location

By default the finished file is written to:

```
test-results/<suite>_combined.mp4
```

The `test-results/` directory is already listed in [`.gitignore`](../.gitignore), so the MP4 will not be committed accidentally.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `[ERROR] No .webm files found` | Run the Playwright suite first; make sure `test-results/` is populated with folders ending in `-suite`. |
| `[ERROR] ffmpeg not found on PATH` | Install ffmpeg (see Prerequisites above). |
| Clips appear out of order | The sort is lexicographic on the full path. The existing `01-`, `02-`, `10-` naming convention handles ordering correctly. |
| Audio/video out of sync | Some `.webm` clips may have been recorded without audio. ffmpeg will insert silence for those segments automatically. |

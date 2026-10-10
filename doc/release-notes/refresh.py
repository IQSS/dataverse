#!/usr/bin/env python3
"""Refresh a release note file from the live release on GitHub.

Release notes are often edited after a release goes out (typos, missing
upgrade steps, etc.) and those edits only live on the GitHub release page.
This script pulls the live body back down into the file in git.

Usage:

    python refresh.py 6.11-release-notes.md
    python refresh.py --dry-run 6.11-release-notes.md

The version is taken from the filename (6.11-release-notes.md -> tag v6.11).
Set GITHUB_TOKEN in the environment to avoid anonymous API rate limits.
"""

import argparse
import difflib
import json
import os
import re
import sys
import urllib.error
import urllib.request

REPO = "IQSS/dataverse"
FILENAME_PATTERN = re.compile(r"^(\d+\.\d+(?:\.\d+)?)-release-notes\.md$")


def tag_from_path(path):
    """Derive a git tag (v6.11) from a release note filename."""
    match = FILENAME_PATTERN.match(os.path.basename(path))
    if not match:
        sys.exit(
            "Expected a filename like 6.11-release-notes.md, got: %s"
            % os.path.basename(path)
        )
    return "v" + match.group(1)


def fetch_release_body(tag):
    """Return the body of the GitHub release for the given tag."""
    url = "https://api.github.com/repos/%s/releases/tags/%s" % (REPO, tag)
    request = urllib.request.Request(url)
    request.add_header("Accept", "application/vnd.github.v3+json")
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request) as response:
            release = json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            sys.exit("No release found on GitHub for tag %s" % tag)
        sys.exit("GitHub API returned %s %s for %s" % (error.code, error.reason, url))
    except urllib.error.URLError as error:
        sys.exit("Could not reach GitHub: %s" % error.reason)
    return release.get("body") or ""


def normalize(text):
    """GitHub serves the body with CRLF line endings. We store LF."""
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    if text and not text.endswith("\n"):
        text += "\n"
    return text


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("path", help="e.g. 6.11-release-notes.md")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="show the diff without writing the file",
    )
    args = parser.parse_args()

    tag = tag_from_path(args.path)
    live = normalize(fetch_release_body(tag))
    if not live:
        sys.exit("The GitHub release %s has an empty body. Refusing to write." % tag)

    try:
        with open(args.path, encoding="utf-8") as file:
            local = file.read()
    except FileNotFoundError:
        local = ""

    if local == live:
        print("%s is already in sync with %s." % (args.path, tag))
        return

    diff = difflib.unified_diff(
        local.splitlines(keepends=True),
        live.splitlines(keepends=True),
        fromfile="%s (git)" % args.path,
        tofile="%s (%s on GitHub)" % (args.path, tag),
    )
    sys.stdout.writelines(diff)

    if args.dry_run:
        print("\nDry run. %s left unchanged." % args.path)
        return

    with open(args.path, "w", encoding="utf-8") as file:
        file.write(live)
    print("\nUpdated %s from %s." % (args.path, tag))


if __name__ == "__main__":
    main()

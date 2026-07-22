#!/usr/bin/env python3

"""Generate release milestone dates from a release date."""

from __future__ import annotations

import sys
from datetime import date, timedelta


USAGE = "Usage: generate_release_dates.py YYYY-MM-DD"


def parse_release_date(value: str) -> date:
    try:
        return date.fromisoformat(value)
    except ValueError as exc:
        raise SystemExit(f"Invalid date '{value}'. Expected YYYY-MM-DD.") from exc


def previous_thursday(reference: date) -> date:
    days_since_thursday = (reference.weekday() - 3) % 7
    return reference - timedelta(days=days_since_thursday or 7)


def build_schedule(release_date: date) -> list[tuple[str, str]]:
    code_freeze = previous_thursday(release_date - timedelta(days=7))
    core_pr_last_call = code_freeze - timedelta(days=7)
    community_pr_last_call = core_pr_last_call - timedelta(days=7)
    curation_team_review = code_freeze + timedelta(days=4)
    start = community_pr_last_call - timedelta(days=56)

    return [
        ("Start", f"Sprint ??, {start.isoformat()}"),
        ("Community PR Last Call", community_pr_last_call.isoformat()),
        ("Core PR Last Call", core_pr_last_call.isoformat()),
        ("Code Freeze", code_freeze.isoformat()),
        (
            "Curation Team Review",
            f"{curation_team_review.isoformat()} (allocate 5 full days)",
        ),
        ("Release Date", release_date.isoformat()),
    ]


def main(argv: list[str]) -> int:
    if len(argv) != 2 or argv[1] in {"-h", "--help"}:
        print(USAGE, file=sys.stderr if len(argv) != 2 else sys.stdout)
        return 1 if len(argv) != 2 else 0

    release_date = parse_release_date(argv[1])

    if release_date.weekday() != 2:
        print(
            "Warning: Release Date is usually expected to be a Wednesday.",
            file=sys.stderr,
        )

    for label, value in build_schedule(release_date):
        print(f"- {label}: {value}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))

#!/usr/bin/env python3
"""Convert ODK all_questions.json into canonical wildwatch_schema.json for the Android app."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SCHEMA_VERSION = "2026.08.01"
ODK_PATH_PREFIX = re.compile(r"/aNieKMp6HsKPSdbtVWLfy3/")

TYPE_MAP = {
    "select1": "select_one",
    "select": "select_multiple",
    "input": "text",
    "upload": "photos",
}


def normalize_relevance(raw: str | None) -> str | None:
    if not raw:
        return None
    expr = ODK_PATH_PREFIX.sub("", raw)
    expr = re.sub(r"\s+", " ", expr.strip())
    return expr or None


def classify_section(label: str, group: str | None) -> str | None:
    """Return section key when label/group marks a new section, else None."""
    group = group or ""
    if "1." in group or label.startswith("1."):
        return "location"
    if "2." in group or label.startswith("2."):
        return "sighting"
    if "3." in group or label.startswith("3."):
        return "conflict"
    return None


def to_canonical_question(raw: dict) -> dict:
    return {
        "id": raw["id"],
        "label": raw["label"],
        "type": TYPE_MAP.get(raw["type"], raw["type"]),
        "choices": [{"id": c["id"], "label": c["label"]} for c in raw.get("choices", [])],
        "required": bool(raw.get("required", False)),
        "relevance": normalize_relevance(raw.get("relevance")),
        "group": raw.get("group"),
    }


def split_questions(questions: list[dict]) -> tuple[list[dict], list[dict], list[dict]]:
    current = "location"
    location: list[dict] = []
    sighting: list[dict] = []
    conflict: list[dict] = []

    for raw in questions:
        label = raw.get("label") or ""
        group = raw.get("group")
        section = classify_section(label, group)
        if section is not None:
            current = section

        canonical = to_canonical_question(raw)
        if current == "location":
            location.append(canonical)
        elif current == "sighting":
            sighting.append(canonical)
        else:
            conflict.append(canonical)

    return location, sighting, conflict


def build_schema(questions: list[dict]) -> dict:
    location, sighting, conflict = split_questions(questions)
    return {
        "schemaVersion": SCHEMA_VERSION,
        "forms": {
            "sighting": {"questions": location + sighting},
            "conflict": {"questions": location + conflict},
        },
    }


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    input_path = root / "art" / "questions" / "all_questions.json"
    output_path = root / "app" / "src" / "main" / "assets" / "forms" / "wildwatch_schema.json"

    if not input_path.exists():
        print(f"Input not found: {input_path}", file=sys.stderr)
        return 1

    with input_path.open(encoding="utf-8") as f:
        questions = json.load(f)

    schema = build_schema(questions)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as f:
        json.dump(schema, f, indent=2, ensure_ascii=False)
        f.write("\n")

    sighting_count = len(schema["forms"]["sighting"]["questions"])
    conflict_count = len(schema["forms"]["conflict"]["questions"])
    print(f"Wrote {output_path}")
    print(f"  source questions: {len(questions)}")
    print(f"  sighting form: {sighting_count}")
    print(f"  conflict form: {conflict_count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

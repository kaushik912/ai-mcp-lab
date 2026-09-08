"""Generate sample values from a resolved JSON schema."""

from __future__ import annotations

import random
import uuid
from typing import Any

from faker import Faker

fake = Faker()

# property-name -> faker call, checked via substring match (order matters)
_NAME_HINTS: list[tuple[str, Any]] = [
    ("email", fake.email),
    ("title", fake.sentence),
    ("name", fake.name),
    ("description", fake.paragraph),
    ("url", fake.url),
    ("phone", fake.phone_number),
    ("address", fake.address),
    ("city", fake.city),
    ("country", fake.country),
    ("username", fake.user_name),
    ("password", lambda: fake.password(length=12)),
    ("id", lambda: str(uuid.uuid4())),
]


def _string_for(prop_name: str, schema: dict[str, Any]) -> str:
    fmt = schema.get("format")
    if fmt == "date-time":
        return fake.iso8601()
    if fmt == "date":
        return fake.date()
    if fmt == "uuid":
        return str(uuid.uuid4())
    if fmt == "email":
        return fake.email()
    if "enum" in schema and schema["enum"]:
        return random.choice(schema["enum"])

    lowered = (prop_name or "").lower()
    for hint, gen in _NAME_HINTS:
        if hint in lowered:
            return gen()
    return fake.word()


def generate(schema: dict[str, Any] | None, prop_name: str = "") -> Any:
    if not schema:
        return None

    if "example" in schema:
        return schema["example"]
    if "enum" in schema and schema["enum"]:
        return random.choice(schema["enum"])
    if "default" in schema:
        return schema["default"]

    t = schema.get("type")

    if t == "string":
        return _string_for(prop_name, schema)
    if t == "integer":
        return random.randint(schema.get("minimum", 1), schema.get("maximum", 1000))
    if t == "number":
        lo, hi = schema.get("minimum", 1), schema.get("maximum", 1000)
        return round(random.uniform(lo, hi), 2)
    if t == "boolean":
        return random.choice([True, False])
    if t == "array":
        item_schema = schema.get("items", {})
        return [generate(item_schema, prop_name) for _ in range(random.randint(1, 2))]
    if t == "object" or "properties" in schema:
        return {
            name: generate(sub, name)
            for name, sub in schema.get("properties", {}).items()
        }

    # untyped schema (e.g. unresolved / empty) - best effort
    return None

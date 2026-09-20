import json
import re
from functools import lru_cache
from pathlib import Path
from typing import Any

from app.config import config

_CJK_CHUNK = re.compile(r"[\u4e00-\u9fff]{2,}")


@lru_cache(maxsize=1)
def load_api_catalog() -> list[dict[str, Any]]:
    """Load the structured API capability catalog from disk."""
    path = Path(config.api_catalog_path)
    body = json.loads(path.read_text(encoding="utf-8"))
    return body.get("endpoints", [])


def _allowed_for_role(entry: dict[str, Any], role: str | None) -> bool:
    roles = entry.get("roles") or ["PUBLIC"]
    return "PUBLIC" in roles or (role is not None and role in roles)


def _query_terms(text: str) -> list[str]:
    normalized = text.lower()
    terms = re.findall(r"[a-z0-9_./{}-]+", normalized)
    for chunk in _CJK_CHUNK.findall(normalized):
        if len(chunk) == 2:
            terms.append(chunk)
        else:
            terms.extend(chunk[index:index + 2] for index in range(len(chunk) - 1))
    return [term for term in terms if len(term) >= 2]


def search_api_catalog(role: str | None, keyword: str | None = None,
                       limit: int = 8) -> list[dict[str, Any]]:
    """Return role-scoped API metadata, ranked by keyword overlap."""
    allowed = [
        entry for entry in load_api_catalog()
        if _allowed_for_role(entry, role)
    ]
    if not keyword or not keyword.strip():
        return allowed[:limit]

    terms = _query_terms(keyword)
    scored = []
    for entry in allowed:
        haystack = " ".join(
            str(entry.get(field, ""))
            for field in ("id", "group", "name", "method", "path", "summary")
        ).lower()
        score = sum(1 for term in terms if term in haystack)
        if score:
            scored.append((score, entry))
    scored.sort(key=lambda item: (-item[0], item[1].get("id", "")))
    return [entry for _, entry in scored[:limit]]


def api_catalog_facts(question: str, role: str | None) -> list[str]:
    """Build compact prompt facts for the current role's relevant API tools."""
    matches = search_api_catalog(role, question, limit=3)
    if not matches:
        return []
    facts = [
        "后端 API 能力目录只用于选择工具，不允许根据目录拼装任意 URL；"
        "数据库访问由 Java 后端完成。"
    ]
    for entry in matches:
        facts.append(
            "可用后端能力："
            f"{entry.get('name')}；方法={entry.get('method')}；"
            f"路径={entry.get('path')}；说明={entry.get('summary')}；"
            f"写操作={'是' if entry.get('write') else '否'}；"
            f"需要确认={'是' if entry.get('confirmation') else '否'}"
        )
    return facts

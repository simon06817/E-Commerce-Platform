import json
from typing import Any

import httpx

from app.config import config

TAVILY_SEARCH_URL = "https://api.tavily.com/search"


def perform_web_search(query: str, max_results: int | None = None) -> str:
    """Search the public web through Tavily and return compact JSON evidence."""
    normalized_query = (query or "").strip()
    if not normalized_query:
        return json.dumps({"error": "搜索关键词不能为空"}, ensure_ascii=False)
    if not config.tavily_enabled:
        return json.dumps(
            {"error": "未配置 TAVILY_API_KEY，联网搜索当前不可用"},
            ensure_ascii=False,
        )

    limit = max_results if max_results is not None else config.tavily_max_results
    limit = max(1, min(int(limit), 8))
    try:
        response = httpx.post(
            TAVILY_SEARCH_URL,
            json={
                "api_key": config.tavily_api_key,
                "query": normalized_query,
                "search_depth": config.tavily_search_depth,
                "include_answer": True,
                "include_raw_content": False,
                "max_results": limit,
            },
            timeout=config.tavily_timeout_seconds,
        )
        response.raise_for_status()
        body: dict[str, Any] = response.json()
    except Exception as exc:
        return json.dumps(
            {"error": f"联网搜索失败：{type(exc).__name__}"},
            ensure_ascii=False,
        )
    results = []
    for item in body.get("results", [])[:limit]:
        results.append({
            "title": item.get("title"),
            "url": item.get("url"),
            "content": item.get("content"),
            "score": item.get("score"),
            "published_date": item.get("published_date"),
        })
    return json.dumps(
        {
            "query": normalized_query,
            "answer": body.get("answer"),
            "results": results,
            "source": "tavily",
        },
        ensure_ascii=False,
    )

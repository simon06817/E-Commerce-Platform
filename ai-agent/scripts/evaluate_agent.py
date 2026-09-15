from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from statistics import mean
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app.agent import run_agent
from app.rag import search_structured


def load_cases(path: Path) -> list[dict[str, Any]]:
    """Load JSON Lines cases and validate the required fields."""
    cases = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        case = json.loads(line)
        required = {"id", "question", "expected_product_ids", "expected_keywords"}
        missing = required.difference(case)
        if missing:
            raise ValueError(f"case line {line_number} missing fields: {sorted(missing)}")
        cases.append(case)
    return cases


def ranked_product_ids(result: dict[str, list[dict[str, Any]]], top_k: int) -> list[int]:
    """Return unique product ids in retrieval order."""
    ranked = []
    for item in result.get("products", [])[:top_k]:
        product_id = item.get("product_id")
        if product_id is None:
            continue
        product_id = int(product_id)
        if product_id not in ranked:
            ranked.append(product_id)
    return ranked


def _keyword_recall(text: str, keywords: list[str]) -> float:
    if not keywords:
        return 1.0
    normalized = text.lower()
    matches = sum(1 for keyword in keywords if keyword.lower() in normalized)
    return matches / len(keywords)


def evaluate_retrieval(
    case: dict[str, Any],
    result: dict[str, list[dict[str, Any]]],
    top_k: int,
) -> dict[str, Any]:
    """Calculate product Hit@K, Recall@K, MRR and keyword recall."""
    expected_products = {int(item) for item in case.get("expected_product_ids", [])}
    ranked = ranked_product_ids(result, top_k)
    relevant_rank = next(
        (index for index, product_id in enumerate(ranked, 1)
         if product_id in expected_products),
        None,
    )

    if expected_products:
        hit_at_k = 1.0 if relevant_rank is not None else 0.0
        recall_at_k = len(expected_products.intersection(ranked)) / len(expected_products)
        reciprocal_rank = 1.0 / relevant_rank if relevant_rank else 0.0
    else:
        hit_at_k = None
        recall_at_k = None
        reciprocal_rank = None

    knowledge_text = "\n".join(
        item.get("content", "") for item in result.get("knowledge", [])[:top_k]
    )
    product_text = "\n".join(
        item.get("content", "") for item in result.get("products", [])[:top_k]
    )
    keyword_recall = _keyword_recall(
        f"{knowledge_text}\n{product_text}", case.get("expected_keywords", [])
    )

    return {
        "id": case["id"],
        "question": case["question"],
        "ranked_product_ids": ranked,
        "expected_product_ids": sorted(expected_products),
        "hit_at_k": hit_at_k,
        "recall_at_k": recall_at_k,
        "reciprocal_rank": reciprocal_rank,
        "keyword_recall": keyword_recall,
        "sources": sorted({
            item.get("source", "")
            for group in ("knowledge", "products")
            for item in result.get(group, [])[:top_k]
            if item.get("source")
        }),
    }


def _average(values: list[float | None]) -> float:
    present = [value for value in values if value is not None]
    return mean(present) if present else 0.0


def summarize(details: list[dict[str, Any]]) -> dict[str, Any]:
    """Aggregate per-case retrieval metrics."""
    return {
        "case_count": len(details),
        "hit_at_k": _average([item["hit_at_k"] for item in details]),
        "recall_at_k": _average([item["recall_at_k"] for item in details]),
        "mrr": _average([item["reciprocal_rank"] for item in details]),
        "keyword_recall": _average([item["keyword_recall"] for item in details]),
    }


def _answer_metrics(case: dict[str, Any], result: dict[str, Any]) -> dict[str, Any]:
    answer = str(result.get("answer", ""))
    recall = _keyword_recall(answer, case.get("expected_keywords", []))
    return {
        "answer_keyword_recall": recall,
        "answer_ok": 1.0 if recall == 1.0 else 0.0,
        "answer": answer,
        "memory_size": result.get("memory_size", 0),
    }


def run_evaluation(args: argparse.Namespace) -> dict[str, Any]:
    cases = load_cases(Path(args.cases))
    if args.max_cases:
        cases = cases[:args.max_cases]

    retrieval_details = []
    answer_details = []
    for case in cases:
        try:
            result = search_structured(case["question"], k=args.top_k)
            retrieval_details.append(evaluate_retrieval(case, result, args.top_k))
            if args.mode in {"answer", "both"}:
                facts = [
                    f"Knowledge: {item['content']}" for item in result["knowledge"]
                ] + [
                    f"Product: {item['content']}" for item in result["products"]
                ]
                agent_result = run_agent(
                    f"eval:{case['id']}",
                    case["question"],
                    facts,
                    "BUYER",
                )
                answer_details.append({
                    "id": case["id"],
                    **_answer_metrics(case, agent_result),
                })
        except Exception as exc:
            retrieval_details.append({
                "id": case["id"],
                "question": case["question"],
                "error": str(exc),
            })

    report: dict[str, Any] = {
        "mode": args.mode,
        "top_k": args.top_k,
        "retrieval": {
            "summary": summarize([
                item for item in retrieval_details if "error" not in item
            ]),
            "details": retrieval_details,
        },
    }
    if args.mode in {"answer", "both"}:
        report["answer"] = {
            "summary": {
                "case_count": len(answer_details),
                "answer_ok": _average([
                    item["answer_ok"] for item in answer_details
                ]),
                "keyword_recall": _average([
                    item["answer_keyword_recall"] for item in answer_details
                ]),
            },
            "details": answer_details,
        }
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate RAG retrieval and answers.")
    parser.add_argument(
        "--cases",
        default=str(ROOT / "data" / "eval" / "questions.jsonl"),
        help="JSON Lines evaluation file",
    )
    parser.add_argument("--top-k", type=int, default=3)
    parser.add_argument("--max-cases", type=int, default=0)
    parser.add_argument("--mode", choices=("retrieval", "answer", "both"), default="retrieval")
    parser.add_argument("--output", help="Optional JSON report path")
    args = parser.parse_args()

    report = run_evaluation(args)
    rendered = json.dumps(report, ensure_ascii=False, indent=2)
    print(rendered)
    if args.output:
        Path(args.output).write_text(rendered + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

import json
from typing import Any

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.agent import run_agent
from app.config import config
from app.rag import search_structured
from app.tools import (
    begin_action_collection,
    end_action_collection,
    execute_proposed_action,
    proposed_actions,
    reset_user_token,
    set_user_token,
)


app = FastAPI(title="E-Commerce AI Agent")
app.add_middleware(
    CORSMiddleware,
    allow_origins=config.cors_origins,
    allow_methods=["GET", "POST", "PUT", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
    allow_credentials=True,
    max_age=3600,
)

# 每个用户一个进程内待确认动作队列，服务重启后清空。
_pending_actions: dict[str, list[dict[str, Any]]] = {}
_memory_sizes: dict[str, int] = {}

_CONFIRM_WORDS = {"确认", "确定", "是", "好的", "好", "yes", "y", "ok"}
_CANCEL_WORDS = {"取消", "不用了", "不要了", "no", "n"}


class ChatRequest(BaseModel):
    """聊天请求体。"""

    message: str


def _java_get(path: str, token: str | None = None) -> Any:
    """调用 Java 后端并用统一 Result 结构解包。"""
    headers = {"Authorization": token} if token else {}
    url = config.java_api_base_url.rstrip("/") + path
    response = httpx.get(url, headers=headers, timeout=20)
    response.raise_for_status()
    body = response.json()
    if body.get("code") != 200:
        raise HTTPException(status_code=502, detail=body.get("message", "java api error"))
    return body.get("data")


def _current_user(authorization: str | None) -> dict:
    """通过 Java /api/auth/me 校验 JWT 并获取角色与用户 id。"""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="missing bearer token")
    try:
        return _java_get("/api/auth/me", authorization)
    except Exception as exc:
        raise HTTPException(status_code=401, detail="invalid token") from exc


def _review_facts(product_id: int) -> list[str]:
    """读取商品平均分和前几条评价，作为回答前的事实来源。"""
    facts = []
    summary = _java_get(f"/api/products/{product_id}/reviews/summary")
    if summary:
        facts.append(
            f"Review summary for product {product_id}: "
            f"average={summary.get('averageRating')}, count={summary.get('reviewCount')}"
        )
    reviews = _java_get(f"/api/products/{product_id}/reviews?page=1&size=3")
    for review in (reviews or {}).get("records", []):
        facts.append(
            f"Review for product {product_id}: rating={review.get('rating')}, "
            f"content={review.get('content')}, buyer={review.get('buyerName')}"
        )
    return facts


def _account_facts(question: str, authorization: str, role: str | None) -> list[str]:
    """Accounts questions are prefetched deterministically with the user's JWT."""
    q = question.lower()
    facts = []
    try:
        if any(keyword in q for keyword in ("通知", "notification")):
            notifications = _java_get("/api/notifications/my", authorization)
            facts.append("Notifications: " + json.dumps(notifications, ensure_ascii=False))
        if any(keyword in q for keyword in ("订单", "order")):
            orders = _java_get("/api/orders", authorization)
            counts = {}
            for order in (orders or {}).get("records", []):
                status = order.get("status")
                counts[status] = counts.get(status, 0) + 1
            facts.append("Orders: " + json.dumps(orders, ensure_ascii=False))
            facts.append("Order status counts: " + json.dumps(counts, ensure_ascii=False))
        if any(keyword in q for keyword in ("购物车", "cart")):
            cart = _java_get("/api/carts", authorization)
            facts.append("Cart: " + json.dumps(cart, ensure_ascii=False))
        if any(keyword in q for keyword in ("退货", "return")):
            path = "/api/seller/returns" if role == "SELLER" else "/api/returns/my"
            returns = _java_get(path, authorization)
            facts.append("Returns: " + json.dumps(returns, ensure_ascii=False))
        if role == "SELLER" and any(
                keyword in q for keyword in ("统计", "收入", "statistics", "revenue")):
            stats = _java_get("/api/seller/stats?range=all", authorization)
            facts.append("Seller stats: " + json.dumps(stats, ensure_ascii=False))
    except Exception as exc:
        facts.append(f"Account tool error: {exc}")
    return facts


def _retrieve_facts(question: str, authorization: str,
                    role: str | None) -> tuple[list[str], list[dict]]:
    """执行确定性前置检索：静态知识 + 商品描述 + 实时信息 + 评价。"""
    retrieved = search_structured(question, k=3)
    facts = _account_facts(question, authorization, role)
    products = []

    for item in retrieved["knowledge"]:
        facts.append(f"Knowledge: {item['content']}")

    for item in retrieved["products"][:3]:
        product_id = item.get("product_id")
        if product_id is None:
            continue
        products.append(item)
        facts.append(f"Product description: {item['content']}")
        try:
            detail = _java_get(f"/api/products/{product_id}")
            if detail:
                facts.append(
                    f"Product realtime data: id={detail.get('id')}, name={detail.get('name')}, "
                    f"price={detail.get('price')}, stock={detail.get('stock')}, "
                    f"status={detail.get('status')}"
                )
            facts.extend(_review_facts(int(product_id)))
        except Exception as exc:
            facts.append(f"Tool error for product {product_id}: {exc}")
    return facts, products


def _is_confirm(text: str) -> bool:
    return text.strip().lower() in _CONFIRM_WORDS


def _is_cancel(text: str) -> bool:
    return text.strip().lower() in _CANCEL_WORDS


def _confirmation_text(action: dict) -> str:
    action_type = action.get("type")
    if action_type == "add_to_cart":
        return (
            f"确认要把商品 {action.get('product_id')} 加入购物车"
            f"（数量 {action.get('num', 1)}）吗？回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "cancel_order":
        return f"确认要取消订单 {action.get('order_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "apply_return":
        return (
            f"确认要为订单明细 {action.get('order_item_id')} 申请退货吗？"
            f"理由：{action.get('reason')}。回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "cancel_return":
        return f"确认要撤销退货申请 {action.get('return_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "approve_return":
        return f"确认同意退货申请 {action.get('return_id')} 并恢复库存吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "reject_return":
        return f"确认拒绝退货申请 {action.get('return_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "reply_review":
        return f"确认回复评价 {action.get('review_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "force_cancel_order":
        return f"确认强制关闭订单 {action.get('order_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "mark_notification_read":
        return f"确认把通知 {action.get('notification_id')} 标记为已读吗？回复“确认”执行，回复“取消”放弃。"
    return "检测到待确认操作，请回复“确认”或“取消”。"


def _format_action_result(action: dict, raw: str) -> str:
    try:
        body = json.loads(raw)
        if body.get("code") == 200:
            if action.get("type") == "add_to_cart":
                return "已成功加入购物车。"
            if action.get("type") == "cancel_order":
                return "订单已成功取消，库存已恢复。"
            if action.get("type") == "apply_return":
                return "退货申请已提交，等待卖家处理。"
            if action.get("type") == "cancel_return":
                return "退货申请已撤销。"
            if action.get("type") == "approve_return":
                return "已同意退货，库存已恢复，模拟退款完成。"
            if action.get("type") == "reject_return":
                return "已拒绝退货申请。"
            if action.get("type") == "reply_review":
                return "评价回复已提交。"
            if action.get("type") == "force_cancel_order":
                return "订单已强制关闭，库存已恢复。"
            if action.get("type") == "mark_notification_read":
                return "通知已标记为已读。"
        return f"操作失败：{body.get('message', 'unknown error')}"
    except Exception:
        return raw


@app.get("/health")
def health():
    """健康检查接口。"""
    return {"status": "ok"}


@app.post("/chat")
async def chat(request: ChatRequest, authorization: str | None = Header(default=None)):
    """校验用户、处理待确认操作、先检索真实信息，再调用 LangGraph。"""
    user = _current_user(authorization)
    thread_id = f"{user.get('role')}:{user.get('id')}"
    question = request.message.strip()

    pending = _pending_actions.get(thread_id)
    if pending:
        if _is_cancel(question):
            _pending_actions.pop(thread_id, None)
            answer = "已取消该操作。"
            return _response(answer, user, [], _memory_sizes.get(thread_id, 0))
        if not _is_confirm(question):
            return _response(
                "你有一个待确认操作，请回复“确认”执行，或回复“取消”放弃。",
                user, [], _memory_sizes.get(thread_id, 0),
            )

        action = pending.pop(0)
        if not pending:
            _pending_actions.pop(thread_id, None)
        token_ctx = set_user_token(authorization)
        try:
            raw = execute_proposed_action(action)
            answer = _format_action_result(action, raw)
        except Exception as exc:
            answer = f"操作执行失败：{exc}"
        finally:
            reset_user_token(token_ctx)
        return _response(answer, user, [], _memory_sizes.get(thread_id, 0))

    facts, products = _retrieve_facts(question, authorization, user.get("role"))
    token_ctx = set_user_token(authorization)
    action_ctx = begin_action_collection()
    actions: list[dict[str, Any]] = []
    result: dict[str, Any] = {}
    try:
        result = run_agent(thread_id, question, facts, user.get("role"))
        actions = proposed_actions()
    finally:
        end_action_collection(action_ctx)
        reset_user_token(token_ctx)

    memory_size = int(result.get("memory_size", 0))
    _memory_sizes[thread_id] = memory_size
    if actions:
        _pending_actions[thread_id] = actions
        answer = _confirmation_text(actions[0])
    else:
        answer = result.get("answer", "Agent call failed.")

    return _response(answer, user, products, memory_size, bool(actions))


def _response(answer: str, user: dict, products: list[dict], memory_size: int,
              pending_action: bool = False) -> dict:
    """统一 AI 服务返回结构。"""
    return {
        "answer": answer,
        "user": {"id": user.get("id"), "role": user.get("role")},
        "product_ids": [item.get("product_id") for item in products],
        "memory_size": memory_size,
        "pending_action": pending_action,
    }

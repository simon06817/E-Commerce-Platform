import json
from contextvars import ContextVar
from typing import Any

import httpx
from langchain_core.tools import tool

from app.config import config

# 当前请求的 JWT 与“待用户确认的写操作”只存在于本次请求上下文。
_user_token: ContextVar[str | None] = ContextVar("user_token", default=None)
_proposed_actions: ContextVar[list[dict[str, Any]] | None] = ContextVar(
    "proposed_actions", default=None
)

_ORDER_STATUS_TEXT = {
    0: "UNPAID",
    1: "PAID",
    2: "SHIPPED",
    3: "COMPLETED",
    4: "CANCELED",
}


def set_user_token(token: str):
    """保存当前请求的 Bearer Token，供工具调用 Java 登录态接口。"""
    return _user_token.set(token)


def reset_user_token(token_ctx) -> None:
    """请求结束后清理 Token 上下文。"""
    _user_token.reset(token_ctx)


def begin_action_collection():
    """开始收集模型提出的写操作，等待用户确认后才真正执行。"""
    return _proposed_actions.set([])


def end_action_collection(token_ctx) -> None:
    """请求结束后清理写操作收集上下文。"""
    _proposed_actions.reset(token_ctx)


def proposed_actions() -> list[dict[str, Any]]:
    """返回当前请求中模型提出但尚未执行的写操作。"""
    return list(_proposed_actions.get() or [])


def _request(
    method: str,
    path: str,
    *,
    auth: bool = False,
    json_body: dict | None = None,
    params: dict | None = None,
) -> str:
    """统一调用 Java 后端，按需附带当前用户的 JWT。"""
    headers = {}
    if auth:
        token = _user_token.get()
        if not token:
            return "ERROR: missing user token"
        headers["Authorization"] = token

    url = config.java_api_base_url.rstrip("/") + path
    response = httpx.request(
        method,
        url,
        headers=headers,
        json=json_body,
        params=params,
        timeout=20,
    )
    response.raise_for_status()
    return response.text


def _enrich_order_status(raw: str) -> str:
    """给订单 JSON 补充状态文本，避免小模型误解数字状态。"""
    body = json.loads(raw)
    data = body.get("data")

    def enrich(order: dict) -> None:
        if isinstance(order, dict) and order.get("status") is not None:
            order["statusText"] = _ORDER_STATUS_TEXT.get(int(order["status"]), "UNKNOWN")

    if isinstance(data, dict) and isinstance(data.get("records"), list):
        for order in data["records"]:
            enrich(order)
    elif isinstance(data, dict):
        enrich(data)
    return json.dumps(body, ensure_ascii=False)


@tool
def search_products(keyword: str) -> str:
    """按关键词搜索商品。"""
    return _request("GET", "/api/products", params={"keyword": keyword, "page": 1, "size": 10})


@tool
def get_product_detail(product_id: int) -> str:
    """按商品 id 查询商品详情。"""
    return _request("GET", f"/api/products/{product_id}")


@tool
def get_categories() -> str:
    """查询启用中的商品分类。"""
    return _request("GET", "/api/categories")


@tool
def get_product_reviews(product_id: int) -> str:
    """查询某个商品的用户评价。"""
    return _request("GET", f"/api/products/{product_id}/reviews", params={"page": 1, "size": 5})


@tool
def get_product_rating_summary(product_id: int) -> str:
    """查询某个商品的平均分和评价数量。"""
    return _request("GET", f"/api/products/{product_id}/reviews/summary")


@tool
def get_my_cart() -> str:
    """查询当前买家的购物车，需要登录态。"""
    return _request("GET", "/api/carts", auth=True)


@tool
def get_my_orders(status: int | None = None) -> str:
    """查询当前买家的订单，可按状态过滤，需要登录态。"""
    params = {"page": 1, "size": 10}
    if status is not None:
        params["status"] = status
    return _enrich_order_status(_request("GET", "/api/orders", auth=True, params=params))


@tool
def get_order_detail(order_id: int) -> str:
    """查询当前买家自己的订单详情，需要登录态。"""
    return _enrich_order_status(_request("GET", f"/api/orders/{order_id}", auth=True))


@tool
def get_my_profile() -> str:
    """查询当前登录用户自己的资料。"""
    return _request("GET", "/api/profile", auth=True)


@tool
def get_my_returns() -> str:
    """查询当前买家的退货申请。"""
    return _request("GET", "/api/returns/my", auth=True, params={"page": 1, "size": 10})


@tool
def get_my_notifications() -> str:
    """查询当前用户的订单通知。"""
    return _request("GET", "/api/notifications/my", auth=True, params={"page": 1, "size": 10})


@tool
def mark_notification_read(notification_id: int) -> str:
    """提出将通知标记为已读，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "mark_notification_read", "notification_id": notification_id})
    return "PROPOSED: mark notification as read. Waiting for user confirmation."


@tool
def add_to_cart(product_id: int, num: int = 1) -> str:
    """提出加入购物车请求，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "add_to_cart", "product_id": product_id, "num": num})
    return "PROPOSED: add product to cart. Waiting for user confirmation."


@tool
def cancel_order(order_id: int) -> str:
    """提出取消订单请求，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "cancel_order", "order_id": order_id})
    return "PROPOSED: cancel order. Waiting for user confirmation."


@tool
def apply_return(order_item_id: int, reason: str) -> str:
    """提出退货申请，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "apply_return", "order_item_id": order_item_id, "reason": reason})
    return "PROPOSED: apply return. Waiting for user confirmation."


@tool
def cancel_return(return_id: int) -> str:
    """提出撤销退货申请，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "cancel_return", "return_id": return_id})
    return "PROPOSED: cancel return. Waiting for user confirmation."


@tool
def get_seller_orders(status: int | None = None) -> str:
    """查询当前卖家的订单列表，需要登录态。"""
    params = {"page": 1, "size": 10}
    if status is not None:
        params["status"] = status
    return _enrich_order_status(_request("GET", "/api/seller/orders", auth=True, params=params))


@tool
def get_seller_returns() -> str:
    """查询当前卖家的退货申请。"""
    return _request("GET", "/api/seller/returns", auth=True, params={"page": 1, "size": 10})


@tool
def get_seller_stats(range: str = "30d") -> str:
    """查询当前卖家的收入统计，range 可为 today/7d/30d/all。"""
    return _request("GET", "/api/seller/stats", auth=True, params={"range": range})


@tool
def approve_return(return_id: int, note: str = "") -> str:
    """提出同意退货，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "approve_return", "return_id": return_id, "note": note})
    return "PROPOSED: approve return. Waiting for user confirmation."


@tool
def reject_return(return_id: int, note: str = "") -> str:
    """提出拒绝退货，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "reject_return", "return_id": return_id, "note": note})
    return "PROPOSED: reject return. Waiting for user confirmation."


@tool
def reply_review(review_id: int, content: str) -> str:
    """提出回复评价，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "reply_review", "review_id": review_id, "content": content})
    return "PROPOSED: reply review. Waiting for user confirmation."


@tool
def get_admin_orders(status: int | None = None) -> str:
    """管理员查询全部订单，需要管理员登录态。"""
    params = {"page": 1, "size": 10}
    if status is not None:
        params["status"] = status
    return _enrich_order_status(_request("GET", "/api/admin/orders", auth=True, params=params))


@tool
def force_cancel_order(order_id: int) -> str:
    """提出管理员强制关单，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "ERROR: action context is not available"
    actions.append({"type": "force_cancel_order", "order_id": order_id})
    return "PROPOSED: force cancel order. Waiting for user confirmation."


def build_tools(role: str | None = None):
    """按角色注册 Agent 可调用的工具，公共工具对所有角色开放。"""
    common = [
        search_products,
        get_product_detail,
        get_categories,
        get_product_reviews,
        get_product_rating_summary,
        get_my_profile,
        get_my_notifications,
        mark_notification_read,
    ]
    buyer = [
        get_my_cart,
        get_my_orders,
        get_order_detail,
        add_to_cart,
        cancel_order,
        get_my_returns,
        apply_return,
        cancel_return,
    ]
    seller = [
        get_seller_orders,
        get_seller_returns,
        get_seller_stats,
        approve_return,
        reject_return,
        reply_review,
    ]
    admin = [
        get_admin_orders,
        force_cancel_order,
    ]
    role_tools = {
        "BUYER": buyer,
        "SELLER": seller,
        "ADMIN": admin,
    }
    return common + role_tools.get(role, [])


def execute_proposed_action(action: dict[str, Any]) -> str:
    """用户确认后执行写操作，全程使用当前请求的 JWT。"""
    action_type = action.get("type")
    if action_type == "add_to_cart":
        return _request(
            "POST",
            "/api/carts",
            auth=True,
            json_body={"productId": action["product_id"], "num": action.get("num", 1)},
        )
    if action_type == "cancel_order":
        return _request("PUT", f"/api/orders/{action['order_id']}/cancel", auth=True)
    if action_type == "apply_return":
        return _request(
            "POST",
            "/api/returns",
            auth=True,
            json_body={"orderItemId": action["order_item_id"], "reason": action["reason"]},
        )
    if action_type == "cancel_return":
        return _request("PUT", f"/api/returns/{action['return_id']}/cancel", auth=True)
    if action_type == "approve_return":
        return _request(
            "PUT",
            f"/api/seller/returns/{action['return_id']}/approve",
            auth=True,
            json_body={"note": action.get("note", "")},
        )
    if action_type == "reject_return":
        return _request(
            "PUT",
            f"/api/seller/returns/{action['return_id']}/reject",
            auth=True,
            json_body={"note": action.get("note", "")},
        )
    if action_type == "reply_review":
        return _request(
            "PUT",
            f"/api/seller/reviews/{action['review_id']}/reply",
            auth=True,
            json_body={"content": action["content"]},
        )
    if action_type == "force_cancel_order":
        return _request("PUT", f"/api/admin/orders/{action['order_id']}/force-cancel", auth=True)
    if action_type == "mark_notification_read":
        return _request(
            "PUT", f"/api/notifications/{action['notification_id']}/read", auth=True)
    return "ERROR: unsupported action"

import json
import re
from contextvars import ContextVar
from typing import Any

import httpx
from langchain_core.tools import tool

from app import metrics
from app.api_catalog import search_api_catalog
from app.config import config
from app.web_search import perform_web_search

# 当前请求的 JWT 与“待用户确认的写操作”只存在于本次请求上下文。
_user_token: ContextVar[str | None] = ContextVar("user_token", default=None)
_user_role: ContextVar[str | None] = ContextVar("user_role", default=None)
_proposed_actions: ContextVar[list[dict[str, Any]] | None] = ContextVar(
    "proposed_actions", default=None
)

_ORDER_STATUS_TEXT = {
    0: "待付款",
    1: "待发货",
    2: "待收货",
    3: "已完成",
    4: "已取消",
}

_PRODUCT_STATUS_TEXT = {
    0: "下架",
    1: "上架",
}

_CATEGORY_STATUS_TEXT = {
    0: "停用",
    1: "启用",
}

# The model sometimes keeps "shop7 的" inside the product keyword. Pull that
# seller reference out before querying so product name and shop name remain
# independent filters.
_SHOP_REFERENCE = re.compile(r"(?i)((?:shop|店铺)\s*[-_]?\s*\d+)")


def split_product_query(keyword: str | None,
                        shop_name: str | None = None) -> tuple[str, str | None]:
    """Return (product keyword, shop name) for natural-language product queries."""
    product_keyword = (keyword or "").strip()
    resolved_shop = (shop_name or "").strip() or None
    if not resolved_shop and product_keyword:
        match = _SHOP_REFERENCE.search(product_keyword)
        if match:
            resolved_shop = match.group(1).strip()
            product_keyword = (
                product_keyword[:match.start()] + " " + product_keyword[match.end():]
            ).strip()
            product_keyword = re.sub(r"^\s*(?:的|是|在)\s*", "", product_keyword)
    return product_keyword.strip(), resolved_shop


def set_user_token(token: str):
    """保存当前请求的 Bearer Token，供工具调用 Java 登录态接口。"""
    return _user_token.set(token)


def reset_user_token(token_ctx) -> None:
    """请求结束后清理 Token 上下文。"""
    _user_token.reset(token_ctx)


def set_user_role(role: str | None):
    """保存当前请求角色，供 API 目录按权限过滤。"""
    return _user_role.set(role)


def reset_user_role(role_ctx) -> None:
    """请求结束后清理角色上下文。"""
    _user_role.reset(role_ctx)


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
            return "错误：缺少用户登录令牌"
        headers["Authorization"] = token

    url = config.java_api_base_url.rstrip("/") + path
    metrics.increment(f"agent.java_tool.{method.lower()}.calls")
    try:
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
    except Exception:
        metrics.increment(f"agent.java_tool.{method.lower()}.failures")
        raise


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


def _enrich_product_status(raw: str) -> str:
    """给商品 JSON 补充上下架文本，避免与订单状态混淆。"""
    body = json.loads(raw)
    data = body.get("data")

    def enrich(product: dict) -> None:
        if isinstance(product, dict) and product.get("status") is not None:
            product["statusText"] = _PRODUCT_STATUS_TEXT.get(
                int(product["status"]), "未知状态"
            )

    if isinstance(data, dict) and isinstance(data.get("records"), list):
        for product in data["records"]:
            enrich(product)
    elif isinstance(data, dict):
        enrich(data)
    return json.dumps(body, ensure_ascii=False)


def _enrich_category_status(raw: str) -> str:
    """给分类 JSON 补充启用状态文本。"""
    body = json.loads(raw)
    data = body.get("data")

    def enrich(category: dict) -> None:
        if isinstance(category, dict) and category.get("status") is not None:
            category["statusText"] = _CATEGORY_STATUS_TEXT.get(
                int(category["status"]), "未知状态"
            )

    if isinstance(data, dict) and isinstance(data.get("records"), list):
        for category in data["records"]:
            enrich(category)
    elif isinstance(data, list):
        for category in data:
            enrich(category)
    elif isinstance(data, dict):
        enrich(data)
    return json.dumps(body, ensure_ascii=False)


@tool
def search_products(keyword: str | None = None,
                    shop_name: str | None = None) -> str:
    """搜索商品。商品名或描述放入 keyword，店铺名放入 shop_name，不要拼在一起。"""
    product_keyword, resolved_shop = split_product_query(keyword, shop_name)
    params = {"page": 1, "size": 10}
    if product_keyword:
        params["keyword"] = product_keyword
    if resolved_shop:
        params["shopName"] = resolved_shop
    return _enrich_product_status(
        _request("GET", "/api/products", params=params))


@tool
def get_product_detail(product_id: int) -> str:
    """按商品 id 查询商品详情。"""
    return _enrich_product_status(
        _request("GET", f"/api/products/{product_id}"))


@tool
def get_product_recommendations(category_id: int | None = None,
                                limit: int = 5) -> str:
    """按真实付款销量和评价表现推荐商品，可按分类编号筛选。"""
    params = {"limit": max(1, min(limit, 20))}
    if category_id is not None:
        params["categoryId"] = category_id
    return _request("GET", "/api/products/recommendations", params=params)


@tool
def get_categories() -> str:
    """查询启用中的商品分类。"""
    return _request("GET", "/api/categories")


@tool
def get_api_catalog(keyword: str = "") -> str:
    """查询当前角色可用的后端接口能力；只用于选择现有工具，不用于直接拼 URL。"""
    entries = search_api_catalog(_user_role.get(), keyword, limit=8)
    return json.dumps(
        {
            "role": _user_role.get(),
            "endpoints": entries,
            "note": "数据库连接信息不在此目录中，实际访问必须通过白名单业务工具。",
        },
        ensure_ascii=False,
    )


@tool
def web_search(query: str, max_results: int = 5) -> str:
    """搜索公开互联网信息。仅用于外部实时信息，不用于订单、购物车或内部账户数据。"""
    return perform_web_search(query, max_results)


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
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "mark_notification_read", "notification_id": notification_id})
    return "已提出将通知标记为已读，等待用户确认。"


@tool
def add_to_cart(product_id: int, num: int = 1) -> str:
    """提出加入购物车请求，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "add_to_cart", "product_id": product_id, "num": num})
    return "已提出将商品加入购物车，等待用户确认。"


@tool
def cancel_order(order_id: int) -> str:
    """提出取消订单请求，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "cancel_order", "order_id": order_id})
    return "已提出取消订单，等待用户确认。"


@tool
def apply_return(order_item_id: int, reason: str) -> str:
    """提出退货申请，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "apply_return", "order_item_id": order_item_id, "reason": reason})
    return "已提出退款申请，等待用户确认。"


@tool
def cancel_return(return_id: int) -> str:
    """提出撤销退货申请，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "cancel_return", "return_id": return_id})
    return "已提出撤销退款申请，等待用户确认。"


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
def get_seller_products(keyword: str | None = None, status: int | None = None) -> str:
    """查询当前卖家自己的商品，可按关键词和上下架状态筛选。"""
    params = {"page": 1, "size": 20}
    if keyword:
        params["keyword"] = keyword
    if status is not None:
        params["status"] = status
    return _enrich_product_status(
        _request("GET", "/api/seller/products", auth=True, params=params))


@tool
def get_seller_stats(range: str = "30d") -> str:
    """查询当前卖家的收入统计，range 可为 today/7d/30d/all。"""
    return _request("GET", "/api/seller/stats", auth=True, params={"range": range})


@tool
def approve_return(return_id: int, note: str = "") -> str:
    """提出同意退货，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "approve_return", "return_id": return_id, "note": note})
    return "已提出同意退款，等待用户确认。"


@tool
def reject_return(return_id: int, note: str = "") -> str:
    """提出拒绝退货，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "reject_return", "return_id": return_id, "note": note})
    return "已提出拒绝退款，等待用户确认。"


@tool
def ship_order(order_id: int) -> str:
    """提出给已付款订单发货，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "ship_order", "order_id": order_id})
    return "已提出订单发货，等待用户确认。"


@tool
def create_product(name: str, description: str, price: float, stock: int,
                   category_id: int, main_image: str = "") -> str:
    """提出新增商品，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "create_product",
        "name": name,
        "description": description,
        "price": price,
        "stock": stock,
        "category_id": category_id,
        "main_image": main_image,
    })
    return "已提出新增商品，等待用户确认。"


@tool
def update_product_stock(product_id: int, stock: int) -> str:
    """提出修改商品库存，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "update_product_stock",
        "product_id": product_id,
        "stock": stock,
    })
    return "已提出修改商品库存，等待用户确认。"


@tool
def set_product_status(product_id: int, status: int) -> str:
    """提出商品上架或下架，status 为 1 表示上架、0 表示下架。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "set_product_status",
        "product_id": product_id,
        "status": status,
    })
    return "已提出修改商品上下架状态，等待用户确认。"


@tool
def delete_product(product_id: int) -> str:
    """提出删除商品，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "delete_product", "product_id": product_id})
    return "已提出删除商品，等待用户确认。"


@tool
def reply_review(review_id: int, content: str) -> str:
    """提出回复评价，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "reply_review", "review_id": review_id, "content": content})
    return "已提出回复评价，等待用户确认。"


@tool
def get_admin_orders(status: int | None = None,
                     order_no: str | None = None,
                     buyer_id: int | None = None) -> str:
    """管理员查询全部订单，可按状态、订单号和买家 ID 筛选。"""
    params = {"page": 1, "size": 10}
    if status is not None:
        params["status"] = status
    if order_no:
        params["orderNo"] = order_no
    if buyer_id is not None:
        params["buyerId"] = buyer_id
    return _enrich_order_status(_request("GET", "/api/admin/orders", auth=True, params=params))


@tool
def get_admin_order_detail(order_id: int) -> str:
    """管理员查询指定订单详情，需要管理员登录态。"""
    return _enrich_order_status(
        _request("GET", f"/api/admin/orders/{order_id}", auth=True))


@tool
def get_admin_buyers(keyword: str | None = None) -> str:
    """管理员查询买家账号，可按账号、昵称或手机号搜索。"""
    params = {"page": 1, "size": 10}
    if keyword:
        params["keyword"] = keyword
    return _request(
        "GET", "/api/admin/users/buyers", auth=True, params=params)


@tool
def get_admin_sellers(keyword: str | None = None) -> str:
    """管理员查询卖家账号，可按账号、店铺名或手机号搜索。"""
    params = {"page": 1, "size": 10}
    if keyword:
        params["keyword"] = keyword
    return _request(
        "GET", "/api/admin/users/sellers", auth=True, params=params)


@tool
def get_admin_categories(name: str | None = None,
                         status: int | None = None) -> str:
    """管理员查询全部分类，包括已停用分类。"""
    params = {"page": 1, "size": 20}
    if name:
        params["name"] = name
    if status is not None:
        params["status"] = status
    return _enrich_category_status(
        _request("GET", "/api/admin/categories", auth=True, params=params))


@tool
def force_cancel_order(order_id: int) -> str:
    """提出管理员强制关单，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "force_cancel_order", "order_id": order_id})
    return "已提出强制关单，等待用户确认。"


@tool
def delete_buyer(buyer_id: int) -> str:
    """提出删除买家账号，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "delete_buyer", "buyer_id": buyer_id})
    return "已提出删除买家账号，等待用户确认。"


@tool
def delete_seller(seller_id: int) -> str:
    """提出删除卖家账号，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({"type": "delete_seller", "seller_id": seller_id})
    return "已提出删除卖家账号，等待用户确认。"


@tool
def create_category(name: str, parent_id: int = 0, sort_order: int = 0,
                    status: int = 1) -> str:
    """提出新增商品分类，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "create_category",
        "name": name,
        "parent_id": parent_id,
        "sort_order": sort_order,
        "status": status,
    })
    return "已提出新增分类，等待用户确认。"


@tool
def update_category(category_id: int, name: str, parent_id: int = 0,
                    sort_order: int = 0, status: int = 1) -> str:
    """提出修改商品分类，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "update_category",
        "category_id": category_id,
        "name": name,
        "parent_id": parent_id,
        "sort_order": sort_order,
        "status": status,
    })
    return "已提出修改分类，等待用户确认。"


@tool
def set_category_status(category_id: int, status: int) -> str:
    """提出启用或停用分类，status 为 1 表示启用、0 表示停用。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "set_category_status",
        "category_id": category_id,
        "status": status,
    })
    return "已提出修改分类状态，等待用户确认。"


@tool
def delete_category(category_id: int) -> str:
    """提出删除商品分类，必须在用户确认后才会真正执行。"""
    actions = _proposed_actions.get()
    if actions is None:
        return "错误：当前请求无法收集待确认操作"
    actions.append({
        "type": "delete_category",
        "category_id": category_id,
    })
    return "已提出删除分类，等待用户确认。"


def build_tools(role: str | None = None):
    """按角色注册 Agent 可调用的工具，公共工具对所有角色开放。"""
    common = [
        search_products,
        get_product_detail,
        get_product_recommendations,
        get_categories,
        get_api_catalog,
        web_search,
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
        get_seller_products,
        get_seller_stats,
        ship_order,
        approve_return,
        reject_return,
        reply_review,
        create_product,
        update_product_stock,
        set_product_status,
        delete_product,
    ]
    admin = [
        get_admin_orders,
        get_admin_order_detail,
        get_admin_buyers,
        get_admin_sellers,
        get_admin_categories,
        force_cancel_order,
        delete_buyer,
        delete_seller,
        create_category,
        update_category,
        set_category_status,
        delete_category,
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
    if action_type == "ship_order":
        return _request(
            "PUT", f"/api/seller/orders/{action['order_id']}/ship", auth=True)
    if action_type == "create_product":
        return _request(
            "POST",
            "/api/products",
            auth=True,
            json_body={
                "name": action["name"],
                "description": action.get("description", ""),
                "price": action["price"],
                "stock": action["stock"],
                "categoryId": action["category_id"],
                "mainImage": action.get("main_image") or None,
                "status": 1,
            },
        )
    if action_type == "update_product_stock":
        raw = _request("GET", f"/api/products/{action['product_id']}")
        body = json.loads(raw)
        product = body.get("data") or {}
        return _request(
            "PUT",
            f"/api/products/{action['product_id']}",
            auth=True,
            json_body={
                "name": product.get("name"),
                "description": product.get("description"),
                "price": product.get("price"),
                "stock": action["stock"],
                "categoryId": product.get("categoryId"),
                "mainImage": product.get("mainImage"),
                "status": product.get("status"),
            },
        )
    if action_type == "set_product_status":
        return _request(
            "PUT",
            f"/api/products/{action['product_id']}/status/{action['status']}",
            auth=True,
        )
    if action_type == "delete_product":
        return _request(
            "DELETE", f"/api/products/{action['product_id']}", auth=True)
    if action_type == "reply_review":
        return _request(
            "PUT",
            f"/api/seller/reviews/{action['review_id']}/reply",
            auth=True,
            json_body={"content": action["content"]},
        )
    if action_type == "force_cancel_order":
        return _request("PUT", f"/api/admin/orders/{action['order_id']}/force-cancel", auth=True)
    if action_type == "delete_buyer":
        return _request(
            "DELETE", f"/api/admin/users/buyers/{action['buyer_id']}", auth=True)
    if action_type == "delete_seller":
        return _request(
            "DELETE", f"/api/admin/users/sellers/{action['seller_id']}", auth=True)
    if action_type == "create_category":
        return _request(
            "POST",
            "/api/categories",
            auth=True,
            json_body={
                "name": action["name"],
                "parentId": action.get("parent_id", 0),
                "sortOrder": action.get("sort_order", 0),
                "status": action.get("status", 1),
            },
        )
    if action_type == "update_category":
        return _request(
            "PUT",
            f"/api/categories/{action['category_id']}",
            auth=True,
            json_body={
                "id": action["category_id"],
                "name": action["name"],
                "parentId": action.get("parent_id", 0),
                "sortOrder": action.get("sort_order", 0),
                "status": action.get("status", 1),
            },
        )
    if action_type == "set_category_status":
        return _request(
            "PUT",
            f"/api/categories/{action['category_id']}/status/{action['status']}",
            auth=True,
        )
    if action_type == "delete_category":
        return _request(
            "DELETE", f"/api/categories/{action['category_id']}", auth=True)
    if action_type == "mark_notification_read":
        return _request(
            "PUT", f"/api/notifications/{action['notification_id']}/read", auth=True)
    return "错误：不支持该操作"

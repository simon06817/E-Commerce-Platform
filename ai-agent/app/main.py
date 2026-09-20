import asyncio
import json
import logging
import re
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any
from urllib.parse import urlencode

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app import metrics
from app.agent import remember_exchange, run_agent
from app.api_catalog import api_catalog_facts
from app.config import config
from app.rag import search_knowledge, search_products
from app.tools import (
    begin_action_collection,
    end_action_collection,
    execute_proposed_action,
    proposed_actions,
    reset_user_role,
    reset_user_token,
    set_user_role,
    set_user_token,
)
from app.web_search import perform_web_search

logger = logging.getLogger("uvicorn.error")

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
_pending_actions: dict[str, dict[str, Any]] = {}
_memory_sizes: dict[str, int] = {}

_CONFIRM_WORDS = {"确认", "确定", "是", "好的", "好", "yes", "y", "ok"}
_CANCEL_WORDS = {"取消", "不用了", "不要了", "no", "n"}
_PENDING_ACTION_TTL_SECONDS = 300
_QUANTITY_HINT = re.compile(
    r"(?:数量|买|要|加)\s*\d+"
    r"|(?:\d+|[一二两三四五六七八九十]+)\s*(?:件|个|台|盒|瓶|份|套)"
)
_CART_ADD_PATTERN = re.compile(
    r"(?:把|将)\s*(.+?)\s*"
    r"(?:添加到|添加|加入|加到|放到|加进|放进|搭配)\s*"
    r"(?:我的)?购物车"
)
_CN_NUMBERS = {
    "一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5,
    "六": 6, "七": 7, "八": 8, "九": 9, "十": 10,
}
_CATEGORY_NAMES = ("电子产品", "图书", "服装", "玩具", "食品")
_ACCOUNT_QUERY_KEYWORDS = (
    "订单", "购物车", "退货", "退款", "通知", "统计", "收入",
    "发货", "商品管理", "我的商品", "库存", "上架", "下架",
    "我叫什么", "我是谁", "我的名字", "用户名", "个人信息", "个人资料", "账号",
)
_CATALOG_QUERY_KEYWORDS = (
    "商品", "产品", "推荐", "搜索", "查找", "商品详情", "价格", "多少钱",
    "分类", "评价", "描述", "比较", "挑选", "怎么样",
)
_PRODUCT_ACTION_KEYWORDS = (
    "加入购物车", "添加到购物车", "加到购物车", "加购", "下单",
)
_SELLER_PRODUCT_KEYWORDS = (
    "商品管理", "我的商品", "商品库存", "库存", "上架", "下架",
)
_IDENTITY_QUESTION_KEYWORDS = (
    "我叫什么", "我是谁", "我的名字", "用户名",
    "个人信息", "个人资料", "账号",
)
_DEEP_PRODUCT_KEYWORDS = (
    "推荐", "搜索", "查找", "商品详情", "价格", "多少钱", "分类",
    "评价", "描述", "比较", "挑选", "怎么样",
)
_WEB_SEARCH_KEYWORDS = (
    "联网", "上网", "网上查", "搜索一下", "搜一下", "查一下网上",
    "最新消息", "实时信息", "新闻", "行业行情", "外部资料",
)
_API_CATALOG_KEYWORDS = (
    "接口", "api", "后端", "调用", "权限", "能做什么", "有哪些功能",
)
_ORDER_STATUS_TEXT = {
    0: "待付款",
    1: "待发货",
    2: "待收货",
    3: "已完成",
    4: "已取消",
}


class ChatRequest(BaseModel):
    """聊天请求体。"""

    message: str


def _attach_product_status_text(product: dict) -> None:
    """Annotate product records so status 1 is never read as an order status."""
    if isinstance(product, dict) and product.get("status") is not None:
        product["statusText"] = "上架" if int(product["status"]) == 1 else "下架"


def _java_get(path: str, token: str | None = None) -> Any:
    """调用 Java 后端并用统一 Result 结构解包。"""
    headers = {"Authorization": token} if token else {}
    url = config.java_api_base_url.rstrip("/") + path
    response = httpx.get(url, headers=headers, timeout=20)
    response.raise_for_status()
    body = response.json()
    if body.get("code") != 200:
        raise HTTPException(status_code=502, detail=body.get("message", "Java 服务调用失败"))
    return body.get("data")


def _current_user(authorization: str | None) -> dict:
    """通过 Java /api/auth/me 校验 JWT 并获取角色与用户 id。"""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="缺少登录令牌")
    try:
        return _java_get("/api/auth/me", authorization)
    except Exception as exc:
        raise HTTPException(status_code=401, detail="登录状态已失效") from exc


def _review_facts(product_id: int) -> list[str]:
    """读取商品平均分和前几条评价，作为回答前的事实来源。"""
    facts = []
    summary = _java_get(f"/api/products/{product_id}/reviews/summary")
    if summary:
        facts.append(
            f"商品 {product_id} 评价摘要："
            f"平均分={summary.get('averageRating')}，评价数={summary.get('reviewCount')}"
        )
    reviews = _java_get(f"/api/products/{product_id}/reviews?page=1&size=3")
    for review in (reviews or {}).get("records", []):
        facts.append(
            f"商品 {product_id} 评价：评分={review.get('rating')}，"
            f"内容={review.get('content')}，买家={review.get('buyerName')}"
        )
    return facts


def _account_facts(question: str, authorization: str, role: str | None) -> list[str]:
    """Accounts questions are prefetched deterministically with the user's JWT."""
    q = question.lower()
    facts = []
    try:
        if any(keyword in q for keyword in (
                "我叫什么", "我是谁", "我的名字", "用户名",
                "个人信息", "个人资料", "账号")):
            profile = _java_get("/api/profile", authorization)
            facts.append("当前用户资料：" + json.dumps(profile, ensure_ascii=False))
        if any(keyword in q for keyword in ("通知", "notification")):
            notifications = _java_get("/api/notifications/my", authorization)
            facts.append("通知：" + json.dumps(notifications, ensure_ascii=False))
        if any(keyword in q for keyword in ("订单", "order")):
            if role == "ADMIN":
                orders = _java_get("/api/admin/orders", authorization)
            elif role == "SELLER":
                orders = _java_get("/api/seller/orders", authorization)
            else:
                orders = _java_get("/api/orders", authorization)
            counts = {}
            for order in (orders or {}).get("records", []):
                status = order.get("status")
                counts[status] = counts.get(status, 0) + 1
                order["statusText"] = _ORDER_STATUS_TEXT.get(status, "未知状态")
            facts.append("订单：" + json.dumps(orders, ensure_ascii=False))
            facts.append("订单状态统计：" + json.dumps(counts, ensure_ascii=False))
        if role == "ADMIN" and any(keyword in q for keyword in ("买家", "buyer")):
            buyers = _java_get(
                "/api/admin/users/buyers?page=1&size=10", authorization)
            facts.append("买家账号：" + json.dumps(buyers, ensure_ascii=False))
        if role == "ADMIN" and any(keyword in q for keyword in ("卖家", "seller")):
            sellers = _java_get(
                "/api/admin/users/sellers?page=1&size=10", authorization)
            facts.append("卖家账号：" + json.dumps(sellers, ensure_ascii=False))
        if role == "ADMIN" and "分类" in q:
            categories = _java_get(
                "/api/admin/categories?page=1&size=20", authorization)
            for category in (categories or {}).get("records", []):
                category["statusText"] = (
                    "启用" if category.get("status") == 1 else "停用"
                )
            facts.append("商品分类：" + json.dumps(categories, ensure_ascii=False))
        if any(keyword in q for keyword in ("购物车", "cart")):
            cart = _java_get("/api/carts", authorization)
            facts.append("购物车：" + json.dumps(cart, ensure_ascii=False))
        if any(keyword in q for keyword in ("退货", "return")):
            path = "/api/seller/returns" if role == "SELLER" else "/api/returns/my"
            returns = _java_get(path, authorization)
            facts.append("退款申请：" + json.dumps(returns, ensure_ascii=False))
        if role == "SELLER" and any(
                keyword in q for keyword in ("统计", "收入", "statistics", "revenue")):
            stats_range = _seller_stats_range(q)
            stats = _java_get(
                f"/api/seller/stats?range={stats_range}", authorization)
            facts.append(
                f"卖家统计（范围={stats_range}）："
                + json.dumps(stats, ensure_ascii=False))
        if role == "SELLER" and any(
                keyword in q for keyword in _SELLER_PRODUCT_KEYWORDS):
            products = _java_get(
                "/api/seller/products?page=1&size=20", authorization)
            for product in (products or {}).get("records", []):
                _attach_product_status_text(product)
            facts.append("我的商品：" + json.dumps(products, ensure_ascii=False))
    except Exception as exc:
        facts.append(f"账户信息工具调用失败：{exc}")
    return facts


def _seller_stats_range(question: str) -> str:
    """Map natural-language time expressions to the stats API range."""
    if "今天" in question or "今日" in question:
        return "today"
    if "7天" in question or "七天" in question or "本周" in question:
        return "7d"
    return "30d"


def _is_identity_question(question: str) -> bool:
    return any(keyword in question for keyword in _IDENTITY_QUESTION_KEYWORDS)


def _identity_answer(profile: dict, role: str | None) -> str:
    username = profile.get("username") or "未知账号"
    nickname = profile.get("nickname") or "未设置"
    if role == "SELLER":
        shop_name = profile.get("shopName") or "未设置"
        return (
            "我是平台的 AI 助手，不是您本人。"
            f"您当前登录的卖家账号是 {username}，"
            f"店铺名称是 {shop_name}，账号昵称是 {nickname}。"
        )
    return (
        "我是平台的 AI 助手，不是您本人。"
        f"您当前登录的买家账号是 {username}，"
        f"账号昵称是 {nickname}。"
    )


def _needs_product_retrieval(question: str, role: str | None) -> bool:
    """Decide whether product vectors add information beyond account tools."""
    q = question.lower()
    if (
        any(keyword in q for keyword in _PRODUCT_ACTION_KEYWORDS)
        or (
            "购物车" in q
            and any(keyword in q for keyword in ("加入", "添加", "加到", "放到"))
        )
    ):
        return True
    if role == "SELLER" and any(
            keyword in q for keyword in _SELLER_PRODUCT_KEYWORDS):
        return any(keyword in q for keyword in _DEEP_PRODUCT_KEYWORDS)
    if any(keyword in q for keyword in _ACCOUNT_QUERY_KEYWORDS):
        return any(keyword in q for keyword in _CATALOG_QUERY_KEYWORDS)
    return any(keyword in q for keyword in _CATALOG_QUERY_KEYWORDS)


def _product_facts(item: dict) -> list[str]:
    """Read realtime product and review facts for one retrieved product."""
    product_id = item.get("product_id")
    if product_id is None:
        return []
    facts = [f"商品描述：{item['content']}"]
    try:
        detail = _java_get(f"/api/products/{product_id}")
        if detail:
            _attach_product_status_text(detail)
            facts.append(
                f"商品实时数据：编号={detail.get('id')}，名称={detail.get('name')}，"
                f"价格={detail.get('price')}，库存={detail.get('stock')}，"
                f"上下架状态={detail.get('statusText')}"
            )
        facts.extend(_review_facts(int(product_id)))
    except Exception as exc:
        facts.append(f"商品 {product_id} 工具调用失败：{exc}")
    return facts


def _web_search_facts(question: str) -> list[str]:
    """Fetch Tavily evidence only for questions that explicitly need the web."""
    metrics.increment("agent.web_search.calls")
    with metrics.timer("agent.web_search_ms"):
        raw = perform_web_search(question, config.tavily_max_results)
    try:
        body = json.loads(raw)
    except Exception:
        return [f"外部联网搜索返回：{raw}"]
    if body.get("error"):
        metrics.increment("agent.web_search.failures")
        return [f"外部联网搜索不可用：{body['error']}"]

    facts = []
    answer = body.get("answer")
    if answer:
        facts.append(f"外部联网搜索摘要：{answer}")
    for item in body.get("results", [])[:3]:
        content = str(item.get("content") or "").strip()
        facts.append(
            "外部联网搜索结果："
            f"标题={item.get('title')}；来源={item.get('url')}；"
            f"内容={content[:500]}"
        )
    return facts


def _needs_web_search(question: str) -> bool:
    return any(keyword in question for keyword in _WEB_SEARCH_KEYWORDS)


def _needs_api_catalog(question: str) -> bool:
    return any(keyword in question.lower() for keyword in _API_CATALOG_KEYWORDS)


def _retrieve_facts(question: str, authorization: str,
                    role: str | None) -> tuple[list[str], list[dict]]:
    """执行确定性前置检索：静态知识 + 商品描述 + 实时信息 + 评价。"""
    metrics.increment("agent.retrieval.calls")
    with metrics.timer("agent.retrieval_ms"):
        with ThreadPoolExecutor(max_workers=4) as executor:
            knowledge_future = executor.submit(search_knowledge, question, 3)
            product_future = (
                executor.submit(search_products, question, 3)
                if _needs_product_retrieval(question, role)
                else None
            )
            web_future = (
                executor.submit(_web_search_facts, question)
                if _needs_web_search(question)
                else None
            )
            account_future = executor.submit(
                _account_facts, question, authorization, role)

            knowledge = knowledge_future.result()
            product_hits = product_future.result() if product_future else []
            web_facts = web_future.result() if web_future else []
            facts = account_future.result()

    metrics.increment("agent.retrieval.knowledge_hits", len(knowledge))
    metrics.increment("agent.retrieval.product_hits", len(product_hits))
    if not knowledge and not product_hits and not web_facts:
        metrics.increment("agent.retrieval.empty")
    if _needs_api_catalog(question):
        facts.extend(api_catalog_facts(question, role))
    facts.extend(web_facts)
    for item in knowledge:
        facts.append(f"知识库：{item['content']}")

    products = [
        item for item in product_hits[:3]
        if item.get("product_id") is not None
    ]
    if products:
        with ThreadPoolExecutor(max_workers=min(3, len(products))) as executor:
            for product_fact_list in executor.map(_product_facts, products):
                facts.extend(product_fact_list)
    return facts, products


def _is_confirm(text: str) -> bool:
    return text.strip().lower() in _CONFIRM_WORDS


def _is_cancel(text: str) -> bool:
    return text.strip().lower() in _CANCEL_WORDS


def _pending_actions_for(thread_id: str) -> list[dict[str, Any]]:
    """Return unexpired pending actions; stale confirmation never blocks a new request."""
    entry = _pending_actions.get(thread_id)
    if not entry:
        return []
    if time.monotonic() >= entry.get("expires_at", 0):
        _pending_actions.pop(thread_id, None)
        return []
    return entry.get("actions", [])


def _set_pending_actions(thread_id: str, actions: list[dict[str, Any]]) -> None:
    _pending_actions[thread_id] = {
        "actions": actions,
        "expires_at": time.monotonic() + _PENDING_ACTION_TTL_SECONDS,
    }


def _normalize_actions(question: str, actions: list[dict[str, Any]]) -> None:
    """Do not let the small model invent a quantity when the user omitted one."""
    if _QUANTITY_HINT.search(question):
        return
    for action in actions:
        if action.get("type") == "add_to_cart":
            action["num"] = 1


def _cart_quantity(question: str) -> int:
    """Read an explicit quantity from an add-to-cart sentence, defaulting to one."""
    match = re.search(
        r"(\d+|[一二两三四五六七八九十]+)\s*(?:件|个|台|盒|瓶|份|套)",
        question,
    )
    if not match:
        match = re.search(r"(?:数量|买|要|加)\s*(\d+)", question)
    if not match:
        return 1
    value = match.group(1)
    if value.isdigit():
        return max(1, int(value))
    return _CN_NUMBERS.get(value[0], 1)


def _cart_product_query(question: str) -> str | None:
    match = _CART_ADD_PATTERN.search(question)
    if not match:
        return None
    product_text = match.group(1)
    product_text = re.sub(
        r"(?:数量|买|要|加)\s*\d+"
        r"|(?:\d+|[一二两三四五六七八九十]+)\s*(?:件|个|台|盒|瓶|份|套)",
        " ",
        product_text,
    )
    return re.sub(r"\s+", " ", product_text).strip(" 的，,。.")


def _resolve_cart_product(question: str,
                          authorization: str) -> dict[str, Any] | None:
    """Resolve a unique product for a direct add-to-cart request."""
    product_text = _cart_product_query(question)
    if not product_text:
        return None

    params = {"page": 1, "size": 10, "status": 1}
    id_match = re.fullmatch(r"(?:商品|产品)\s*#?(\d+)", product_text)
    if id_match:
        product = _java_get(f"/api/products/{id_match.group(1)}")
        records = [product] if product else []
    else:
        keyword = product_text
        shop_name = ""
        if "的" in product_text:
            left, right = (part.strip() for part in product_text.split("的", 1))
            if left and right:
                shop_name = left
                keyword = right
        params["keyword"] = keyword
        if shop_name:
            params["shopName"] = shop_name
        data = _java_get("/api/products?" + urlencode(params))
        records = data.get("records", []) if data else []
        if not records and shop_name:
            params.pop("shopName", None)
            params["keyword"] = product_text
            data = _java_get("/api/products?" + urlencode(params))
            records = data.get("records", []) if data else []

    if len(records) != 1:
        return None
    product = records[0]
    return {
        "product_id": product.get("id"),
        "name": product.get("name"),
        "seller_name": product.get("sellerName"),
        "quantity": _cart_quantity(question),
    }


def _is_cart_count_question(question: str) -> bool:
    return "购物车" in question and any(
        keyword in question
        for keyword in ("几件", "多少件", "数量", "总数", "几样", "多少样", "多少商品")
    )


def _cart_count_answer(authorization: str) -> tuple[str, list[dict[str, Any]]]:
    cart = _java_get("/api/carts", authorization) or []
    if not cart:
        return "您的购物车目前是空的。", []
    total = sum(int(item.get("num") or 0) for item in cart)
    products = [
        {"product_id": item.get("productId")}
        for item in cart
        if item.get("productId") is not None
    ]
    return (
        f"您的购物车目前有 {total} 件商品，共 {len(cart)} 种。",
        products,
    )


def _is_recommendation_question(question: str) -> bool:
    if "推荐" not in question:
        return False
    return (
        any(category in question for category in _CATEGORY_NAMES)
        or ("销量" in question and "评价" in question)
    )


def _recommendation_category_id(question: str) -> int | None:
    categories = _java_get("/api/categories") or []
    for category in categories:
        name = category.get("name")
        if name and name in question:
            return category.get("id")
    return None


def _recommendation_answer(question: str) -> tuple[str, list[dict[str, Any]]]:
    category_id = _recommendation_category_id(question)
    params = {"limit": 5}
    if category_id is not None:
        params["categoryId"] = category_id
    recommendations = _java_get(
        "/api/products/recommendations?" + urlencode(params)
    ) or []
    if not recommendations:
        return "当前没有符合条件的在售商品可以推荐。", []

    has_metric_data = any(
        int(item.get("salesQuantity") or 0) > 0
        or int(item.get("reviewCount") or 0) > 0
        for item in recommendations
    )
    scope = next(
        (name for name in _CATEGORY_NAMES if name in question),
        "商品",
    )
    lines = []
    if has_metric_data:
        lines.append(f"根据真实付款销量和评价表现，为您推荐这些{scope}：")
    else:
        lines.append(f"当前销量和评价数据还较少，先为您列出这些在售{scope}：")

    products = []
    for index, item in enumerate(recommendations, start=1):
        sales = int(item.get("salesQuantity") or 0)
        review_count = int(item.get("reviewCount") or 0)
        average_rating = float(item.get("averageRating") or 0)
        rating_text = (
            f"{average_rating:.1f}/5（{review_count}条评价）"
            if review_count > 0 else "暂无评价"
        )
        price = float(item.get("price") or 0)
        lines.append(
            f"{index}. {item.get('name')}，¥{price:.2f}，"
            f"销量 {sales} 件，评分 {rating_text}，"
            f"店铺 {item.get('sellerName') or '未知店铺'}"
        )
        products.append({
            "product_id": item.get("id"),
            "name": item.get("name"),
        })
    return "\n".join(lines), products


def _is_admin_read_question(question: str) -> bool:
    return any(keyword in question for keyword in (
        "查看", "查询", "列出", "有哪些", "最近", "详情", "多少个"
    ))


def _admin_order_status_filter(question: str) -> int | None:
    for text, status in (
        ("待付款", 0),
        ("待发货", 1),
        ("待收货", 2),
        ("已完成", 3),
        ("已取消", 4),
    ):
        if text in question:
            return status
    return None


def _admin_read_answer(question: str,
                       authorization: str) -> tuple[str, list[dict[str, Any]]] | None:
    """Return deterministic admin read answers for common management queries."""
    if not _is_admin_read_question(question):
        return None
    if "订单" in question:
        status = _admin_order_status_filter(question)
        params = {"page": 1, "size": 10}
        if status is not None:
            params["status"] = status
        data = _java_get(
            "/api/admin/orders?" + urlencode(params), authorization
        ) or {}
        records = data.get("records", [])
        if not records:
            return "没有查询到符合条件的订单。", []
        lines = ["查询到以下订单："]
        for order in records:
            status_text = _ORDER_STATUS_TEXT.get(
                int(order.get("status") or 0), "未知状态"
            )
            lines.append(
                f"- 订单 {order.get('orderNo')}，买家 ID {order.get('buyerId')}，"
                f"金额 ¥{order.get('totalAmount')}，状态 {status_text}"
            )
        return "\n".join(lines), []

    if "买家" in question:
        data = _java_get(
            "/api/admin/users/buyers?page=1&size=10", authorization
        ) or {}
        records = data.get("records", [])
        if not records:
            return "没有查询到买家账号。", []
        lines = ["查询到以下买家账号："]
        for buyer in records:
            lines.append(
                f"- ID {buyer.get('id')}，账号 {buyer.get('username')}，"
                f"昵称 {buyer.get('nickname') or '未设置'}，"
                f"手机号 {buyer.get('phone') or '未设置'}"
            )
        return "\n".join(lines), []

    if "卖家" in question:
        data = _java_get(
            "/api/admin/users/sellers?page=1&size=10", authorization
        ) or {}
        records = data.get("records", [])
        if not records:
            return "没有查询到卖家账号。", []
        lines = ["查询到以下卖家账号："]
        for seller in records:
            lines.append(
                f"- ID {seller.get('id')}，账号 {seller.get('username')}，"
                f"店铺 {seller.get('shopName') or '未设置'}，"
                f"手机号 {seller.get('phone') or '未设置'}"
            )
        return "\n".join(lines), []

    if "分类" in question:
        data = _java_get(
            "/api/admin/categories?page=1&size=20", authorization
        ) or {}
        records = data.get("records", [])
        if not records:
            return "没有查询到商品分类。", []
        lines = ["查询到以下商品分类："]
        for category in records:
            status_text = "启用" if category.get("status") == 1 else "停用"
            lines.append(
                f"- ID {category.get('id')}，名称 {category.get('name')}，"
                f"排序 {category.get('sortOrder')}，状态 {status_text}"
            )
        return "\n".join(lines), []

    return None


def _admin_direct_action(question: str) -> dict[str, Any] | None:
    """Resolve explicit high-risk admin commands without model guessing."""
    match = re.search(r"强制(?:关闭|取消)订单\s*#?(\d+)", question)
    if match:
        return {"type": "force_cancel_order", "order_id": int(match.group(1))}

    match = re.search(r"删除买家\s*#?(\d+)", question)
    if match:
        return {"type": "delete_buyer", "buyer_id": int(match.group(1))}

    match = re.search(r"删除卖家\s*#?(\d+)", question)
    if match:
        return {"type": "delete_seller", "seller_id": int(match.group(1))}

    match = re.search(r"删除分类\s*#?(\d+)", question)
    if match:
        return {"type": "delete_category", "category_id": int(match.group(1))}

    match = re.search(r"(启用|停用)分类\s*#?(\d+)", question)
    if match:
        return {
            "type": "set_category_status",
            "category_id": int(match.group(2)),
            "status": 1 if match.group(1) == "启用" else 0,
        }
    return None


def _confirmation_text(action: dict) -> str:
    action_type = action.get("type")
    if action_type == "add_to_cart":
        product_text = (
            f"商品“{action.get('product_name')}”"
            if action.get("product_name")
            else f"商品 {action.get('product_id')}"
        )
        return (
            f"确认要把{product_text}加入购物车"
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
    if action_type == "delete_buyer":
        return f"确认删除买家账号 {action.get('buyer_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "delete_seller":
        return f"确认删除卖家账号 {action.get('seller_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "create_category":
        return (
            f"确认新增分类“{action.get('name')}”吗？"
            f"上级分类 {action.get('parent_id', 0)}，排序 {action.get('sort_order', 0)}。"
            "回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "update_category":
        return (
            f"确认修改分类 {action.get('category_id')} 为“{action.get('name')}”吗？"
            "回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "set_category_status":
        status_text = "启用" if action.get("status") == 1 else "停用"
        return (
            f"确认{status_text}分类 {action.get('category_id')} 吗？"
            "回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "delete_category":
        return f"确认删除分类 {action.get('category_id')} 吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "mark_notification_read":
        return f"确认把通知 {action.get('notification_id')} 标记为已读吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "ship_order":
        return f"确认给订单 {action.get('order_id')} 发货吗？回复“确认”执行，回复“取消”放弃。"
    if action_type == "create_product":
        return (
            f"确认新增商品“{action.get('name')}”吗？"
            f"价格 {action.get('price')}，库存 {action.get('stock')}，"
            f"分类编号 {action.get('category_id')}。回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "update_product_stock":
        return (
            f"确认把商品 {action.get('product_id')} 的库存改为 {action.get('stock')} 吗？"
            "回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "set_product_status":
        status_text = "上架" if action.get("status") == 1 else "下架"
        return (
            f"确认把商品 {action.get('product_id')} {status_text}吗？"
            "回复“确认”执行，回复“取消”放弃。"
        )
    if action_type == "delete_product":
        return f"确认删除商品 {action.get('product_id')} 吗？回复“确认”执行，回复“取消”放弃。"
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
            if action.get("type") == "delete_buyer":
                return "买家账号已删除。"
            if action.get("type") == "delete_seller":
                return "卖家账号已删除。"
            if action.get("type") == "create_category":
                return "商品分类已创建。"
            if action.get("type") == "update_category":
                return "商品分类已更新。"
            if action.get("type") == "set_category_status":
                return "商品分类状态已更新。"
            if action.get("type") == "delete_category":
                return "商品分类已删除。"
            if action.get("type") == "mark_notification_read":
                return "通知已标记为已读。"
            if action.get("type") == "ship_order":
                return "订单已发货。"
            if action.get("type") == "create_product":
                return "商品已创建。"
            if action.get("type") == "update_product_stock":
                return "商品库存已更新。"
            if action.get("type") == "set_product_status":
                return "商品上下架状态已更新。"
            if action.get("type") == "delete_product":
                return "商品已删除。"
        return f"操作失败：{body.get('message', '未知错误')}"
    except Exception:
        return raw


@app.get("/health")
def health():
    """检查 Agent 及其关键依赖，避免只检查 HTTP 进程本身。"""
    dependencies = {}
    checks = (
        ("java", config.java_api_base_url.rstrip("/") + "/actuator/health"),
        ("ollama", config.ollama_base_url.rstrip("/") + "/api/tags"),
    )
    for name, url in checks:
        try:
            response = httpx.get(url, timeout=3)
            dependencies[name] = "up" if response.is_success else f"down:{response.status_code}"
        except Exception:
            dependencies[name] = "down"

    dependencies["chroma"] = "up" if Path(config.vector_dir).is_dir() else "down"
    status = "ok" if all(value == "up" for value in dependencies.values()) else "degraded"
    return {"status": status, "dependencies": dependencies}


@app.get("/metrics")
def metrics_snapshot():
    """Expose lightweight in-process Agent counters and latency summaries."""
    return metrics.snapshot()


@app.post("/chat")
async def chat(request: ChatRequest, authorization: str | None = Header(default=None)):
    """兼容非流式调用。"""
    metrics.increment("agent.requests.non_stream")
    with metrics.timer("agent.request_ms"):
        return await asyncio.to_thread(_handle_chat, request, authorization)


def _handle_chat(request: ChatRequest, authorization: str | None) -> dict:
    """校验用户、处理待确认操作、先检索真实信息，再调用 LangGraph。"""
    user = _current_user(authorization)
    thread_id = f"{user.get('role')}:{user.get('id')}"
    question = request.message.strip()

    pending = _pending_actions_for(thread_id)
    if pending:
        if _is_cancel(question):
            _pending_actions.pop(thread_id, None)
            metrics.increment("agent.actions.cancelled")
            answer = "已取消该操作。"
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            return _response(answer, user, [], memory_size)
        if not _is_confirm(question):
            # A new request supersedes an unresolved confirmation. This avoids
            # answering a fresh cart/product question with an old order action.
            logger.info(
                "replacing unresolved agent action for thread %s: %s",
                thread_id,
                pending[0].get("type"),
            )
            _pending_actions.pop(thread_id, None)
        else:
            action = pending.pop(0)
            if pending:
                _set_pending_actions(thread_id, pending)
            else:
                _pending_actions.pop(thread_id, None)
            token_ctx = set_user_token(authorization)
            try:
                with metrics.timer("agent.action_execution_ms"):
                    raw = execute_proposed_action(action)
                answer = _format_action_result(action, raw)
                metrics.increment("agent.actions.confirmed")
            except Exception:
                metrics.increment("agent.actions.failed")
                logger.exception("confirmed agent action failed: %s", action)
                answer = "操作执行失败，请稍后重试。"
            finally:
                reset_user_token(token_ctx)
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            return _response(answer, user, [], memory_size)

    if _is_identity_question(question):
        try:
            profile = _java_get("/api/profile", authorization)
            return _response(
                _identity_answer(profile or {}, user.get("role")),
                user,
                [],
                _memory_sizes.get(thread_id, 0),
            )
        except Exception:
            logger.exception("identity profile lookup failed")

    if user.get("role") == "ADMIN":
        admin_action = _admin_direct_action(question)
        if admin_action:
            _set_pending_actions(thread_id, [admin_action])
            answer = _confirmation_text(admin_action)
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            return _response(answer, user, [], memory_size, True)

        try:
            admin_answer = _admin_read_answer(question, authorization)
            if admin_answer:
                answer, products = admin_answer
                memory_size = remember_exchange(thread_id, question, answer)
                _memory_sizes[thread_id] = memory_size
                return _response(answer, user, products, memory_size)
        except Exception:
            logger.exception("admin deterministic lookup failed")

    if _is_cart_count_question(question):
        try:
            answer, products = _cart_count_answer(authorization)
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            return _response(answer, user, products, memory_size)
        except Exception:
            logger.exception("cart count lookup failed")

    if _is_recommendation_question(question):
        try:
            answer, products = _recommendation_answer(question)
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            return _response(answer, user, products, memory_size)
        except Exception:
            logger.exception("recommendation lookup failed")

    if _cart_product_query(question):
        try:
            cart_product = _resolve_cart_product(question, authorization)
        except Exception:
            logger.exception("direct cart product resolution failed")
            cart_product = None
        if cart_product and cart_product.get("product_id") is not None:
            action = {
                "type": "add_to_cart",
                "product_id": cart_product["product_id"],
                "product_name": cart_product.get("name"),
                "num": cart_product["quantity"],
            }
            _set_pending_actions(thread_id, [action])
            answer = _confirmation_text(action)
            memory_size = remember_exchange(thread_id, question, answer)
            _memory_sizes[thread_id] = memory_size
            products = [{
                "product_id": cart_product["product_id"],
                "name": cart_product.get("name"),
            }]
            return _response(answer, user, products, memory_size, True)

    try:
        facts, products = _retrieve_facts(question, authorization, user.get("role"))
    except Exception:
        logger.exception("agent fact retrieval failed")
        facts, products = [], []

    token_ctx = set_user_token(authorization)
    role_ctx = set_user_role(user.get("role"))
    action_ctx = begin_action_collection()
    actions: list[dict[str, Any]] = []
    result: dict[str, Any] = {}
    try:
        metrics.increment("agent.llm.calls")
        with metrics.timer("agent.llm_ms"):
            result = run_agent(thread_id, question, facts, user.get("role"))
        actions = proposed_actions()
    except Exception:
        metrics.increment("agent.llm.failures")
        logger.exception("agent execution failed for thread %s", thread_id)
        memory_size = _memory_sizes.get(thread_id, 0)
        return _response(
            "AI 助手暂时无法完成这次请求，请稍后重试。",
            user,
            products,
            memory_size,
        )
    finally:
        end_action_collection(action_ctx)
        reset_user_role(role_ctx)
        reset_user_token(token_ctx)

    _normalize_actions(question, actions)
    memory_size = int(result.get("memory_size", 0))
    _memory_sizes[thread_id] = memory_size
    if actions:
        _set_pending_actions(thread_id, actions)
        metrics.increment("agent.actions.proposed")
        answer = _confirmation_text(actions[0])
    else:
        answer = result.get("answer", "AI 助手暂时无法生成回答")

    return _response(answer, user, products, memory_size, bool(actions))


def _stream_line(payload: dict) -> str:
    return json.dumps(payload, ensure_ascii=False) + "\n"


async def _chat_event_stream(request: ChatRequest, authorization: str | None):
    metrics.increment("agent.requests.stream")
    request_started = time.perf_counter()
    yield _stream_line({"type": "status", "text": "正在读取账户和商品数据…"})
    try:
        result = await asyncio.to_thread(_handle_chat, request, authorization)
    except HTTPException as exc:
        yield _stream_line({
            "type": "error",
            "message": str(exc.detail),
        })
        return
    except Exception:
        logger.exception("streaming agent execution failed")
        yield _stream_line({
            "type": "error",
            "message": "AI 助手暂时无法完成这次请求，请稍后重试。",
        })
        return

    answer = result.get("answer", "")
    for start in range(0, len(answer), 4):
        yield _stream_line({"type": "delta", "text": answer[start:start + 4]})
        await asyncio.sleep(0.012)
    yield _stream_line({"type": "done", "data": result})
    metrics.observe_ms(
        "agent.request_ms",
        (time.perf_counter() - request_started) * 1000,
    )


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest,
                      authorization: str | None = Header(default=None)):
    return StreamingResponse(
        _chat_event_stream(request, authorization),
        media_type="application/x-ndjson",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


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

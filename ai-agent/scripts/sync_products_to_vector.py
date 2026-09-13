import sys
from pathlib import Path

import httpx

# 允许直接执行 scripts 下的脚本时导入项目根目录下的 app 包
sys.path.append(str(Path(__file__).resolve().parents[1]))

from app.config import config
from app.rag import rebuild_product_store


def fetch_categories() -> dict[int, str]:
    """读取分类 id -> 分类名映射。"""
    url = config.java_api_base_url.rstrip("/") + "/api/categories"
    response = httpx.get(url, timeout=20)
    response.raise_for_status()
    return {
        int(item["id"]): item.get("name", "")
        for item in response.json().get("data", [])
    }


def fetch_products(category_names: dict[int, str]) -> list[dict]:
    """分页读取 Java 后端商品，并补齐分类名称。"""
    products = []
    current = 1
    page_size = config.product_sync_page_size
    base = config.java_api_base_url.rstrip("/")

    while True:
        response = httpx.get(
            f"{base}/api/products",
            params={"page": current, "size": page_size},
            timeout=20,
        )
        response.raise_for_status()
        page = response.json().get("data", {})
        records = page.get("records", [])
        for record in records:
            category_id = int(record.get("categoryId") or 0)
            record["categoryName"] = category_names.get(category_id, "")
            products.append(record)

        if len(records) < page_size or not page.get("pages") or current >= int(page["pages"]):
            break
        current += 1
    return products


def main():
    """从 Java 商品接口同步商品描述到 Chroma 向量库。"""
    categories = fetch_categories()
    products = fetch_products(categories)
    count = rebuild_product_store(products)
    print(f"product vector store synced, count={count}")


if __name__ == "__main__":
    main()

from pathlib import Path
from typing import Any

from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.config import config

# 商品描述超过该长度才切块；短描述保持“一商品一向量”。
PRODUCT_CHUNK_THRESHOLD = 300
PRODUCT_CHUNK_SIZE = 300
PRODUCT_CHUNK_OVERLAP = 50


def _embeddings():
    """使用 Ollama 的 embedding 模型生成向量。"""
    return OllamaEmbeddings(
        model=config.embedding_model,
        base_url=config.ollama_base_url,
    )


def _vector_store(collection_name: str) -> Chroma:
    """按集合名打开本地 Chroma 向量库。"""
    return Chroma(
        collection_name=collection_name,
        embedding_function=_embeddings(),
        persist_directory=config.vector_dir,
    )


def build_vector_store() -> Chroma:
    """读取 data/kb 下的文本，切块后写入静态知识集合。"""
    kb_path = Path(config.kb_dir)
    splitter = RecursiveCharacterTextSplitter(chunk_size=400, chunk_overlap=50)
    store = _vector_store(config.kb_collection)

    texts = []
    ids = []
    for file in kb_path.glob("*.txt"):
        chunks = splitter.split_text(file.read_text(encoding="utf-8"))
        for index, chunk in enumerate(chunks):
            texts.append(chunk)
            # 稳定 id 让重复执行脚本时覆盖旧数据。
            ids.append(f"{file.name}-{index}")

    if texts:
        store.add_texts(texts=texts, ids=ids)
    return store


def rebuild_product_store(products: list[dict[str, Any]]) -> int:
    """重建商品描述集合；长描述分块，短描述保持单条向量。"""
    store = _vector_store(config.product_collection)
    try:
        store.delete_collection()
    except Exception:
        # 首次运行时集合可能还不存在。
        pass
    store = _vector_store(config.product_collection)

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=PRODUCT_CHUNK_SIZE,
        chunk_overlap=PRODUCT_CHUNK_OVERLAP,
    )
    texts = []
    metadatas = []
    ids = []

    for product in products:
        product_id = product.get("id")
        name = (product.get("name") or "").strip()
        if not name or product_id is None:
            continue
        description = (product.get("description") or "").strip()
        category_name = (product.get("categoryName") or "").strip()
        full_text = f"{name}. {description} Category: {category_name}".strip()

        if len(full_text) <= PRODUCT_CHUNK_THRESHOLD:
            chunks = [full_text]
        else:
            chunks = splitter.split_text(full_text)

        for index, chunk in enumerate(chunks):
            texts.append(chunk)
            metadatas.append(
                {
                    "product_id": int(product_id),
                    "name": name,
                    "category_id": int(product.get("categoryId") or 0),
                    "category_name": category_name,
                    "status": int(product.get("status") or 0),
                    "chunk_index": index,
                    "chunk_count": len(chunks),
                }
            )
            ids.append(f"product-{product_id}-{index}")

    if texts:
        store.add_texts(texts=texts, metadatas=metadatas, ids=ids)
    return len(texts)


def search_structured(query: str, k: int = 3) -> dict[str, list[dict[str, Any]]]:
    """返回结构化的静态知识与商品检索结果，供确定性前置检索使用。"""
    knowledge = []
    for doc in _vector_store(config.kb_collection).similarity_search(query, k=k):
        knowledge.append({"content": doc.page_content})

    products = []
    for doc in _vector_store(config.product_collection).similarity_search(query, k=k):
        metadata = doc.metadata or {}
        products.append(
            {
                "product_id": metadata.get("product_id"),
                "name": metadata.get("name"),
                "category_name": metadata.get("category_name"),
                "content": doc.page_content,
            }
        )
    return {"knowledge": knowledge, "products": products}


def search(query: str, k: int = 3) -> str:
    """兼容旧调用：把结构化检索结果拼成文本。"""
    result = search_structured(query, k=k)
    parts = []
    for item in result["knowledge"]:
        parts.append(f"[knowledge] {item['content']}")
    for item in result["products"]:
        parts.append(
            f"[product product_id={item['product_id']} "
            f"category={item['category_name']}] {item['content']}"
        )
    return "\n\n".join(parts)
